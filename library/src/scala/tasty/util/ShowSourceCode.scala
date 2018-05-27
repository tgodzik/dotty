package scala.tasty
package util

class ShowSourceCode[T <: Tasty with Singleton](tasty0: T) extends Show[T](tasty0) {
  import tasty._

  private[this] val out = new StringBuilder

  def showTree(tree: Tree)(implicit ctx: Context): String = {
    out.clear()
    printTree(tree)
    out.result()
  }

  def showTypeOrBoundsTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): String = {
    out.clear()
    printTypeOrBoundsTree(tpt)
    out.result()
  }

  def showTypeOrBounds(tpe: TypeOrBounds)(implicit ctx: Context): String = {
    out.clear()
    printTypeOrBound(tpe)
    out.result()
  }

  def showConstant(const: Constant)(implicit ctx: Context): String = {
    out.clear()
    printConstant(const)
    out.result()
  }

  private def lineBreak: String = "\n" + ("  " * indent)

  private def printTree(tree: Tree)(implicit ctx: Context): Unit = tree match {
    case tree @ PackageClause(Term.Ident(name), stats) =>
      val stats1 = stats.collect {
        case stat@Definition() if !(stat.flags.isObject && stat.flags.isLazy) => stat
        case stat@Import(_, _) => stat
      }

      if (name == "<empty>") {
        printTrees(stats1, lineBreak)
      } else {
        out.append("package " + name + " {")
        indented {
          out.append(lineBreak)
          printTrees(stats1, lineBreak)
        }
        out.append(lineBreak)
        out.append("}")
        out.append(lineBreak)
      }

    case cdef @ ClassDef(name, DefDef(_, targs, argss, _, _), parents, self, stats) =>
      val flags = cdef.flags
      if (flags.isCase) out.append("case ")

      if (cdef.flags.isObject) out.append("object " ).append(name.stripSuffix("$"))
      else out.append("class ").append(name)

      if (!cdef.flags.isObject) {
        printTargsDefs(targs)
        argss.foreach(x => printArgsDefs(x))
      }

      val parents1 = parents.filter {
        case Term.Apply(Term.Select(Term.New(tpt), _, _), _) if Types.isJavaLangObject(tpt.tpe) => false
        case TypeTree.TypeSelect(Term.Select(Term.Ident("_root_"), "scala", _), "Product") => false
        case _ => true
      }
      if (parents1.nonEmpty) {
        out.append(" extends")
        parents1.foreach {
          case parent @ Term.Apply(Term.Select(Term.New(tpt), _, _), args) =>
            out.append(" ")
            printTypeTree(tpt)
            if (args.nonEmpty) {
              out.append("(")
              printTrees(args, ", ")
              out.append(")")
            }

          case parent @ TypeTree() =>
            out.append(" ")
            printTypeTree(parent)
        }
      }

      val stats1 = stats.collect {
        case stat @ Definition() if !stat.flags.isParam && !stat.flags.isParamAccessor => stat
        case stat @ Import(_, _) => stat
        case stat @ Term() => stat
      }
      if (stats1.nonEmpty) {
        out.append(" {")
        indented {
          out.append(lineBreak)
          printTrees(stats1, lineBreak)
        }
        out.append(lineBreak)
        out.append("}")
      }

    case tdef @ TypeDef(name, rhs) =>
      out.append("type ")
      printTargDef(tdef)

    case vdef @ ValDef(name, tpt, rhs) =>
      val flags = vdef.flags
      if (flags.isOverride) out.append("override ")

      if (flags.isLazy) out.append("lazy ")
      if (vdef.flags.isMutable) out.append("var ")
      else out.append("val ")

      out.append(name)
      out.append(": ")
      printTypeTree(tpt)
      rhs match {
        case Some(tree) =>
          out.append(" = ")
          printTree(tree)
        case None =>
      }

    case ddef @ DefDef(name, targs, argss, tpt, rhs) =>
      val flags = ddef.flags
      if (flags.isOverride) out.append("override ")

      out.append("def " + name)
      printTargsDefs(targs)
      argss.foreach(printArgsDefs)
      out.append(": ")
      printTypeTree(tpt)
      rhs match {
        case Some(tree) =>
          out.append(" = ")
          printTree(tree)
        case None =>
      }

    case tree @ Term.Ident(name) =>
      printType(tree.tpe)

    case Term.Select(qual, name, sig) =>
      printTree(qual)
      if (name != "<init>")
        out.append(".").append(name)

    case Term.Literal(const) =>
      printConstant(const)

    case Term.This(id) =>
      out.append("this") // TODO add id

    case Term.New(tpt) =>
      out.append("new ")
      printTypeTree(tpt)

    case Term.NamedArg(name, arg) =>
      ???

    case SpecialOp("throw", expr :: Nil) =>
      out.append("throw ")
      printTree(expr)

    case Term.Apply(fn, args) =>
      printTree(fn)
      out.append("(")
      printTrees(args, ", ")
      out.append(")")

    case Term.TypeApply(fn, args) =>
      printTree(fn)
      out.append("[")
      printTypeTrees(args, ", ")
      out.append("]")

    case Term.Super(qual, tptOpt) =>
      ???

    case Term.Typed(term, tpt) =>
      out.append("(")
      printTree(term)
      out.append(": ")
      printTypeTree(tpt)
      out.append(")")

    case Term.Assign(lhs, rhs) =>
      printTree(lhs)
      out.append(" = ")
      printTree(rhs)

    case Term.Block(stats, expr) =>
      expr match {
        case Term.Lambda(_, _) =>
          // Decompile lambda from { def annon$(...) = ...; closure(annon$, ...)}
          val DefDef(_, _, args :: Nil, _, Some(rhs)) :: Nil = stats
          out.append("(")
          printArgsDefs(args)
          out.append(" => ")
          printTree(rhs)
          out.append(")")

        case Term.Apply(Term.Ident("while$"), _) =>
          val DefDef("while$", _, _, _, Some(Term.If(cond, Term.Block(body :: Nil, _), _))) = stats.head
          out.append("while (")
          printTree(cond)
          out.append(") ")
          printTree(body)

        case Term.Apply(Term.Ident("doWhile$"), _) =>
          val DefDef("doWhile$", _, _, _, Some(Term.Block(List(body), Term.If(cond, _, _)))) = stats.head
          out.append("do ")
          printTree(body)
          out.append(" while (")
          printTree(cond)
          out.append(")")

        case _ =>
          out.append("{")
          indented {
            if (!stats.isEmpty) {
              out.append(lineBreak)
              printTrees(stats, lineBreak)
            }
            out.append(lineBreak)
            printTree(expr)
          }
          out.append(lineBreak)
          out.append("}")
      }

    case Term.Inlined(call, bindings, expansion) =>
      out.append("{ // inlined")
      indented {
        if (!bindings.isEmpty) {
          out.append(lineBreak)
          printTrees(bindings, lineBreak)
        }
        out.append(lineBreak)
        printTree(expansion)
      }
      out.append(lineBreak)
      out.append("}")

    case Term.Lambda(meth, tpt) =>
      // Printed in Term.Block branch

    case Term.If(cond, thenp, elsep) =>
      out.append("if (")
      printTree(cond)
      out.append(") ")
      printTree(thenp)
      out.append(" else ")
      printTree(elsep)

    case Term.Match(selector, cases) =>
      printTree(selector)
      out.append(" match {")
      indented {
        out.append(lineBreak)
        printCases(cases, lineBreak)
      }
      out.append(lineBreak)
      out.append("}")

    case Term.Try(_, _, _) =>
      ???

    case Term.Return(_) =>
      ???

    case Term.Repeated(elems) =>
      printTrees(elems, ", ")

    case Term.SelectOuter(_, _, _) =>
      ???

  }

  object SpecialOp {
    def unapply(arg: Term)(implicit ctx: Context): Option[(String, List[Term])] = arg match {
      case arg @ Term.Apply(fn, args) =>
        fn.tpe match {
          case Type.SymRef(DefDef(op, _, _, _, _), Type.ThisType(Type.SymRef(PackageDef("<special-ops>", _), NoPrefix()))) =>
            Some((op, args))
          case _ => None
        }
      case _ => None
    }
  }

  object Types {
    def isJavaLangObject(tpe: Type)(implicit ctx: Context): Boolean = tpe match {
      case Type.TypeRef("Object", Type.SymRef(PackageDef("lang", _), Type.ThisType(Type.SymRef(PackageDef("java", _), NoPrefix())))) => true
      case _ => false
    }
  }

  private def printTrees(trees: List[Tree], sep: String)(implicit ctx: Context): Unit = {
    def printSeparated(list: List[Tree]): Unit = list match {
      case Nil =>
      case x :: Nil => printTree(x)
      case x :: xs =>
        printTree(x)
        out.append(sep)
        printSeparated(xs)
    }
    printSeparated(trees)
  }

  private def printCases(cases: List[CaseDef], sep: String)(implicit ctx: Context): Unit = {
    def printSeparated(list: List[CaseDef]): Unit = list match {
      case Nil =>
      case x :: Nil => printCase(x)
      case x :: xs =>
        printCase(x)
        out.append(sep)
        printSeparated(xs)
    }
    printSeparated(cases)
  }

  private def printTypeTrees(typesTrees: List[TypeTree], sep: String)(implicit ctx: Context): Unit = {
    def printSeparated(list: List[TypeTree]): Unit = list match {
      case Nil =>
      case x :: Nil => printTypeTree(x)
      case x :: xs =>
        printTypeTree(x)
        out.append(sep)
        printSeparated(xs)
    }
    printSeparated(typesTrees)
  }

  private def printTypesOrBounds(types: List[TypeOrBounds], sep: String)(implicit ctx: Context): Unit = {
    def printSeparated(list: List[TypeOrBounds]): Unit = list match {
      case Nil =>
      case x :: Nil => printTypeOrBound(x)
      case x :: xs =>
        printTypeOrBound(x)
        out.append(sep)
        printSeparated(xs)
    }
    printSeparated(types)
  }

  private def printTargsDefs(targs: List[TypeDef])(implicit ctx: Context): Unit = {
    if (!targs.isEmpty) {
      def printSeparated(list: List[TypeDef]): Unit = list match {
        case Nil =>
        case x :: Nil => printTargDef(x)
        case x :: xs =>
          printTargDef(x)
          out.append(", ")
          printSeparated(xs)
      }
      out.append("[")
      printSeparated(targs)
      out.append("]")
    }
  }

  private def printTargDef(arg: TypeDef)(implicit ctx: Context): Unit = {
    val TypeDef(name, rhs) = arg
    out.append(name)
    rhs match {
      case TypeBoundsTree(lo, hi) =>
        lo match {
          case TypeTree.Synthetic() =>
          case _ =>
            out.append(" >: ")
            printTypeTree(lo)
        }
        hi match {
          case TypeTree.Synthetic() =>
          case _ =>
            out.append(" <: ")
            printTypeTree(hi)
        }
      case tpt @ TypeTree() =>
        out.append(" = ")
        printTypeTree(tpt)
    }
  }

  private def printArgsDefs(args: List[ValDef])(implicit ctx: Context): Unit = {
    out.append("(")
    def printSeparated(list: List[ValDef]): Unit = list match {
      case Nil =>
      case x :: Nil => printArgDef(x)
      case x :: xs =>
        printArgDef(x)
        out.append(", ")
        printSeparated(xs)
    }
    printSeparated(args)
    out.append(")")
  }

  private def printArgDef(arg: ValDef)(implicit ctx: Context): Unit = {
    val ValDef(name, tpt, rhs) = arg
    out.append(name).append(": ")
    printTypeTree(tpt)
  }

  private def printCase(caseDef: CaseDef)(implicit ctx: Context): Unit = {
    val CaseDef(pat, guard, body) = caseDef
    out.append("case ")
    printPattern(pat)
//    out.append(" if ")
    out.append(" =>")
    indented {
      out.append(lineBreak)
      printTree(body)
    }

  }

  private def printPattern(pattern: Pattern)(implicit ctx: Context): Unit = pattern match {
    case Pattern.Value(v) =>
      v match {
        case Term.Ident("_") => out.append("_")
        case _ => printTree(v)
      }

    case Pattern.Bind(name, Pattern.TypeTest(tpt)) =>
      out.append(name).append(": ")
      printTypeTree(tpt)

    case Pattern.Unapply(_, _, _) =>
      ???

    case Pattern.Alternative(_) =>
      ???

    case Pattern.TypeTest(_) =>
      ???

  }

  private def printConstant(const: Constant)(implicit ctx: Context): Unit = const match {
    case Constant.Unit() => out.append("()")
    case Constant.Null() => out.append("null")
    case Constant.Boolean(v) => out.append(v.toString)
    case Constant.Byte(v) => out.append(v)
    case Constant.Short(v) => out.append(v)
    case Constant.Int(v) => out.append(v)
    case Constant.Long(v) => out.append(v).append("L")
    case Constant.Float(v) => out.append(v)
    case Constant.Double(v) => out.append(v)
    case Constant.Char(v) => out.append('\'').append(v.toString).append('\'') // TODO escape char
    case Constant.String(v) => out.append('"').append(v.toString).append('"') // TODO escape string
  }

  private def printTypeOrBoundsTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): Unit = tpt match {
    case TypeBoundsTree(lo, hi) => ???
    case tpt @ Type() => printType(tpt)
  }

  private def printTypeTree(tree: TypeTree)(implicit ctx: Context): Unit = tree match {
    case TypeTree.Synthetic() =>
      printType(tree.tpe)

    case TypeTree.TypeIdent(name) =>
      printType(tree.tpe)

    case TypeTree.TypeSelect(qual, name) =>
      printTree(qual)
      out.append(".").append(name)

    case TypeTree.Singleton(_) =>
      ???

    case TypeTree.Refined(tpt, refinements) =>
      printTypeTree(tpt)
      out.append(" {")
      indented {
        out.append(lineBreak)
        printTrees(refinements, "; ")
      }
      out.append(lineBreak)
      out.append("}")

    case TypeTree.Applied(tpt, args) =>
      printTypeTree(tpt)
      out.append("[")
      printTypeTrees(args, ", ")
      out.append("]")

    case TypeTree.Annotated(tpt, annots) =>
      ???

    case TypeTree.And(left, right) =>
      printTypeTree(left)
      out.append(" & ")
      printTypeTree(right)

    case TypeTree.Or(left, right) =>
      printTypeTree(left)
      out.append(" | ")
      printTypeTree(right)

    case TypeTree.ByName(_) =>
      ???

  }

  private def printTypeOrBound(tpe: TypeOrBounds)(implicit ctx: Context): Unit = tpe match {
    case tpe @ TypeBounds(lo, hi) => ???
    case tpe @ Type() => printType(tpe)
  }

  private def printType(tpe: Type)(implicit ctx: Context): Unit = tpe match {
    case Type.ConstantType(const) =>
      printConstant(const)

    case Type.SymRef(sym, prefix) =>
      prefix match {
        case Type.ThisType(Type.SymRef(PackageDef(pack, _), NoPrefix())) if pack == "<root>" || pack == "<empty>" =>
        case prefix @ Type.SymRef(ClassDef(_, _, _,_, _), _) =>
          printType(prefix)
          out.append("#")
        case prefix @ Type() =>
          printType(prefix)
          out.append(".")
        case prefix @ NoPrefix() =>
      }
      printDefinitionName(sym)

    case Type.TermRef(name, prefix) =>
      prefix match {
        case prefix@Type() =>
          printType(prefix)
          if (name != "package")
            out.append(".").append(name)
        case NoPrefix() =>
          out.append(name)
      }

    case Type.TypeRef(name, prefix) =>
      prefix match {
        case prefix @ Type() =>
          printType(prefix)
          out.append(".")
        case NoPrefix() =>
      }
      out.append(name.stripSuffix("$"))

    case Type.SuperType(_, _) =>
      ???

    case Type.Refinement(parent, name, info) =>
      printType(parent)
      // TODO add refinements

    case Type.AppliedType(tp, args) =>
      printType(tp)
      out.append("[")
      printTypesOrBounds(args, ", ")
      out.append("]")

    case Type.AnnotatedType(tp, annot) =>
      printType(tp)

    case Type.AndType(left, right) =>
      printType(left)
      out.append(" & ")
      printType(right)

    case Type.OrType(left, right) =>
      printType(left)
      out.append(" | ")
      printType(right)

    case Type.ByNameType(tp) =>
      ???

    case Type.ParamRef(_, _) =>
      ???

    case Type.ThisType(tp) =>
      printType(tp)

    case Type.RecursiveThis(rec) =>
      ???

    case Type.RecursiveType(tp) =>
      ???

    case Type.MethodType(_, _, _) =>
      ???

    case Type.PolyType(_, _, _) =>
      ???

    case Type.TypeLambda(_, _, _) =>
      ???

  }

  def printDefinitionName(sym: Definition)(implicit ctx: Context): Unit = sym match {
    case ValDef(name, _, _) => out.append(name)
    case DefDef(name, _, _, _, _) => out.append(name)
    case ClassDef(name, _, _, _, _) => out.append(name.stripSuffix("$"))
    case TypeDef(name, _) => out.append(name)
    case PackageDef(name, _) => out.append(name)
  }

  private[this] var indent: Int = 0
  def indented(printIndented: => Unit): Unit = {
    indent += 1
    printIndented
    indent -= 1
  }

}
