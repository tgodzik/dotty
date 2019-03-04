package dotty.tools.dotc
package tastyreflect

import dotty.tools.dotc.ast.{Trees, tpd, untpd}
import dotty.tools.dotc.ast.tpd.TreeOps
import dotty.tools.dotc.core.{Constants, NameKinds, Types}
import dotty.tools.dotc.core.Flags._
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.quoted.PickledQuotes
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.tastyreflect.FromSymbol.{definitionFromSym, packageDefFromSym}

import scala.tasty.reflect.Kernel

class KernelImpl(val rootContext: core.Contexts.Context, val rootPosition: util.SourcePosition) extends Kernel {

  private implicit def ctx: core.Contexts.Context = rootContext

  def settings: Settings = rootContext.settings

  //
  // CONTEXT
  //

  type Context = core.Contexts.Context

  def Context_owner(self: Context): Symbol = self.owner

  def Context_source(self: Context): java.nio.file.Path = self.compilationUnit.source.file.jpath

  //
  // Settings
  //

  type Settings = config.ScalaSettings

  def Settings_color(self: Settings): Boolean = self.color.value(rootContext) == "always"

  //
  // TREES
  //

  type TermOrTypeTree = tpd.Tree

  type Tree = tpd.Tree

  def Tree_pos(self: Tree)(implicit ctx: Context): Position = self.sourcePos
  def Tree_symbol(self: Tree)(implicit ctx: Context): Symbol = self.symbol

  type PackageClause = tpd.PackageDef

  def isPackageClause(tree: Tree)(implicit ctx: Context): Option[PackageClause] = tree match {
    case x: tpd.PackageDef => Some(x)
    case _ => None
  }

  def PackageClause_pid(self: PackageClause)(implicit ctx: Context): Term_Ref = self.pid
  def PackageClause_stats(self: PackageClause)(implicit ctx: Context): List[Tree] = self.stats

  def PackageClause_apply(pid: Term_Ref, stats: List[Tree])(implicit ctx: Context): PackageClause =
    withDefaultPos(ctx => tpd.PackageDef(pid.asInstanceOf[tpd.RefTree], stats)(ctx))

  def PackageClause_copy(original: PackageClause)(pid: Term_Ref, stats: List[Tree])(implicit ctx: Context): PackageClause =
    tpd.cpy.PackageDef(original)(pid, stats)

  type Statement = tpd.Tree

  def isStatement(tree: Tree)(implicit ctx: Context): Option[Statement] = tree match {
    case tree if tree.isTerm => Some(tree)
    case _ => isDefinition(tree)
  }

  type Import = tpd.Import

  override def isImport(tree: Tree)(implicit ctx: Context): Option[Import] = tree match {
    case tree: tpd.Import => Some(tree)
    case _ => None
  }

  def Import_impliedOnly(self: Import): Boolean = self.impliedOnly
  def Import_expr(self: Import)(implicit ctx: Context): Tree = self.expr
  def Import_selectors(self: Import)(implicit ctx: Context): List[ImportSelector] = self.selectors

  def Import_apply(impliedOnly: Boolean, expr: Term, selectors: List[ImportSelector])(implicit ctx: Context): Import =
    withDefaultPos(ctx => tpd.Import(impliedOnly, expr, selectors)(ctx))

  def Import_copy(original: Import)(impliedOnly: Boolean, expr: Term, selectors: List[ImportSelector])(implicit ctx: Context): Import =
    tpd.cpy.Import(original)(impliedOnly, expr, selectors)

  type Definition = tpd.Tree

  def isDefinition(tree: Tree)(implicit ctx: Context): Option[Definition] = tree match {
    case tree: tpd.MemberDef => Some(tree)
    case tree: PackageDefinition => Some(tree)
    case _ => None
  }

  def Definition_name(self: Definition)(implicit ctx: Context): String = self match {
    case self: tpd.MemberDef => self.name.toString
    case self: PackageDefinition => self.symbol.name.toString // TODO make PackageDefinition a MemberDef or NameTree
  }

  type PackageDef = PackageDefinition

  def isPackageDef(tree: Tree)(implicit ctx: Context): Option[PackageDef] = tree match {
    case x: PackageDefinition => Some(x)
    case _ => None
  }

  def PackageDef_owner(self: PackageDef)(implicit ctx: Context): PackageDef = packageDefFromSym(self.symbol.owner)

  def PackageDef_members(self: PackageDef)(implicit ctx: Context): List[Statement] = {
    if (self.symbol.is(core.Flags.JavaDefined)) Nil // FIXME should also support java packages
    else self.symbol.info.decls.iterator.map(definitionFromSym).toList
  }

  def PackageDef_symbol(self: PackageDef)(implicit ctx: Context): PackageSymbol = self.symbol

  type ClassDef = tpd.TypeDef

  def isClassDef(tree: Tree)(implicit ctx: Context): Option[ClassDef] = tree match {
    case x: tpd.TypeDef if x.isClassDef => Some(x)
    case _ => None
  }

  def ClassDef_constructor(self: ClassDef)(implicit ctx: Context): DefDef = ClassDef_rhs(self).constr
  def ClassDef_parents(self: ClassDef)(implicit ctx: Context): List[TermOrTypeTree] = ClassDef_rhs(self).parents
  def ClassDef_derived(self: ClassDef)(implicit ctx: Context): List[TypeTree] = ClassDef_rhs(self).derived.asInstanceOf[List[TypeTree]]
  def ClassDef_self(self: ClassDef)(implicit ctx: Context): Option[ValDef] = optional(ClassDef_rhs(self).self)
  def ClassDef_body(self: ClassDef)(implicit ctx: Context): List[Statement] = ClassDef_rhs(self).body
  def ClassDef_symbol(self: ClassDef)(implicit ctx: Context): ClassSymbol = self.symbol.asClass
  private def ClassDef_rhs(self: ClassDef) = self.rhs.asInstanceOf[tpd.Template]

  def ClassDef_copy(original: ClassDef)(name: String, constr: DefDef, parents: List[TermOrTypeTree], derived: List[TypeTree], selfOpt: Option[ValDef], body: List[Statement])(implicit ctx: Context): ClassDef = {
    val Trees.TypeDef(_, originalImpl: tpd.Template) = original
    tpd.cpy.TypeDef(original)(name.toTypeName, tpd.cpy.Template(originalImpl)(constr, parents, derived, selfOpt.getOrElse(tpd.EmptyValDef), body))
  }

  type TypeDef = tpd.TypeDef

  def isTypeDef(tree: Tree)(implicit ctx: Context): Option[TypeDef] = tree match {
    case x: tpd.TypeDef if !x.symbol.isClass => Some(x)
    case _ => None
  }

  def TypeDef_rhs(self: TypeDef)(implicit ctx: Context): TypeOrBoundsTree = self.rhs
  def TypeDef_symbol(self: TypeDef)(implicit ctx: Context): TypeSymbol = self.symbol.asType

  def TypeDef_apply(symbol: TypeSymbol)(implicit ctx: Context): TypeDef = withDefaultPos(ctx => tpd.TypeDef(symbol)(ctx))
  def TypeDef_copy(original: TypeDef)(name: String, rhs: TypeOrBoundsTree)(implicit ctx: Context): TypeDef =
    tpd.cpy.TypeDef(original)(name.toTypeName, rhs)

  type DefDef = tpd.DefDef

  def isDefDef(tree: Tree)(implicit ctx: Context): Option[DefDef] = tree match {
    case x: tpd.DefDef => Some(x)
    case _ => None
  }

  def DefDef_typeParams(self: DefDef)(implicit ctx: Context): List[TypeDef] = self.tparams
  def DefDef_paramss(self: DefDef)(implicit ctx: Context): List[List[ValDef]] = self.vparamss
  def DefDef_returnTpt(self: DefDef)(implicit ctx: Context): TypeTree = self.tpt
  def DefDef_rhs(self: DefDef)(implicit ctx: Context): Option[Tree] = optional(self.rhs)
  def DefDef_symbol(self: DefDef)(implicit ctx: Context): DefSymbol = self.symbol.asTerm

  def DefDef_apply(symbol: DefSymbol, rhsFn: List[Type] => List[List[Term]] => Option[Term])(implicit ctx: Context): DefDef =
    withDefaultPos(ctx => tpd.polyDefDef(symbol, tparams => vparamss => rhsFn(tparams)(vparamss).getOrElse(tpd.EmptyTree))(ctx))

  def DefDef_copy(original: DefDef)(name: String, typeParams: List[TypeDef], paramss: List[List[ValDef]], tpt: TypeTree, rhs: Option[Term])(implicit ctx: Context): DefDef =
    tpd.cpy.DefDef(original)(name.toTermName, typeParams, paramss, tpt, rhs.getOrElse(tpd.EmptyTree))

  type ValDef = tpd.ValDef

  def isValDef(tree: Tree)(implicit ctx: Context): Option[ValDef] = tree match {
    case x: tpd.ValDef => Some(x)
    case _ => None
  }

  def ValDef_tpt(self: ValDef)(implicit ctx: Context): TypeTree = self.tpt
  def ValDef_rhs(self: ValDef)(implicit ctx: Context): Option[Tree] = optional(self.rhs)
  def ValDef_symbol(self: ValDef)(implicit ctx: Context): ValSymbol = self.symbol.asTerm

  def ValDef_apply(symbol: ValSymbol, rhs: Option[Term])(implicit ctx: Context): ValDef =
    tpd.ValDef(symbol, rhs.getOrElse(tpd.EmptyTree))

  def ValDef_copy(original: ValDef)(name: String, tpt: TypeTree, rhs: Option[Term])(implicit ctx: Context): ValDef =
    tpd.cpy.ValDef(original)(name.toTermName, tpt, rhs.getOrElse(tpd.EmptyTree))

  type Term = tpd.Tree

  def isTerm(tree: Tree)(implicit ctx: Context): Option[Term] =
    if (tree.isTerm) Some(tree) else None

  // TODO move to Kernel and use isTerm directly with a cast
  def isTermNotTypeTree(termOrTypeTree: TermOrTypeTree)(implicit ctx: Context): Option[Term] =
    if (termOrTypeTree.isTerm) Some(termOrTypeTree) else None

  def Term_pos(self: Term)(implicit ctx: Context): Position = self.sourcePos
  def Term_tpe(self: Term)(implicit ctx: Context): Type = self.tpe
  def Term_underlyingArgument(self: Term)(implicit ctx: Context): Term = self.underlyingArgument
  def Term_underlying(self: Term)(implicit ctx: Context): Term = self.underlying

  type Term_Ref = tpd.RefTree

  def Term_Ref_apply(sym: Symbol)(implicit ctx: Context): Term_Ref =
    withDefaultPos(ctx => tpd.ref(sym)(ctx).asInstanceOf[tpd.RefTree])

  type Term_Ident = tpd.Ident

  def isTerm_Ident(x: Term)(implicit ctx: Context): Option[Term_Ident] = x match {
    case x: tpd.Ident if x.isTerm => Some(x)
    case _ => None
  }

  def Term_Ident_name(self: Term_Ident)(implicit ctx: Context): String = self.name.show

  def Term_Ident_apply(tmref: TermRef)(implicit ctx: Context): Term =
    withDefaultPos(implicit ctx => tpd.ref(tmref).asInstanceOf[Term])

  def Term_Ident_copy(original: Tree)(name: String)(implicit ctx: Context): Term_Ident =
    tpd.cpy.Ident(original)(name.toTermName)

  type Term_Select = tpd.Select

  def isTerm_Select(x: Term)(implicit ctx: Context): Option[Term_Select] = x match {
    case x: tpd.Select if x.isTerm => Some(x)
    case _ => None
  }

  def Term_Select_qualifier(self: Term_Select)(implicit ctx: Context): Term = self.qualifier
  def Term_Select_name(self: Term_Select)(implicit ctx: Context): String = self.name.toString
  def Term_Select_signature(self: Term_Select)(implicit ctx: Context): Option[Signature] =
    if (self.symbol.signature == core.Signature.NotAMethod) None
    else Some(self.symbol.signature)

  def Term_Select_unique(qualifier: Term, name: String)(implicit ctx: Context): Term_Select = {
    val denot = qualifier.tpe.member(name.toTermName)
    assert(!denot.isOverloaded, s"The symbol `$name` is overloaded. The method Select.unique can only be used for non-overloaded symbols.")
    withDefaultPos(implicit ctx => tpd.Select(qualifier, name.toTermName))
  }

  // TODO rename, this returns an Apply and not a Select
  def Term_Select_overloaded(qualifier: Term, name: String, targs: List[Type], args: List[Term])(implicit ctx: Context): Term_Apply =
    withDefaultPos(implicit ctx => tpd.applyOverloaded(qualifier, name.toTermName, args, targs, Types.WildcardType).asInstanceOf[Term_Apply])

  def Term_Select_copy(original: Tree)(qualifier: Term, name: String)(implicit ctx: Context): Term_Select =
    tpd.cpy.Select(original)(qualifier, name.toTermName)

  type Term_Literal = tpd.Literal

  def isTerm_Literal(x: Term)(implicit ctx: Context): Option[Term_Literal] = x match {
    case x: tpd.Literal => Some(x)
    case _ => None
  }

  def Term_Literal_constant(self: Term_Literal)(implicit ctx: Context): Constant = self.const

  def Term_Literal_apply(constant: Constant)(implicit ctx: Context): Term_Literal =
    withDefaultPos(ctx => tpd.Literal(constant)(ctx))

  def Term_Literal_copy(original: Tree)(constant: Constant)(implicit ctx: Context): Term_Literal =
    tpd.cpy.Literal(original)(constant)

  type Term_This = tpd.This

  def isTerm_This(x: Term)(implicit ctx: Context): Option[Term_This] = x match {
    case x: tpd.This => Some(x)
    case _ => None
  }

  def Term_This_id(self: Term_This)(implicit ctx: Context): Option[Id] = optional(self.qual)

  def Term_This_apply(cls: ClassSymbol)(implicit ctx: Context): Term_This =
    withDefaultPos(ctx => tpd.This(cls)(ctx))

  def Term_This_copy(original: Tree)(qual: Option[Id])(implicit ctx: Context): Term_This =
    tpd.cpy.This(original)(qual.getOrElse(untpd.EmptyTypeIdent))

  type Term_New = tpd.New

  def isTerm_New(x: Term)(implicit ctx: Context): Option[Term_New] = x match {
    case x: tpd.New => Some(x)
    case _ => None
  }

  def Term_New_tpt(self: Term_New)(implicit ctx: Context): TypeTree = self.tpt

  def Term_New_apply(tpt: TypeTree)(implicit ctx: Context): Term_New = withDefaultPos(ctx => tpd.New(tpt)(ctx))

  def Term_New_copy(original: Tree)(tpt: TypeTree)(implicit ctx: Context): Term_New =
    tpd.cpy.New(original)(tpt)

  type Term_NamedArg = tpd.NamedArg

  def isTerm_NamedArg(x: Term)(implicit ctx: Context): Option[Term_NamedArg] = x match {
    case x: tpd.NamedArg if x.name.isInstanceOf[core.Names.TermName] => Some(x) // TODO: Now, the name should alwas be a term name
    case _ => None
  }

  def Term_NamedArg_name(self: Term_NamedArg)(implicit ctx: Context): String = self.name.toString
  def Term_NamedArg_value(self: Term_NamedArg)(implicit ctx: Context): Term = self.arg

  def Term_NamedArg_apply(name: String, arg: Term)(implicit ctx: Context): Term_NamedArg =
    withDefaultPos(ctx => tpd.NamedArg(name.toTermName, arg)(ctx))

  def Term_NamedArg_copy(tree: Term_NamedArg)(name: String, arg: Term)(implicit ctx: Context): Term_NamedArg =
    tpd.cpy.NamedArg(tree)(name.toTermName, arg)

  type Term_Apply = tpd.Apply

  def isTerm_Apply(x: Term)(implicit ctx: Context): Option[Term_Apply] = x match {
    case x: tpd.Apply => Some(x)
    case _ => None
  }

  def Term_Apply_fun(self: Term_Apply)(implicit ctx: Context): Term = self.fun
  def Term_Apply_args(self: Term_Apply)(implicit ctx: Context): List[Term] = self.args


  def Term_Apply_apply(fn: Term, args: List[Term])(implicit ctx: Context): Term_Apply =
    withDefaultPos(ctx => tpd.Apply(fn, args)(ctx))

  def Term_Apply_copy(original: Tree)(fun: Term, args: List[Term])(implicit ctx: Context): Term_Apply =
    tpd.cpy.Apply(original)(fun, args)

  type Term_TypeApply = tpd.TypeApply

  def isTerm_TypeApply(x: Term)(implicit ctx: Context): Option[Term_TypeApply] = x match {
    case x: tpd.TypeApply => Some(x)
    case _ => None
  }

  def Term_TypeApply_fun(self: Term_TypeApply)(implicit ctx: Context): Term = self.fun
  def Term_TypeApply_args(self: Term_TypeApply)(implicit ctx: Context): List[TypeTree] = self.args

  def Term_TypeApply_apply(fn: Term, args: List[TypeTree])(implicit ctx: Context): Term_TypeApply =
    withDefaultPos(ctx => tpd.TypeApply(fn, args)(ctx))

  def Term_TypeApply_copy(original: Tree)(fun: Term, args: List[TypeTree])(implicit ctx: Context): Term_TypeApply =
    tpd.cpy.TypeApply(original)(fun, args)

  type Term_Super = tpd.Super

  def isTerm_Super(x: Term)(implicit ctx: Context): Option[Term_Super] = x match {
    case x: tpd.Super => Some(x)
    case _ => None
  }

  def Term_Super_qualifier(self: Term_Super)(implicit ctx: Context): Term = self.qual
  def Term_Super_id(self: Term_Super)(implicit ctx: Context): Option[Id] = optional(self.mix)

  def Term_Super_apply(qual: Term, mix: Option[Id])(implicit ctx: Context): Term_Super =
    withDefaultPos(ctx => tpd.Super(qual, mix.getOrElse(untpd.EmptyTypeIdent), false, NoSymbol)(ctx))

  def Term_Super_copy(original: Tree)(qual: Term, mix: Option[Id])(implicit ctx: Context): Term_Super =
    tpd.cpy.Super(original)(qual, mix.getOrElse(untpd.EmptyTypeIdent))

  type Term_Typed = tpd.Typed

  def isTerm_Typed(x: Term)(implicit ctx: Context): Option[Term_Typed] = x match {
    case x: tpd.Typed => Some(x)
    case _ => None
  }

  def Term_Typed_expr(self: Term_Typed)(implicit ctx: Context): Term = self.expr
  def Term_Typed_tpt(self: Term_Typed)(implicit ctx: Context): TypeTree = self.tpt

  def Term_Typed_apply(expr: Term, tpt: TypeTree)(implicit ctx: Context): Term_Typed =
    withDefaultPos(ctx => tpd.Typed(expr, tpt)(ctx))

  def Term_Typed_copy(original: Tree)(expr: Term, tpt: TypeTree)(implicit ctx: Context): Term_Typed =
    tpd.cpy.Typed(original)(expr, tpt)

  type Term_Assign = tpd.Assign

  def isTerm_Assign(x: Term)(implicit ctx: Context): Option[Term_Assign] = x match {
    case x: tpd.Assign => Some(x)
    case _ => None
  }

  def Term_Assign_lhs(self: Term_Assign)(implicit ctx: Context): Term = self.lhs
  def Term_Assign_rhs(self: Term_Assign)(implicit ctx: Context): Term = self.rhs

  def Term_Assign_apply(lhs: Term, rhs: Term)(implicit ctx: Context): Term_Assign =
    withDefaultPos(ctx => tpd.Assign(lhs, rhs)(ctx))

  def Term_Assign_copy(original: Tree)(lhs: Term, rhs: Term)(implicit ctx: Context): Term_Assign =
    tpd.cpy.Assign(original)(lhs, rhs)

  type Term_Block = tpd.Block

  def isTerm_Block(x: Term)(implicit ctx: Context): Option[Term_Block] = normalizedLoops(x) match {
    case x: tpd.Block => Some(x)
    case _ => None
  }

  /** Normalizes non Blocks.
   *  i) Put `while` and `doWhile` loops in their own blocks: `{ def while$() = ...; while$() }`
   *  ii) Put closures in their own blocks: `{ def anon$() = ...; closure(anon$, ...) }`
   */
  private def normalizedLoops(tree: tpd.Tree)(implicit ctx: Context): tpd.Tree = tree match {
    case block: tpd.Block if block.stats.size > 1 =>
      def normalizeInnerLoops(stats: List[tpd.Tree]): List[tpd.Tree] = stats match {
        case (x: tpd.DefDef) :: y :: xs if needsNormalization(y) =>
          tpd.Block(x :: Nil, y) :: normalizeInnerLoops(xs)
        case x :: xs => x :: normalizeInnerLoops(xs)
        case Nil => Nil
      }
      if (needsNormalization(block.expr)) {
        val stats1 = normalizeInnerLoops(block.stats.init)
        val normalLoop = tpd.Block(block.stats.last :: Nil, block.expr)
        tpd.Block(stats1, normalLoop)
      } else {
        val stats1 = normalizeInnerLoops(block.stats)
        tpd.cpy.Block(block)(stats1, block.expr)
      }
    case _ => tree
  }

  /** If it is the second statement of a closure. See: `normalizedLoops` */
  private def needsNormalization(tree: tpd.Tree)(implicit ctx: Context): Boolean = tree match {
    case _: tpd.Closure => true
    case _ => false
  }

  def Term_Block_statements(self: Term_Block)(implicit ctx: Context): List[Statement] = self.stats
  def Term_Block_expr(self: Term_Block)(implicit ctx: Context): Term = self.expr

  def Term_Block_apply(stats: List[Statement], expr: Term)(implicit ctx: Context): Term_Block =
    withDefaultPos(ctx => tpd.Block(stats, expr)(ctx))

  def Term_Block_copy(original: Tree)(stats: List[Statement], expr: Term)(implicit ctx: Context): Term_Block =
    tpd.cpy.Block(original)(stats, expr)

  type Term_Inlined = tpd.Inlined

  def isTerm_Inlined(x: Term)(implicit ctx: Context): Option[Term_Inlined] = x match {
    case x: tpd.Inlined => Some(x)
    case _ => None
  }

  def Term_Inlined_call(self: Term_Inlined)(implicit ctx: Context): Option[TermOrTypeTree] = optional(self.call)
  def Term_Inlined_bindings(self: Term_Inlined)(implicit ctx: Context): List[Definition] = self.bindings
  def Term_Inlined_body(self: Term_Inlined)(implicit ctx: Context): Term = self.expansion

  def Term_Inlined_apply(call: Option[TermOrTypeTree], bindings: List[Definition], expansion: Term)(implicit ctx: Context): Term_Inlined =
    withDefaultPos(ctx => tpd.Inlined(call.getOrElse(tpd.EmptyTree), bindings.map { case b: tpd.MemberDef => b }, expansion)(ctx))

  def Term_Inlined_copy(original: Tree)(call: Option[TermOrTypeTree], bindings: List[Definition], expansion: Term)(implicit ctx: Context): Term_Inlined =
    tpd.cpy.Inlined(original)(call.getOrElse(tpd.EmptyTree), bindings.asInstanceOf[List[tpd.MemberDef]], expansion)

  type Term_Lambda = tpd.Closure

  def isTerm_Lambda(x: Term)(implicit ctx: Context): Option[Term_Lambda] = x match {
    case x: tpd.Closure => Some(x)
    case _ => None
  }

  def Term_Lambda_meth(self: Term_Lambda)(implicit ctx: Context): Term = self.meth
  def Term_Lambda_tptOpt(self: Term_Lambda)(implicit ctx: Context): Option[TypeTree] = optional(self.tpt)

  def Term_Lambda_apply(meth: Term, tpt: Option[TypeTree])(implicit ctx: Context): Term_Lambda =
    withDefaultPos(ctx => tpd.Closure(Nil, meth, tpt.getOrElse(tpd.EmptyTree))(ctx))

  def Term_Lambda_copy(original: Tree)(meth: Tree, tpt: Option[TypeTree])(implicit ctx: Context): Term_Lambda =
    tpd.cpy.Closure(original)(Nil, meth, tpt.getOrElse(tpd.EmptyTree))

  type Term_If = tpd.If

  def isTerm_If(x: Term)(implicit ctx: Context): Option[Term_If] = x match {
    case x: tpd.If => Some(x)
    case _ => None
  }

  def Term_If_cond(self: Term_If)(implicit ctx: Context): Term = self.cond
  def Term_If_thenp(self: Term_If)(implicit ctx: Context): Term = self.thenp
  def Term_If_elsep(self: Term_If)(implicit ctx: Context): Term = self.elsep

  def Term_If_apply(cond: Term, thenp: Term, elsep: Term)(implicit ctx: Context): Term_If =
    withDefaultPos(ctx => tpd.If(cond, thenp, elsep)(ctx))

  def Term_If_copy(original: Tree)(cond: Term, thenp: Term, elsep: Term)(implicit ctx: Context): Term_If =
    tpd.cpy.If(original)(cond, thenp, elsep)

  type Term_Match = tpd.Match

  def isTerm_Match(x: Term)(implicit ctx: Context): Option[Term_Match] = x match {
    case x: tpd.Match => Some(x)
    case _ => None
  }

  def Term_Match_scrutinee(self: Term_Match)(implicit ctx: Context): Term = self.selector
  def Term_Match_cases(self: Term_Match)(implicit ctx: Context): List[CaseDef] = self.cases

  def Term_Match_apply(selector: Term, cases: List[CaseDef])(implicit ctx: Context): Term_Match =
    withDefaultPos(ctx => tpd.Match(selector, cases)(ctx))

  def Term_Match_copy(original: Tree)(selector: Term, cases: List[CaseDef])(implicit ctx: Context): Term_Match =
    tpd.cpy.Match(original)(selector, cases)

  type Term_Try = tpd.Try

  def isTerm_Try(x: Term)(implicit ctx: Context): Option[Term_Try] = x match {
    case x: tpd.Try => Some(x)
    case _ => None
  }

  def Term_Try_body(self: Term_Try)(implicit ctx: Context): Term = self.expr
  def Term_Try_cases(self: Term_Try)(implicit ctx: Context): List[CaseDef] = self.cases
  def Term_Try_finalizer(self: Term_Try)(implicit ctx: Context): Option[Term] = optional(self.finalizer)

  def Term_Try_apply(expr: Term, cases: List[CaseDef], finalizer: Option[Term])(implicit ctx: Context): Term_Try =
    withDefaultPos(ctx => tpd.Try(expr, cases, finalizer.getOrElse(tpd.EmptyTree))(ctx))

  def Term_Try_copy(original: Tree)(expr: Term, cases: List[CaseDef], finalizer: Option[Term])(implicit ctx: Context): Term_Try =
    tpd.cpy.Try(original)(expr, cases, finalizer.getOrElse(tpd.EmptyTree))

  type Term_Return = tpd.Return

  def isTerm_Return(x: Term)(implicit ctx: Context): Option[Term_Return] = x match {
    case x: tpd.Return => Some(x)
    case _ => None
  }

  def Term_Return_expr(self: Term_Return)(implicit ctx: Context): Term = self.expr

  def Term_Return_apply(expr: Term)(implicit ctx: Context): Term_Return =
    withDefaultPos(ctx => tpd.Return(expr, ctx.owner)(ctx))

  def Term_Return_copy(original: Tree)(expr: Term)(implicit ctx: Context): Term_Return =
    tpd.cpy.Return(original)(expr, tpd.ref(ctx.owner))

  type Term_Repeated = tpd.SeqLiteral

  def isTerm_Repeated(x: Term)(implicit ctx: Context): Option[Term_Repeated] = x match {
    case x: tpd.SeqLiteral => Some(x)
    case _ => None
  }

  def Term_Repeated_elems(self: Term_Repeated)(implicit ctx: Context): List[Term] = self.elems
  def Term_Repeated_elemtpt(self: Term_Repeated)(implicit ctx: Context): TypeTree = self.elemtpt

  def Term_Repeated_apply(elems: List[Term], elemtpt: TypeTree)(implicit ctx: Context): Term_Repeated =
    withDefaultPos(ctx => tpd.SeqLiteral(elems, elemtpt)(ctx))

  def Term_Repeated_copy(original: Tree)(elems: List[Term], elemtpt: TypeTree)(implicit ctx: Context): Term_Repeated =
    tpd.cpy.SeqLiteral(original)(elems, elemtpt)

  type Term_SelectOuter = tpd.Select

  def isTerm_SelectOuter(x: Term)(implicit ctx: Context): Option[Term_SelectOuter] = x match {
    case x: tpd.Select =>
      x.name match {
        case NameKinds.OuterSelectName(_, _) => Some(x)
        case _ => None
      }
    case _ => None
  }

  def Term_SelectOuter_qualifier(self: Term_SelectOuter)(implicit ctx: Context): Term = self.qualifier
  def Term_SelectOuter_level(self: Term_SelectOuter)(implicit ctx: Context): Int = {
    val NameKinds.OuterSelectName(_, levels) = self.name
    levels
  }
  def Term_SelectOuter_tpe(self: Term_SelectOuter)(implicit ctx: Context): Type = self.tpe.stripTypeVar

  def Term_SelectOuter_apply(qualifier: Term, name: String, levels: Int)(implicit ctx: Context): Term_SelectOuter =
    withDefaultPos(ctx => tpd.Select(qualifier, NameKinds.OuterSelectName(name.toTermName, levels))(ctx))

  def Term_SelectOuter_copy(original: Tree)(qualifier: Term, name: String, levels: Int)(implicit ctx: Context): Term_SelectOuter =
    tpd.cpy.Select(original)(qualifier, NameKinds.OuterSelectName(name.toTermName, levels))

  type Term_While = tpd.WhileDo

  def isTerm_While(x: Term)(implicit ctx: Context): Option[Term_While] = x match {
    case x: tpd.WhileDo => Some(x)
    case _ => None
  }

  def Term_While_cond(self: Term_While)(implicit ctx: Context): Term = self.cond
  def Term_While_body(self: Term_While)(implicit ctx: Context): Term = self.body

  def Term_While_apply(cond: Term, body: Term)(implicit ctx: Context): Term_While =
    withDefaultPos(ctx => tpd.WhileDo(cond, body)(ctx))

  def Term_While_copy(original: Tree)(cond: Term, body: Term)(implicit ctx: Context): Term_While =
    tpd.cpy.WhileDo(original)(cond, body)

  //
  // CASES
  //

  type CaseDef = tpd.CaseDef

  def CaseDef_pattern(self: CaseDef)(implicit ctx: Context): Pattern = self.pat
  def CaseDef_guard(self: CaseDef)(implicit ctx: Context): Option[Term] = optional(self.guard)
  def CaseDef_rhs(self: CaseDef)(implicit ctx: Context): Term = self.body

  def CaseDef_module_apply(pattern: Pattern, guard: Option[Term], body: Term)(implicit ctx: Context): CaseDef =
    tpd.CaseDef(pattern, guard.getOrElse(tpd.EmptyTree), body)

  def CaseDef_module_copy(original: CaseDef)(pattern: Pattern, guard: Option[Term], body: Term)(implicit ctx: Context): CaseDef =
    tpd.cpy.CaseDef(original)(pattern, guard.getOrElse(tpd.EmptyTree), body)

  type TypeCaseDef = tpd.CaseDef

  def TypeCaseDef_pattern(self: TypeCaseDef)(implicit ctx: Context): TypeTree = self.pat
  def TypeCaseDef_rhs(self: TypeCaseDef)(implicit ctx: Context): TypeTree = self.body

  def TypeCaseDef_module_apply(pattern: TypeTree, body: TypeTree)(implicit ctx: Context): TypeCaseDef =
    tpd.CaseDef(pattern, tpd.EmptyTree, body)

  def TypeCaseDef_module_copy(original: TypeCaseDef)(pattern: TypeTree, body: TypeTree)(implicit ctx: Context): TypeCaseDef =
    tpd.cpy.CaseDef(original)(pattern, tpd.EmptyTree, body)

  //
  // PATTERNS
  //

  type Pattern = tpd.Tree

  def Pattern_pos(self: Pattern)(implicit ctx: Context): Position = self.sourcePos
  def Pattern_tpe(self: Pattern)(implicit ctx: Context): Type = self.tpe.stripTypeVar
  def Pattern_symbol(self: Pattern)(implicit ctx: Context): Symbol = self.symbol

  type Value = tpd.Tree

  def isPattern_Value(pattern: Pattern): Option[Value] = pattern match {
    case lit: tpd.Literal => Some(lit)
    case ref: tpd.RefTree if ref.isTerm => Some(ref)
    case ths: tpd.This => Some(ths)
    case _ => None
  }

  def Pattern_Value_value(self: Value)(implicit ctx: Context): Term = self

  def Pattern_Value_module_apply(term: Term)(implicit ctx: Context): Value = term match {
    case lit: tpd.Literal => lit
    case ref: tpd.RefTree if ref.isTerm => ref
    case ths: tpd.This => ths
  }
  def Pattern_Value_module_copy(original: Value)(term: Term)(implicit ctx: Context): Value = term match {
    case lit: tpd.Literal => tpd.cpy.Literal(original)(lit.const)
    case ref: tpd.RefTree if ref.isTerm => tpd.cpy.Ref(original.asInstanceOf[tpd.RefTree])(ref.name)
    case ths: tpd.This => tpd.cpy.This(original)(ths.qual)
  }

  type Bind = tpd.Bind

  def isPattern_Bind(x: Pattern)(implicit ctx: Context): Option[Bind] = x match {
    case x: tpd.Bind if x.name.isTermName => Some(x)
    case _ => None
  }

  def Pattern_Bind_name(self: Bind)(implicit ctx: Context): String = self.name.toString

  def Pattern_Bind_pattern(self: Bind)(implicit ctx: Context): Pattern = self.body

  def Pattern_Bind_module_copy(original: Bind)(name: String, pattern: Pattern)(implicit ctx: Context): Bind =
    withDefaultPos(ctx => tpd.cpy.Bind(original)(name.toTermName, pattern)(ctx))

  type Unapply = tpd.UnApply

  def isPattern_Unapply(pattern: Pattern)(implicit ctx: Context): Option[Unapply] = pattern match {
    case pattern @ Trees.UnApply(_, _, _) => Some(pattern)
    case Trees.Typed(pattern @ Trees.UnApply(_, _, _), _) => Some(pattern)
    case _ => None
  }

  def Pattern_Unapply_fun(self: Unapply)(implicit ctx: Context): Term = self.fun
  def Pattern_Unapply_implicits(self: Unapply)(implicit ctx: Context): List[Term] = self.implicits
  def Pattern_Unapply_patterns(self: Unapply)(implicit ctx: Context): List[Pattern] = effectivePatterns(self.patterns)

  def Pattern_Unapply_module_copy(original: Unapply)(fun: Term, implicits: List[Term], patterns: List[Pattern])(implicit ctx: Context): Unapply =
    withDefaultPos(ctx => tpd.cpy.UnApply(original)(fun, implicits, patterns)(ctx))

  private def effectivePatterns(patterns: List[Pattern]): List[Pattern] = patterns match {
    case patterns0 :+ Trees.SeqLiteral(elems, _) => patterns0 ::: elems
    case _ => patterns
  }

  type Alternatives = tpd.Alternative

  def isPattern_Alternatives(pattern: Pattern)(implicit ctx: Context): Option[Alternatives] = pattern match {
    case pattern: tpd.Alternative => Some(pattern)
    case _ => None
  }

  def Pattern_Alternatives_patterns(self: Alternatives)(implicit ctx: Context): List[Pattern] = self.trees

  def Pattern_Alternatives_module_apply(patterns: List[Pattern])(implicit ctx: Context): Alternatives =
    withDefaultPos(ctx => tpd.Alternative(patterns)(ctx))

  def Pattern_Alternatives_module_copy(original: Alternatives)(patterns: List[Pattern])(implicit ctx: Context): Alternatives =
    tpd.cpy.Alternative(original)(patterns)

  type TypeTest = tpd.Typed

  def isPattern_TypeTest(pattern: Pattern)(implicit ctx: Context): Option[TypeTest] = pattern match {
    case Trees.Typed(_: tpd.UnApply, _) => None
    case pattern: tpd.Typed => Some(pattern)
    case _ => None
  }

  def Pattern_TypeTest_tpt(self: TypeTest)(implicit ctx: Context): TypeTree = self.tpt

  def Pattern_TypeTest_module_apply(tpt: TypeTree)(implicit ctx: Context): TypeTest =
    withDefaultPos(ctx => tpd.Typed(untpd.Ident(nme.WILDCARD)(ctx.source).withType(tpt.tpe)(ctx), tpt)(ctx))

  def Pattern_TypeTest_module_copy(original: TypeTest)(tpt: TypeTree)(implicit ctx: Context): TypeTest =
    tpd.cpy.Typed(original)(untpd.Ident(nme.WILDCARD).withSpan(original.span).withType(tpt.tpe), tpt)

  //
  // TYPE TREES
  //

  type TypeOrBoundsTree = tpd.Tree

  def TypeOrBoundsTree_tpe(self: TypeOrBoundsTree)(implicit ctx: Context): Type = self.tpe.stripTypeVar

  type TypeTree = tpd.Tree

  def isTypeTree(x: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree] = x match {
    case x: tpd.TypeBoundsTree => None
    case _ => if (x.isType) Some(x) else None
  }

  // TODO move to Kernel and use isTypeTree directly with a cast
  def isTypeTreeNotTerm(termOrTypeTree: TermOrTypeTree)(implicit ctx: Context): Option[TypeTree] = termOrTypeTree match {
    case _: tpd.TypeBoundsTree => None
    case _ => if (termOrTypeTree.isType) Some(termOrTypeTree) else None
  }

  def TypeTree_pos(self: TypeTree)(implicit ctx: Context): Position = self.sourcePos
  def TypeTree_symbol(self: TypeTree)(implicit ctx: Context): Symbol = self.symbol
  def TypeTree_tpe(self: TypeTree)(implicit ctx: Context): Type = self.tpe.stripTypeVar

  type TypeTree_Inferred = tpd.TypeTree

  def isTypeTree_Inferred(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Inferred] = tpt match {
    case tpt: tpd.TypeTree if !tpt.tpe.isInstanceOf[Types.TypeBounds] => Some(tpt)
    case _ => None
  }

  def TypeTree_Inferred_apply(tpe: Type)(implicit ctx: Context): TypeTree_Inferred = withDefaultPos(ctx => tpd.TypeTree(tpe)(ctx))

  type TypeTree_Ident = tpd.Ident

  def isTypeTree_Ident(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Ident] = tpt match {
    case tpt: tpd.Ident if tpt.isType => Some(tpt)
    case _ => None
  }

  def TypeTree_Ident_name(self: TypeTree_Ident)(implicit ctx: Context): String = self.name.toString

  def TypeTree_Ident_copy(original: TypeTree_Ident)(name: String)(implicit ctx: Context): TypeTree_Ident =
    tpd.cpy.Ident(original)(name.toTypeName)

  type TypeTree_Select = tpd.Select

  def isTypeTree_Select(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Select] = tpt match {
    case tpt: tpd.Select if tpt.isType && tpt.qualifier.isTerm  => Some(tpt)
    case _ => None
  }

  def TypeTree_Select_qualifier(self: TypeTree_Select)(implicit ctx: Context): Term = self.qualifier
  def TypeTree_Select_name(self: TypeTree_Select)(implicit ctx: Context): String = self.name.toString

  def TypeTree_Select_apply(qualifier: Term, name: String)(implicit ctx: Context): TypeTree_Select =
    withDefaultPos(ctx => tpd.Select(qualifier, name.toTypeName)(ctx))

  def TypeTree_Select_copy(original: TypeTree_Select)(qualifier: Term, name: String)(implicit ctx: Context): TypeTree_Select =
    tpd.cpy.Select(original)(qualifier, name.toTypeName)


  type TypeTree_Projection = tpd.Select

  def isTypeTree_Projection(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Projection] = tpt match {
    case tpt: tpd.Select if tpt.isType && tpt.qualifier.isType => Some(tpt)
    case _ => None
  }

  def TypeTree_Projection_qualifier(self: TypeTree_Projection)(implicit ctx: Context): TypeTree = self.qualifier
  def TypeTree_Projection_name(self: TypeTree_Projection)(implicit ctx: Context): String = self.name.toString

  def TypeTree_Projection_copy(original: TypeTree_Projection)(qualifier: TypeTree, name: String)(implicit ctx: Context): TypeTree_Projection =
    tpd.cpy.Select(original)(qualifier, name.toTypeName)

  type TypeTree_Singleton = tpd.SingletonTypeTree

  def isTypeTree_Singleton(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Singleton] = tpt match {
    case tpt: tpd.SingletonTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_Singleton_ref(self: TypeTree_Singleton)(implicit ctx: Context): Term = self.ref

  def TypeTree_Singleton_apply(ref: Term)(implicit ctx: Context): TypeTree_Singleton =
    withDefaultPos(ctx => tpd.SingletonTypeTree(ref)(ctx))

  def TypeTree_Singleton_copy(original: TypeTree_Singleton)(ref: Term)(implicit ctx: Context): TypeTree_Singleton =
    tpd.cpy.SingletonTypeTree(original)(ref)

  type TypeTree_Refined = tpd.RefinedTypeTree

  def isTypeTree_Refined(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Refined] = tpt match {
    case tpt: tpd.RefinedTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_Refined_tpt(self: TypeTree_Refined)(implicit ctx: Context): TypeTree = self.tpt
  def TypeTree_Refined_refinements(self: TypeTree_Refined)(implicit ctx: Context): List[Definition] = self.refinements

  def TypeTree_Refined_copy(original: TypeTree_Refined)(tpt: TypeTree, refinements: List[Definition])(implicit ctx: Context): TypeTree_Refined =
    tpd.cpy.RefinedTypeTree(original)(tpt, refinements)

  type TypeTree_Applied = tpd.AppliedTypeTree

  def isTypeTree_Applied(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Applied] = tpt match {
    case tpt: tpd.AppliedTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_Applied_tpt(self: TypeTree_Applied)(implicit ctx: Context): TypeTree = self.tpt
  def TypeTree_Applied_args(self: TypeTree_Applied)(implicit ctx: Context): List[TypeOrBoundsTree] = self.args

  def TypeTree_Applied_apply(tpt: TypeTree, args: List[TypeOrBoundsTree])(implicit ctx: Context): TypeTree_Applied =
    withDefaultPos(ctx => tpd.AppliedTypeTree(tpt, args)(ctx))

  def TypeTree_Applied_copy(original: TypeTree_Applied)(tpt: TypeTree, args: List[TypeOrBoundsTree])(implicit ctx: Context): TypeTree_Applied =
    tpd.cpy.AppliedTypeTree(original)(tpt, args)

  type TypeTree_Annotated = tpd.Annotated

  def isTypeTree_Annotated(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_Annotated] = tpt match {
    case tpt: tpd.Annotated => Some(tpt)
    case _ => None
  }

  def TypeTree_Annotated_arg(self: TypeTree_Annotated)(implicit ctx: Context): TypeTree = self.arg
  def TypeTree_Annotated_annotation(self: TypeTree_Annotated)(implicit ctx: Context): Term = self.annot

  def TypeTree_Annotated_apply(arg: TypeTree, annotation: Term)(implicit ctx: Context): TypeTree_Annotated =
    withDefaultPos(ctx => tpd.Annotated(arg, annotation)(ctx))

  def TypeTree_Annotated_copy(original: TypeTree_Annotated)(arg: TypeTree, annotation: Term)(implicit ctx: Context): TypeTree_Annotated =
    tpd.cpy.Annotated(original)(arg, annotation)

  type TypeTree_MatchType = tpd.MatchTypeTree

  def isTypeTree_MatchType(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_MatchType] = tpt match {
    case tpt: tpd.MatchTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_MatchType_bound(self: TypeTree_MatchType)(implicit ctx: Context): Option[TypeTree] = if (self.bound == tpd.EmptyTree) None else Some(self.bound)
  def TypeTree_MatchType_selector(self: TypeTree_MatchType)(implicit ctx: Context): TypeTree = self.selector
  def TypeTree_MatchType_cases(self: TypeTree_MatchType)(implicit ctx: Context): List[CaseDef] = self.cases

  def TypeTree_MatchType_apply(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef])(implicit ctx: Context): TypeTree_MatchType =
    withDefaultPos(ctx => tpd.MatchTypeTree(bound.getOrElse(tpd.EmptyTree), selector, cases)(ctx))

  def TypeTree_MatchType_copy(original: TypeTree_MatchType)(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef])(implicit ctx: Context): TypeTree_MatchType =
    tpd.cpy.MatchTypeTree(original)(bound.getOrElse(tpd.EmptyTree), selector, cases)

  type TypeTree_ByName = tpd.ByNameTypeTree

  def isTypeTree_ByName(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_ByName] = tpt match {
    case tpt: tpd.ByNameTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_ByName_result(self: TypeTree_ByName)(implicit ctx: Context): TypeTree = self.result

  def TypeTree_ByName_apply(result: TypeTree)(implicit ctx: Context): TypeTree_ByName =
    withDefaultPos(ctx => tpd.ByNameTypeTree(result)(ctx))

  def TypeTree_ByName_copy(original: TypeTree_ByName)(result: TypeTree)(implicit ctx: Context): TypeTree_ByName =
    tpd.cpy.ByNameTypeTree(original)(result)

  type TypeTree_LambdaTypeTree = tpd.LambdaTypeTree

  def isTypeTree_LambdaTypeTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_LambdaTypeTree] = tpt match {
    case tpt: tpd.LambdaTypeTree => Some(tpt)
    case _ => None
  }

  def TypeTree_LambdaTypeTree_tparams(self: TypeTree_LambdaTypeTree)(implicit ctx: Context): List[TypeDef] = self.tparams
  def TypeTree_LambdaTypeTree_body(self: TypeTree_LambdaTypeTree)(implicit ctx: Context): TypeOrBoundsTree = self.body

  def TypeTree_LambdaTypeTree_apply(tparams: List[TypeDef], body: TypeOrBoundsTree)(implicit ctx: Context): TypeTree_LambdaTypeTree =
    withDefaultPos(ctx => tpd.LambdaTypeTree(tparams, body)(ctx))

  def TypeTree_LambdaTypeTree_copy(original: TypeTree_LambdaTypeTree)(tparams: List[TypeDef], body: TypeOrBoundsTree)(implicit ctx: Context): TypeTree_LambdaTypeTree =
    tpd.cpy.LambdaTypeTree(original)(tparams, body)

  type TypeTree_TypeBind = tpd.Bind

  def isTypeTree_TypeBind(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_TypeBind] = tpt match {
    case tpt: tpd.Bind if tpt.name.isTypeName => Some(tpt)
    case _ => None
  }

  def TypeTree_TypeBind_name(self: TypeTree_TypeBind)(implicit ctx: Context): String = self.name.toString
  def TypeTree_TypeBind_body(self: TypeTree_TypeBind)(implicit ctx: Context): TypeOrBoundsTree = self.body

  def TypeTree_TypeBind_copy(original: TypeTree_TypeBind)(name: String, tpt: TypeOrBoundsTree)(implicit ctx: Context): TypeTree_TypeBind =
    tpd.cpy.Bind(original)(name.toTypeName, tpt)

  type TypeTree_TypeBlock = tpd.Block

  def isTypeTree_TypeBlock(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree_TypeBlock] = tpt match {
    case tpt: tpd.Block => Some(tpt)
    case _ => None
  }

  def TypeTree_TypeBlock_aliases(self: TypeTree_TypeBlock)(implicit ctx: Context): List[TypeDef] = self.stats.map { case alias: TypeDef => alias }
  def TypeTree_TypeBlock_tpt(self: TypeTree_TypeBlock)(implicit ctx: Context): TypeTree = self.expr

  def TypeTree_TypeBlock_apply(aliases: List[TypeDef], tpt: TypeTree)(implicit ctx: Context): TypeTree_TypeBlock =
    withDefaultPos(ctx => tpd.Block(aliases, tpt)(ctx))

  def TypeTree_TypeBlock_copy(original: TypeTree_TypeBlock)(aliases: List[TypeDef], tpt: TypeTree)(implicit ctx: Context): TypeTree_TypeBlock =
    tpd.cpy.Block(original)(aliases, tpt)

  type TypeBoundsTree = tpd.TypeBoundsTree

  def isTypeBoundsTree(x: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeBoundsTree] = x match {
    case x: tpd.TypeBoundsTree => Some(x)
    case x @ Trees.TypeTree() =>
      // TODO only enums generate this kind of type bounds. Is this possible without enums? If not generate tpd.TypeBoundsTree for enums instead
      x.tpe match {
        case tpe: Types.TypeBounds =>
          Some(tpd.TypeBoundsTree(tpd.TypeTree(tpe.lo).withSpan(x.span), tpd.TypeTree(tpe.hi).withSpan(x.span)))
        case _ => None
      }
    case _ => None
  }

  def TypeBoundsTree_tpe(self: TypeBoundsTree)(implicit ctx: Context): TypeBounds = self.tpe.asInstanceOf[Types.TypeBounds]
  def TypeBoundsTree_low(self: TypeBoundsTree)(implicit ctx: Context): TypeTree = self.lo
  def TypeBoundsTree_hi(self: TypeBoundsTree)(implicit ctx: Context): TypeTree = self.hi

  type WildcardTypeTree = tpd.Ident

  def isWildcardTypeTree(x: TypeOrBoundsTree)(implicit ctx: Context): Option[WildcardTypeTree] = x match {
    case x @ Trees.Ident(nme.WILDCARD) => Some(x)
    case _ => None
  }

  //
  // TYPES
  //

  type TypeOrBounds = Types.Type

  type NoPrefix = Types.NoPrefix.type

  type TypeBounds = Types.TypeBounds

  def TypeBounds_low(self: TypeBounds)(implicit ctx: Context): Type = self.lo
  def TypeBounds_hi(self: TypeBounds)(implicit ctx: Context): Type = self.hi

  type Type = Types.Type

  def `Type_=:=`(self: Type)(that: Type)(implicit ctx: Context): Boolean = self =:= that

  def `Type_<:<`(self: Type)(that: Type)(implicit ctx: Context): Boolean = self <:< that

  /** Widen from singleton type to its underlying non-singleton
    *  base type by applying one or more `underlying` dereferences,
    *  Also go from => T to T.
    *  Identity for all other types. Example:
    *
    *  class Outer { class C ; val x: C }
    *  def o: Outer
    *  <o.x.type>.widen = o.C
    */
  def Type_widen(self: Type)(implicit ctx: Context): Type = self.widen

  def Type_classSymbol(self: Type)(implicit ctx: Context): Option[ClassSymbol] =
    if (self.classSymbol.exists) Some(self.classSymbol.asClass) else None

  def Type_typeSymbol(self: Type)(implicit ctx: Context): Symbol = self.typeSymbol

  def Type_isSingleton(self: Type)(implicit ctx: Context): Boolean = self.isSingleton

  def Type_memberType(self: Type)(member: Symbol)(implicit ctx: Context): Type =
    member.info.asSeenFrom(self, member.owner)

  type ConstantType = Types.ConstantType

  def ConstantType_value(self: ConstantType)(implicit ctx: Context): Any = self.value

  type SymRef = Types.NamedType

  def SymRef_qualifier(self: SymRef)(implicit ctx: Context): TypeOrBounds = self.prefix

  type TermRef = Types.NamedType

  def TermRef_qualifier(self: TermRef)(implicit ctx: Context): TypeOrBounds = self.prefix

  type TypeRef = Types.NamedType

  def TypeRef_name(self: TypeRef)(implicit ctx: Context): String = self.name.toString
  def TypeRef_qualifier(self: TypeRef)(implicit ctx: Context): TypeOrBounds = self.prefix

  type SuperType = Types.SuperType

  def SuperType_thistpe(self: SuperType)(implicit ctx: Context): Type = self.thistpe
  def SuperType_supertpe(self: SuperType)(implicit ctx: Context): Type = self.supertpe

  type Refinement = Types.RefinedType

  def Refinement_parent(self: Refinement)(implicit ctx: Context): Type = self.parent
  def Refinement_name(self: Refinement)(implicit ctx: Context): String = self.refinedName.toString
  def Refinement_info(self: Refinement)(implicit ctx: Context): TypeOrBounds = self.refinedInfo

  type AppliedType = Types.AppliedType

  def AppliedType_tycon(self: AppliedType)(implicit ctx: Context): Type = self.tycon
  def AppliedType_args(self: AppliedType)(implicit ctx: Context): List[TypeOrBounds] = self.args

  type AnnotatedType = Types.AnnotatedType

  def AnnotatedType_underlying(self: AnnotatedType)(implicit ctx: Context): Type = self.underlying.stripTypeVar
  def AnnotatedType_annot(self: AnnotatedType)(implicit ctx: Context): Term = self.annot.tree

  type AndType = Types.AndType

  def AndType_left(self: AndType)(implicit ctx: Context): Type = self.tp1.stripTypeVar
  def AndType_right(self: AndType)(implicit ctx: Context): Type = self.tp2.stripTypeVar

  type OrType = Types.OrType

  def OrType_left(self: OrType)(implicit ctx: Context): Type = self.tp1.stripTypeVar
  def OrType_right(self: OrType)(implicit ctx: Context): Type = self.tp2.stripTypeVar

  type MatchType = Types.MatchType

  def MatchType_bound(self: MatchType)(implicit ctx: Context): Type = self.bound
  def MatchType_scrutinee(self: MatchType)(implicit ctx: Context): Type = self.scrutinee
  def MatchType_cases(self: MatchType)(implicit ctx: Context): List[Type] = self.cases

  type ByNameType = Types.ExprType

  def ByNameType_underlying(self: ByNameType)(implicit ctx: Context): Type = self.resType.stripTypeVar

  type ParamRef = Types.ParamRef

  def ParamRef_binder(self: ParamRef)(implicit ctx: Context): LambdaType[TypeOrBounds] =
    self.binder.asInstanceOf[LambdaType[TypeOrBounds]] // Cast to tpd
  def ParamRef_paramNum(self: ParamRef)(implicit ctx: Context): Int = self.paramNum

  type ThisType = Types.ThisType

  def ThisType_underlying(self: ThisType)(implicit ctx: Context): Type = self.underlying

  type RecursiveThis = Types.RecThis

  def RecursiveThis_binder(self: RecursiveThis)(implicit ctx: Context): RecursiveType = self.binder

  type RecursiveType = Types.RecType

  def RecursiveType_underlying(self: RecursiveType)(implicit ctx: Context): Type = self.underlying.stripTypeVar

  type LambdaType[ParamInfo] = Types.LambdaType { type PInfo = ParamInfo }

  type MethodType = Types.MethodType

  def MethodType_isErased(self: MethodType): Boolean = self.isErasedMethod
  def MethodType_isImplicit(self: MethodType): Boolean = self.isImplicitMethod
  def MethodType_paramNames(self: MethodType)(implicit ctx: Context): List[String] = self.paramNames.map(_.toString)
  def MethodType_paramTypes(self: MethodType)(implicit ctx: Context): List[Type] = self.paramInfos
  def MethodType_resType(self: MethodType)(implicit ctx: Context): Type = self.resType

  type PolyType = Types.PolyType

  def PolyType_paramNames(self: PolyType)(implicit ctx: Context): List[String] = self.paramNames.map(_.toString)
  def PolyType_paramBounds(self: PolyType)(implicit ctx: Context): List[TypeBounds] = self.paramInfos
  def PolyType_resType(self: PolyType)(implicit ctx: Context): Type = self.resType

  type TypeLambda = Types.TypeLambda

  def TypeLambda_paramNames(self: TypeLambda)(implicit ctx: Context): List[String] = self.paramNames.map(_.toString)
  def TypeLambda_paramBounds(self: TypeLambda)(implicit ctx: Context): List[TypeBounds] = self.paramInfos
  def TypeLambda_resType(self: TypeLambda)(implicit ctx: Context): Type = self.resType

  //
  // IMPORT SELECTORS
  //

  type ImportSelector = untpd.Tree

  type SimpleSelector = untpd.Ident

  def isSimpleSelector(self: ImportSelector)(implicit ctx: Context): Option[SimpleSelector] = self match {
    case self: untpd.Ident => Some(self)
    case _ => None
  }

  def SimpleSelector_selection(self: SimpleSelector)(implicit ctx: Context): Id = self

  type RenameSelector = untpd.Thicket

  def isRenameSelector(self: ImportSelector)(implicit ctx: Context): Option[RenameSelector] = self match {
    case self @ Trees.Thicket((id1: untpd.Ident) :: (id2: untpd.Ident) :: Nil) if id2.name != nme.WILDCARD => Some(self)
    case _ => None
  }

  def RenameSelector_from(self: RenameSelector)(implicit ctx: Context): Id =
    self.trees.head.asInstanceOf[untpd.Ident]
  def RenameSelector_to(self: RenameSelector)(implicit ctx: Context): Id =
    self.trees.last.asInstanceOf[untpd.Ident]

  type OmitSelector = untpd.Thicket

  def isOmitSelector(self: ImportSelector)(implicit ctx: Context): Option[OmitSelector] = self match {
    case self @ Trees.Thicket((id: untpd.Ident) :: Trees.Ident(nme.WILDCARD) :: Nil) => Some(self)
    case _ => None
  }

  def SimpleSelector_omited(self: OmitSelector)(implicit ctx: Context): Id =
    self.trees.head.asInstanceOf[untpd.Ident]

  //
  // IDENTIFIERS
  //

  type Id = untpd.Ident

  def Id_pos(self: Id)(implicit ctx: Context): Position = self.sourcePos

  def Id_name(self: Id)(implicit ctx: Context): String = self.name.toString

  //
  // SIGNATURES
  //

  type Signature = core.Signature

  def Signature_paramSigs(self: Signature): List[String] =
    self.paramsSig.map(_.toString)

  def Signature_resultSig(self: Signature): String =
    self.resSig.toString

  //
  // POSITIONS
  //

  type Position = util.SourcePosition

  def Position_start(self: Position): Int = self.start

  def Position_end(self: Position): Int = self.end

  def Position_exists(self: Position): Boolean = self.exists

  def Position_sourceFile(self: Position): java.nio.file.Path = self.source.file.jpath

  def Position_startLine(self: Position): Int = self.startLine

  def Position_endLine(self: Position): Int = self.endLine

  def Position_startColumn(self: Position): Int = self.startColumn

  def Position_endColumn(self: Position): Int = self.endColumn

  def Position_sourceCode(self: Position): String =
    new String(self.source.content(), self.start, self.end - self.start)

  //
  // CONSTANTS
  //

  type Constant = Constants.Constant

  def Constant_value(const: Constant): Any = const.value

  def isConstant_Unit(x: Constant): Boolean = x.tag == Constants.UnitTag
  def isConstant_Null(x: Constant): Boolean = x.tag == Constants.NullTag
  def isConstant_Boolean(x: Constant): Option[Boolean] =
    if (x.tag == Constants.BooleanTag) Some(x.booleanValue) else None
  def isConstant_Byte(x: Constant): Option[Byte] =
    if (x.tag == Constants.ByteTag) Some(x.byteValue) else None
  def isConstant_Short(x: Constant): Option[Short] =
    if (x.tag == Constants.ShortTag) Some(x.shortValue) else None
  def isConstant_Char(x: Constant): Option[Char] =
    if (x.tag == Constants.CharTag) Some(x.charValue) else None
  def isConstant_Int(x: Constant): Option[Int] =
    if (x.tag == Constants.IntTag) Some(x.intValue) else None
  def isConstant_Long(x: Constant): Option[Long] =
    if (x.tag == Constants.LongTag) Some(x.longValue) else None
  def isConstant_Float(x: Constant): Option[Float] =
    if (x.tag == Constants.FloatTag) Some(x.floatValue) else None
  def isConstant_Double(x: Constant): Option[Double] =
    if (x.tag == Constants.DoubleTag) Some(x.doubleValue) else None
  def isConstant_String(x: Constant): Option[String] =
    if (x.tag == Constants.StringTag) Some(x.stringValue) else None
  def isConstant_ClassTag(x: Constant): Option[Type] =
    if (x.tag == Constants.ClazzTag) Some(x.typeValue) else None
  def isConstant_Symbol(x: Constant): Option[scala.Symbol] =
    if (x.tag == Constants.ScalaSymbolTag) Some(x.scalaSymbolValue) else None

  def Constant_Unit_apply(): Constant = Constants.Constant(())
  def Constant_Null_apply(): Constant = Constants.Constant(null)
  def Constant_Boolean_apply(x: Boolean): Constant = Constants.Constant(x)
  def Constant_Byte_apply(x: Byte): Constant = Constants.Constant(x)
  def Constant_Short_apply(x: Short): Constant = Constants.Constant(x)
  def Constant_Char_apply(x: Char): Constant = Constants.Constant(x)
  def Constant_Int_apply(x: Int): Constant = Constants.Constant(x)
  def Constant_Long_apply(x: Long): Constant = Constants.Constant(x)
  def Constant_Float_apply(x: Float): Constant = Constants.Constant(x)
  def Constant_Double_apply(x: Double): Constant = Constants.Constant(x)
  def Constant_String_apply(x: String): Constant = Constants.Constant(x)
  def Constant_ClassTag_apply(x: scala.reflect.ClassTag[_]): Constant = Constants.Constant(x)
  def Constant_Symbol_apply(x: scala.Symbol): Constant = Constants.Constant(x)

  //
  // SYMBOLS
  //

  type Symbol = core.Symbols.Symbol

  def Symbol_owner(self: Symbol)(implicit ctx: Context): Symbol = self.owner

  def Symbol_flags(self: Symbol)(implicit ctx: Context): Flags = self.flags

  def Symbol_privateWithin(self: Symbol)(implicit ctx: Context): Option[Type] = {
    val within = self.privateWithin
    if (within.exists && !self.is(core.Flags.Protected)) Some(within.typeRef)
    else None
  }

  def Symbol_protectedWithin(self: Symbol)(implicit ctx: Context): Option[Type] = {
    val within = self.privateWithin
    if (within.exists && self.is(core.Flags.Protected)) Some(within.typeRef)
    else None
  }

  def Symbol_name(self: Symbol)(implicit ctx: Context): String = self.name.toString

  def Symbol_fullName(self: Symbol)(implicit ctx: Context): String = self.fullName.toString

  def Symbol_pos(self: Symbol)(implicit ctx: Context): Position = self.sourcePos

  def Symbol_localContext(self: Symbol)(implicit ctx: Context): Context = {
    if (self.exists) ctx.withOwner(self)
    else ctx
  }

  def Symbol_annots(self: Symbol)(implicit ctx: Context): List[Term] = {
    self.annotations.flatMap {
      case _: core.Annotations.LazyBodyAnnotation => Nil
      case annot => annot.tree :: Nil
    }
  }

  def Symbol_isDefinedInCurrentRun(self: Symbol)(implicit ctx: Context): Boolean =
    self.topLevelClass.asClass.isDefinedInCurrentRun

  def Symbol_isLocalDummy(self: Symbol)(implicit ctx: Context): Boolean = self.isLocalDummy
  def Symbol_isRefinementClass(self: Symbol)(implicit ctx: Context): Boolean = self.isRefinementClass
  def Symbol_isAliasType(self: Symbol)(implicit ctx: Context): Boolean = self.isAliasType
  def Symbol_isAnonymousClass(self: Symbol)(implicit ctx: Context): Boolean = self.isAnonymousClass
  def Symbol_isAnonymousFunction(self: Symbol)(implicit ctx: Context): Boolean = self.isAnonymousFunction
  def Symbol_isAbstractType(self: Symbol)(implicit ctx: Context): Boolean = self.isAbstractType
  def Symbol_isClassConstructor(self: Symbol)(implicit ctx: Context): Boolean = self.isClassConstructor

  type PackageSymbol = core.Symbols.Symbol

  def isPackageSymbol(symbol: Symbol)(implicit ctx: Context): Option[PackageSymbol] =
    if (symbol.is(core.Flags.Package)) Some(symbol) else None

  type ClassSymbol = core.Symbols.ClassSymbol

  def isClassSymbol(symbol: Symbol)(implicit ctx: Context): Option[ClassSymbol] =
    if (symbol.isClass) Some(symbol.asClass) else None

  type TypeSymbol = core.Symbols.TypeSymbol

  def isTypeSymbol(symbol: Symbol)(implicit ctx: Context): Option[TypeSymbol] =
    if (symbol.isType) Some(symbol.asType) else None

  type DefSymbol = core.Symbols.TermSymbol

  def isDefSymbol(symbol: Symbol)(implicit ctx: Context): Option[DefSymbol] =
    if (symbol.isTerm && symbol.is(core.Flags.Method)) Some(symbol.asTerm) else None

  type BindSymbol = core.Symbols.TermSymbol

  def isBindSymbol(symbol: Symbol)(implicit ctx: Context): Option[BindSymbol] =
    if (symbol.isTerm && symbol.is(core.Flags.Case)) Some(symbol.asTerm) else None

  type ValSymbol = core.Symbols.TermSymbol

  def isValSymbol(symbol: Symbol)(implicit ctx: Context): Option[ValSymbol] =
    if (symbol.isTerm && !symbol.is(core.Flags.Method) && !symbol.is(core.Flags.Case)) Some(symbol.asTerm) else None

  type NoSymbol = core.Symbols.NoSymbol.type

  def isNoSymbol(symbol: Symbol)(implicit ctx: Context): Boolean = symbol ne core.Symbols.NoSymbol

  //
  // FLAGS
  //

  type Flags = core.Flags.FlagSet

  /** Is the given flag set a subset of this flag sets */
  def Flags_is(self: Flags)(that: Flags): Boolean = self.is(that)

  /** Union of the two flag sets */
  def Flags_or(self: Flags)(that: Flags): Flags = self | that

  /** Intersection of the two flag sets */
  def Flags_and(self: Flags)(that: Flags): Flags = self & that

  def Flags_Private: Flags = core.Flags.Private
  def Flags_Protected: Flags = core.Flags.Protected
  def Flags_Abstract: Flags = core.Flags.Abstract
  def Flags_Final: Flags = core.Flags.Final
  def Flags_Sealed: Flags = core.Flags.Sealed
  def Flags_Case: Flags = core.Flags.Case
  def Flags_Implicit: Flags = core.Flags.Implicit
  def Flags_Implied: Flags = core.Flags.Implied
  def Flags_Erased: Flags = core.Flags.Erased
  def Flags_Lazy: Flags = core.Flags.Lazy
  def Flags_Override: Flags = core.Flags.Override
  def Flags_Inline: Flags = core.Flags.Inline
  def Flags_Macro: Flags = core.Flags.Macro
  def Flags_Static: Flags = core.Flags.JavaStatic
  def Flags_JavaDefined: Flags = core.Flags.JavaDefined
  def Flags_Object: Flags = core.Flags.Module
  def Flags_Trait: Flags = core.Flags.Trait
  def Flags_Local: Flags = core.Flags.Local
  def Flags_Synthetic: Flags = core.Flags.Synthetic
  def Flags_Artifact: Flags = core.Flags.Artifact
  def Flags_Mutable: Flags = core.Flags.Mutable
  def Flags_FieldAccessor: Flags = core.Flags.Accessor
  def Flags_CaseAcessor: Flags = core.Flags.CaseAccessor
  def Flags_Covariant: Flags = core.Flags.Covariant
  def Flags_Contravariant: Flags = core.Flags.Contravariant
  def Flags_Scala2X: Flags = core.Flags.Scala2x
  def Flags_DefaultParameterized: Flags = core.Flags.DefaultParameterized
  def Flags_StableRealizable: Flags = core.Flags.StableRealizable
  def Flags_Param: Flags = core.Flags.Param
  def Flags_ParamAccessor: Flags = core.Flags.ParamAccessor
  def Flags_Enum: Flags = core.Flags.Enum
  def Flags_ModuleClass: Flags = core.Flags.ModuleClass
  def Flags_PrivateLocal: Flags = core.Flags.PrivateLocal
  def Flags_Package: Flags = core.Flags.Package
  def Flags_ImplClass: Flags = core.Flags.ImplClass

  //
  // QUOTED SEAL/UNSEAL
  //

  /** View this expression `Expr[_]` as a `Term` */
  def QuotedExpr_unseal(self: scala.quoted.Expr[_])(implicit ctx: Context): Term =
    PickledQuotes.quotedExprToTree(self)

  /** View this expression `Type[T]` as a `TypeTree` */
  def QuotedType_unseal(self: scala.quoted.Type[_])(implicit ctx: Context): TypeTree =
    PickledQuotes.quotedTypeToTree(self)

  /** Convert `Term` to an `Expr[T]` and check that it conforms to `T` */
  def QuotedExpr_seal[T](self: Term)(tpe: scala.quoted.Type[T])(implicit ctx: Context): scala.quoted.Expr[T] = {

    val expectedType = QuotedType_unseal(tpe).tpe

    def etaExpand(term: Term): Term = term.tpe.widen match {
      case mtpe: Types.MethodType if !mtpe.isParamDependent =>
        val closureResType = mtpe.resType match {
          case t: Types.MethodType => t.toFunctionType()
          case t => t
        }
        val closureTpe = Types.MethodType(mtpe.paramNames, mtpe.paramInfos, closureResType)
        val closureMethod = ctx.newSymbol(ctx.owner, nme.ANON_FUN, Synthetic | Method, closureTpe)
        tpd.Closure(closureMethod, tss => etaExpand(new tpd.TreeOps(term).appliedToArgs(tss.head)))
      case _ => term
    }

    val expanded = etaExpand(self)
    if (expanded.tpe <:< expectedType) {
      new scala.quoted.Exprs.TastyTreeExpr(expanded).asInstanceOf[scala.quoted.Expr[T]]
    } else {
      throw new scala.tasty.TastyTypecheckError(
        s"""Term: ${self.show}
           |did not conform to type: ${expectedType.show}
           |""".stripMargin
      )
    }
  }

  /** Convert `Type` to an `quoted.Type[T]` */
  def QuotedType_seal(self: Type)(implicit ctx: Context): scala.quoted.Type[_] = {
    val dummySpan = ctx.owner.span // FIXME
    new scala.quoted.Types.TreeType(tpd.TypeTree(self).withSpan(dummySpan))
  }

  //
  // DEFINITIONS
  //

  def Definitions_RootPackage: Symbol = defn.RootPackage
  def Definitions_RootClass: Symbol = defn.RootClass

  def Definitions_EmptyPackageClass: Symbol = defn.EmptyPackageClass

  def Definitions_ScalaPackage: Symbol = defn.ScalaPackageVal
  def Definitions_ScalaPackageClass: Symbol = defn.ScalaPackageClass

  def Definitions_AnyClass: Symbol = defn.AnyClass
  def Definitions_AnyValClass: Symbol = defn.AnyValClass
  def Definitions_ObjectClass: Symbol = defn.ObjectClass
  def Definitions_AnyRefClass: Symbol = defn.AnyRefAlias
  def Definitions_NullClass: Symbol = defn.AnyClass
  def Definitions_NothingClass: Symbol = defn.NothingClass
  def Definitions_UnitClass: Symbol = defn.UnitClass
  def Definitions_ByteClass: Symbol = defn.ByteClass
  def Definitions_ShortClass: Symbol = defn.ShortClass
  def Definitions_CharClass: Symbol = defn.CharClass
  def Definitions_IntClass: Symbol = defn.IntClass
  def Definitions_LongClass: Symbol = defn.LongClass
  def Definitions_FloatClass: Symbol = defn.FloatClass
  def Definitions_DoubleClass: Symbol = defn.DoubleClass
  def Definitions_BooleanClass: Symbol = defn.BooleanClass
  def Definitions_StringClass: Symbol = defn.StringClass
  def Definitions_ClassClass: Symbol = defn.ClassClass
  def Definitions_ArrayClass: Symbol = defn.ArrayClass
  def Definitions_PredefModule: Symbol = defn.ScalaPredefModule.asTerm

  def Definitions_JavaLangPackage: Symbol = defn.JavaLangPackageVal

  def Definitions_ArrayModule: Symbol = defn.ArrayClass.companionModule.asTerm

  def Definitions_Array_apply: Symbol = defn.Array_apply.asTerm
  def Definitions_Array_clone: Symbol = defn.Array_clone.asTerm
  def Definitions_Array_length: Symbol = defn.Array_length.asTerm
  def Definitions_Array_update: Symbol = defn.Array_update.asTerm

  def Definitions_RepeatedParamClass: Symbol = defn.RepeatedParamClass

  def Definitions_OptionClass: Symbol = defn.OptionClass
  def Definitions_NoneModule: Symbol = defn.NoneClass.companionModule.asTerm
  def Definitions_SomeModule: Symbol = defn.SomeClass.companionModule.asTerm

  def Definitions_ProductClass: Symbol = defn.ProductClass
  def Definitions_FunctionClass(arity: Int, isImplicit: Boolean, isErased: Boolean): Symbol =
    defn.FunctionClass(arity, isImplicit, isErased).asClass
  def Definitions_TupleClass(arity: Int): Symbol = defn.TupleType(arity).classSymbol.asClass

  def Definitions_UnitType: Type = defn.UnitType
  def Definitions_ByteType: Type = defn.ByteType
  def Definitions_ShortType: Type = defn.ShortType
  def Definitions_CharType: Type = defn.CharType
  def Definitions_IntType: Type = defn.IntType
  def Definitions_LongType: Type = defn.LongType
  def Definitions_FloatType: Type = defn.FloatType
  def Definitions_DoubleType: Type = defn.DoubleType
  def Definitions_BooleanType: Type = defn.BooleanType
  def Definitions_AnyType: Type = defn.AnyType
  def Definitions_AnyValType: Type = defn.AnyValType
  def Definitions_AnyRefType: Type = defn.AnyRefType
  def Definitions_ObjectType: Type = defn.ObjectType
  def Definitions_NothingType: Type = defn.NothingType
  def Definitions_NullType: Type = defn.NullType
  def Definitions_StringType: Type = defn.StringType

  //
  // HELPERS
  //

  private def optional[T <: Trees.Tree[_]](tree: T): Option[tree.type] =
    if (tree.isEmpty) None else Some(tree)

  private def withDefaultPos[T <: Tree](fn: Context => T)(implicit ctx: Context): T = {
    fn(ctx.withSource(rootPosition.source)).withSpan(rootPosition.span)
  }
}
