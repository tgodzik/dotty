package tasty4scalac.pickler

import dotty.tools.dotc.core.tasty.TastyBuffer.Addr
import dotty.tools.dotc.core.tasty.{TastyBuffer, TastyFormat, TastyPickler, TreeBuffer}
import tasty4scalac.MutableSymbolAddressMap
import tasty4scalac.ast.{AST, ASTTranslator}


object TreePicklerT {
  val sectionName = "ASTs"
}

class TreePicklerT[A <: AST](pickler: TastyPicklerT[A]) {

  import pickler.translator._
  import pickler.translator

  val buf: TreeBuffer = new TreeBuffer
  pickler.newSection(TreePicklerT.sectionName, buf)

  import pickler.nameBuffer.nameIndex

  // TODO - this should be extracted from dotty
  private val symRefs = MutableSymbolAddressMap.newMutableSymbolMap[A#Symbol, Addr]
  private val forwardSymRefs = MutableSymbolAddressMap.newMutableSymbolMap[A#Symbol, List[Addr]]

  private val pickledTypes = new java.util.IdentityHashMap[A#Type, Addr]

  def pickle(trees: List[A#Tree])(implicit ctx: A#Context): Unit = {
    trees.foreach(tree => if (!translator.isEmpty(tree)) pickleTree(tree))

    def missing = forwardSymRefs.keysIterator.toList // TODO map for .map(_.showLocated)

    assert(forwardSymRefs.isEmpty, s"unresolved symbols: $missing%, % when pickling ${ctx}") // TODO ctx.source
  }

  def compactify(): Unit = {
    buf.compactify()

    def updateMapWithDeltas(mp: MutableSymbolAddressMap[A#Symbol, Addr]) =
      for (key <- mp.keysIterator.toBuffer[Symbol]) mp(key) = buf.adjusted(mp(key))

    updateMapWithDeltas(symRefs)
  }

  private def withLength(op: => Unit) = {
    val lengthAddr = buf.reserveRef(relative = true)
    op
    buf.fillRef(lengthAddr, relative = true)
  }

  private def addrOfSym(sym: A#Symbol): Option[Addr] = {
    symRefs.get(sym)
  }

  private def preRegister(tree: A#Tree)(implicit ctx: A#Context): Unit = tree match {
    case translator.MemberDef(symbol) =>
      if (!symRefs.contains(symbol)) symRefs(symbol) = TastyBuffer.NoAddr
    case _ =>
  }

  private def registerDef(sym: A#Symbol): Unit = {
    symRefs(sym) = buf.currentAddr
    forwardSymRefs.get(sym) match {
      case Some(refs) =>
        refs.foreach(buf.fillRef(_, relative = false))
        forwardSymRefs -= sym
      case None =>
    }
  }

  private def pickleName(name: A#Name): Unit = buf.writeNat(nameIndex(name).index)


  private def pickleForwardSymRef(sym: A#Symbol)(implicit ctx: A#Context) = {
    val ref = buf.reserveRef(relative = false)
    assert(!symbols.isPackage(sym), sym)
    forwardSymRefs(sym) = ref :: forwardSymRefs.getOrElse(sym, Nil)
  }

  private def pickleConstant(constant: A#Constant)(implicit ctx: A#Context): Unit = {
    if (constants.isUnit(constant)) {
      buf.writeByte(TastyFormat.UNITconst)
    } else if (constants.isBoolean(constant)) {
      buf.writeByte(if (constants.getBoolean(constant)) TastyFormat.TRUEconst else TastyFormat.FALSEconst)
    }
    // TODO write the rest of possible constants
  }

  private def pickleType(tpe0: A#Type, richTypes: Boolean = false)(implicit ctx: A#Context): Unit = {
    val tpe = types.stripTypeVar(tpe0) // TODO should we strip type var here or can it happen before
    try {
      if (pickledTypes.containsKey(tpe)) {
        val prev = pickledTypes.get(tpe)
        buf.writeByte(TastyFormat.SHAREDtype)
        buf.writeRef(prev.asInstanceOf[Addr])
      } else {
        pickledTypes.put(tpe, buf.currentAddr)
        pickleNewType(tpe, richTypes)
      }
    } catch {
      case ex: AssertionError =>
        // TODO find a better way of using i from dotty
        println(s"error when pickling type $tpe")
        throw ex
    }
  }

  private def pickleNewType(tpe: A#Type, richTypes: Boolean)(implicit ctx: A#Context): Unit = tpe match {
    // TODO - add omitted types
    case types.ConstantType(value) =>
      pickleConstant(value)
    case types.ThisType(tref, cls) =>
      if (symbols.isPackage(cls) && !symbols.isEffectiveRoot(cls)) {
        buf.writeByte(TastyFormat.TERMREFpkg)
        pickleName(symbols.fullName(cls))
      }
      else {
        buf.writeByte(TastyFormat.THIS)
        pickleType(tref)
      }

    case types.AnnotatedType(parent, annot) =>
      buf.writeByte(TastyFormat.ANNOTATEDtype)
      withLength {
        pickleType(parent, richTypes)
        pickleTree(translator.getTree(annot))
      }
    case types.MethodType(isContextual, isImplicitMethod, isErasedMethod) if richTypes =>
      pickleMethodic(TastyFormat.methodType(isContextual, isImplicitMethod, isErasedMethod), tpe)
    case tpe if types.isParamRef(tpe) =>
      assert(pickleParamRef(types.paramRef(tpe)), s"orphan parameter reference: $tpe")
  }

  private def pickleMethodic(tag: Int, tpe: A#Type)(implicit ctx: A#Context): Unit = {
    // TODO should be Lambda type
    //    buf.writeByte(tag)
    //    withLength {
    //      pickleType(tpe.resultType, richTypes = true)
    //      (tpe.paramNames, tpe.paramInfos).zipped.foreach { (name, tpe) =>
    //        pickleName(name); pickleType(tpe)
    //      }
    //    }
  }

  // TODO - have more specialized types than TP?
  def pickleParamRef(tpe: A#ParamRef)(implicit ctx: A#Context): Boolean = {
    val binder = pickledTypes.get(types.binder(tpe))
    val pickled = binder != null
    if (pickled) {
      buf.writeByte(TastyFormat.PARAMtype)
      withLength {
        buf.writeRef(binder.asInstanceOf[Addr])
        buf.writeNat(types.paramNum(tpe))
      }
    }
    pickled
  }

  private def pickleTreeUnlessEmpty(tree: A#Tree)(implicit ctx: A#Context): Unit =
    if (!translator.isEmpty(tree)) pickleTree(tree)

  private def pickleDef(tag: Int, sym: A#Symbol, tpt: A#Tree, rhs: A#Tree = translator.emptyTree, pickleParams: => Unit = ())(implicit ctx: A#Context): Unit = {
    assert(symRefs(sym) == TastyBuffer.NoAddr, sym)
    registerDef(sym)
    buf.writeByte(tag)
    withLength {
      pickleName(symbols.name(sym))
      pickleParams
      if (translator.shouldPickleTree(tpt)) {
        pickleTree(tpt)
      }
      pickleTreeUnlessEmpty(rhs)
      pickleModifiers(sym)
    }
  }

  private def pickleParam(tree: A#Tree)(implicit ctx: A#Context): Unit = {
    buf.registerTreeAddr(tree)
    tree match {
      case translator.ValDef(symbol, tpt, _) => pickleDef(TastyFormat.PARAM, symbol, tpt)
      case translator.DefDef(symbol, tpt, rhs, _, _) => pickleDef(TastyFormat.PARAM, symbol, tpt, rhs)
      case translator.TypeDef(symbol, rhs) => pickleDef(TastyFormat.TYPEPARAM, symbol, rhs)
    }
  }

  private def pickleParams(trees: List[A#Tree])(implicit ctx: A#Context): Unit = {
    trees.foreach(preRegister)
    trees.foreach(pickleParam)
  }


  private def pickleTree(tree: A#Tree)(implicit ctx: A#Context): Unit = {
    val addr = buf.registerTreeAddr(tree)
    if (addr != buf.currentAddr) {
      buf.writeByte(TastyFormat.SHAREDterm)
      buf.writeRef(addr)
    }
    else
      try tree match {
        // identifier
        case translator.Ident(name, tpe) =>
          if (translator.isTermRef(tpe) && names.isNotWildcardName(name)) {
            // wildcards are pattern bound, need to be preserved as ids.
            pickleType(tpe)
          }
          else {
            buf.writeByte(if (translator.isType(tree)) TastyFormat.IDENTtpt else TastyFormat.IDENT)
            pickleName(name)
            pickleType(tpe)
          }

        case translator.This(qual) =>
          if (translator.isEmpty(qual)) pickleType(translator.getTpe(tree))
          else {
            buf.writeByte(TastyFormat.QUALTHIS)
            pickleTree(translator.withTypeRef(qual, translator.getTpe(tree)))
          }
        case translator.ValDef(symbol, tpt, rhs) =>
          pickleDef(TastyFormat.VALDEF, symbol, tpt, rhs)
        case translator.DefDef(symbol, tpt, rhs, tparams, vparamss) =>
          def pickleAllParams = {
            pickleParams(tparams)
            for (vparams <- vparamss) {
              buf.writeByte(TastyFormat.PARAMS)
              withLength {
                pickleParams(vparams)
              }
            }
          }

          pickleDef(TastyFormat.DEFDEF, symbol, tpt, rhs, pickleAllParams)
        case TypeDef(symbol, rhs) =>
          pickleDef(TastyFormat.TYPEDEF, symbol, rhs)

        case PackageDef(pid, stats) =>
          buf.writeByte(TastyFormat.PACKAGE)
          withLength {
            pickleType(getTpe(pid))
            pickleStats(stats)
          }

      }
      catch {
        case ex: AssertionError =>
          println(s"error when pickling tree $tree")
          throw ex
      }
  }

  private def pickleStats(stats: List[A#Tree])(implicit ctx: A#Context): Unit = {
    stats.foreach(preRegister)
    stats.foreach(stat => if (!translator.isEmpty(stat)) pickleTree(stat))
  }

  private def pickleModifiers(sym: A#Symbol, moduleClass: Boolean = false, mutable: Boolean = false,
                              field: Boolean = false, methodParam: Boolean = false): Unit = {


    // TODO: implement
    // sym.annotations.foreach(pickleAnnotation(sym, _))
  }


}
