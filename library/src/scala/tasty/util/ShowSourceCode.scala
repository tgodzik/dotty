package scala.tasty
package util

class ShowSourceCode[T <: Tasty with Singleton](tasty0: T) extends Show[T](tasty0) {
  import tasty._

  private[this] var indent: Int = 0
  def indented(printIndented: => Unit): Unit = {
    indent += 1
    printIndented
    indent -= 1
  }

  def showTree(tree: Tree)(implicit ctx: Context): String =
    (new Buffer).printTree(tree).result()

  def showTypeOrBoundsTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): String =
    (new Buffer).printTypeOrBoundsTree(tpt).result()

  def showTypeOrBounds(tpe: TypeOrBounds)(implicit ctx: Context): String =
    (new Buffer).printTypeOrBound(tpe).result()

  def showConstant(const: Constant)(implicit ctx: Context): String =
    (new Buffer).printConstant(const).result()

  private class Buffer(implicit ctx: Context) {
    self =>

    private val sb: StringBuilder = new StringBuilder

    def result(): String = sb.result()

    private def lineBreak: String = "\n" + ("  " * indent)

    def printTree(tree: Tree): Buffer = {
      tree match {
        case tree@PackageClause(Term.Ident(name), stats) =>
          val stats1 = stats.collect {
            case stat@Definition() if !(stat.flags.isObject && stat.flags.isLazy) => stat
            case stat@Import(_, _) => stat
          }

          if (name == "<empty>") {
            printTrees(stats1, lineBreak)
          } else {
            sb.append("package " + name + " {")
            indented {
              sb.append(lineBreak)
              printTrees(stats1, lineBreak)
            }
            sb.append(lineBreak)
            sb.append("}")
            sb.append(lineBreak)
          }

        case cdef@ClassDef(name, DefDef(_, targs, argss, _, _), parents, self, stats) =>
          val flags = cdef.flags
          if (flags.isCase) sb.append("case ")

          if (cdef.flags.isObject) sb.append("object ").append(name.stripSuffix("$"))
          else sb.append("class ").append(name)

          if (!cdef.flags.isObject) {
            printTargsDefs(targs)
            val it = argss.iterator
            while (it.hasNext)
              printArgsDefs(it.next())
          }

          val parents1 = parents.filter {
            case Term.Apply(Term.Select(Term.New(tpt), _, _), _) if Types.isJavaLangObject(tpt.tpe) => false
            case TypeTree.TypeSelect(Term.Select(Term.Ident("_root_"), "scala", _), "Product") => false
            case _ => true
          }
          if (parents1.nonEmpty) {
            sb.append(" extends")
            parents1.foreach {
              case parent@Term.Apply(Term.Select(Term.New(tpt), _, _), args) =>
                sb.append(" ")
                printTypeTree(tpt)
                if (args.nonEmpty) {
                  sb.append("(")
                  printTrees(args, ", ")
                  sb.append(")")
                }

              case parent@TypeTree() =>
                sb.append(" ")
                printTypeTree(parent)
            }
          }

          val stats1 = stats.collect {
            case stat@Definition() if !stat.flags.isParam && !stat.flags.isParamAccessor => stat
            case stat@Import(_, _) => stat
            case stat@Term() => stat
          }
          if (stats1.nonEmpty) {
            sb.append(" {")
            indented {
              sb.append(lineBreak)
              printTrees(stats1, lineBreak)
            }
            sb.append(lineBreak)
            sb.append("}")
          }

        case tdef@TypeDef(name, rhs) =>
          sb.append("type ")
          printTargDef(tdef)

        case vdef@ValDef(name, tpt, rhs) =>
          val flags = vdef.flags
          if (flags.isOverride) sb.append("override ")

          if (flags.isLazy) sb.append("lazy ")
          if (vdef.flags.isMutable) sb.append("var ")
          else sb.append("val ")

          sb.append(name)
          sb.append(": ")
          printTypeTree(tpt)
          rhs match {
            case Some(tree) =>
              sb.append(" = ")
              printTree(tree)
            case None =>
          }

        case ddef@DefDef(name, targs, argss, tpt, rhs) =>
          val flags = ddef.flags
          if (flags.isOverride) sb.append("override ")

          sb.append("def " + name)
          printTargsDefs(targs)
          val it = argss.iterator
          while (it.hasNext)
            printArgsDefs(it.next())
          sb.append(": ")
          printTypeTree(tpt)
          rhs match {
            case Some(tree) =>
              sb.append(" = ")
              printTree(tree)
            case None =>
          }

        case tree@Term.Ident(name) =>
          printType(tree.tpe)

        case Term.Select(qual, name, sig) =>
          printTree(qual)
          if (name != "<init>")
            sb.append(".").append(name)

        case Term.Literal(const) =>
          printConstant(const)

        case Term.This(id) =>
          sb.append("this") // TODO add id

        case Term.New(tpt) =>
          sb.append("new ")
          printTypeTree(tpt)

        case Term.NamedArg(name, arg) =>
          ???

        case SpecialOp("throw", expr :: Nil) =>
          sb.append("throw ")
          printTree(expr)

        case Term.Apply(fn, args) =>
          printTree(fn)
          sb.append("(")
          printTrees(args, ", ")
          sb.append(")")

        case Term.TypeApply(fn, args) =>
          printTree(fn)
          sb.append("[")
          printTypeTrees(args, ", ")
          sb.append("]")

        case Term.Super(qual, tptOpt) =>
          ???

        case Term.Typed(term, tpt) =>
          sb.append("(")
          printTree(term)
          sb.append(": ")
          printTypeTree(tpt)
          sb.append(")")

        case Term.Assign(lhs, rhs) =>
          printTree(lhs)
          sb.append(" = ")
          printTree(rhs)

        case Term.Block(stats, expr) =>
          expr match {
            case Term.Lambda(_, _) =>
              // Decompile lambda from { def annon$(...) = ...; closure(annon$, ...)}
              val DefDef(_, _, args :: Nil, _, Some(rhs)) :: Nil = stats
              sb.append("(")
              printArgsDefs(args)
              sb.append(" => ")
              printTree(rhs)
              sb.append(")")

            case Term.Apply(Term.Ident("while$"), _) =>
              val DefDef("while$", _, _, _, Some(Term.If(cond, Term.Block(body :: Nil, _), _))) = stats.head
              sb.append("while (")
              printTree(cond)
              sb.append(") ")
              printTree(body)

            case Term.Apply(Term.Ident("doWhile$"), _) =>
              val DefDef("doWhile$", _, _, _, Some(Term.Block(List(body), Term.If(cond, _, _)))) = stats.head
              sb.append("do ")
              printTree(body)
              sb.append(" while (")
              printTree(cond)
              sb.append(")")

            case _ =>
              sb.append("{")
              indented {
                if (!stats.isEmpty) {
                  sb.append(lineBreak)
                  printTrees(stats, lineBreak)
                }
                sb.append(lineBreak)
                printTree(expr)
              }
              sb.append(lineBreak)
              sb.append("}")
          }

        case Term.Inlined(call, bindings, expansion) =>
          sb.append("{ // inlined")
          indented {
            if (!bindings.isEmpty) {
              sb.append(lineBreak)
              printTrees(bindings, lineBreak)
            }
            sb.append(lineBreak)
            printTree(expansion)
          }
          sb.append(lineBreak)
          sb.append("}")

        case Term.Lambda(meth, tpt) =>
        // Printed in Term.Block branch

        case Term.If(cond, thenp, elsep) =>
          sb.append("if (")
          printTree(cond)
          sb.append(") ")
          printTree(thenp)
          sb.append(" else ")
          printTree(elsep)

        case Term.Match(selector, cases) =>
          printTree(selector)
          sb.append(" match {")
          indented {
            sb.append(lineBreak)
            printCases(cases, lineBreak)
          }
          sb.append(lineBreak)
          sb.append("}")

        case Term.Try(_, _, _) =>
          ???

        case Term.Return(_) =>
          ???

        case Term.Repeated(elems) =>
          printTrees(elems, ", ")

        case Term.SelectOuter(_, _, _) =>
          ???

      }
      this
    }



    def printTrees(trees: List[Tree], sep: String): Unit = {
      def printSeparated(list: List[Tree]): Unit = list match {
        case Nil =>
        case x :: Nil => printTree(x)
        case x :: xs =>
          printTree(x)
          sb.append(sep)
          printSeparated(xs)
      }

      printSeparated(trees)
    }

    def printCases(cases: List[CaseDef], sep: String): Unit = {
      def printSeparated(list: List[CaseDef]): Unit = list match {
        case Nil =>
        case x :: Nil => printCase(x)
        case x :: xs =>
          printCase(x)
          sb.append(sep)
          printSeparated(xs)
      }

      printSeparated(cases)
    }

    def printTypeTrees(typesTrees: List[TypeTree], sep: String): Unit = {
      def printSeparated(list: List[TypeTree]): Unit = list match {
        case Nil =>
        case x :: Nil => printTypeTree(x)
        case x :: xs =>
          printTypeTree(x)
          sb.append(sep)
          printSeparated(xs)
      }

      printSeparated(typesTrees)
    }

    def printTypesOrBounds(types: List[TypeOrBounds], sep: String): Unit = {
      def printSeparated(list: List[TypeOrBounds]): Unit = list match {
        case Nil =>
        case x :: Nil => printTypeOrBound(x)
        case x :: xs =>
          printTypeOrBound(x)
          sb.append(sep)
          printSeparated(xs)
      }

      printSeparated(types)
    }

    def printTargsDefs(targs: List[TypeDef]): Unit = {
      if (!targs.isEmpty) {
        def printSeparated(list: List[TypeDef]): Unit = list match {
          case Nil =>
          case x :: Nil => printTargDef(x)
          case x :: xs =>
            printTargDef(x)
            sb.append(", ")
            printSeparated(xs)
        }

        sb.append("[")
        printSeparated(targs)
        sb.append("]")
      }
    }

    def printTargDef(arg: TypeDef): Unit = {
      val TypeDef(name, rhs) = arg
      sb.append(name)
      rhs match {
        case TypeBoundsTree(lo, hi) =>
          lo match {
            case TypeTree.Synthetic() =>
            case _ =>
              sb.append(" >: ")
              printTypeTree(lo)
          }
          hi match {
            case TypeTree.Synthetic() =>
            case _ =>
              sb.append(" <: ")
              printTypeTree(hi)
          }
        case tpt@TypeTree() =>
          sb.append(" = ")
          printTypeTree(tpt)
      }
    }

    def printArgsDefs(args: List[ValDef]): Unit = {
      sb.append("(")

      def printSeparated(list: List[ValDef]): Unit = list match {
        case Nil =>
        case x :: Nil => printArgDef(x)
        case x :: xs =>
          printArgDef(x)
          sb.append(", ")
          printSeparated(xs)
      }

      printSeparated(args)
      sb.append(")")
    }

    def printArgDef(arg: ValDef): Unit = {
      val ValDef(name, tpt, rhs) = arg
      sb.append(name).append(": ")
      printTypeTree(tpt)
    }

    def printCase(caseDef: CaseDef): Unit = {
      val CaseDef(pat, guard, body) = caseDef
      sb.append("case ")
      printPattern(pat)
      //    out.append(" if ")
      sb.append(" =>")
      indented {
        sb.append(lineBreak)
        printTree(body)
      }

    }

    def printPattern(pattern: Pattern): Unit = pattern match {
      case Pattern.Value(v) =>
        v match {
          case Term.Ident("_") => sb.append("_")
          case _ => printTree(v)
        }

      case Pattern.Bind(name, Pattern.TypeTest(tpt)) =>
        sb.append(name).append(": ")
        printTypeTree(tpt)

      case Pattern.Unapply(_, _, _) =>
        ???

      case Pattern.Alternative(_) =>
        ???

      case Pattern.TypeTest(_) =>
        ???

    }

    def printConstant(const: Constant): Buffer = {
      const match {
        case Constant.Unit() => sb.append("()")
        case Constant.Null() => sb.append("null")
        case Constant.Boolean(v) => sb.append(v.toString)
        case Constant.Byte(v) => sb.append(v)
        case Constant.Short(v) => sb.append(v)
        case Constant.Int(v) => sb.append(v)
        case Constant.Long(v) => sb.append(v).append("L")
        case Constant.Float(v) => sb.append(v)
        case Constant.Double(v) => sb.append(v)
        case Constant.Char(v) => sb.append('\'').append(v.toString).append('\'') // TODO escape char
        case Constant.String(v) => sb.append('"').append(v.toString).append('"') // TODO escape string
      }
      this
    }

    def printTypeOrBoundsTree(tpt: TypeOrBoundsTree): Buffer = {
      tpt match {
        case TypeBoundsTree(lo, hi) => ???
        case tpt@Type() => printType(tpt)
      }
      this
    }

    def printTypeTree(tree: TypeTree): Buffer = {
      tree match {
        case TypeTree.Synthetic() =>
          printType(tree.tpe)

        case TypeTree.TypeIdent(name) =>
          printType(tree.tpe)

        case TypeTree.TypeSelect(qual, name) =>
          printTree(qual)
          sb.append(".").append(name)

        case TypeTree.Singleton(_) =>
          ???

        case TypeTree.Refined(tpt, refinements) =>
          printTypeTree(tpt)
          sb.append(" {")
          indented {
            sb.append(lineBreak)
            printTrees(refinements, "; ")
          }
          sb.append(lineBreak)
          sb.append("}")

        case TypeTree.Applied(tpt, args) =>
          printTypeTree(tpt)
          sb.append("[")
          printTypeTrees(args, ", ")
          sb.append("]")

        case TypeTree.Annotated(tpt, annots) =>
          ???

        case TypeTree.And(left, right) =>
          printTypeTree(left)
          sb.append(" & ")
          printTypeTree(right)

        case TypeTree.Or(left, right) =>
          printTypeTree(left)
          sb.append(" | ")
          printTypeTree(right)

        case TypeTree.ByName(_) =>
          ???

      }
      this
    }

    def printTypeOrBound(tpe: TypeOrBounds): Buffer = tpe match {
      case tpe@TypeBounds(lo, hi) => ???
      case tpe@Type() => printType(tpe)
    }

    def printType(tpe: Type): Buffer = {
      tpe match {
        case Type.ConstantType(const) =>
          printConstant(const)

        case Type.SymRef(sym, prefix) =>
          prefix match {
            case Type.ThisType(Type.SymRef(PackageDef(pack, _), NoPrefix())) if pack == "<root>" || pack == "<empty>" =>
            case prefix@Type.SymRef(ClassDef(_, _, _, _, _), _) =>
              printType(prefix)
              sb.append("#")
            case prefix@Type() =>
              printType(prefix)
              sb.append(".")
            case prefix@NoPrefix() =>
          }
          printDefinitionName(sym)

        case Type.TermRef(name, prefix) =>
          prefix match {
            case prefix@Type() =>
              printType(prefix)
              if (name != "package")
                sb.append(".").append(name)
            case NoPrefix() =>
              sb.append(name)
          }

        case Type.TypeRef(name, prefix) =>
          prefix match {
            case prefix@Type() =>
              printType(prefix)
              sb.append(".")
            case NoPrefix() =>
          }
          sb.append(name.stripSuffix("$"))

        case Type.SuperType(_, _) =>
          ???

        case Type.Refinement(parent, name, info) =>
          printType(parent)
        // TODO add refinements

        case Type.AppliedType(tp, args) =>
          printType(tp)
          sb.append("[")
          printTypesOrBounds(args, ", ")
          sb.append("]")

        case Type.AnnotatedType(tp, annot) =>
          printType(tp)

        case Type.AndType(left, right) =>
          printType(left)
          sb.append(" & ")
          printType(right)

        case Type.OrType(left, right) =>
          printType(left)
          sb.append(" | ")
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
      this
    }

    def printDefinitionName(sym: Definition): Unit = sym match {
      case ValDef(name, _, _) => sb.append(name)
      case DefDef(name, _, _, _, _) => sb.append(name)
      case ClassDef(name, _, _, _, _) => sb.append(name.stripSuffix("$"))
      case TypeDef(name, _) => sb.append(name)
      case PackageDef(name, _) => sb.append(name)
    }
  }


  private object SpecialOp {
    def unapply(arg: Term)(implicit ctx: Context): Option[(String, List[Term])] = arg match {
      case arg@Term.Apply(fn, args) =>
        fn.tpe match {
          case Type.SymRef(DefDef(op, _, _, _, _), Type.ThisType(Type.SymRef(PackageDef("<special-ops>", _), NoPrefix()))) =>
            Some((op, args))
          case _ => None
        }
      case _ => None
    }
  }

  private object Types {
    def isJavaLangObject(tpe: Type)(implicit ctx: Context): Boolean = tpe match {
      case Type.TypeRef("Object", Type.SymRef(PackageDef("lang", _), Type.ThisType(Type.SymRef(PackageDef("java", _), NoPrefix())))) => true
      case _ => false
    }
  }
}
