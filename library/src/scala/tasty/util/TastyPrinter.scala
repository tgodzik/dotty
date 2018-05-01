package scala.tasty.util

import scala.tasty.Tasty

object TastyPrinter {

  def stringOfTree(tasty: Tasty)(tree: tasty.TopLevelStatement)(implicit ctx: tasty.Context): String = {
    implicit val buff: StringBuilder = new StringBuilder
    visit(tasty)(tree)
    buff.toString()
  }

  def stringOfType(tasty: Tasty)(tpe: tasty.MaybeType)(implicit ctx: tasty.Context): String = {
    implicit val buff: StringBuilder = new StringBuilder
    visit(tasty)(tpe)
    buff.toString()
  }

  def stringOfModifier(tasty: Tasty)(mod: tasty.Modifier)(implicit ctx: tasty.Context): String = {
    implicit val buff: StringBuilder = new StringBuilder
    visit(tasty)(mod)
    buff.toString()
  }

  def stringOfConstant(tasty: Tasty)(mod: tasty.Constant)(implicit ctx: tasty.Context): String = {
    implicit val buff: StringBuilder = new StringBuilder
    visit(tasty)(mod)
    buff.toString()
  }

  private def visit(tasty: Tasty)(x: Any)(implicit buff: StringBuilder, ctx: tasty.Context): Unit = {
    import tasty._
    x match {
      case Ident(name) =>
        buff append "Ident(" append name append ")"
      case Select(qualifier, name) =>
        buff append "Select("
        visit(tasty)(qualifier)
        buff append ", " append name append ")"
      case This(qual) =>
        buff append "This(" append qual append ")"
      case Super(qual, mix) =>
        buff append "TypeApply("
        visit(tasty)(qual)
        buff append ", " append mix append ")"
      case Apply(fun, args) =>
        buff append "Apply("
        visit(tasty)(fun)
        buff append ", "
        visit(tasty)(args)
        buff append ")"
      case TypeApply(fun, args) =>
        buff append "TypeApply("
        visit(tasty)(fun)
        buff append ", "
        visit(tasty)(args)
        buff append ")"
      case Literal(const) =>
        buff append "Literal("
        visit(tasty)(const)
        buff append ")"
      case New(tpt) =>
        buff append "New("
        visit(tasty)(tpt)
        buff append ")"
      case Typed(expr, tpt) =>
        buff append "Typed("
        visit(tasty)(expr)
        buff append ", "
        visit(tasty)(tpt)
        buff append ")"
      case NamedArg(name, arg) =>
        buff append "NamedArg(" append name append ", "
        visit(tasty)(arg)
        buff append ")"
      case Assign(lhs, rhs) =>
        buff append "Assign("
        visit(tasty)(lhs)
        buff append ", "
        visit(tasty)(rhs)
        buff append ")"
      case Block(stats, expr) =>
        buff append "Block("
        visit(tasty)(stats)
        buff append ", "
        visit(tasty)(expr)
        buff append ")"
      case If(cond, thenp, elsep) =>
        buff append "If("
        visit(tasty)(cond)
        buff append ", "
        visit(tasty)(thenp)
        buff append ", "
        visit(tasty)(elsep)
        buff append ")"
      case Lambda(meth, tpt) =>
        buff append "Lambda("
        visit(tasty)(meth)
        buff append ", "
        visit(tasty)(tpt)
        buff append ")"
      case Match(selector, cases) =>
        buff append "Match("
        visit(tasty)(selector)
        buff append ", "
        visit(tasty)(cases)
        buff append ")"
      case CaseDef(pat, guard, body) =>
        buff append "CaseDef("
        visit(tasty)(pat)
        buff append ", "
        visit(tasty)(guard)
        buff append ", "
        visit(tasty)(body)
        buff append ")"
      case Return(expr) =>
        buff append "Return("
        visit(tasty)(expr)
        buff append ")"
      case Try(block, handlers, finalizer) =>
        buff append "Try("
        visit(tasty)(block)
        buff append ", "
        visit(tasty)(handlers)
        buff append ", "
        visit(tasty)(finalizer)
        buff append ")"
      case Repeated(elems) =>
        buff append "Repeated("
        visit(tasty)(elems)
        buff append ")"
      case Inlined(call, bindings, expansion) =>
        buff append "Inlined("
        visit(tasty)(call)
        buff append ", "
        visit(tasty)(bindings)
        buff append ", "
        visit(tasty)(expansion)
        buff append ")"
      case Synthetic() =>
        buff append "Synthetic()"
      case TypeIdent(name) =>
        buff append "Ident(" // FIXME print as TypeIdent
        visit(tasty)(name)
        buff append ")"
      case TypeSelect(qualifier, name) =>
        buff append "TypeSelect("
        visit(tasty)(qualifier)
        buff append ", "
        visit(tasty)(name)
        buff append ")"
      case Singleton(ref) =>
        buff append "Singleton("
        visit(tasty)(ref)
        buff append ")"
      case And(left, right) =>
        buff append "And("
        visit(tasty)(left)
        buff append ", "
        visit(tasty)(right)
        buff append ")"
      case Or(left, right) =>
        buff append "Or("
        visit(tasty)(left)
        buff append ", "
        visit(tasty)(right)
        buff append ")"
      case Refined(tpt, refinements) =>
        buff append "Refined("
        visit(tasty)(tpt)
        buff append ", "
        visit(tasty)(refinements)
        buff append ")"
      case Applied(tpt, args) =>
        buff append "Applied("
        visit(tasty)(tpt)
        buff append ", "
        visit(tasty)(args)
        buff append ")"
      case ByName(result) =>
        buff append "ByName("
        visit(tasty)(result)
        buff append ")"
      case TypeBoundsTree(lo, hi) =>
        buff append "TypeBoundsTree("
        visit(tasty)(lo)
        buff append ", "
        visit(tasty)(hi)
        buff append ")"
      case Annotated(arg, annot) =>
        buff append "Annotated("
        visit(tasty)(arg)
        buff append ", "
        visit(tasty)(annot)
        buff append ")"
      case Value(v) =>
        buff append "Value("
        visit(tasty)(v)
        buff append ")"
      case Bind(name, body) =>
        buff append "Bind(" append name append ", "
        visit(tasty)(body)
        buff append ")"
      case Unapply(fun, implicits, patterns) =>
        buff append "Unapply("
        visit(tasty)(fun)
        buff append ", "
        visit(tasty)(implicits)
        buff append ", "
        visit(tasty)(patterns)
        buff append ")"
      case Alternative(patterns) =>
        buff append "Alternative("
        visit(tasty)(patterns)
        buff append ")"
      case TypeTest(tpt) =>
        buff append "TypeTest("
        visit(tasty)(tpt)
        buff append ")"
      case ValDef(name, tpt, rhs) =>
        buff append "ValDef(" append name append ", "
        visit(tasty)(tpt)
        buff append ", "
        visit(tasty)(rhs)
        buff append ")"
      case DefDef(name, typeParams, paramss, returnTpt, rhs) =>
        buff append "DefDef(" append name append ", "
        visit(tasty)(typeParams)
        buff append ", "
        visit(tasty)(paramss)
        buff append ", "
        visit(tasty)(returnTpt)
        buff append ", "
        visit(tasty)(rhs)
        buff append ")"
      case TypeDef(name, rhs) =>
        buff append "TypeDef(" append name append ", "
        visit(tasty)(rhs)
        buff append ")"
      case ClassDef(name, constr, parents, self, body) =>
        buff append "ClassDef(" append name append ", "
        visit(tasty)(constr)
        buff append ", "
        visit(tasty)(parents)
        buff append ", "
        visit(tasty)(self)
        buff append ", "
        visit(tasty)(body)
        buff append ")"
      //    case PackageDef(name, members) =>
      //      buff append "PackageDef("
      //      buff append name
      //      buff append ", "
      //      visit(tasty)(members)
      //      buff append ")"
      case Import(expr, selectors) =>
        buff append "Import("
        visit(tasty)(expr)
        buff append ", "
        visit(tasty)(selectors)
        buff append ")"
      case PackageClause(pid, stats) =>
        buff append "PackageClause("
        visit(tasty)(pid)
        buff append ", "
        visit(tasty)(stats)
        buff append ")"

      case UnitConstant() => buff append "Unit()"
      case NullConstant() => buff append "Null()"
      case BooleanConstant(value) => buff append "Boolean(" append value append ")"
      case ByteConstant(value) => buff append "Byte(" append value append ")"
      case ShortConstant(value) => buff append "Short(" append value append ")"
      case CharConstant(value) => buff append "Char('" append value append "')"
      case IntConstant(value) => buff append "Int(" append value append ")"
      case LongConstant(value) => buff append "Long(" append value append ")"
      case FloatConstant(value) => buff append "Float(" append value append ")"
      case DoubleConstant(value) => buff append "Double(" append value append ")"
      case StringConstant(value) => buff append "String(\"" append value append "\")" // TODO escape string characters?

      case ConstantType(value) =>
        buff append "ConstantType("
        visit(tasty)(value)
        buff append ")"
      case SymRef(sym, qual) =>
        def visitName(sym: Definition): Unit = sym match {
    case ValDef(name, _, _) => buff append name
    case DefDef(name, _, _, _, _) => buff append name
    case TypeDef(name, _) => buff append name
    case ClassDef(name, _, _, _, _) => buff append name
      //      case PackageDef(name, _) => name
    case _ => buff append "NoDefinition"
    }
    buff append "SymRef(<"
    visitName(sym)
    buff append ">, "
    visit(tasty)(qual)
    buff append ")"
      case NameRef(name, qual) =>
        buff append "NameRef("
        visit(tasty)(name)
        buff append ", "
        visit(tasty)(qual)
        buff append ")"
      case Refinement(parent, name, info) =>
        buff append "Refinement("
        visit(tasty)(parent)
        buff append ", "
        visit(tasty)(name)
        buff append ", "
        visit(tasty)(info)
        buff append ")"
      case AppliedType(tycon, args) =>
        buff append "AppliedType("
        visit(tasty)(tycon)
        buff append ", "
        visit(tasty)(args)
        buff append ")"
      case AnnotatedType(underlying, annot) =>
        buff append "AnnotatedType("
        visit(tasty)(underlying)
        buff append ", "
        visit(tasty)(annot)
        buff append ")"
      case AndType(left, right) =>
        buff append "AndType("
        visit(tasty)(left)
        buff append ", "
        visit(tasty)(right)
        buff append ")"
      case OrType(left, right) =>
        buff append "OrType("
        visit(tasty)(left)
        buff append ", "
        visit(tasty)(right)
        buff append ")"
      case ByNameType(underlying) =>
        buff append "ByNameType("
        visit(tasty)(underlying)
        buff append ")"
      case ParamRef(binder, idx) =>
        buff append "ParamRef("
        visit(tasty)(binder)
        buff append ", " append idx append ")"
      case ThisType(tp) =>
        buff append "ThisType("
        visit(tasty)(tp)
        buff append ")"
      case RecursiveThis(binder) =>
        buff append "RecursiveThis("
        visit(tasty)(binder)
        buff append ")"
      case MethodType(argNames, argTypes, resType) =>
        buff append "MethodType("
        visit(tasty)(argNames)
        buff append ", "
        visit(tasty)(argTypes)
        buff append ", "
        visit(tasty)(resType)
        buff append ")"
      case PolyType(argNames, argBounds, resType) =>
        buff append "PolyType("
        visit(tasty)(argNames)
        buff append ", "
        visit(tasty)(argBounds)
        buff append ", "
        visit(tasty)(resType)
        buff append ")"
      case TypeLambda(argNames, argBounds, resType) =>
        buff append "TypeLambda("
        visit(tasty)(argNames)
        buff append ", "
        visit(tasty)(argBounds)
        buff append ", "
        visit(tasty)(resType)
        buff append ")"
      case TypeBounds(lo, hi) =>
        buff append "TypeBounds("
        visit(tasty)(lo)
        buff append ", "
        visit(tasty)(hi)
        buff append ")"
      case NoPrefix() =>
        buff append "NoPrefix"

      case SimpleSelector(id) => buff append "SimpleSelector(" append id append ")"
      case OmitSelector(id) => buff append "OmitSelector(" append id append ")"
      case RenameSelector(id1, id2) =>
        buff append "RenameSelector(" append id1 append ", " append id2 append ")"

      case Flags(flags) =>
        buff append "Flags(" append flags append ")"
      case QualifiedPrivate(tp) =>
        buff append "QualifiedPrivate("
        visit(tasty)(tp)
        buff append ")"
      case QualifiedProtected(tp) =>
        buff append "QualifiedProtected("
        visit(tasty)(tp)
        buff append ")"
      case Annotation(tree) =>
        buff append "Annotation("
        visit(tasty)(tree)
        buff append ")"

      case x0 :: xs =>
        buff append "List("
        visit(tasty)(x0)
        def visitNext(xs: List[_]): Unit = xs match {
          case y :: ys =>
            buff append ", "
            visit(tasty)(y)
            visitNext(ys)
          case Nil =>
        }
        visitNext(xs)
        buff append ")"
      case Nil => buff append "Nil"
      case Some(y) =>
        buff append "Some("
        visit(tasty)(y)
        buff append ")"
      case None => buff append "None"
      case str: String => buff append "(\"" append str append "\")" // TODO escape string characters?
    }
  }

}
