package scala.tasty

import scala.tasty.reflect._

// Keep doc in syncwith docs/docs/reference/tasty-reflect.md
/** Tasty reflect abstract types
 *
 *  ```none
 *
 *  +- Tree -+- PackageClause
 *           +- Import
 *           +- Statement -+- Definition --+- PackageDef
 *                         |               +- ClassDef
 *                         |               +- TypeDef
 *                         |               +- DefDef
 *                         |               +- ValDef
 *                         |
 *                         +- Term --------+- Ref -+- Ident
 *                                         |       +- Select
 *                                         |
 *                                         +- Literal
 *                                         +- This
 *                                         +- New
 *                                         +- NamedArg
 *                                         +- Apply
 *                                         +- TypeApply
 *                                         +- Super
 *                                         +- Typed
 *                                         +- Assign
 *                                         +- Block
 *                                         +- Lambda
 *                                         +- If
 *                                         +- Match
 *                                         +- Try
 *                                         +- Return
 *                                         +- Repeated
 *                                         +- Inlined
 *                                         +- SelectOuter
 *                                         +- While
 *
 *
 *                         +- TypeTree ----+- Inferred
 *                         |               +- Ident
 *                         |               +- Select
 *                         |               +- Project
 *                         |               +- Singleton
 *  +- TypeOrBoundsTree ---+               +- Refined
 *                         |               +- Applied
 *                         |               +- Annotated
 *                         |               +- MatchType
 *                         |               +- ByName
 *                         |               +- LambdaTypeTree
 *                         |               +- TypeBind
 *                         |               +- TypeBlock
 *                         |
 *                         +- TypeBoundsTree
 *                         +- WildcardTypeTree
 *
 *  +- CaseDef
 *  +- TypeCaseDef
 *
 *  +- Pattern --+- Value
 *               +- Bind
 *               +- Unapply
 *               +- Alternatives
 *               +- TypeTest
 *
 *
 *                   +- NoPrefix
 *  +- TypeOrBounds -+- TypeBounds
 *                   |
 *                   +- Type -------+- ConstantType
 *                                  +- SymRef
 *                                  +- TermRef
 *                                  +- TypeRef
 *                                  +- SuperType
 *                                  +- Refinement
 *                                  +- AppliedType
 *                                  +- AnnotatedType
 *                                  +- AndType
 *                                  +- OrType
 *                                  +- MatchType
 *                                  +- ByNameType
 *                                  +- ParamRef
 *                                  +- ThisType
 *                                  +- RecursiveThis
 *                                  +- RecursiveType
 *                                  +- LambdaType[ParamInfo <: TypeOrBounds] -+- MethodType
 *                                                                            +- PolyType
 *                                                                            +- TypeLambda
 *
 *  +- ImportSelector -+- SimpleSelector
 *                     +- RenameSelector
 *                     +- OmitSelector
 *
 *  +- Id
 *
 *  +- Signature
 *
 *  +- Position
 *
 *  +- Constant
 *
 *  +- Symbol --+- PackageSymbol
 *              +- ClassSymbol
 *              +- TypeSymbol
 *              +- DefSymbol
 *              +- ValSymbol
 *              +- BindSymbol
 *              +- NoSymbol
 *
 *  +- Flags
 *
 *  Aliases:
 *   # TermOrTypeTree = Term | TypeTree
 *
 *  ```
 */
abstract class Reflection { self =>

  //
  // KERNEL
  //

  val kernel: Kernel

  /** Root position of this tasty context. For macros it corresponds to the expansion site. */
  def rootPosition: Position = kernel.rootPosition

  //
  // CONTEXT
  //

  /** Compilation context */
  type Context = kernel.Context

  implicit class ContextAPI(self: Context) {
    /** Returns the owner of the context */
    def owner: Symbol = kernel.Context_owner(self)

    /** Returns the source file being compiled. The path is relative to the current working directory. */
    def source: java.nio.file.Path = kernel.Context_source(self)
  }

  /** Context of the macro expansion */
  implicit def ctx: Context = kernel.rootContext

  //
  // SETTINGS
  //

  /** Settings */
  type Settings = kernel.Settings

  /** Compiler settings */
  def settings: Settings = kernel.settings

  implicit class SettingsAPI(self: Settings) {
    /** Can print output using colors? */
    def color: Boolean = kernel.Settings_color(self)
  }

  //
  // TREES OR TYPE TREES
  //

  // TODO: When bootstrapped, remove `TermOrTypeTree` and use `Term | TypeTree` type directly in other files

  /** Workaround missing `|` types in Scala 2 to represent `Term | TypeTree` */
  type TermOrTypeTree /* Term | TypeTree */ = kernel.TermOrTypeTree

  implicit def termAsTermOrTypeTree(term: Term): TermOrTypeTree = term.asInstanceOf[TermOrTypeTree]
  implicit def typeTreeAsTermOrTypeTree(term: TypeTree): TermOrTypeTree = term.asInstanceOf[TermOrTypeTree]

  //
  // TREES
  //

  /** Tree representing code written in the source */
  type Tree = kernel.Tree

  implicit class TreeAPI(self: Tree) {

    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showTree(tree)

    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showTree(tree)

    /** Position in the source code */
    def pos(implicit ctx: Context): Position = kernel.Tree_pos(self)

    def symbol(implicit ctx: Context): Symbol = kernel.Tree_symbol(self)
  }

  /** Tree representing a pacakage clause in the source code */
  type PackageClause = kernel.PackageClause

  object IsPackageClause {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageClause] =
      kernel.isPackageClause(tree)
  }

  object PackageClause {
    def apply(pid: Term.Ref, stats: List[Tree])(implicit ctx: Context): PackageClause =
      kernel.PackageClause_apply(pid, stats)
    def copy(original: PackageClause)(pid: Term.Ref, stats: List[Tree])(implicit ctx: Context): PackageClause =
      kernel.PackageClause_copy(original)(pid, stats)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(Term.Ref, List[Tree])] =
      kernel.isPackageClause(tree).map(x => (x.pid, x.stats))
  }

  implicit class PackageClauseAPI(self: PackageClause) {
    def pid(implicit ctx: Context): Term.Ref = kernel.PackageClause_pid(self)
    def stats(implicit ctx: Context): List[Tree] = kernel.PackageClause_stats(self)
  }

  /** Tree representing a statement in the source code */
  type Statement = kernel.Statement

  object IsStatement {
    /** Matches any Statement and returns it */
    def unapply(tree: Tree)(implicit ctx: Context): Option[Statement] = kernel.isStatement(tree)
  }

  /** Tree representing an import in the source code */
  type Import = kernel.Import

  object IsImport {
    def unapply(tree: Tree)(implicit ctx: Context): Option[Import] =
      kernel.isImport(tree)
  }

  object Import {
    def apply(impliedOnly: Boolean, expr: Term, selectors: List[ImportSelector])(implicit ctx: Context): Import =
      kernel.Import_apply(impliedOnly, expr, selectors)
    def copy(original: Import)(impliedOnly: Boolean, expr: Term, selectors: List[ImportSelector])(implicit ctx: Context): Import =
      kernel.Import_copy(original)(impliedOnly, expr, selectors)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(Boolean, Term, List[ImportSelector])] =
      kernel.isImport(tree).map(x => (x.impliedOnly, x.expr, x.selectors))
  }

  implicit class ImportAPI(self: Import)  {
    def impliedOnly: Boolean = kernel.Import_impliedOnly(self)
    def expr(implicit ctx: Context): Term = kernel.Import_expr(self)
    def selectors(implicit ctx: Context): List[ImportSelector] =
      kernel.Import_selectors(self)
  }

  /** Tree representing a definition in the source code. It can be `PackageDef`, `ClassDef`, `TypeDef`, `DefDef` or `ValDef` */
  type Definition = kernel.Definition

  object IsDefinition {
    def unapply(tree: Tree)(implicit ctx: Context): Option[Definition] = kernel.isDefinition(tree)
  }

  implicit class DefinitionAPI(self: Definition) {
    def name(implicit ctx: Context): String = kernel.Definition_name(self)
  }

  /** Tree representing a package definition. This includes definitions in all source files */
  type PackageDef = kernel.PackageDef

  object IsPackageDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageDef] =
      kernel.isPackageDef(tree)
  }

  implicit class PackageDefAPI(self: PackageDef) {
    def owner(implicit ctx: Context): PackageDef = kernel.PackageDef_owner(self)
    def members(implicit ctx: Context): List[Statement] = kernel.PackageDef_members(self)
    def symbol(implicit ctx: Context): PackageSymbol = kernel.PackageDef_symbol(self)
  }

  object PackageDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, PackageDef)] =
      kernel.isPackageDef(tree).map(x => (x.name, x.owner))
  }

  /** Tree representing a class definition. This includes annonymus class definitions and the class of a module object */
  type ClassDef = kernel.ClassDef

  object IsClassDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ClassDef] = kernel.isClassDef(tree)
  }

  object ClassDef {
    // TODO def apply(name: String, constr: DefDef, parents: List[TermOrTypeTree], selfOpt: Option[ValDef], body: List[Statement])(implicit ctx: Context): ClassDef
    def copy(original: ClassDef)(name: String, constr: DefDef, parents: List[TermOrTypeTree], derived: List[TypeTree], selfOpt: Option[ValDef], body: List[Statement])(implicit ctx: Context): ClassDef =
      kernel.ClassDef_copy(original)(name, constr, parents, derived, selfOpt, body)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, DefDef, List[TermOrTypeTree], List[TypeTree], Option[ValDef], List[Statement])] =
      kernel.isClassDef(tree).map(x => (x.name, x.constructor, x.parents, x.derived, x.self, x.body))
  }

  implicit class ClassDefAPI(self: ClassDef) {
    def constructor(implicit ctx: Context): DefDef = kernel.ClassDef_constructor(self)
    def parents(implicit ctx: Context): List[TermOrTypeTree] = kernel.ClassDef_parents(self)
    def derived(implicit ctx: Context): List[TypeTree] = kernel.ClassDef_derived(self)
    def self(implicit ctx: Context): Option[ValDef] = kernel.ClassDef_self(self)
    def body(implicit ctx: Context): List[Statement] = kernel.ClassDef_body(self)
    def symbol(implicit ctx: Context): ClassSymbol = kernel.ClassDef_symbol(self)
  }

  /** Tree representing a type (paramter or member) definition in the source code */
  type TypeDef = kernel.TypeDef

  object IsTypeDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[TypeDef] = kernel.isTypeDef(tree)
  }

  object TypeDef {
    def apply(symbol: TypeSymbol)(implicit ctx: Context): TypeDef =
      kernel.TypeDef_apply(symbol)
    def copy(original: TypeDef)(name: String, rhs: TypeOrBoundsTree)(implicit ctx: Context): TypeDef =
      kernel.TypeDef_copy(original)(name, rhs)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeOrBoundsTree /* TypeTree | TypeBoundsTree */)] =
      kernel.isTypeDef(tree).map(x => (x.name, x.rhs))
  }

  implicit class TypeDefAPI(self: TypeDef) {
    def rhs(implicit ctx: Context): TypeOrBoundsTree = kernel.TypeDef_rhs(self)
    def symbol(implicit ctx: Context): TypeSymbol = kernel.TypeDef_symbol(self)
  }

  /** Tree representing a method definition in the source code */
  type DefDef = kernel.DefDef

  object IsDefDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[DefDef] = kernel.isDefDef(tree)
  }

  object DefDef {
    def apply(symbol: DefSymbol, rhsFn: List[Type] => List[List[Term]] => Option[Term])(implicit ctx: Context): DefDef =
      kernel.DefDef_apply(symbol, rhsFn)
    def copy(original: DefDef)(name: String, typeParams: List[TypeDef], paramss: List[List[ValDef]], tpt: TypeTree, rhs: Option[Term])(implicit ctx: Context): DefDef =
      kernel.DefDef_copy(original)(name, typeParams, paramss, tpt, rhs)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, List[TypeDef], List[List[ValDef]], TypeTree, Option[Term])] =
      kernel.isDefDef(tree).map(x => (x.name, x.typeParams, x.paramss, x.returnTpt, x.rhs))
  }

  implicit class DefDefAPI(self: DefDef) {
    def typeParams(implicit ctx: Context): List[TypeDef] = kernel.DefDef_typeParams(self)
    def paramss(implicit ctx: Context): List[List[ValDef]] = kernel.DefDef_paramss(self)
    def returnTpt(implicit ctx: Context): TypeTree = kernel.DefDef_returnTpt(self) // TODO rename to tpt
    def rhs(implicit ctx: Context): Option[Term] = kernel.DefDef_rhs(self)
    def symbol(implicit ctx: Context): DefSymbol = kernel.DefDef_symbol(self)
  }

  /** Tree representing a value definition in the source code This inclues `val`, `lazy val`, `var`, `object` and parameter defintions. */
  type ValDef = kernel.ValDef

  object IsValDef {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ValDef] = kernel.isValDef(tree)
  }

  object ValDef {
    def apply(symbol: ValSymbol, rhs: Option[Term])(implicit ctx: Context): ValDef =
      kernel.ValDef_apply(symbol, rhs)
    def copy(original: ValDef)(name: String, tpt: TypeTree, rhs: Option[Term])(implicit ctx: Context): ValDef =
      kernel.ValDef_copy(original)(name, tpt, rhs)
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeTree, Option[Term])] =
      kernel.isValDef(tree).map(x => (x.name, x.tpt, x.rhs))
  }

  implicit class ValDefAPI(self: ValDef) {
    def tpt(implicit ctx: Context): TypeTree = kernel.ValDef_tpt(self)
    def rhs(implicit ctx: Context): Option[Term] = kernel.ValDef_rhs(self)
    def symbol(implicit ctx: Context): ValSymbol = kernel.ValDef_symbol(self)
  }

  /** Tree representing an expression in the source code */
  type Term = kernel.Term

  implicit class TermAPI(self: Term) {
    def tpe(implicit ctx: Context): Type = kernel.Term_tpe(self)
    def pos(implicit ctx: Context): Position = kernel.Term_pos(self)
    def underlyingArgument(implicit ctx: Context): Term = kernel.Term_underlyingArgument(self)
    def underlying(implicit ctx: Context): Term = kernel.Term_underlying(self)
  }

  object IsTerm {
    /** Matches any term */
    def unapply(tree: Tree)(implicit ctx: Context): Option[Term] =
      kernel.isTerm(tree)

    /** Matches any term */
    def unapply(parent: TermOrTypeTree)(implicit ctx: Context, dummy: DummyImplicit): Option[Term] =
      kernel.isTermNotTypeTree(parent)
  }

  /** Trees representing an expression in the source code */
  /** Scala term. Any tree that can go in expression position. */
  object Term extends TermCoreModule { // TODO merge with TermCoreModule

    object IsIdent {
      /** Matches any Ident and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Ident] = kernel.isTerm_Ident(tree)
    }

    object Ref {

      /** Create a reference tree */
      def apply(sym: Symbol)(implicit ctx: Context): Ref =
        kernel.Term_Ref_apply(sym)

      // TODO def copy(original: Tree)(name: String)(implicit ctx: Context): Ref

    }

    /** Scala term identifier */
    object Ident {
      def apply(tmref: TermRef)(implicit ctx: Context): Term =
        kernel.Term_Ident_apply(tmref)

      def copy(original: Tree)(name: String)(implicit ctx: Context): Ident =
        kernel.Term_Ident_copy(original)(name)

      /** Matches a term identifier and returns its name */
      def unapply(tree: Tree)(implicit ctx: Context): Option[String] =
        kernel.isTerm_Ident(tree).map(_.name)
    }

    object IsSelect {
      /** Matches any Select and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Select] = kernel.isTerm_Select(tree)
    }

    /** Scala term selection */
    object Select {
      /** Select a field or a non-overloaded method by name
       *
       *  @note The method will produce an assertion error if the selected
       *        method is overloaded. The method `overloaded` should be used
       *        in that case.
       */
      def unique(qualifier: Term, name: String)(implicit ctx: Context): Select =
        kernel.Term_Select_unique(qualifier, name)

      // TODO rename, this returns an Apply and not a Select
      /** Call an overloaded method with the given type and term parameters */
      def overloaded(qualifier: Term, name: String, targs: List[Type], args: List[Term])(implicit ctx: Context): Apply =
        kernel.Term_Select_overloaded(qualifier, name, targs, args)

      def copy(original: Tree)(qualifier: Term, name: String)(implicit ctx: Context): Select =
        kernel.Term_Select_copy(original)(qualifier, name)

      /** Matches `<qualifier: Term>.<name: String>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, String)] =
        kernel.isTerm_Select(tree).map(x => (x.qualifier, x.name))
    }

    object IsLiteral {
      /** Matches any Literal and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Literal] = kernel.isTerm_Literal(tree)
    }

    /** Scala literal constant */
    object Literal {

      /** Create a literal constant */
      def apply(constant: Constant)(implicit ctx: Context): Literal =
        kernel.Term_Literal_apply(constant)

      def copy(original: Tree)(constant: Constant)(implicit ctx: Context): Literal =
        kernel.Term_Literal_copy(original)(constant)

      /** Matches a literal constant */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Constant] =
        kernel.isTerm_Literal(tree).map(_.constant)
    }

    object IsThis {
      /** Matches any This and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[This] = kernel.isTerm_This(tree)
    }

    /** Scala `this` or `this[id]` */
    object This {

      /** Create a `this[<id: Id]>` */
      def apply(cls: ClassSymbol)(implicit ctx: Context): This =
        kernel.Term_This_apply(cls)

      def copy(original: Tree)(qual: Option[Id])(implicit ctx: Context): This =
        kernel.Term_This_copy(original)(qual)

      /** Matches `this[<id: Option[Id]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Option[Id]] =
        kernel.isTerm_This(tree).map(_.id)

    }

    object IsNew {
      /** Matches any New and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[New] = kernel.isTerm_New(tree)
    }

    /** Scala `new` */
    object New {

      /** Create a `new <tpt: TypeTree>` */
      def apply(tpt: TypeTree)(implicit ctx: Context): New =
        kernel.Term_New_apply(tpt)

      def copy(original: Tree)(tpt: TypeTree)(implicit ctx: Context): New =
        kernel.Term_New_copy(original)(tpt)

      /** Matches a `new <tpt: TypeTree>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[TypeTree] =
        kernel.isTerm_New(tree).map(_.tpt)
    }

    object IsNamedArg {
      /** Matches any NamedArg and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[NamedArg] = kernel.isTerm_NamedArg(tree)
    }

    /** Scala named argument `x = y` in argument position */
    object NamedArg {

      /** Create a named argument `<name: String> = <value: Term>` */
      def apply(name: String, arg: Term)(implicit ctx: Context): NamedArg =
        kernel.Term_NamedArg_apply(name, arg)

      def copy(original: NamedArg)(name: String, arg: Term)(implicit ctx: Context): NamedArg =
        kernel.Term_NamedArg_copy(original)(name, arg)

      /** Matches a named argument `<name: String> = <value: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(String, Term)] =
        kernel.isTerm_NamedArg(tree).map(x => (x.name, x.value))

    }

    object IsApply {
      /** Matches any Apply and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Apply] = kernel.isTerm_Apply(tree)
    }

    /** Scala parameter application */
    object Apply {

      /** Create a function application `<fun: Term>(<args: List[Term]>)` */
      def apply(fun: Term, args: List[Term])(implicit ctx: Context): Apply =
        kernel.Term_Apply_apply(fun, args)

      def copy(original: Tree)(fun: Term, args: List[Term])(implicit ctx: Context): Apply =
        kernel.Term_Apply_copy(original)(fun, args)

      /** Matches a function application `<fun: Term>(<args: List[Term]>)` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[Term])] =
        kernel.isTerm_Apply(tree).map(x => (x.fun, x.args))
    }

    object IsTypeApply {
      /** Matches any TypeApply and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[TypeApply] =
        kernel.isTerm_TypeApply(tree)
    }

    /** Scala type parameter application */
    object TypeApply {

      /** Create a function type application `<fun: Term>[<args: List[TypeTree]>]` */
      def apply(fun: Term, args: List[TypeTree])(implicit ctx: Context): TypeApply =
        kernel.Term_TypeApply_apply(fun, args)

      def copy(original: Tree)(fun: Term, args: List[TypeTree])(implicit ctx: Context): TypeApply =
        kernel.Term_TypeApply_copy(original)(fun, args)

      /** Matches a function type application `<fun: Term>[<args: List[TypeTree]>]` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[TypeTree])] =
        kernel.isTerm_TypeApply(tree).map(x => (x.fun, x.args))

    }

    object IsSuper {
      /** Matches any Super and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Super] = kernel.isTerm_Super(tree)
    }

    /** Scala `x.super` or `x.super[id]` */
    object Super {

      /** Creates a `<qualifier: Term>.super[<id: Option[Id]>` */
      def apply(qual: Term, mix: Option[Id])(implicit ctx: Context): Super =
        kernel.Term_Super_apply(qual, mix)

      def copy(original: Tree)(qual: Term, mix: Option[Id])(implicit ctx: Context): Super =
        kernel.Term_Super_copy(original)(qual, mix)

      /** Matches a `<qualifier: Term>.super[<id: Option[Id]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Option[Id])] =
        kernel.isTerm_Super(tree).map(x => (x.qualifier, x.id))
    }

    object IsTyped {
      /** Matches any Typed and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Typed] = kernel.isTerm_Typed(tree)
    }

    /** Scala ascription `x: T` */
    object Typed {

      /** Create a type ascription `<x: Term>: <tpt: TypeTree>` */
      def apply(expr: Term, tpt: TypeTree)(implicit ctx: Context): Typed =
        kernel.Term_Typed_apply(expr, tpt)

      def copy(original: Tree)(expr: Term, tpt: TypeTree)(implicit ctx: Context): Typed =
        kernel.Term_Typed_copy(original)(expr, tpt)

      /** Matches `<expr: Term>: <tpt: TypeTree>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, TypeTree)] =
        kernel.isTerm_Typed(tree).map(x => (x.expr, x.tpt))

    }

    object IsAssign {
      /** Matches any Assign and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Assign] = kernel.isTerm_Assign(tree)
    }

    /** Scala assign `x = y` */
    object Assign {

      /** Create an assignment `<lhs: Term> = <rhs: Term>` */
      def apply(lhs: Term, rhs: Term)(implicit ctx: Context): Assign =
        kernel.Term_Assign_apply(lhs, rhs)

      def copy(original: Tree)(lhs: Term, rhs: Term)(implicit ctx: Context): Assign =
        kernel.Term_Assign_copy(original)(lhs, rhs)

      /** Matches an assignment `<lhs: Term> = <rhs: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term)] =
        kernel.isTerm_Assign(tree).map(x => (x.lhs, x.rhs))
    }

    object IsBlock {
      /** Matches any Block and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Block] = kernel.isTerm_Block(tree)
    }

    /** Scala code block `{ stat0; ...; statN; expr }` term */
    object Block {

      /** Creates a block `{ <statements: List[Statement]>; <expr: Term> }` */
      def apply(stats: List[Statement], expr: Term)(implicit ctx: Context): Block =
        kernel.Term_Block_apply(stats, expr)

      def copy(original: Tree)(stats: List[Statement], expr: Term)(implicit ctx: Context): Block =
        kernel.Term_Block_copy(original)(stats, expr)

      /** Matches a block `{ <statements: List[Statement]>; <expr: Term> }` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(List[Statement], Term)] =
        kernel.isTerm_Block(tree).map(x => (x.statements, x.expr))
    }

    object IsLambda {
      /** Matches any Lambda and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Lambda] = kernel.isTerm_Lambda(tree)
    }

    object Lambda {

      def apply(meth: Term, tpt: Option[TypeTree])(implicit ctx: Context): Lambda =
        kernel.Term_Lambda_apply(meth, tpt)

      def copy(original: Tree)(meth: Tree, tpt: Option[TypeTree])(implicit ctx: Context): Lambda =
        kernel.Term_Lambda_copy(original)(meth, tpt)

      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Option[TypeTree])] =
        kernel.isTerm_Lambda(tree).map(x => (x.meth, x.tptOpt))
    }

    object IsIf {
      /** Matches any If and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[If] = kernel.isTerm_If(tree)
    }

    /** Scala `if`/`else` term */
    object If {

      /** Create an if/then/else `if (<cond: Term>) <thenp: Term> else <elsep: Term>` */
      def apply(cond: Term, thenp: Term, elsep: Term)(implicit ctx: Context): If =
        kernel.Term_If_apply(cond, thenp, elsep)

      def copy(original: Tree)(cond: Term, thenp: Term, elsep: Term)(implicit ctx: Context): If =
        kernel.Term_If_copy(original)(cond, thenp, elsep)

      /** Matches an if/then/else `if (<cond: Term>) <thenp: Term> else <elsep: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term, Term)] =
        kernel.isTerm_If(tree).map(x => (x.cond, x.thenp, x.elsep))

    }

    object IsMatch {
      /** Matches any Match and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Match] = kernel.isTerm_Match(tree)
    }

    /** Scala `match` term */
    object Match {

      /** Creates a pattern match `<scrutinee: Term> match { <cases: List[CaseDef]> }` */
      def apply(selector: Term, cases: List[CaseDef])(implicit ctx: Context): Match =
        kernel.Term_Match_apply(selector, cases)

      def copy(original: Tree)(selector: Term, cases: List[CaseDef])(implicit ctx: Context): Match =
        kernel.Term_Match_copy(original)(selector, cases)

      /** Matches a pattern match `<scrutinee: Term> match { <cases: List[CaseDef]> }` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[CaseDef])] =
        kernel.isTerm_Match(tree).map(x => (x.scrutinee, x.cases))

    }

    object IsTry {
      /** Matches any Try and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Try] = kernel.isTerm_Try(tree)
    }

    /** Scala `try`/`catch`/`finally` term */
    object Try {

      /** Create a try/catch `try <body: Term> catch { <cases: List[CaseDef]> } finally <finalizer: Option[Term]>` */
      def apply(expr: Term, cases: List[CaseDef], finalizer: Option[Term])(implicit ctx: Context): Try =
        kernel.Term_Try_apply(expr, cases, finalizer)

      def copy(original: Tree)(expr: Term, cases: List[CaseDef], finalizer: Option[Term])(implicit ctx: Context): Try =
        kernel.Term_Try_copy(original)(expr, cases, finalizer)

      /** Matches a try/catch `try <body: Term> catch { <cases: List[CaseDef]> } finally <finalizer: Option[Term]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[CaseDef], Option[Term])] =
        kernel.isTerm_Try(tree).map(x => (x.body, x.cases, x.finalizer))

    }

    object IsReturn {
      /** Matches any Return and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Return] = kernel.isTerm_Return(tree)
    }

    /** Scala local `return` */
    object Return {

      /** Creates `return <expr: Term>` */
      def apply(expr: Term)(implicit ctx: Context): Return =
        kernel.Term_Return_apply(expr)

      def copy(original: Tree)(expr: Term)(implicit ctx: Context): Return =
        kernel.Term_Return_copy(original)(expr)

      /** Matches `return <expr: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Term] =
        kernel.isTerm_Return(tree).map(_.expr)

    }

    object IsRepeated {
      /** Matches any Repeated and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Repeated] = kernel.isTerm_Repeated(tree)
    }

    object Repeated {

      def apply(elems: List[Term], tpt: TypeTree)(implicit ctx: Context): Repeated =
        kernel.Term_Repeated_apply(elems, tpt)

      def copy(original: Tree)(elems: List[Term], tpt: TypeTree)(implicit ctx: Context): Repeated =
        kernel.Term_Repeated_copy(original)(elems, tpt)

      def unapply(tree: Tree)(implicit ctx: Context): Option[(List[Term], TypeTree)] =
        kernel.isTerm_Repeated(tree).map(x => (x.elems, x.elemtpt))

    }

    object IsInlined {
      /** Matches any Inlined and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Inlined] = kernel.isTerm_Inlined(tree)
    }

    object Inlined {

      def apply(call: Option[TermOrTypeTree], bindings: List[Definition], expansion: Term)(implicit ctx: Context): Inlined =
        kernel.Term_Inlined_apply(call, bindings, expansion)

      def copy(original: Tree)(call: Option[TermOrTypeTree], bindings: List[Definition], expansion: Term)(implicit ctx: Context): Inlined =
        kernel.Term_Inlined_copy(original)(call, bindings, expansion)

      def unapply(tree: Tree)(implicit ctx: Context): Option[(Option[TermOrTypeTree], List[Definition], Term)] =
        kernel.isTerm_Inlined(tree).map(x => (x.call, x.bindings, x.body))

    }

    object IsSelectOuter {
      /** Matches any SelectOuter and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[SelectOuter] = kernel.isTerm_SelectOuter(tree)
    }

    object SelectOuter {

      def apply(qualifier: Term, name: String, levels: Int)(implicit ctx: Context): SelectOuter =
        kernel.Term_SelectOuter_apply(qualifier, name, levels)

      def copy(original: Tree)(qualifier: Term, name: String, levels: Int)(implicit ctx: Context): SelectOuter =
        kernel.Term_SelectOuter_copy(original)(qualifier, name, levels)

      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Int, Type)] = // TODO homogenize order of parameters
        kernel.isTerm_SelectOuter(tree).map(x => (x.qualifier, x.level, x.tpe))

    }

    object IsWhile {
      /** Matches any While and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[While] = kernel.isTerm_While(tree)
    }

    object While {

      /** Creates a while loop `while (<cond>) <body>` and returns (<cond>, <body>) */
      def apply(cond: Term, body: Term)(implicit ctx: Context): While =
        kernel.Term_While_apply(cond, body)

      def copy(original: Tree)(cond: Term, body: Term)(implicit ctx: Context): While =
        kernel.Term_While_copy(original)(cond, body)

      /** Extractor for while loops. Matches `while (<cond>) <body>` and returns (<cond>, <body>) */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term)] =
        kernel.isTerm_While(tree).map(x => (x.cond, x.body))

    }
  }

  /** Trees representing an expression in the source code */
  trait TermCoreModule {

    /** Tree representing a reference to definition */
    type Ref = kernel.Term_Ref

    /** Tree representing a reference to definition with a given name */
    type Ident = kernel.Term_Ident

    /** Tree representing a selection of definition with a given name on a given prefix */
    type Select = kernel.Term_Select

    /** Tree representing a literal value in the source code */
    type Literal = kernel.Term_Literal

    /** Tree representing `this` in the source code */
    type This = kernel.Term_This

    /** Tree representing `new` in the source code */
    type New = kernel.Term_New

    /** Tree representing an argument passed with an explicit name. Such as `arg1 = x` in `foo(arg1 = x)` */
    type NamedArg = kernel.Term_NamedArg

    /** Tree an application of arguments. It represents a single list of arguments, multiple argument lists will have nested `Apply`s  */
    type Apply = kernel.Term_Apply

    /** Tree an application of type arguments */
    type TypeApply = kernel.Term_TypeApply

    /** Tree representing `super` in the source code */
    type Super = kernel.Term_Super

    /** Tree representing a type ascription `x: T` in the source code */
    type Typed = kernel.Term_Typed

    /** Tree representing an assignment `x = y` in the source code */
    type Assign = kernel.Term_Assign

    /** Tree representing a block `{ ... }` in the source code */
    type Block = kernel.Term_Block

    /** Tree representing a lambda `(...) => ...` in the source code */
    type Lambda = kernel.Term_Lambda

    /** Tree representing an if/then/else `if (...) ... else ...` in the source code */
    type If = kernel.Term_If

    /** Tree representing a pattern match `x match  { ... }` in the source code */
    type Match = kernel.Term_Match

    /** Tree representing a tyr catch `try x catch { ... } finally { ... }` in the source code */
    type Try = kernel.Term_Try

    /** Tree representing a `return` in the source code */
    type Return = kernel.Term_Return

    /** Tree representing a variable argument list in the source code */
    type Repeated = kernel.Term_Repeated

    /** Tree representing the scope of an inlined tree */
    type Inlined = kernel.Term_Inlined

    /** Tree representing a selection of definition with a given name on a given prefix and number of nested scopes of inlined trees */
    type SelectOuter = kernel.Term_SelectOuter

    /** Tree representing a while loop */
    type While = kernel.Term_While

  }

  implicit class Term_IdentAPI(self: Term.Ident) {
    def name(implicit ctx: Context): String = kernel.Term_Ident_name(self)
  }

  implicit class Term_SelectAPI(self: Term.Select) {
    def qualifier(implicit ctx: Context): Term = kernel.Term_Select_qualifier(self)
    def name(implicit ctx: Context): String = kernel.Term_Select_name(self)
    def signature(implicit ctx: Context): Option[Signature] = kernel.Term_Select_signature(self)
  }

  implicit class Term_LiteralAPI(self: Term.Literal) {
    def constant(implicit ctx: Context): Constant = kernel.Term_Literal_constant(self)
  }

  implicit class Term_ThisAPI(self: Term.This) {
    def id(implicit ctx: Context): Option[Id] = kernel.Term_This_id(self)
  }

  implicit class Term_NewAPI(self: Term.New) {
    def tpt(implicit ctx: Context): TypeTree = kernel.Term_New_tpt(self)
  }

  implicit class Term_NamedArgAPI(self: Term.NamedArg) {
    def name(implicit ctx: Context): String = kernel.Term_NamedArg_name(self)
    def value(implicit ctx: Context): Term = kernel.Term_NamedArg_value(self)
  }

  implicit class Term_ApplyAPI(self: Term.Apply) {
    def fun(implicit ctx: Context): Term = kernel.Term_Apply_fun(self)
    def args(implicit ctx: Context): List[Term] = kernel.Term_Apply_args(self)
  }

  implicit class Term_TypeApplyAPI(self: Term.TypeApply) {
    def fun(implicit ctx: Context): Term = kernel.Term_TypeApply_fun(self)
    def args(implicit ctx: Context): List[TypeTree] = kernel.Term_TypeApply_args(self)
  }

  implicit class Term_SuperAPI(self: Term.Super) {
    def qualifier(implicit ctx: Context): Term = kernel.Term_Super_qualifier(self)
    def id(implicit ctx: Context): Option[Id] = kernel.Term_Super_id(self)
  }

  implicit class Term_TypedAPI(self: Term.Typed) {
    def expr(implicit ctx: Context): Term = kernel.Term_Typed_expr(self)
    def tpt(implicit ctx: Context): TypeTree = kernel.Term_Typed_tpt(self)
  }

  implicit class Term_AssignAPI(self: Term.Assign) {
    def lhs(implicit ctx: Context): Term = kernel.Term_Assign_lhs(self)
    def rhs(implicit ctx: Context): Term = kernel.Term_Assign_rhs(self)
  }

  implicit class Term_BlockAPI(self: Term.Block) {
    def statements(implicit ctx: Context): List[Statement] = kernel.Term_Block_statements(self)
    def expr(implicit ctx: Context): Term = kernel.Term_Block_expr(self)
  }

  implicit class Term_LambdaAPI(self: Term.Lambda) {
    def meth(implicit ctx: Context): Term = kernel.Term_Lambda_meth(self)
    def tptOpt(implicit ctx: Context): Option[TypeTree] = kernel.Term_Lambda_tptOpt(self)
  }

  implicit class Term_IfAPI(self: Term.If) {
    def cond(implicit ctx: Context): Term = kernel.Term_If_cond(self)
    def thenp(implicit ctx: Context): Term = kernel.Term_If_thenp(self)
    def elsep(implicit ctx: Context): Term = kernel.Term_If_elsep(self)
  }

  implicit class Term_MatchAPI(self: Term.Match) {
    def scrutinee(implicit ctx: Context): Term = kernel.Term_Match_scrutinee(self)
    def cases(implicit ctx: Context): List[CaseDef] = kernel.Term_Match_cases(self)
  }

  implicit class Term_TryAPI(self: Term.Try) {
    def body(implicit ctx: Context): Term = kernel.Term_Try_body(self)
    def cases(implicit ctx: Context): List[CaseDef] = kernel.Term_Try_cases(self)
    def finalizer(implicit ctx: Context): Option[Term] = kernel.Term_Try_finalizer(self)
  }

  implicit class Term_ReturnAPI(self: Term.Return) {
    def expr(implicit ctx: Context): Term = kernel.Term_Return_expr(self)
  }

  implicit class Term_RepeatedAPI(self: Term.Repeated) {
    def elems(implicit ctx: Context): List[Term] = kernel.Term_Repeated_elems(self)
    def elemtpt(implicit ctx: Context): TypeTree = kernel.Term_Repeated_elemtpt(self)
  }

  implicit class Term_InlinedAPI(self: Term.Inlined) {
    def call(implicit ctx: Context): Option[TermOrTypeTree] = kernel.Term_Inlined_call(self)
    def bindings(implicit ctx: Context): List[Definition] = kernel.Term_Inlined_bindings(self)
    def body(implicit ctx: Context): Term = kernel.Term_Inlined_body(self)
  }

  implicit class Term_SelectOuterAPI(self: Term.SelectOuter) {
    def qualifier(implicit ctx: Context): Term = kernel.Term_SelectOuter_qualifier(self)
    def level(implicit ctx: Context): Int = kernel.Term_SelectOuter_level(self)
    def tpe(implicit ctx: Context): Type = kernel.Term_SelectOuter_tpe(self)
  }

  implicit class Term_WhileAPI(self: Term.While) {
    def cond(implicit ctx: Context): Term = kernel.Term_While_cond(self)
    def body(implicit ctx: Context): Term = kernel.Term_While_body(self)
  }

  //
  // CASE DEFS
  //

  /** Branch of a pattern match or catch clause */
  type CaseDef = kernel.CaseDef

  implicit class CaseDefAPI(caseDef: CaseDef) {
    def pattern(implicit ctx: Context): Pattern = kernel.CaseDef_pattern(caseDef)
    def guard(implicit ctx: Context): Option[Term] = kernel.CaseDef_guard(caseDef)
    def rhs(implicit ctx: Context): Term = kernel.CaseDef_rhs(caseDef)
  }

  object CaseDef {
    def apply(pattern: Pattern, guard: Option[Term], rhs: Term)(implicit ctx: Context): CaseDef =
      kernel.CaseDef_module_apply(pattern, guard, rhs)

    def copy(original: CaseDef)(pattern: Pattern, guard: Option[Term], rhs: Term)(implicit ctx: Context): CaseDef =
      kernel.CaseDef_module_copy(original)(pattern, guard, rhs)

    def unapply(x: CaseDef)(implicit ctx: Context): Option[(Pattern, Option[Term], Term)] =
      Some((x.pattern, x.guard, x.rhs))
  }

  /** Branch of a type pattern match */
  type TypeCaseDef = kernel.TypeCaseDef

  implicit class TypeCaseDefAPI(caseDef: TypeCaseDef) {
    def pattern(implicit ctx: Context): TypeTree = kernel.TypeCaseDef_pattern(caseDef)
    def rhs(implicit ctx: Context): TypeTree = kernel.TypeCaseDef_rhs(caseDef)
  }

  object TypeCaseDef {
    def apply(pattern: TypeTree, rhs: TypeTree)(implicit ctx: Context): TypeCaseDef =
      kernel.TypeCaseDef_module_apply(pattern, rhs)

    def copy(original: TypeCaseDef)(pattern: TypeTree, rhs: TypeTree)(implicit ctx: Context): TypeCaseDef =
      kernel.TypeCaseDef_module_copy(original)(pattern, rhs)

    def unapply(x: TypeCaseDef)(implicit ctx: Context): Option[(TypeTree, TypeTree)] =
      Some((x.pattern, x.rhs))
  }

  //
  // PATTERNS
  //

  /** Pattern tree of the pattern part of a CaseDef */
  type Pattern = kernel.Pattern

  implicit class PatternAPI(self: Pattern) {
    /** Position in the source code */
    def pos(implicit ctx: Context): Position = kernel.Pattern_pos(self)

    def tpe(implicit ctx: Context): Type = kernel.Pattern_tpe(self)

    def symbol(implicit ctx: Context): Symbol = kernel.Pattern_symbol(self)
  }

  /** Pattern representing a value. This includes `1`, ```x``` and `_` */
  type Value = kernel.Value

  implicit class ValueAPI(value: Value) {
    def value(implicit ctx: Context): Term = kernel.Pattern_Value_value(value)
  }

  /** Pattern representing a `_ @ _` binding. */
  type Bind = kernel.Bind

  implicit class BindAPI(bind: Bind) {
    def name(implicit ctx: Context): String = kernel.Pattern_Bind_name(bind)
    def pattern(implicit ctx: Context): Pattern = kernel.Pattern_Bind_pattern(bind)
  }

  /** Pattern representing a `Xyz(...)` unapply. */
  type Unapply = kernel.Unapply

  implicit class UnapplyAPI(unapply: Unapply) {
    def fun(implicit ctx: Context): Term = kernel.Pattern_Unapply_fun(unapply)
    def implicits(implicit ctx: Context): List[Term] = kernel.Pattern_Unapply_implicits(unapply)
    def patterns(implicit ctx: Context): List[Pattern] = kernel.Pattern_Unapply_patterns(unapply)
  }

  /** Pattern representing `X | Y | ...` alternatives. */
  type Alternatives = kernel.Alternatives

  implicit class AlternativesAPI(alternatives: Alternatives) {
    def patterns(implicit ctx: Context): List[Pattern] = kernel.Pattern_Alternatives_patterns(alternatives)
  }

  /** Pattern representing a `x: Y` type test. */
  type TypeTest = kernel.TypeTest

  implicit class TypeTestAPI(typeTest: TypeTest) {
    def tpt(implicit ctx: Context): TypeTree = kernel.Pattern_TypeTest_tpt(typeTest)
  }

  object Pattern {

    object IsValue {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Value] =
        kernel.isPattern_Value(pattern)
    }

    object Value {
      def apply(tpt: Term)(implicit ctx: Context): Value =
        kernel.Pattern_Value_module_apply(tpt)
      def copy(original: Value)(tpt: Term)(implicit ctx: Context): Value =
        kernel.Pattern_Value_module_copy(original)(tpt)
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Term] =
        kernel.isPattern_Value(pattern).map(_.value)
    }

    object IsBind {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Bind] =
        kernel.isPattern_Bind(pattern)
    }

    object Bind {
      // TODO def apply(name: String, pattern: Pattern)(implicit ctx: Context): Bind
      def copy(original: Bind)(name: String, pattern: Pattern)(implicit ctx: Context): Bind =
        kernel.Pattern_Bind_module_copy(original)(name, pattern)
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[(String, Pattern)] =
        kernel.isPattern_Bind(pattern).map(x => (x.name, x.pattern))
    }

    object IsUnapply {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Unapply] =
        kernel.isPattern_Unapply(pattern)
    }

    object Unapply {
      // TODO def apply(fun: Term, implicits: List[Term], patterns: List[Pattern])(implicit ctx: Context): Unapply
      def copy(original: Unapply)(fun: Term, implicits: List[Term], patterns: List[Pattern])(implicit ctx: Context): Unapply =
        kernel.Pattern_Unapply_module_copy(original)(fun, implicits, patterns)
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[(Term, List[Term], List[Pattern])] =
        kernel.isPattern_Unapply(pattern).map(x => (x.fun, x.implicits, x.patterns))
    }

    object IsAlternatives {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Alternatives] =
        kernel.isPattern_Alternatives(pattern)
    }

    object Alternatives {
      def apply(patterns: List[Pattern])(implicit ctx: Context): Alternatives =
        kernel.Pattern_Alternatives_module_apply(patterns)
      def copy(original: Alternatives)(patterns: List[Pattern])(implicit ctx: Context): Alternatives =
        kernel.Pattern_Alternatives_module_copy(original)(patterns)
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[List[Pattern]] =
        kernel.isPattern_Alternatives(pattern).map(_.patterns)
    }

    object IsTypeTest {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[TypeTest] =
        kernel.isPattern_TypeTest(pattern)
    }

    object TypeTest {
      def apply(tpt: TypeTree)(implicit ctx: Context): TypeTest =
        kernel.Pattern_TypeTest_module_apply(tpt)
      def copy(original: TypeTest)(tpt: TypeTree)(implicit ctx: Context): TypeTest =
        kernel.Pattern_TypeTest_module_copy(original)(tpt)
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[TypeTree] =
        kernel.isPattern_TypeTest(pattern).map(_.tpt)
    }

  }

  //
  // TYPE OR BOUNDS TREES
  //

  /** Type tree representing a type or a bounds written in the source */
  type TypeOrBoundsTree = kernel.TypeOrBoundsTree

  implicit class TypeOrBoundsTreeAPI(self: TypeOrBoundsTree) {
    def tpe(implicit ctx: Context): TypeOrBounds = kernel.TypeOrBoundsTree_tpe(self)
  }

  /** Type tree representing a type written in the source */
  type TypeTree = kernel.TypeTree

  implicit class TypeTreeAPI(self: TypeTree) {
    /** Position in the source code */
    def pos(implicit ctx: Context): Position = kernel.TypeTree_pos(self)

    /** Type of this type tree */
    def tpe(implicit ctx: Context): Type = kernel.TypeTree_tpe(self)

    /** Symbol of this type tree */
    def symbol(implicit ctx: Context): Symbol = kernel.TypeTree_symbol(self)
  }

  object IsTypeTree {
    def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree] =
      kernel.isTypeTree(tpt)
    def unapply(termOrTypeTree: TermOrTypeTree)(implicit ctx: Context, dummy: DummyImplicit): Option[TypeTree] =
      kernel.isTypeTreeNotTerm(termOrTypeTree)
  }

  /** Type trees representing a type written in the source */
  object TypeTree extends TypeTreeCoreModule { // TODO merge with TypeTreeCoreModule

    object IsInferred {
      /** Matches any Inferred and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Inferred] =
        kernel.isTypeTree_Inferred(tpt)
    }

    /** TypeTree containing an inferred type */
    object Inferred {
      def apply(tpe: Type)(implicit ctx: Context): Inferred =
        kernel.TypeTree_Inferred_apply(tpe)
      /** Matches a TypeTree containing an inferred type */
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Boolean =
        kernel.isTypeTree_Inferred(typeOrBoundsTree).isDefined
    }

    object IsIdent {
      /** Matches any Ident and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Ident] =
        kernel.isTypeTree_Ident(tpt)
    }

    object Ident {
      // TODO def apply(name: String)(implicit ctx: Context): Ident
      def copy(original: Ident)(name: String)(implicit ctx: Context): Ident =
        kernel.TypeTree_Ident_copy(original)(name)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[String] =
        kernel.isTypeTree_Ident(typeOrBoundsTree).map(_.name)
    }

    object IsSelect {
      /** Matches any Select and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Select] =
        kernel.isTypeTree_Select(tpt)
    }

    object Select {
      def apply(qualifier: Term, name: String)(implicit ctx: Context): Select =
        kernel.TypeTree_Select_apply(qualifier, name)
      def copy(original: Select)(qualifier: Term, name: String)(implicit ctx: Context): Select =
        kernel.TypeTree_Select_copy(original)(qualifier, name)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(Term, String)] =
        kernel.isTypeTree_Select(typeOrBoundsTree).map(x => (x.qualifier, x.name))
    }

    object IsProjection {
      /** Matches any Projection and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Projection] =
        kernel.isTypeTree_Projection(tpt)
    }

    object Projection {
      // TODO def apply(qualifier: TypeTree, name: String)(implicit ctx: Context): Project
      def copy(original: Projection)(qualifier: TypeTree, name: String)(implicit ctx: Context): Projection =
        kernel.TypeTree_Projection_copy(original)(qualifier, name)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(TypeTree, String)] =
        kernel.isTypeTree_Projection(typeOrBoundsTree).map(x => (x.qualifier, x.name))
    }

    object IsSingleton {
      /** Matches any Singleton and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Singleton] =
        kernel.isTypeTree_Singleton(tpt)
    }

    object Singleton {
      def apply(ref: Term)(implicit ctx: Context): Singleton =
        kernel.TypeTree_Singleton_apply(ref)
      def copy(original: Singleton)(ref: Term)(implicit ctx: Context): Singleton =
        kernel.TypeTree_Singleton_copy(original)(ref)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[Term] =
        kernel.isTypeTree_Singleton(typeOrBoundsTree).map(_.ref)
    }

    object IsRefined {
      /** Matches any Refined and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Refined] =
        kernel.isTypeTree_Refined(tpt)
    }

    object Refined {
      // TODO def apply(tpt: TypeTree, refinements: List[Definition])(implicit ctx: Context): Refined
      def copy(original: Refined)(tpt: TypeTree, refinements: List[Definition])(implicit ctx: Context): Refined =
        kernel.TypeTree_Refined_copy(original)(tpt, refinements)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(TypeTree, List[Definition])] =
        kernel.isTypeTree_Refined(typeOrBoundsTree).map(x => (x.tpt, x.refinements))
    }

    object IsApplied {
      /** Matches any Applied and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Applied] =
        kernel.isTypeTree_Applied(tpt)
    }

    object Applied {
      def apply(tpt: TypeTree, args: List[TypeOrBoundsTree])(implicit ctx: Context): Applied =
        kernel.TypeTree_Applied_apply(tpt, args)
      def copy(original: Applied)(tpt: TypeTree, args: List[TypeOrBoundsTree])(implicit ctx: Context): Applied =
        kernel.TypeTree_Applied_copy(original)(tpt, args)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(TypeTree, List[TypeOrBoundsTree])] =
        kernel.isTypeTree_Applied(typeOrBoundsTree).map(x => (x.tpt, x.args))
    }

    object IsAnnotated {
      /** Matches any Annotated and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[Annotated] =
        kernel.isTypeTree_Annotated(tpt)
    }

    object Annotated {
      def apply(arg: TypeTree, annotation: Term)(implicit ctx: Context): Annotated =
        kernel.TypeTree_Annotated_apply(arg, annotation)
      def copy(original: Annotated)(arg: TypeTree, annotation: Term)(implicit ctx: Context): Annotated =
        kernel.TypeTree_Annotated_copy(original)(arg, annotation)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(TypeTree, Term)] =
        kernel.isTypeTree_Annotated(typeOrBoundsTree).map(x => (x.arg, x.annotation))
    }

    object IsMatchType {
      /** Matches any MatchType and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[MatchType] =
        kernel.isTypeTree_MatchType(tpt)
    }

    object MatchType {
      def apply(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef])(implicit ctx: Context): MatchType =
        kernel.TypeTree_MatchType_apply(bound, selector, cases)
      def copy(original: MatchType)(bound: Option[TypeTree], selector: TypeTree, cases: List[TypeCaseDef])(implicit ctx: Context): MatchType =
        kernel.TypeTree_MatchType_copy(original)(bound, selector, cases)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(Option[TypeTree], TypeTree, List[TypeCaseDef])] =
        kernel.isTypeTree_MatchType(typeOrBoundsTree).map(x => (x.bound, x.selector, x.cases))
    }

    object IsByName {
      /** Matches any ByName and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[ByName] =
        kernel.isTypeTree_ByName(tpt)
    }

    object ByName {
      def apply(result: TypeTree)(implicit ctx: Context): ByName =
        kernel.TypeTree_ByName_apply(result)
      def copy(original: ByName)(result: TypeTree)(implicit ctx: Context): ByName =
        kernel.TypeTree_ByName_copy(original)(result)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeTree] =
        kernel.isTypeTree_ByName(typeOrBoundsTree).map(_.result)
    }

    object IsLambdaTypeTree {
      /** Matches any LambdaTypeTree and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[LambdaTypeTree] =
        kernel.isTypeTree_LambdaTypeTree(tpt)
    }

    object LambdaTypeTree {
      def apply(tparams: List[TypeDef], body: TypeOrBoundsTree)(implicit ctx: Context): LambdaTypeTree =
        kernel.TypeTree_LambdaTypeTree_apply(tparams, body)
      def copy(original: LambdaTypeTree)(tparams: List[TypeDef], body: TypeOrBoundsTree)(implicit ctx: Context): LambdaTypeTree =
        kernel.TypeTree_LambdaTypeTree_copy(original)(tparams, body)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(List[TypeDef], TypeOrBoundsTree)] =
        kernel.isTypeTree_LambdaTypeTree(typeOrBoundsTree).map(x => (x.tparams, x.body))
    }

    object IsTypeBind {
      /** Matches any TypeBind and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeBind] =
        kernel.isTypeTree_TypeBind(tpt)
    }

    object TypeBind {
      // TODO def apply(name: String, tpt: TypeOrBoundsTree)(implicit ctx: Context): TypeBind
      def copy(original: TypeBind)(name: String, tpt: TypeOrBoundsTree)(implicit ctx: Context): TypeBind =
        kernel.TypeTree_TypeBind_copy(original)(name, tpt)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(String, TypeOrBoundsTree)] =
        kernel.isTypeTree_TypeBind(typeOrBoundsTree).map(x => (x.name, x.body))
    }

    object IsTypeBlock {
      /** Matches any TypeBlock and returns it */
      def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeBlock] =
        kernel.isTypeTree_TypeBlock(tpt)
    }

    object TypeBlock {
      def apply(aliases: List[TypeDef], tpt: TypeTree)(implicit ctx: Context): TypeBlock =
        kernel.TypeTree_TypeBlock_apply(aliases, tpt)
      def copy(original: TypeBlock)(aliases: List[TypeDef], tpt: TypeTree)(implicit ctx: Context): TypeBlock =
        kernel.TypeTree_TypeBlock_copy(original)(aliases, tpt)
      def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(List[TypeDef], TypeTree)] =
        kernel.isTypeTree_TypeBlock(typeOrBoundsTree).map(x => (x.aliases, x.tpt))
    }
  }

  /** Type trees representing a type written in the source */
  trait TypeTreeCoreModule {

    /** Type tree representing an inferred type */
    type Inferred = kernel.TypeTree_Inferred

    /** Type tree representing a reference to definition with a given name */
    type Ident = kernel.TypeTree_Ident

    /** Type tree representing a selection of definition with a given name on a given term prefix */
    type Select = kernel.TypeTree_Select

    /** Type tree representing a selection of definition with a given name on a given type prefix */
    type Projection = kernel.TypeTree_Projection

    /** Type tree representing a singleton type */
    type Singleton = kernel.TypeTree_Singleton

    /** Type tree representing a type refinement */
    type Refined = kernel.TypeTree_Refined

    /** Type tree representing a type application */
    type Applied = kernel.TypeTree_Applied

    /** Type tree representing an annotated type */
    type Annotated = kernel.TypeTree_Annotated

    /** Type tree representing a type match */
    type MatchType = kernel.TypeTree_MatchType

    /** Type tree representing a by name parameter */
    type ByName = kernel.TypeTree_ByName

    /** Type tree representing a lambda abstraction type */
    type LambdaTypeTree = kernel.TypeTree_LambdaTypeTree

    /** Type tree representing a type binding */
    type TypeBind = kernel.TypeTree_TypeBind

    /** Type tree within a block with aliases `{ type U1 = ... ; T[U1, U2] }` */
    type TypeBlock = kernel.TypeTree_TypeBlock

  }

  implicit class TypeTree_IdentAPI(self: TypeTree.Ident) {
    def name(implicit ctx: Context): String = kernel.TypeTree_Ident_name(self)
  }

  implicit class TypeTree_SelectAPI(self: TypeTree.Select) {
    def qualifier(implicit ctx: Context): Term = kernel.TypeTree_Select_qualifier(self)
    def name(implicit ctx: Context): String = kernel.TypeTree_Select_name(self)
  }

  implicit class TypeTree_ProjectionAPI(self: TypeTree.Projection) {
    def qualifier(implicit ctx: Context): TypeTree = kernel.TypeTree_Projection_qualifier(self)
    def name(implicit ctx: Context): String = kernel.TypeTree_Projection_name(self)
  }

  implicit class TypeTree_SingletonAPI(self: TypeTree.Singleton) {
    def ref(implicit ctx: Context): Term = kernel.TypeTree_Singleton_ref(self)
  }

  implicit class TypeTree_RefinedAPI(self: TypeTree.Refined) {
    def tpt(implicit ctx: Context): TypeTree = kernel.TypeTree_Refined_tpt(self)
    def refinements(implicit ctx: Context): List[Definition] = kernel.TypeTree_Refined_refinements(self)
  }

  implicit class TypeTree_AppliedAPI(self: TypeTree.Applied) {
    def tpt(implicit ctx: Context): TypeTree = kernel.TypeTree_Applied_tpt(self)
    def args(implicit ctx: Context): List[TypeOrBoundsTree] = kernel.TypeTree_Applied_args(self)
  }

  implicit class TypeTree_AnnotatedAPI(self: TypeTree.Annotated) {
    def arg(implicit ctx: Context): TypeTree = kernel.TypeTree_Annotated_arg(self)
    def annotation(implicit ctx: Context): Term = kernel.TypeTree_Annotated_annotation(self)
  }

  implicit class TypeTree_MatchTypeAPI(self: TypeTree.MatchType) {
    def bound(implicit ctx: Context): Option[TypeTree] = kernel.TypeTree_MatchType_bound(self)
    def selector(implicit ctx: Context): TypeTree = kernel.TypeTree_MatchType_selector(self)
    def cases(implicit ctx: Context): List[TypeCaseDef] = kernel.TypeTree_MatchType_cases(self)
  }

  implicit class TypeTree_ByNameAPI(self: TypeTree.ByName) {
    def result(implicit ctx: Context): TypeTree = kernel.TypeTree_ByName_result(self)
  }

  implicit class TypeTree_LambdaTypeTreeAPI(self: TypeTree.LambdaTypeTree) {
    def tparams(implicit ctx: Context): List[TypeDef] = kernel.TypeTree_LambdaTypeTree_tparams(self)
    def body(implicit ctx: Context): TypeOrBoundsTree = kernel.TypeTree_LambdaTypeTree_body(self)
  }

  implicit class TypeTree_TypeBindAPI(self: TypeTree.TypeBind) {
    def name(implicit ctx: Context): String = kernel.TypeTree_TypeBind_name(self)
    def body(implicit ctx: Context): TypeOrBoundsTree = kernel.TypeTree_TypeBind_body(self)
  }

  implicit class TypeTree_TypeBlockAPI(self: TypeTree.TypeBlock) {
    def aliases(implicit ctx: Context): List[TypeDef] = kernel.TypeTree_TypeBlock_aliases(self)
    def tpt(implicit ctx: Context): TypeTree = kernel.TypeTree_TypeBlock_tpt(self)
  }

  /** Type tree representing a type bound written in the source */
  type TypeBoundsTree = kernel.TypeBoundsTree

  implicit class TypeBoundsTreeAPI(self: TypeBoundsTree) {
    def tpe(implicit ctx: Context): TypeBounds = kernel.TypeBoundsTree_tpe(self)
    def low(implicit ctx: Context): TypeTree = kernel.TypeBoundsTree_low(self)
    def hi(implicit ctx: Context): TypeTree = kernel.TypeBoundsTree_hi(self)
  }

  object IsTypeBoundsTree {
    def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[TypeBoundsTree] =
      kernel.isTypeBoundsTree(tpt)
  }

  object TypeBoundsTree {
    def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Option[(TypeTree, TypeTree)] =
      kernel.isTypeBoundsTree(typeOrBoundsTree).map(x => (x.low, x.hi))
  }

  /** Type tree representing wildcard type bounds written in the source.
   *  The wildcard type `_` (for example in in `List[_]`) will be a type tree that
   *  represents a type but has `TypeBound`a inside.
   */
  type WildcardTypeTree = kernel.WildcardTypeTree

  object IsWildcardTypeTree {
    def unapply(tpt: TypeOrBoundsTree)(implicit ctx: Context): Option[WildcardTypeTree] =
      kernel.isWildcardTypeTree(tpt)
  }

  /** TypeBoundsTree containing wildcard type bounds */
  object WildcardTypeTree {
    /** Matches a TypeBoundsTree containing wildcard type bounds */
    def unapply(typeOrBoundsTree: TypeOrBoundsTree)(implicit ctx: Context): Boolean =
      kernel.isWildcardTypeTree(typeOrBoundsTree).isDefined
  }

  //
  // TYPE OR BOUNDS
  //

  /** Type or bounds */
  type TypeOrBounds = kernel.TypeOrBounds

  /** NoPrefix for a type selection */
  type NoPrefix = kernel.NoPrefix

  object NoPrefix {
    def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Boolean =
      kernel.isNoPrefix(typeOrBounds).isDefined
  }

  /** Type bounds */
  type TypeBounds = kernel.TypeBounds

  object IsTypeBounds {
    def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[TypeBounds] =
      kernel.isTypeBounds(typeOrBounds)
  }

  object TypeBounds {
    def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Type)] =
      kernel.isTypeBounds(typeOrBounds).map(x => (x.low, x.hi))
  }

  implicit class TypeBoundsAPI(self: TypeBounds) {
    def low(implicit ctx: Context): Type = kernel.TypeBounds_low(self)
    def hi(implicit ctx: Context): Type = kernel.TypeBounds_hi(self)
  }

  /** A type */
  type Type = kernel.Type

  implicit class TypeAPI(self: Type) {
    def =:=(that: Type)(implicit ctx: Context): Boolean = kernel.`Type_=:=`(self)(that)
    def <:<(that: Type)(implicit ctx: Context): Boolean = kernel.`Type_<:<`(self)(that)
    def widen(implicit ctx: Context): Type = kernel.Type_widen(self)
    def classSymbol(implicit ctx: Context): Option[ClassSymbol] = kernel.Type_classSymbol(self)
    def typeSymbol(implicit ctx: Context): Symbol = kernel.Type_typeSymbol(self)
    def isSingleton(implicit ctx: Context): Boolean = kernel.Type_isSingleton(self)
    def memberType(member: Symbol)(implicit ctx: Context): Type = kernel.Type_memberType(self)(member)
  }

  object IsType {
    def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[Type] =
      kernel.isType(typeOrBounds)
  }

  /** A singleton type representing a known constant value */
  type ConstantType = kernel.ConstantType

  /** Type of a reference to a symbol */
  type SymRef = kernel.SymRef

  /** Type of a reference to a term */
  type TermRef = kernel.TermRef

  /** Type of a reference to a type */
  type TypeRef = kernel.TypeRef

  /** Type of a `super` refernce */
  type SuperType = kernel.SuperType

  /** A type with a type refinement `T { type U }` */
  type Refinement = kernel.Refinement

  /** A higher kinded type applied to some types `T[U]` */
  type AppliedType = kernel.AppliedType

  /** A type with an anottation `T @foo` */
  type AnnotatedType = kernel.AnnotatedType

  /** Intersection type `T & U` */
  type AndType = kernel.AndType

  /** Union type `T | U` */
  type OrType = kernel.OrType

  /** Type match `T match { case U => ... }` */
  type MatchType = kernel.MatchType

  /** Type of a by by name parameter */
  type ByNameType = kernel.ByNameType

  /** Type of a parameter reference */
  type ParamRef = kernel.ParamRef

  /** Type of `this` */
  type ThisType = kernel.ThisType

  /** A type that is recursively defined `this` */
  type RecursiveThis = kernel.RecursiveThis

  /** A type that is recursively defined */
  type RecursiveType = kernel.RecursiveType

  // TODO can we add the bound back without an cake?
  // TODO is LambdaType really needed? ParamRefExtractor could be split into more precise extractors
  /** Common abstraction for lambda types (MethodType, PolyType and TypeLambda). */
  type LambdaType[ParamInfo /*<: TypeOrBounds*/] = kernel.LambdaType[ParamInfo]

  /** Type of the definition of a method taking a single list of parameters. It's return type may be a MethodType. */
  type MethodType = kernel.MethodType

  /** Type of the definition of a method taking a list of type parameters. It's return type may be a MethodType. */
  type PolyType = kernel.PolyType

  /** Type of the definition of a type lambda taking a list of type parameters. It's return type may be a TypeLambda. */
  type TypeLambda = kernel.TypeLambda

  object Type {

    object IsConstantType {
      /** Matches any ConstantType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[ConstantType] =
        kernel.isConstantType(tpe)
    }

    object ConstantType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[Constant] =
        kernel.isConstantType(typeOrBounds).map(_.constant)
    }

    object IsSymRef {
      /** Matches any SymRef and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[SymRef] =
        kernel.isSymRef(tpe)
    }

    object SymRef {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Symbol, TypeOrBounds /* Type | NoPrefix */)] =
        kernel.isSymRef_unapply(typeOrBounds)
    }

    object IsTermRef {
      /** Matches any TermRef and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[TermRef] =
        kernel.isTermRef(tpe)
    }

    object TermRef {
      // TODO should qual be a Type?
      def apply(qual: TypeOrBounds, name: String)(implicit ctx: Context): TermRef =
        kernel.TermRef_apply(qual, name)
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(String, TypeOrBounds /* Type | NoPrefix */)] =
        kernel.isTermRef(typeOrBounds).map(x => (x.name, x.qualifier))
    }

    object IsTypeRef {
      /** Matches any TypeRef and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[TypeRef] =
        kernel.isTypeRef(tpe)
    }

    object TypeRef {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(String, TypeOrBounds /* Type | NoPrefix */)] =
        kernel.isTypeRef(typeOrBounds).map(x => (x.name, x.qualifier))
    }

    object IsSuperType {
      /** Matches any SuperType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[SuperType] =
        kernel.isSuperType(tpe)
    }

    object SuperType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Type)] =
        kernel.isSuperType(typeOrBounds).map(x => (x.thistpe, x.supertpe))
    }

    object IsRefinement {
      /** Matches any Refinement and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[Refinement] =
        kernel.isRefinement(tpe)
    }

    object Refinement {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, String, TypeOrBounds /* Type | TypeBounds */)] =
        kernel.isRefinement(typeOrBounds).map(x => (x.parent, x.name, x.info))
    }

    object IsAppliedType {
      /** Matches any AppliedType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[AppliedType] =
        kernel.isAppliedType(tpe)
    }

    object AppliedType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, List[TypeOrBounds /* Type | TypeBounds */])] =
        kernel.isAppliedType(typeOrBounds).map(x => (x.tycon, x.args))
    }

    object IsAnnotatedType {
      /** Matches any AnnotatedType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[AnnotatedType] =
        kernel.isAnnotatedType(tpe)
    }

    object AnnotatedType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Term)] =
        kernel.isAnnotatedType(typeOrBounds).map(x => (x.underlying, x.annot))
    }

    object IsAndType {
      /** Matches any AndType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[AndType] =
        kernel.isAndType(tpe)
    }

    object AndType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Type)] =
        kernel.isAndType(typeOrBounds).map(x => (x.left, x.right))
    }

    object IsOrType {
      /** Matches any OrType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[OrType] =
        kernel.isOrType(tpe)
    }

    object OrType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Type)] =
        kernel.isOrType(typeOrBounds).map(x => (x.left, x.right))
    }

    object IsMatchType {
      /** Matches any MatchType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[MatchType] =
        kernel.isMatchType(tpe)
    }

    object MatchType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(Type, Type, List[Type])] =
        kernel.isMatchType(typeOrBounds).map(x => (x.bound, x.scrutinee, x.cases))
    }

    object IsByNameType {
      /** Matches any ByNameType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[ByNameType] =
        kernel.isByNameType(tpe)
    }

    object ByNameType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[Type] =
        kernel.isByNameType(typeOrBounds).map(_.underlying)
    }

    object IsParamRef {
      /** Matches any ParamRef and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[ParamRef] =
        kernel.isParamRef(tpe)
    }

    object ParamRef {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(LambdaType[TypeOrBounds], Int)] =
        kernel.isParamRef(typeOrBounds).map(x => (x.binder, x.paramNum))
    }

    object IsThisType {
      /** Matches any ThisType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[ThisType] =
        kernel.isThisType(tpe)
    }

    object ThisType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[Type] =
        kernel.isThisType(typeOrBounds).map(_.tref)
    }

    object IsRecursiveThis {
      /** Matches any RecursiveThis and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[RecursiveThis] =
        kernel.isRecursiveThis(tpe)
    }

    object RecursiveThis {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[RecursiveType] =
        kernel.isRecursiveThis(typeOrBounds).map(_.binder)
    }

    object IsRecursiveType {
      /** Matches any RecursiveType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[RecursiveType] =
        kernel.isRecursiveType(tpe)
    }

    object RecursiveType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[Type] =
        kernel.isRecursiveType(typeOrBounds).map(_.underlying)
    }

    object IsMethodType {
      /** Matches any MethodType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[MethodType] =
        kernel.isMethodType(tpe)
    }

    object MethodType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(List[String], List[Type], Type)] =
        kernel.isMethodType(typeOrBounds).map(x => (x.paramNames, x.paramTypes, x.resType))
    }

    object IsPolyType {
      /** Matches any PolyType and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[PolyType] =
        kernel.isPolyType(tpe)
    }

    object PolyType {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(List[String], List[TypeBounds], Type)] =
        kernel.isPolyType(typeOrBounds).map(x => (x.paramNames, x.paramBounds, x.resType))
    }

    object IsTypeLambda {
      /** Matches any TypeLambda and returns it */
      def unapply(tpe: TypeOrBounds)(implicit ctx: Context): Option[TypeLambda] =
        kernel.isTypeLambda(tpe)
    }

    object TypeLambda {
      def unapply(typeOrBounds: TypeOrBounds)(implicit ctx: Context): Option[(List[String], List[TypeBounds], Type)] =
        kernel.isTypeLambda(typeOrBounds).map(x => (x.paramNames, x.paramBounds, x.resType))
    }

  }

  implicit class Type_ConstantTypeAPI(self: ConstantType) {
    def constant(implicit ctx: Context): Constant = kernel.ConstantType_constant(self)
  }

  implicit class Type_SymRefAPI(self: SymRef) {
    def qualifier(implicit ctx: Context): TypeOrBounds /* Type | NoPrefix */ = kernel.SymRef_qualifier(self)
  }

  implicit class Type_TermRefAPI(self: TermRef) {
    def name(implicit ctx: Context): String = kernel.TermRef_name(self)
    def qualifier(implicit ctx: Context): TypeOrBounds /* Type | NoPrefix */ = kernel.TermRef_qualifier(self)
  }

  implicit class Type_TypeRefAPI(self: TypeRef) {
    def name(implicit ctx: Context): String = kernel.TypeRef_name(self)
    def qualifier(implicit ctx: Context): TypeOrBounds /* Type | NoPrefix */ = kernel.TypeRef_qualifier(self)
  }

  implicit class Type_SuperTypeAPI(self: SuperType) {
    def thistpe(implicit ctx: Context): Type = kernel.SuperType_thistpe(self)
    def supertpe(implicit ctx: Context): Type = kernel.SuperType_supertpe(self)
  }

  implicit class Type_RefinementAPI(self: Refinement) {
    def parent(implicit ctx: Context): Type = kernel.Refinement_parent(self)
    def name(implicit ctx: Context): String = kernel.Refinement_name(self)
    def info(implicit ctx: Context): TypeOrBounds = kernel.Refinement_info(self)
  }

  implicit class Type_AppliedTypeAPI(self: AppliedType) {
    def tycon(implicit ctx: Context): Type = kernel.AppliedType_tycon(self)
    def args(implicit ctx: Context): List[TypeOrBounds /* Type | TypeBounds */] = kernel.AppliedType_args(self)
  }

  implicit class Type_AnnotatedTypeAPI(self: AnnotatedType) {
    def underlying(implicit ctx: Context): Type = kernel.AnnotatedType_underlying(self)
    def annot(implicit ctx: Context): Term = kernel.AnnotatedType_annot(self)
  }

  implicit class Type_AndTypeAPI(self: AndType) {
    def left(implicit ctx: Context): Type = kernel.AndType_left(self)
    def right(implicit ctx: Context): Type = kernel.AndType_right(self)
  }

  implicit class Type_OrTypeAPI(self: OrType) {
    def left(implicit ctx: Context): Type = kernel.OrType_left(self)
    def right(implicit ctx: Context): Type = kernel.OrType_right(self)
  }

  implicit class Type_MatchTypeAPI(self: MatchType) {
    def bound(implicit ctx: Context): Type = kernel.MatchType_bound(self)
    def scrutinee(implicit ctx: Context): Type = kernel.MatchType_scrutinee(self)
    def cases(implicit ctx: Context): List[Type] = kernel.MatchType_cases(self)
  }

  implicit class Type_ByNameTypeAPI(self: ByNameType) {
    def underlying(implicit ctx: Context): Type = kernel.ByNameType_underlying(self)
  }

  implicit class Type_ParamRefAPI(self: ParamRef) {
    def binder(implicit ctx: Context): LambdaType[TypeOrBounds] = kernel.ParamRef_binder(self)
    def paramNum(implicit ctx: Context): Int = kernel.ParamRef_paramNum(self)
  }

  implicit class Type_ThisTypeAPI(self: ThisType) {
    def tref(implicit ctx: Context): Type = kernel.ThisType_tref(self)
  }

  implicit class Type_RecursiveThisAPI(self: RecursiveThis) {
    def binder(implicit ctx: Context): RecursiveType = kernel.RecursiveThis_binder(self)
  }

  implicit class Type_RecursiveTypeAPI(self: RecursiveType) {
    def underlying(implicit ctx: Context): Type = kernel.RecursiveType_underlying(self)
  }

  implicit class Type_MethodTypeAPI(self: MethodType) {
    def isImplicit: Boolean = kernel.MethodType_isImplicit(self)
    def isErased: Boolean = kernel.MethodType_isErased(self)
    def paramNames(implicit ctx: Context): List[String] = kernel.MethodType_paramNames(self)
    def paramTypes(implicit ctx: Context): List[Type] = kernel.MethodType_paramTypes(self)
    def resType(implicit ctx: Context): Type = kernel.MethodType_resType(self)
  }

  implicit class Type_PolyTypeAPI(self: PolyType) {
    def paramNames(implicit ctx: Context): List[String] = kernel.PolyType_paramNames(self)
    def paramBounds(implicit ctx: Context): List[TypeBounds] = kernel.PolyType_paramBounds(self)
    def resType(implicit ctx: Context): Type = kernel.PolyType_resType(self)
  }

  implicit class Type_TypeLambdaAPI(self: TypeLambda) {
    def paramNames(implicit ctx: Context): List[String] = kernel.TypeLambda_paramNames(self)
    def paramBounds(implicit ctx: Context): List[TypeBounds] = kernel.TypeLambda_paramBounds(self)
    def resType(implicit ctx: Context): Type = kernel.TypeLambda_resType(self)
  }

  //
  // IMPORT SELECTORS
  //

  /** Import selectors:
   *   * SimpleSelector: `.bar` in `import foo.bar`
   *   * RenameSelector: `.{bar => baz}` in `import foo.{bar => baz}`
   *   * OmitSelector: `.{bar => _}` in `import foo.{bar => _}`
   */
  type ImportSelector = kernel.ImportSelector

  type SimpleSelector = kernel.SimpleSelector

  implicit class SimpleSelectorAPI(self: SimpleSelector) {
    def selection(implicit ctx: Context): Id =
      kernel.SimpleSelector_selection(self)
  }

  object SimpleSelector {
    def unapply(importSelector: ImportSelector)(implicit ctx: Context): Option[Id] =
      kernel.isSimpleSelector(importSelector).map(_.selection)
  }

  type RenameSelector = kernel.RenameSelector

  implicit class RenameSelectorAPI(self: RenameSelector) {
    def from(implicit ctx: Context): Id =
      kernel.RenameSelector_from(self)

    def to(implicit ctx: Context): Id =
      kernel.RenameSelector_to(self)
  }

  object RenameSelector {
    def unapply(importSelector: ImportSelector)(implicit ctx: Context): Option[(Id, Id)] =
      kernel.isRenameSelector(importSelector).map(x => (x.from, x.to))
  }

  type OmitSelector = kernel.OmitSelector

  implicit class OmitSelectorAPI(self: OmitSelector) {
    def omitted(implicit ctx: Context): Id =
      kernel.SimpleSelector_omited(self)
  }

  object OmitSelector {
    def unapply(importSelector: ImportSelector)(implicit ctx: Context): Option[Id] =
      kernel.isOmitSelector(importSelector).map(_.omitted)
  }

  //
  // IDENTIFIERS
  //

  /** Untyped identifier */
  type Id = kernel.Id

  implicit class IdAPI(id: Id) {

    /** Position in the source code */
    def pos(implicit ctx: Context): Position = kernel.Id_pos(id)

    /** Name of the identifier */
    def name(implicit ctx: Context): String = kernel.Id_name(id)

  }

  object Id {
    def unapply(id: Id)(implicit ctx: Context): Option[String] = Some(id.name)
  }

  //
  // SIGNATURES
  //

  /** JVM signature of a method */
  type Signature = kernel.Signature

  /** Erased (JVM) signatures. */
  object Signature {
    /** Matches the erased (JVM) signature and returns its parameters and result type. */
    def unapply(sig: Signature)(implicit ctx: Context): Option[(List[String], String)] =
      Some((sig.paramSigs, sig.resultSig))
  }

  implicit class SignatureAPI(sig: Signature) {

    /** The (JVM) erased signatures of the parameters */
    def paramSigs: List[String]= kernel.Signature_paramSigs(sig)

    /** The (JVM) erased result type */
    def resultSig: String = kernel.Signature_resultSig(sig)

  }

  //
  // POSITIONS
  //

  /** Source position */
  type Position = kernel.Position

  implicit class PositionAPI(pos: Position) {

    /** The start offset in the source file */
    def start: Int = kernel.Position_start(pos)

    /** The end offset in the source file */
    def end: Int = kernel.Position_end(pos)

    /** Does this position exist */
    def exists: Boolean = kernel.Position_exists(pos)

    /** Source file in which this position is located */
    def sourceFile: java.nio.file.Path = kernel.Position_sourceFile(pos)

    /** The start line in the source file */
    def startLine: Int = kernel.Position_startLine(pos)

    /** The end line in the source file */
    def endLine: Int = kernel.Position_endLine(pos)

    /** The start column in the source file */
    def startColumn: Int = kernel.Position_startColumn(pos)

    /** The end column in the source file */
    def endColumn: Int = kernel.Position_endColumn(pos)

    /** Source code within the position */
    def sourceCode: String = kernel.Position_sourceCode(pos)

  }


  //
  // CONSTANTS
  //

  /** Constant value represented as the constant itself */
  type Constant = kernel.Constant

  implicit class ConstantAPI(const: Constant) {
    def value: Any = kernel.Constant_value(const)
  }

  /** Module of Constant literals */
  object Constant {

    /** Module of Null literals */
    object Unit {
      /** Unit `()` literal */
      def apply(): Constant =
        kernel.Constant_Unit_apply()

      /** Extractor for Unit literals */
      def unapply(constant: Constant): Boolean =
        kernel.isConstant_Unit(constant)
    }

    /** Module of Null literals */
    object Null {
      /** `null` literal */
      def apply(): Constant =
        kernel.Constant_Null_apply()

      /** Extractor for Null literals */
      def unapply(constant: Constant): Boolean =
        kernel.isConstant_Null(constant)
    }

    /** Module of Boolean literals */
    object Boolean {
      /** Boolean literal */
      def apply(x: Boolean): Constant =
        kernel.Constant_Boolean_apply(x)

      /** Extractor for Boolean literals */
      def unapply(constant: Constant): Option[Boolean] =
        kernel.isConstant_Boolean(constant)
    }

    /** Module of Byte literals */
    object Byte {
      /** Byte literal */
      def apply(x: Byte): Constant =
        kernel.Constant_Byte_apply(x)

      /** Extractor for Byte literals */
      def unapply(constant: Constant): Option[Byte] =
        kernel.isConstant_Byte(constant)
    }

    /** Module of Short literals */
    object Short {
      /** Short literal */
      def apply(x: Short): Constant =
        kernel.Constant_Short_apply(x)

      /** Extractor for Short literals */
      def unapply(constant: Constant): Option[Short] =
        kernel.isConstant_Short(constant)
    }

    /** Module of Char literals */
    object Char {
      /** Char literal */
      def apply(x: Char): Constant =
        kernel.Constant_Char_apply(x)

      /** Extractor for Char literals */
      def unapply(constant: Constant): Option[Char] =
        kernel.isConstant_Char(constant)
    }

    /** Module of Int literals */
    object Int {
      /** Int literal */
      def apply(x: Int): Constant =
        kernel.Constant_Int_apply(x)

      /** Extractor for Int literals */
      def unapply(constant: Constant): Option[Int] =
        kernel.isConstant_Int(constant)
    }

    /** Module of Long literals */
    object Long {
      /** Long literal */
      def apply(x: Long): Constant =
        kernel.Constant_Long_apply(x)

      /** Extractor for Long literals */
      def unapply(constant: Constant): Option[Long] =
        kernel.isConstant_Long(constant)
    }

    /** Module of Float literals */
    object Float {
      /** Float literal */
      def apply(x: Float): Constant =
        kernel.Constant_Float_apply(x)

      /** Extractor for Float literals */
      def unapply(constant: Constant): Option[Float] =
        kernel.isConstant_Float(constant)
    }

    /** Module of Double literals */
    object Double {
      /** Double literal */
      def apply(x: Double): Constant =
        kernel.Constant_Double_apply(x)

      /** Extractor for Double literals */
      def unapply(constant: Constant): Option[Double] =
        kernel.isConstant_Double(constant)
    }

    /** Module of String literals */
    object String {
      /** String literal */
      def apply(x: String): Constant =
        kernel.Constant_String_apply(x)

      /** Extractor for String literals */
      def unapply(constant: Constant): Option[String] =
        kernel.isConstant_String(constant)
    }

    /** Module of ClassTag literals */
    object ClassTag {
      /** scala.reflect.ClassTag literal */
      def apply[T](implicit x: scala.reflect.ClassTag[T]): Constant =
        kernel.Constant_ClassTag_apply(x)

      /** Extractor for ClassTag literals */
      def unapply(constant: Constant): Option[Type] =
        kernel.isConstant_ClassTag(constant)
    }

    /** Module of scala.Symbol literals */
    object Symbol {
      /** scala.Symbol literal */
      def apply(x: scala.Symbol): Constant =
        kernel.Constant_Symbol_apply(x)

      /** Extractor for scala.Symbol literals */
      def unapply(constant: Constant): Option[scala.Symbol] =
        kernel.isConstant_Symbol(constant)
    }
  }

  //
  // SYMBOLS
  //

  /** Symbol of a definition.
   *  Then can be compared with == to know if the definition is the same.
   */
  type Symbol = kernel.Symbol

  implicit class SymbolAPI(self: Symbol) {

    /** Owner of this symbol. The owner is the symbol in which this symbol is defined */
    def owner(implicit ctx: Context): Symbol = kernel.Symbol_owner(self)

    /** Flags of this symbol */
    def flags(implicit ctx: Context): Flags = kernel.Symbol_flags(self)

    /** This symbol is private within the resulting type */
    def privateWithin(implicit ctx: Context): Option[Type] = kernel.Symbol_privateWithin(self)

    /** This symbol is protected within the resulting type */
    def protectedWithin(implicit ctx: Context): Option[Type] = kernel.Symbol_protectedWithin(self)

    /** The name of this symbol */
    def name(implicit ctx: Context): String = kernel.Symbol_name(self)

    /** The full name of this symbol up to the root package */
    def fullName(implicit ctx: Context): String = kernel.Symbol_fullName(self)

    /** The position of this symbol */
    def pos(implicit ctx: Context): Position = kernel.Symbol_pos(self)

    def localContext(implicit ctx: Context): Context = kernel.Symbol_localContext(self)

    /** Unsafe cast as to PackageSymbol. Use IsPackageSymbol to safly check and cast to PackageSymbol */
    def asPackage(implicit ctx: Context): PackageSymbol = self match {
      case IsPackageSymbol(self) => self
      case _ => throw new Exception("not a PackageSymbol")
    }

    /** Unsafe cast as to ClassSymbol. Use IsClassSymbol to safly check and cast to ClassSymbol */
    def asClass(implicit ctx: Context): ClassSymbol = self match {
      case IsClassSymbol(self) => self
      case _ => throw new Exception("not a ClassSymbol")
    }

    /** Unsafe cast as to DefSymbol. Use IsDefSymbol to safly check and cast to DefSymbol */
    def asDef(implicit ctx: Context): DefSymbol = self match {
      case IsDefSymbol(self) => self
      case _ => throw new Exception("not a DefSymbol")
    }

    /** Unsafe cast as to ValSymbol. Use IsValSymbol to safly check and cast to ValSymbol */
    def asVal(implicit ctx: Context): ValSymbol = self match {
      case IsValSymbol(self) => self
      case _ => throw new Exception("not a ValSymbol")
    }

    /** Unsafe cast as to TypeSymbol. Use IsTypeSymbol to safly check and cast to TypeSymbol */
    def asType(implicit ctx: Context): TypeSymbol = self match {
      case IsTypeSymbol(self) => self
      case _ => throw new Exception("not a TypeSymbol")
    }

    /** Unsafe cast as to BindSymbol. Use IsBindSymbol to safly check and cast to BindSymbol */
    def asBind(implicit ctx: Context): BindSymbol = self match {
      case IsBindSymbol(self) => self
      case _ => throw new Exception("not a BindSymbol")
    }

    /** Annotations attached to this symbol */
    def annots(implicit ctx: Context): List[Term] = kernel.Symbol_annots(self)

    def isDefinedInCurrentRun(implicit ctx: Context): Boolean = kernel.Symbol_isDefinedInCurrentRun(self)

    def isLocalDummy(implicit ctx: Context): Boolean = kernel.Symbol_isLocalDummy(self)
    def isRefinementClass(implicit ctx: Context): Boolean = kernel.Symbol_isRefinementClass(self)
    def isAliasType(implicit ctx: Context): Boolean = kernel.Symbol_isAliasType(self)
    def isAnonymousClass(implicit ctx: Context): Boolean = kernel.Symbol_isAnonymousClass(self)
    def isAnonymousFunction(implicit ctx: Context): Boolean = kernel.Symbol_isAnonymousFunction(self)
    def isAbstractType(implicit ctx: Context): Boolean = kernel.Symbol_isAbstractType(self)
    def isClassConstructor(implicit ctx: Context): Boolean = kernel.Symbol_isClassConstructor(self)
  }

  /** Symbol of a package definition */
  type PackageSymbol = kernel.PackageSymbol

  object IsPackageSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[PackageSymbol] =
      kernel.isPackageSymbol(symbol)
  }

  implicit class PackageSymbolAPI(self: PackageSymbol) {
    def tree(implicit ctx: Context): PackageDef =
      kernel.PackageSymbol_tree(self)
  }

  /** Symbol of a class definition. This includes anonymous class definitions and the class of a module object. */
  type ClassSymbol = kernel.ClassSymbol

  object IsClassSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[ClassSymbol] =
      kernel.isClassSymbol(symbol)
  }

  object ClassSymbol {
    /** The ClassSymbol of a global class definition */
    def of(fullName: String)(implicit ctx: Context): ClassSymbol =
      kernel.ClassSymbol_of(fullName)
  }

  implicit class ClassSymbolAPI(self: ClassSymbol) {
    /** ClassDef tree of this defintion */
    def tree(implicit ctx: Context): ClassDef =
      kernel.ClassSymbol_tree(self)

    /** Fields directly declared in the class */
    def fields(implicit ctx: Context): List[Symbol] =
      kernel.ClassSymbol_fields(self)

    /** Field with the given name directly declared in the class */
    def field(name: String)(implicit ctx: Context): Option[Symbol] =
      kernel.ClassSymbol_field(self)(name)

    /** Get non-private named methods defined directly inside the class */
    def classMethod(name: String)(implicit ctx: Context): List[DefSymbol] =
      kernel.ClassSymbol_classMethod(self)(name)

    /** Get all non-private methods defined directly inside the class, exluding constructors */
    def classMethods(implicit ctx: Context): List[DefSymbol] =
      kernel.ClassSymbol_classMethods(self)

    /** Get named non-private methods declared or inherited */
    def method(name: String)(implicit ctx: Context): List[DefSymbol] =
      kernel.ClassSymbol_method(self)(name)

    /** Get all non-private methods declared or inherited */
    def methods(implicit ctx: Context): List[DefSymbol] =
      kernel.ClassSymbol_methods(self)

    /** Fields of a case class type -- only the ones declared in primary constructor */
    def caseFields(implicit ctx: Context): List[ValSymbol] =
      kernel.ClassSymbol_caseFields(self)

    /** The class symbol of the companion module class */
    def companionClass(implicit ctx: Context): Option[ClassSymbol] =
      kernel.ClassSymbol_companionClass(self)

    /** The symbol of the companion module */
    def companionModule(implicit ctx: Context): Option[ValSymbol] =
      kernel.ClassSymbol_companionModule(self)

    /** The symbol of the class of the companion module */
    def moduleClass(implicit ctx: Context): Option[Symbol] =
      kernel.ClassSymbol_moduleClass(self)
  }

  /** Symbol of a type (parameter or member) definition. */
  type TypeSymbol = kernel.TypeSymbol

  object IsTypeSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[TypeSymbol] =
      kernel.isTypeSymbol(symbol)
  }

  implicit class TypeSymbolAPI(self: TypeSymbol) {
    /** TypeDef tree of this definition */
    def tree(implicit ctx: Context): TypeDef =
      kernel.TypeSymbol_tree(self)

    def isTypeParam(implicit ctx: Context): Boolean =
      kernel.TypeSymbol_isTypeParam(self)
  }

  /** Symbol representing a method definition. */
  type DefSymbol = kernel.DefSymbol

  object IsDefSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[DefSymbol] =
      kernel.isDefSymbol(symbol)
  }

  implicit class DefSymbolAPI(self: DefSymbol) {
    /** DefDef tree of this defintion */
    def tree(implicit ctx: Context): DefDef =
      kernel.DefSymbol_tree(self)

    /** Signature of this defintion */
    def signature(implicit ctx: Context): Signature =
      kernel.DefSymbol_signature(self)
  }

  /** Symbol representing a value definition. This includes `val`, `lazy val`, `var`, `object` and parameter definitions. */
  type ValSymbol = kernel.ValSymbol

  object IsValSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[ValSymbol] =
      kernel.isValSymbol(symbol)
  }

  implicit class ValSymbolAPI(self: ValSymbol) {
    /** ValDef tree of this defintion */
    def tree(implicit ctx: Context): ValDef =
      kernel.ValSymbol_tree(self)

    /** The class symbol of the companion module class */
    def moduleClass(implicit ctx: Context): Option[ClassSymbol] =
      kernel.ValSymbol_moduleClass(self)

    def companionClass(implicit ctx: Context): Option[ClassSymbol] =
      kernel.ValSymbol_companionClass(self)
  }

  /** Symbol representing a bind definition. */
  type BindSymbol = kernel.BindSymbol

  object IsBindSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Option[BindSymbol] =
      kernel.isBindSymbol(symbol)
  }

  implicit class BindSymbolAPI(self: BindSymbol) {
    /** Bind pattern of this definition */
    def tree(implicit ctx: Context): Bind =
      kernel.BindSymbol_tree(self)
  }

  /** No symbol available. */
  type NoSymbol = kernel.NoSymbol

  object NoSymbol {
    def unapply(symbol: Symbol)(implicit ctx: Context): Boolean =
      kernel.isNoSymbol(symbol)
  }

  //
  // FLAGS
  //

  /** FlagSet of a Symbol */
  type Flags = kernel.Flags

  implicit class FlagsAPI(self: Flags) {

    /** Is the given flag set a subset of this flag sets */
    def is(that: Flags): Boolean = kernel.Flags_is(self)(that)

    /** Union of the two flag sets */
    def |(that: Flags): Flags = kernel.Flags_or(self)(that)

    /** Intersection of the two flag sets */
    def &(that: Flags): Flags = kernel.Flags_and(self)(that)

  }

  object Flags {

    /** Is this symbol `private` */
    def Private: Flags = kernel.Flags_Private

    /** Is this symbol `protected` */
    def Protected: Flags = kernel.Flags_Protected

    /** Is this symbol `abstract` */
    def Abstract: Flags = kernel.Flags_Abstract

    /** Is this symbol `final` */
    def Final: Flags = kernel.Flags_Final

    /** Is this symbol `sealed` */
    def Sealed: Flags = kernel.Flags_Sealed

    /** Is this symbol `case` */
    def Case: Flags = kernel.Flags_Case

    /** Is this symbol `implicit` */
    def Implicit: Flags = kernel.Flags_Implicit

    /** Is this symbol `erased` */
    def Erased: Flags = kernel.Flags_Erased

    /** Is this symbol `lazy` */
    def Lazy: Flags = kernel.Flags_Lazy

    /** Is this symbol `override` */
    def Override: Flags = kernel.Flags_Override

    /** Is this symbol `inline` */
    def Inline: Flags = kernel.Flags_Inline

    /** Is this symbol markes as a macro. An inline method containing toplevel splices */
    def Macro: Flags = kernel.Flags_Macro

    /** Is this symbol marked as static. Mapped to static Java member */
    def Static: Flags = kernel.Flags_Static

    /** Is this symbol defined in a Java class */
    def JavaDefined: Flags = kernel.Flags_JavaDefined

    /** Is this symbol an object or its class (used for a ValDef or a ClassDef extends Modifier respectively) */
    def Object: Flags = kernel.Flags_Object

    /** Is this symbol a trait */
    def Trait: Flags = kernel.Flags_Trait

    /** Is this symbol local? Used in conjunction with private/private[Type] to mean private[this] extends Modifier proctected[this] */
    def Local: Flags = kernel.Flags_Local

    /** Was this symbol generated by Scala compiler */
    def Synthetic: Flags = kernel.Flags_Synthetic

    /** Is this symbol to be tagged Java Synthetic */
    def Artifact: Flags = kernel.Flags_Artifact

    /** Is this symbol a `var` (when used on a ValDef) */
    def Mutable: Flags = kernel.Flags_Mutable

    /** Is this symbol a getter or a setter */
    def FieldAccessor: Flags = kernel.Flags_FieldAccessor

    /** Is this symbol a getter for case class parameter */
    def CaseAcessor: Flags = kernel.Flags_CaseAcessor

    /** Is this symbol a type parameter marked as covariant `+` */
    def Covariant: Flags = kernel.Flags_Covariant

    /** Is this symbol a type parameter marked as contravariant `-` */
    def Contravariant: Flags = kernel.Flags_Contravariant

    /** Was this symbol imported from Scala2.x */
    def Scala2X: Flags = kernel.Flags_Scala2X

    /** Is this symbol a method with default parameters */
    def DefaultParameterized: Flags = kernel.Flags_DefaultParameterized

    /** Is this symbol member that is assumed to be stable and realizable */
    def StableRealizable: Flags = kernel.Flags_StableRealizable

    /** Is this symbol a parameter */
    def Param: Flags = kernel.Flags_Param

    /** Is this symbol a parameter accessor */
    def ParamAccessor: Flags = kernel.Flags_ParamAccessor

    /** Is this symbol an enum */
    def Enum: Flags = kernel.Flags_Enum

    /** Is this symbol a module class */
    def ModuleClass: Flags = kernel.Flags_ModuleClass

    /** Is this symbol labeled private[this] */
    def PrivateLocal: Flags = kernel.Flags_PrivateLocal

    /** Is this symbol a package */
    def Package: Flags = kernel.Flags_Package

    /** Is this symbol an implementation class of a Scala2 trait */
    def ImplClass: Flags = kernel.Flags_ImplClass
  }

  //
  // QUOTES AND SPLICES
  //

  implicit class QuotedExprAPI[T](expr: scala.quoted.Expr[T]) {
    /** View this expression `Expr[T]` as a `Term` */
    def unseal(implicit ctx: Context): Term =
      kernel.QuotedExpr_unseal(expr)
  }

  implicit class QuotedTypeAPI[T](tpe: scala.quoted.Type[T]) {
    /** View this expression `Type[T]` as a `TypeTree` */
    def unseal(implicit ctx: Context): TypeTree =
      kernel.QuotedType_unseal(tpe)
  }

  implicit class TermToQuotedAPI(term: Term) {
    /** Convert `Term` to an `Expr[T]` and check that it conforms to `T` */
    def seal[T](implicit tpe: scala.quoted.Type[T], ctx: Context): scala.quoted.Expr[T] =
      kernel.QuotedExpr_seal(term)(tpe)
  }

  implicit class TypeToQuotedAPI(tpe: Type) {
    /** Convert `Type` to an `quoted.Type[T]` */
    def seal(implicit ctx: Context): scala.quoted.Type[_] =
      kernel.QuotedType_seal(tpe)
  }

  //
  // STANDARD DEFINITIONS
  //

  /** A value containing all standard definitions in [[DefinitionsAPI]]
   *  @group Definitions
   */
  object definitions extends StandardSymbols with StandardTypes

  /** Defines standard symbols (and types via its base trait).
   *  @group API
   */
  trait StandardSymbols {

    /** The module symbol of root package `_root_`. */
    def RootPackage: Symbol = kernel.Definitions_RootPackage

    /** The class symbol of root package `_root_`. */
    def RootClass: Symbol = kernel.Definitions_RootClass

    /** The class symbol of empty package `_root_._empty_`. */
    def EmptyPackageClass: Symbol = kernel.Definitions_EmptyPackageClass

    /** The module symbol of package `scala`. */
    def ScalaPackage: Symbol = kernel.Definitions_ScalaPackage

    /** The class symbol of package `scala`. */
    def ScalaPackageClass: Symbol = kernel.Definitions_ScalaPackageClass

    /** The class symbol of core class `scala.Any`. */
    def AnyClass: Symbol = kernel.Definitions_AnyClass

    /** The class symbol of core class `scala.AnyVal`. */
    def AnyValClass: Symbol = kernel.Definitions_AnyValClass

    /** The class symbol of core class `java.lang.Object`. */
    def ObjectClass: Symbol = kernel.Definitions_ObjectClass

    /** The type symbol of core class `scala.AnyRef`. */
    def AnyRefClass: Symbol = kernel.Definitions_AnyRefClass

    /** The class symbol of core class `scala.Null`. */
    def NullClass: Symbol = kernel.Definitions_NullClass

    /** The class symbol of core class `scala.Nothing`. */
    def NothingClass: Symbol = kernel.Definitions_NothingClass

    /** The class symbol of primitive class `scala.Unit`. */
    def UnitClass: Symbol = kernel.Definitions_UnitClass

    /** The class symbol of primitive class `scala.Byte`. */
    def ByteClass: Symbol = kernel.Definitions_ByteClass

    /** The class symbol of primitive class `scala.Short`. */
    def ShortClass: Symbol = kernel.Definitions_ShortClass

    /** The class symbol of primitive class `scala.Char`. */
    def CharClass: Symbol = kernel.Definitions_CharClass

    /** The class symbol of primitive class `scala.Int`. */
    def IntClass: Symbol = kernel.Definitions_IntClass

    /** The class symbol of primitive class `scala.Long`. */
    def LongClass: Symbol = kernel.Definitions_LongClass

    /** The class symbol of primitive class `scala.Float`. */
    def FloatClass: Symbol = kernel.Definitions_FloatClass

    /** The class symbol of primitive class `scala.Double`. */
    def DoubleClass: Symbol = kernel.Definitions_DoubleClass

    /** The class symbol of primitive class `scala.Boolean`. */
    def BooleanClass: Symbol = kernel.Definitions_BooleanClass

    /** The class symbol of class `scala.String`. */
    def StringClass: Symbol = kernel.Definitions_StringClass

    /** The class symbol of class `java.lang.Class`. */
    def ClassClass: Symbol = kernel.Definitions_ClassClass

    /** The class symbol of class `scala.Array`. */
    def ArrayClass: Symbol = kernel.Definitions_ArrayClass

    /** The module symbol of module `scala.Predef`. */
    def PredefModule: Symbol = kernel.Definitions_PredefModule

    /** The module symbol of package `java.lang`. */
    def JavaLangPackage: Symbol = kernel.Definitions_JavaLangPackage

    /** The module symbol of module `scala.Array`. */
    def ArrayModule: Symbol = kernel.Definitions_ArrayModule

    /** The method symbol of method `apply` in class `scala.Array`. */
    def Array_apply: Symbol = kernel.Definitions_Array_apply

    /** The method symbol of method `clone` in class `scala.Array`. */
    def Array_clone: Symbol = kernel.Definitions_Array_clone

    /** The method symbol of method `length` in class `scala.Array`. */
    def Array_length: Symbol = kernel.Definitions_Array_length

    /** The method symbol of method `update` in class `scala.Array`. */
    def Array_update: Symbol = kernel.Definitions_Array_update

    /** A dummy class symbol that is used to indicate repeated parameters
     *  compiled by the Scala compiler.
     */
    def RepeatedParamClass: Symbol = kernel.Definitions_RepeatedParamClass

    /** The class symbol of class `scala.Option`. */
    def OptionClass: Symbol = kernel.Definitions_OptionClass

    /** The module symbol of module `scala.None`. */
    def NoneModule: Symbol = kernel.Definitions_NoneModule

    /** The module symbol of module `scala.Some`. */
    def SomeModule: Symbol = kernel.Definitions_SomeModule

    /** Function-like object that maps arity to symbols for classes `scala.Product` */
    def ProductClass: Symbol = kernel.Definitions_ProductClass

    /** Function-like object that maps arity to symbols for classes `scala.FunctionX`.
     *   -  0th element is `Function0`
     *   -  1st element is `Function1`
     *   -  ...
     *   -  Nth element is `FunctionN`
     */
    def FunctionClass(arity: Int, isImplicit: Boolean = false, isErased: Boolean = false): Symbol =
      kernel.Definitions_FunctionClass(arity, isImplicit, isErased)

    /** Function-like object that maps arity to symbols for classes `scala.TupleX`.
     *   -  0th element is `NoSymbol`
     *   -  1st element is `NoSymbol`
     *   -  2st element is `Tuple2`
     *   -  ...
     *   - 22nd element is `Tuple22`
     *   - 23nd element is `NoSymbol`  // TODO update when we will have more tuples
     *   - ...
     */
    def TupleClass(arity: Int): Symbol =
      kernel.Definitions_TupleClass(arity)

    /** Contains Scala primitive value classes:
     *   - Byte
     *   - Short
     *   - Int
     *   - Long
     *   - Float
     *   - Double
     *   - Char
     *   - Boolean
     *   - Unit
     */
    def ScalaPrimitiveValueClasses: List[Symbol] =
      UnitClass :: BooleanClass :: ScalaNumericValueClasses

    /** Contains Scala numeric value classes:
     *   - Byte
     *   - Short
     *   - Int
     *   - Long
     *   - Float
     *   - Double
     *   - Char
     */
    def ScalaNumericValueClasses: List[Symbol] =
      ByteClass :: ShortClass :: IntClass :: LongClass :: FloatClass :: DoubleClass :: CharClass :: Nil

  }

  /** Defines standard types.
   *  @group Definitions
   */
  trait StandardTypes {
    /** The type of primitive type `Unit`. */
    def UnitType: Type = kernel.Definitions_UnitType

    /** The type of primitive type `Byte`. */
    def ByteType: Type = kernel.Definitions_ByteType

    /** The type of primitive type `Short`. */
    def ShortType: Type = kernel.Definitions_ShortType

    /** The type of primitive type `Char`. */
    def CharType: Type = kernel.Definitions_CharType

    /** The type of primitive type `Int`. */
    def IntType: Type = kernel.Definitions_IntType

    /** The type of primitive type `Long`. */
    def LongType: Type = kernel.Definitions_LongType

    /** The type of primitive type `Float`. */
    def FloatType: Type = kernel.Definitions_FloatType

    /** The type of primitive type `Double`. */
    def DoubleType: Type = kernel.Definitions_DoubleType

    /** The type of primitive type `Boolean`. */
    def BooleanType: Type = kernel.Definitions_BooleanType

    /** The type of core type `Any`. */
    def AnyType: Type = kernel.Definitions_AnyType

    /** The type of core type `AnyVal`. */
    def AnyValType: Type = kernel.Definitions_AnyValType

    /** The type of core type `AnyRef`. */
    def AnyRefType: Type = kernel.Definitions_AnyRefType

    /** The type of core type `Object`. */
    def ObjectType: Type = kernel.Definitions_ObjectType

    /** The type of core type `Nothing`. */
    def NothingType: Type = kernel.Definitions_NothingType

    /** The type of core type `Null`. */
    def NullType: Type = kernel.Definitions_NullType

    /** The type for `scala.String`. */
    def StringType: Type = kernel.Definitions_StringType
  }

  //
  // TREE UTILS
  //

  abstract class TreeAccumulator[X] {

    // Ties the knot of the traversal: call `foldOver(x, tree))` to dive in the `tree` node.
    def foldTree(x: X, tree: Tree)(implicit ctx: Context): X
    def foldTypeTree(x: X, tree: TypeOrBoundsTree)(implicit ctx: Context): X
    def foldCaseDef(x: X, tree: CaseDef)(implicit ctx: Context): X
    def foldTypeCaseDef(x: X, tree: TypeCaseDef)(implicit ctx: Context): X
    def foldPattern(x: X, tree: Pattern)(implicit ctx: Context): X

    def foldTrees(x: X, trees: Iterable[Tree])(implicit ctx: Context): X = (x /: trees)(foldTree)
    def foldTypeTrees(x: X, trees: Iterable[TypeOrBoundsTree])(implicit ctx: Context): X = (x /: trees)(foldTypeTree)
    def foldCaseDefs(x: X, trees: Iterable[CaseDef])(implicit ctx: Context): X = (x /: trees)(foldCaseDef)
    def foldTypeCaseDefs(x: X, trees: Iterable[TypeCaseDef])(implicit ctx: Context): X = (x /: trees)(foldTypeCaseDef)
    def foldPatterns(x: X, trees: Iterable[Pattern])(implicit ctx: Context): X = (x /: trees)(foldPattern)
    private def foldParents(x: X, trees: Iterable[TermOrTypeTree])(implicit ctx: Context): X = (x /: trees)(foldTermOrTypeTree)

    def foldOverTree(x: X, tree: Tree)(implicit ctx: Context): X = {
      def localCtx(definition: Definition): Context = definition.symbol.localContext
      tree match {
        case Term.Ident(_) =>
          x
        case Term.Select(qualifier, _) =>
          foldTree(x, qualifier)
        case Term.This(qual) =>
          x
        case Term.Super(qual, _) =>
          foldTree(x, qual)
        case Term.Apply(fun, args) =>
          foldTrees(foldTree(x, fun), args)
        case Term.TypeApply(fun, args) =>
          foldTypeTrees(foldTree(x, fun), args)
        case Term.Literal(const) =>
          x
        case Term.New(tpt) =>
          foldTypeTree(x, tpt)
        case Term.Typed(expr, tpt) =>
          foldTypeTree(foldTree(x, expr), tpt)
        case Term.NamedArg(_, arg) =>
          foldTree(x, arg)
        case Term.Assign(lhs, rhs) =>
          foldTree(foldTree(x, lhs), rhs)
        case Term.Block(stats, expr) =>
          foldTree(foldTrees(x, stats), expr)
        case Term.If(cond, thenp, elsep) =>
          foldTree(foldTree(foldTree(x, cond), thenp), elsep)
        case Term.Lambda(meth, tpt) =>
          val a = foldTree(x, meth)
          tpt.fold(a)(b => foldTypeTree(a, b))
        case Term.Match(selector, cases) =>
          foldCaseDefs(foldTree(x, selector), cases)
        case Term.Return(expr) =>
          foldTree(x, expr)
        case Term.Try(block, handler, finalizer) =>
          foldTrees(foldCaseDefs(foldTree(x, block), handler), finalizer)
        case Term.Repeated(elems, elemtpt) =>
          foldTrees(foldTypeTree(x, elemtpt), elems)
        case Term.Inlined(call, bindings, expansion) =>
          foldTree(foldTrees(x, bindings), expansion)
        case IsDefinition(vdef @ ValDef(_, tpt, rhs)) =>
          implicit val ctx = localCtx(vdef)
          foldTrees(foldTypeTree(x, tpt), rhs)
        case IsDefinition(ddef @ DefDef(_, tparams, vparamss, tpt, rhs)) =>
          implicit val ctx = localCtx(ddef)
          foldTrees(foldTypeTree((foldTrees(x, tparams) /: vparamss)(foldTrees), tpt), rhs)
        case IsDefinition(tdef @ TypeDef(_, rhs)) =>
          implicit val ctx = localCtx(tdef)
          foldTypeTree(x, rhs)
        case IsDefinition(cdef @ ClassDef(_, constr, parents, derived, self, body)) =>
          implicit val ctx = localCtx(cdef)
          foldTrees(foldTrees(foldTypeTrees(foldParents(foldTree(x, constr), parents), derived), self), body)
        case Import(_, expr, _) =>
          foldTree(x, expr)
        case IsPackageClause(clause @ PackageClause(pid, stats)) =>
          foldTrees(foldTree(x, pid), stats)(clause.symbol.localContext)
      }
    }

    def foldOverTypeTree(x: X, tree: TypeOrBoundsTree)(implicit ctx: Context): X = tree match {
      case TypeTree.Inferred() => x
      case TypeTree.Ident(_) => x
      case TypeTree.Select(qualifier, _) => foldTree(x, qualifier)
      case TypeTree.Projection(qualifier, _) => foldTypeTree(x, qualifier)
      case TypeTree.Singleton(ref) => foldTree(x, ref)
      case TypeTree.Refined(tpt, refinements) => foldTrees(foldTypeTree(x, tpt), refinements)
      case TypeTree.Applied(tpt, args) => foldTypeTrees(foldTypeTree(x, tpt), args)
      case TypeTree.ByName(result) => foldTypeTree(x, result)
      case TypeTree.Annotated(arg, annot) => foldTree(foldTypeTree(x, arg), annot)
      case TypeTree.LambdaTypeTree(typedefs, arg) => foldTypeTree(foldTrees(x, typedefs), arg)
      case TypeTree.TypeBind(_, tbt) => foldTypeTree(x, tbt)
      case TypeTree.TypeBlock(typedefs, tpt) => foldTypeTree(foldTrees(x, typedefs), tpt)
      case TypeTree.MatchType(boundopt, selector, cases) =>
        foldTypeCaseDefs(foldTypeTree(boundopt.fold(x)(foldTypeTree(x, _)), selector), cases)
      case WildcardTypeTree() => x
      case TypeBoundsTree(lo, hi) => foldTypeTree(foldTypeTree(x, lo), hi)
    }

    def foldOverCaseDef(x: X, tree: CaseDef)(implicit ctx: Context): X = tree match {
      case CaseDef(pat, guard, body) => foldTree(foldTrees(foldPattern(x, pat), guard), body)
    }

    def foldOverTypeCaseDef(x: X, tree: TypeCaseDef)(implicit ctx: Context): X = tree match {
      case TypeCaseDef(pat, body) => foldTypeTree(foldTypeTree(x, pat), body)
    }

    def foldOverPattern(x: X, tree: Pattern)(implicit ctx: Context): X = tree match {
      case Pattern.Value(v) => foldTree(x, v)
      case Pattern.Bind(_, body) => foldPattern(x, body)
      case Pattern.Unapply(fun, implicits, patterns) => foldPatterns(foldTrees(foldTree(x, fun), implicits), patterns)
      case Pattern.Alternatives(patterns) => foldPatterns(x, patterns)
      case Pattern.TypeTest(tpt) => foldTypeTree(x, tpt)
    }

    private def foldTermOrTypeTree(x: X, tree: TermOrTypeTree)(implicit ctx: Context): X = tree match {
      case IsTerm(termOrTypeTree) => foldTree(x, termOrTypeTree)
      case IsTypeTree(termOrTypeTree) => foldTypeTree(x, termOrTypeTree)
    }

  }

  abstract class TreeTraverser extends TreeAccumulator[Unit] {

    def traverseTree(tree: Tree)(implicit ctx: Context): Unit = traverseTreeChildren(tree)
    def traverseTypeTree(tree: TypeOrBoundsTree)(implicit ctx: Context): Unit = traverseTypeTreeChildren(tree)
    def traverseCaseDef(tree: CaseDef)(implicit ctx: Context): Unit = traverseCaseDefChildren(tree)
    def traverseTypeCaseDef(tree: TypeCaseDef)(implicit ctx: Context): Unit = traverseTypeCaseDefChildren(tree)
    def traversePattern(tree: Pattern)(implicit ctx: Context): Unit = traversePatternChildren(tree)

    def foldTree(x: Unit, tree: Tree)(implicit ctx: Context): Unit = traverseTree(tree)
    def foldTypeTree(x: Unit, tree: TypeOrBoundsTree)(implicit ctx: Context) = traverseTypeTree(tree)
    def foldCaseDef(x: Unit, tree: CaseDef)(implicit ctx: Context) = traverseCaseDef(tree)
    def foldTypeCaseDef(x: Unit, tree: TypeCaseDef)(implicit ctx: Context) = traverseTypeCaseDef(tree)
    def foldPattern(x: Unit, tree: Pattern)(implicit ctx: Context) = traversePattern(tree)

    protected def traverseTreeChildren(tree: Tree)(implicit ctx: Context): Unit = foldOverTree((), tree)
    protected def traverseTypeTreeChildren(tree: TypeOrBoundsTree)(implicit ctx: Context): Unit = foldOverTypeTree((), tree)
    protected def traverseCaseDefChildren(tree: CaseDef)(implicit ctx: Context): Unit = foldOverCaseDef((), tree)
    protected def traverseTypeCaseDefChildren(tree: TypeCaseDef)(implicit ctx: Context): Unit = foldOverTypeCaseDef((), tree)
    protected def traversePatternChildren(tree: Pattern)(implicit ctx: Context): Unit = foldOverPattern((), tree)

  }

  abstract class TreeMap { self =>

    def transformTree(tree: Tree)(implicit ctx: Context): Tree = {
      tree match {
        case IsPackageClause(tree) =>
          PackageClause.copy(tree)(transformTerm(tree.pid).asInstanceOf[Term.Ref], transformTrees(tree.stats)(tree.symbol.localContext))
        case IsImport(tree) =>
          Import.copy(tree)(tree.impliedOnly, transformTerm(tree.expr), tree.selectors)
        case IsStatement(tree) =>
          transformStatement(tree)
      }
    }

    def transformStatement(tree: Statement)(implicit ctx: Context): Statement = {
      def localCtx(definition: Definition): Context = definition.symbol.localContext
      tree match {
        case IsTerm(tree) =>
          transformTerm(tree)
        case IsValDef(tree) =>
          implicit val ctx = localCtx(tree)
          val tpt1 = transformTypeTree(tree.tpt)
          val rhs1 = tree.rhs.map(x => transformTerm(x))
          ValDef.copy(tree)(tree.name, tpt1, rhs1)
        case IsDefDef(tree) =>
          implicit val ctx = localCtx(tree)
          DefDef.copy(tree)(tree.name, transformSubTrees(tree.typeParams), tree.paramss mapConserve (transformSubTrees(_)), transformTypeTree(tree.returnTpt), tree.rhs.map(x => transformTerm(x)))
        case IsTypeDef(tree) =>
          implicit val ctx = localCtx(tree)
          TypeDef.copy(tree)(tree.name, transformTypeOrBoundsTree(tree.rhs))
        case IsClassDef(tree) =>
          ClassDef.copy(tree)(tree.name, tree.constructor, tree.parents, tree.derived, tree.self, tree.body)
        case IsImport(tree) =>
          Import.copy(tree)(tree.impliedOnly, transformTerm(tree.expr), tree.selectors)
      }
    }

    def transformTerm(tree: Term)(implicit ctx: Context): Term = {
      tree match {
        case Term.Ident(name) =>
          tree
        case Term.Select(qualifier, name) =>
          Term.Select.copy(tree)(transformTerm(qualifier), name)
        case Term.This(qual) =>
          tree
        case Term.Super(qual, mix) =>
          Term.Super.copy(tree)(transformTerm(qual), mix)
        case Term.Apply(fun, args) =>
          Term.Apply.copy(tree)(transformTerm(fun), transformTerms(args))
        case Term.TypeApply(fun, args) =>
          Term.TypeApply.copy(tree)(transformTerm(fun), transformTypeTrees(args))
        case Term.Literal(const) =>
          tree
        case Term.New(tpt) =>
          Term.New.copy(tree)(transformTypeTree(tpt))
        case Term.Typed(expr, tpt) =>
          Term.Typed.copy(tree)(transformTerm(expr), transformTypeTree(tpt))
        case Term.IsNamedArg(tree) =>
          Term.NamedArg.copy(tree)(tree.name, transformTerm(tree.value))
        case Term.Assign(lhs, rhs) =>
          Term.Assign.copy(tree)(transformTerm(lhs), transformTerm(rhs))
        case Term.Block(stats, expr) =>
          Term.Block.copy(tree)(transformStats(stats), transformTerm(expr))
        case Term.If(cond, thenp, elsep) =>
          Term.If.copy(tree)(transformTerm(cond), transformTerm(thenp), transformTerm(elsep))
        case Term.Lambda(meth, tpt) =>
          Term.Lambda.copy(tree)(transformTerm(meth), tpt.map(x => transformTypeTree(x)))
        case Term.Match(selector, cases) =>
          Term.Match.copy(tree)(transformTerm(selector), transformCaseDefs(cases))
        case Term.Return(expr) =>
          Term.Return.copy(tree)(transformTerm(expr))
        case Term.While(cond, body) =>
          Term.While.copy(tree)(transformTerm(cond), transformTerm(body))
        case Term.Try(block, cases, finalizer) =>
          Term.Try.copy(tree)(transformTerm(block), transformCaseDefs(cases), finalizer.map(x => transformTerm(x)))
        case Term.Repeated(elems, elemtpt) =>
          Term.Repeated.copy(tree)(transformTerms(elems), transformTypeTree(elemtpt))
        case Term.Inlined(call, bindings, expansion) =>
          Term.Inlined.copy(tree)(call, transformSubTrees(bindings), transformTerm(expansion)/*()call.symbol.localContext)*/)
      }
    }

    def transformTypeOrBoundsTree(tree: TypeOrBoundsTree)(implicit ctx: Context): TypeOrBoundsTree = tree match {
      case IsTypeTree(tree) => transformTypeTree(tree)
      case IsTypeBoundsTree(tree) => tree // TODO traverse tree
    }

    def transformTypeTree(tree: TypeTree)(implicit ctx: Context): TypeTree = tree match {
      case TypeTree.Inferred() => tree
      case TypeTree.IsIdent(tree) => tree
      case TypeTree.IsSelect(tree) =>
        TypeTree.Select.copy(tree)(tree.qualifier, tree.name)
      case TypeTree.IsProjection(tree) =>
        TypeTree.Projection.copy(tree)(tree.qualifier, tree.name)
      case TypeTree.IsAnnotated(tree) =>
        TypeTree.Annotated.copy(tree)(tree.arg, tree.annotation)
      case TypeTree.IsSingleton(tree) =>
        TypeTree.Singleton.copy(tree)(transformTerm(tree.ref))
      case TypeTree.IsRefined(tree) =>
        TypeTree.Refined.copy(tree)(transformTypeTree(tree.tpt), transformTrees(tree.refinements).asInstanceOf[List[Definition]])
      case TypeTree.IsApplied(tree) =>
        TypeTree.Applied.copy(tree)(transformTypeTree(tree.tpt), transformTypeOrBoundsTrees(tree.args))
      case TypeTree.IsMatchType(tree) =>
        TypeTree.MatchType.copy(tree)(tree.bound.map(b => transformTypeTree(b)), transformTypeTree(tree.selector), transformTypeCaseDefs(tree.cases))
      case TypeTree.IsByName(tree) =>
        TypeTree.ByName.copy(tree)(transformTypeTree(tree.result))
      case TypeTree.IsLambdaTypeTree(tree) =>
        TypeTree.LambdaTypeTree.copy(tree)(transformSubTrees(tree.tparams), transformTypeOrBoundsTree(tree.body))(tree.symbol.localContext)
      case TypeTree.IsTypeBind(tree) =>
        TypeTree.TypeBind.copy(tree)(tree.name, tree.body)
      case TypeTree.IsTypeBlock(tree) =>
        TypeTree.TypeBlock.copy(tree)(tree.aliases, tree.tpt)
    }

    def transformCaseDef(tree: CaseDef)(implicit ctx: Context): CaseDef = {
      CaseDef.copy(tree)(transformPattern(tree.pattern), tree.guard.map(transformTerm), transformTerm(tree.rhs))
    }

    def transformTypeCaseDef(tree: TypeCaseDef)(implicit ctx: Context): TypeCaseDef = {
      TypeCaseDef.copy(tree)(transformTypeTree(tree.pattern), transformTypeTree(tree.rhs))
    }

    def transformPattern(pattern: Pattern)(implicit ctx: Context): Pattern = pattern match {
      case Pattern.Value(_) =>
        pattern
      case Pattern.IsTypeTest(pattern) =>
        Pattern.TypeTest.copy(pattern)(transformTypeTree(pattern.tpt))
      case Pattern.IsUnapply(pattern) =>
        Pattern.Unapply.copy(pattern)(transformTerm(pattern.fun), transformSubTrees(pattern.implicits), transformPatterns(pattern.patterns))
      case Pattern.IsAlternatives(pattern) =>
        Pattern.Alternatives.copy(pattern)(transformPatterns(pattern.patterns))
      case Pattern.IsBind(pattern) =>
        Pattern.Bind.copy(pattern)(pattern.name, transformPattern(pattern.pattern))
    }

    def transformStats(trees: List[Statement])(implicit ctx: Context): List[Statement] =
      trees mapConserve (transformStatement(_))

    def transformTrees(trees: List[Tree])(implicit ctx: Context): List[Tree] =
      trees mapConserve (transformTree(_))

    def transformTerms(trees: List[Term])(implicit ctx: Context): List[Term] =
      trees mapConserve (transformTerm(_))

    def transformTypeTrees(trees: List[TypeTree])(implicit ctx: Context): List[TypeTree] =
      trees mapConserve (transformTypeTree(_))

    def transformTypeOrBoundsTrees(trees: List[TypeOrBoundsTree])(implicit ctx: Context): List[TypeOrBoundsTree] =
      trees mapConserve (transformTypeOrBoundsTree(_))

    def transformCaseDefs(trees: List[CaseDef])(implicit ctx: Context): List[CaseDef] =
      trees mapConserve (transformCaseDef(_))

    def transformTypeCaseDefs(trees: List[TypeCaseDef])(implicit ctx: Context): List[TypeCaseDef] =
      trees mapConserve (transformTypeCaseDef(_))

    def transformPatterns(trees: List[Pattern])(implicit ctx: Context): List[Pattern] =
      trees mapConserve (transformPattern(_))

    def transformSubTrees[Tr <: Tree](trees: List[Tr])(implicit ctx: Context): List[Tr] =
      transformTrees(trees).asInstanceOf[List[Tr]]

  }

  //
  // PRINTERS
  //

  private val printers = new Printers[self.type](this)
  import printers._

  abstract class Printer {

    def showTree(tree: Tree)(implicit ctx: Context): String

    def showCaseDef(caseDef: CaseDef)(implicit ctx: Context): String

    def showPattern(pattern: Pattern)(implicit ctx: Context): String

    def showTypeOrBoundsTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): String

    def showTypeOrBounds(tpe: TypeOrBounds)(implicit ctx: Context): String

    def showConstant(const: Constant)(implicit ctx: Context): String

    def showSymbol(symbol: Symbol)(implicit ctx: Context): String

    def showFlags(flags: Flags)(implicit ctx: Context): String

  }

  /** Adds `show` as an extension method of a `Tree` */
  implicit class TreeShowDeco(tree: Tree) {

  }

  /** Adds `show` as an extension method of a `TypeOrBoundsTree` */
  implicit class TypeOrBoundsTreeShowDeco(tpt: TypeOrBoundsTree) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showTypeOrBoundsTree(tpt)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showTypeOrBoundsTree(tpt)
  }

  /** Adds `show` as an extension method of a `TypeOrBounds` */
  implicit class TypeOrBoundsShowDeco(tpe: TypeOrBounds) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showTypeOrBounds(tpe)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showTypeOrBounds(tpe)
  }

  /** Adds `show` as an extension method of a `CaseDef` */
  implicit class CaseDefShowDeco(caseDef: CaseDef) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showCaseDef(caseDef)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showCaseDef(caseDef)
  }

  /** Adds `show` as an extension method of a `Pattern` */
  implicit class PatternShowDeco(pattern: Pattern) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showPattern(pattern)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showPattern(pattern)
  }

  /** Adds `show` as an extension method of a `Constant` */
  implicit class ConstantShowDeco(const: Constant) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showConstant(const)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showConstant(const)
  }

  /** Adds `show` as an extension method of a `Symbol` */
  implicit class SymbolShowDeco(symbol: Symbol) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showSymbol(symbol)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showSymbol(symbol)
  }

  /** Adds `show` as an extension method of a `Flags` */
  implicit class FlagsShowDeco(flags: Flags) {
    /** Shows the tree as extractors */
    def show(implicit ctx: Context): String = new ExtractorsPrinter().showFlags(flags)
    /** Shows the tree as source code */
    def showCode(implicit ctx: Context): String = new SourceCodePrinter().showFlags(flags)
  }

  //
  // OTHERS
  //

  def typeOf[T: scala.quoted.Type]: Type =
    kernel.QuotedType_unseal(implicitly[scala.quoted.Type[T]]).tpe

  val util: reflect.utils.TreeUtils { val reflect: self.type } = new reflect.utils.TreeUtils {
    val reflect: self.type = self
  }
}

object Reflection {
  /** Compiler tasty context available in a top level ~ of an inline macro */
  def macroContext: Reflection = throw new Exception("Not in inline macro.")
}
