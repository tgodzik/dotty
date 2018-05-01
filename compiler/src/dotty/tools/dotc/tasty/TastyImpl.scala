package dotty.tools.dotc.tasty

import dotty.tools.dotc.ast.{Trees, tpd, untpd}
import dotty.tools.dotc.core
import dotty.tools.dotc.core._
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Symbols.{ClassSymbol, Symbol}
import dotty.tools.dotc.core.Decorators._

import scala.tasty.{Context, Position}

object TastyImpl extends scala.tasty.Tasty {

  type Id = untpd.Ident

  // ===== Names ====================================================

  type Name = Names.Name
  type PossiblySignedName = Names.TermName
  type TermName = Names.TermName
  type SignedName = Names.TermName
  type TypeName = Names.TypeName

  val Simple: SimpleExtractor = new SimpleExtractor {
    def unapply(x: TermName): Option[String] = x match {
      case x: Names.SimpleName => Some(x.toString)
      case _ => None
    }
  }

  val Qualified: QualifiedExtractor = new QualifiedExtractor {
    def unapply(x: TermName): Option[(TermName, String)] = x match {
      case x: Names.DerivedName if x.is(NameKinds.QualifiedName) =>
        Some(x.underlying, x.lastPart.toString)
      case _ => None
    }
  }

  val DefaultGetter: DefaultGetterExtractor = new DefaultGetterExtractor {
    def unapply(x: TermName): Option[(TermName, String)] = x match {
      case x: Names.DerivedName if x.is(NameKinds.DefaultGetterName) =>
        Some(x.underlying, x.lastPart.toString)
      case _ => None
    }
  }

  val Variant: VariantExtractor = new VariantExtractor {
    def unapply(x: TermName): Option[(TermName, Boolean)] = x match {
      case x: Names.DerivedName if x.is(NameKinds.VariantName) =>
        Some(x.underlying, x.info.asInstanceOf[NameKinds.NumberedInfo].num == 1)
      case _ => None
    }
  }

  val SuperAccessor: SuperAccessorExtractor = new SuperAccessorExtractor {
    def unapply(x: TermName): Option[TermName] = x match {
      case x: Names.DerivedName if x.is(NameKinds.SuperAccessorName) => Some(x.underlying)
      case _ => None
    }
  }

  val ProtectedAccessor: ProtectedAccessorExtractor = new ProtectedAccessorExtractor {
    def unapply(x: TermName): Option[TermName] = x match {
      case x: Names.DerivedName if x.is(NameKinds.ProtectedAccessorName) => Some(x.underlying)
      case _ => None
    }
  }

  val ProtectedSetter: ProtectedSetterExtractor = new ProtectedSetterExtractor {
    def unapply(x: TermName): Option[TermName] = x match {
      case x: Names.DerivedName if x.is(NameKinds.ProtectedSetterName) => Some(x.underlying)
      case _ => None
    }
  }

  val ObjectClass: ObjectClassExtractor = new ObjectClassExtractor {
    def unapply(x: TermName): Option[TermName] = x match {
      case x: Names.DerivedName if x.is(NameKinds.ModuleClassName) => Some(x.underlying)
      case _ => None
    }
  }

  val SignedName: SignedNameExtractor = new SignedNameExtractor {
    def unapply(x: SignedName): Option[(TermName, TypeName, List[TypeName])] = {
      if (!x.is(NameKinds.SignedName)) None
      else {
        val NameKinds.SignedName.SignedInfo(sig) = x.info
        Some(x.underlying, sig.resSig, sig.paramsSig)
      }
    }
  }

  val TypeName: TypeNameExtractor = new TypeNameExtractor {
    def unapply(x: TypeName): Option[TermName] = x match {
      case x: Names.TypeName => Some(x.toTermName)
      case _ => None
    }
  }

  // ===== Trees ====================================================

  type Tree = tpd.Tree

  implicit def TreeDeco(t: Tree): Positioned = new Positioned {
    override def pos(implicit ctx: Context): Position = new TastyPosition(t.pos)
  }

  // ----- Top Level Statements -----------------------------------------------

  type TopLevelStatement = tpd.Tree

  val PackageClause: PackageClauseExtractor = new PackageClauseExtractor {
    def unapply(x: TopLevelStatement): Option[(Term, List[TopLevelStatement])] = x match {
      case Trees.PackageDef(pid, stats) => Some((pid, stats))
      case _ => None
    }
  }

  // ----- Statements -----------------------------------------------

  type Statement = tpd.Tree

  type Import = tpd.Import
  val Import: ImportExtractor = new ImportExtractor {
    def unapply(x: Import): Option[(Term, List[ImportSelector])] = x match {
      case Trees.Import(expr, selectors) => Some((expr, selectors))
      case _ => None
    }
  }

  type ImportSelector = untpd.Tree
  val SimpleSelector: SimpleSelectorExtractor = new SimpleSelectorExtractor {
    def unapply(x: ImportSelector): Option[Id] = x match {
      case id @ Trees.Ident(_) => Some(id)
      case _ => None
    }
  }

  val RenameSelector: RenameSelectorExtractor = new RenameSelectorExtractor {
    def unapply(x: ImportSelector): Option[(Id, Id)] = x match {
      case Trees.Thicket((id1@Trees.Ident(_)) :: (id2@Trees.Ident(_)) :: Nil) if id2.name != nme.WILDCARD => Some(id1, id2)
      case _ => None
    }
  }

  val OmitSelector: OmitSelectorExtractor = new OmitSelectorExtractor {
    def unapply(x: ImportSelector): Option[Id] = x match {
      case Trees.Thicket((id@Trees.Ident(_)) :: Trees.Ident(nme.WILDCARD) :: Nil) => Some(id)
      case _ => None
    }
  }

  // ----- Definitions ----------------------------------------------

  type Definition = tpd.Tree

  def Definition(sym: Symbol)(implicit ctx: Context): Definition = {
//    if (sym.is(Package)) PackageDef(sym)
//    else if (sym == defn.AnyClass) NoDefinition // FIXME
//    else if (sym == defn.NothingClass) NoDefinition // FIXME
//    else if (sym.isClass) ClassDef(sym.asClass)
//    else if (sym.isType) TypeDef(sym.asType)
//    else if (sym.is(core.Flags.Method)) DefDef(sym.asTerm)
//    else ValDef(sym.asTerm)
    ???
  }

  implicit def DefinitionDeco(x: Definition): AbstractDefinition = new AbstractDefinition {

    def owner(implicit ctx: Context): Definition = Definition(x.symbol.owner)

    def mods(implicit ctx: Context): List[Modifier] = {
      val privateWithin = x.symbol.privateWithin
      val isProtected = x.symbol.is(core.Flags.Protected)
      ModFlags(new FlagSet(x.symbol.flags)) ::
      (if (privateWithin.exists) List(ModQual(privateWithin.typeRef, isProtected)) else Nil) :::
      x.symbol.annotations.map(t => ModAnnot(t.tree))
    }

    def localContext(implicit ctx: Context): Context =  new TastyContext(
      if (x.hasType && x.symbol.exists) context.withOwner(x.symbol)
      else context
    )
  }

  type ClassDef = tpd.TypeDef
  val ClassDef: ClassDefExtractor = new ClassDefExtractor {

//    def apply(sym: ClassSymbol)(implicit ctx: Context): trees.ClassDef = {
//      def toTree(sym: ClassSymbol): tpd.TypeDef = {
//        val constr = tpd.DefDef(sym.unforcedDecls.find(_.isPrimaryConstructor).asTerm)
//        val body = sym.unforcedDecls.filter(!_.isPrimaryConstructor).map(s =>
//          if (s.isClass) toTree(s.asClass)
//          else if (s.isType) tpd.TypeDef(s.asType)
//          else if (s.is(Method)) tpd.DefDef(s.asTerm)
//          else tpd.ValDef(s.asTerm)
//        )
//        val superArgs = Nil // TODO
//        tpd.ClassDef(sym, constr, body, superArgs)
//      }
//      new Impl(toTree(sym))
//    }

    def unapply(x: ClassDef)(implicit ctx: Context): Option[(TypeName, DefDef, List[Tree] /* List[Term | TypeTree] */,  Option[ValDef], List[Statement])] = x match {
      case x @ Trees.TypeDef(name, impl@Trees.Template(constr, parents, self, _)) =>
        val selfVal = if (self.isEmpty) None else Some(self)
        Some((name, constr, parents, selfVal, impl.body))
      case _ => None
    }
  }

  type DefDef = tpd.DefDef
  val DefDef: DefDefExtractor = new DefDefExtractor {
    def unapply(x: DefDef)(implicit ctx: Context): Option[(TermName, List[TypeDef],  List[List[ValDef]], TypeTree, Option[Term])] = x match {
      case x @ Trees.DefDef(name, tparams, vparamss, tpt, _) =>
        Some((name, tparams, vparamss, tpt, if (x.rhs.isEmpty) None else Some(x.rhs)))
      case _ => None
    }
  }

  type ValDef = tpd.ValDef
  val ValDef: ValDefExtractor = new ValDefExtractor {
    def unapply(x: ValDef)(implicit ctx: Context): Option[(TermName, TypeTree, Option[Term])] = x match {
      case x @ Trees.ValDef(name, tpt, _) =>
        Some((name, tpt, if (x.rhs.isEmpty) None else Some(x.rhs)))
      case _ => None
    }
  }

  type TypeDef = tpd.TypeDef
  val TypeDef: TypeDefExtractor = new TypeDefExtractor {
    def unapply(x: TypeDef)(implicit ctx: Context): Option[(TypeName, Tree /* Type | TypeBounds */)] = x match {
      case x @ Trees.TypeDef(name, rhs) if !x.symbol.isClass => Some((name, rhs))
      case _ => None
    }
  }

//  type PackageDef = Symbol
//  val PackageDef: PackageDefExtractor = new PackageDefExtractor {
//    def unapply(x: PackageDef)(implicit ctx: Context): Option[(Name, List[Statement])] = ???
//  }

  // ----- Terms ----------------------------------------------------

  type Term = tpd.Tree

  override implicit def TermDeco(t: Term): Typed = typed(t)

  val Ident: IdentExtractor = new IdentExtractor {
    def unapply(x: Term): Option[TermName] = x match {
      case Trees.Ident(name: Names.TermName) if x.isTerm => Some(name)
      case _ => None
    }
  }

  val Select: SelectExtractor = new SelectExtractor {
    def unapply(x: Term): Option[(Term, PossiblySignedName)] = x match {
      case id@Trees.Select(qual, name: Names.TermName) if id.isTerm => Some(qual, name)
      case _ => None
    }
  }

  val Literal: LiteralExtractor = new LiteralExtractor {
    def unapply(x: Term): Option[Constant] = x match {
      case Trees.Literal(const) => Some(const)
      case _ => None
    }
  }

  val This: ThisExtractor = new ThisExtractor {
    def unapply(x: Term): Option[Option[Id]] = x match {
      case Trees.This(qual) => Some(if (qual.isEmpty) None else Some(qual))
      case _ => None
    }
  }

  val New: NewExtractor = new NewExtractor {
    def unapply(x: Term): Option[TypeTree] = x match {
      case Trees.New(tpt) => Some(tpt)
      case _ => None
    }
  }

  val NamedArg: NamedArgExtractor = new NamedArgExtractor {
    def unapply(x: Term): Option[(TermName, Term)] = x match {
      case Trees.NamedArg(name: Names.TermName, argument) => Some(name, argument)
      case _ => None
    }
  }

  val Apply: ApplyExtractor = new ApplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term])] = x match {
      case Trees.Apply(fn, args) => Some((fn, args))
      case _ => None
    }
  }

  val TypeApply: TypeApplyExtractor = new TypeApplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term])] = x match {
      case Trees.TypeApply(fn, args) => Some((fn, args))
      case _ => None
    }
  }

  val Super: SuperExtractor = new SuperExtractor {
    def unapply(x: Term): Option[(Term, Option[Id])] = x match {
      case Trees.Super(qual, mixin) => Some((qual, if (mixin.isEmpty) None else Some(mixin)))
      case _ => None
    }
  }

  val Typed: TypedExtractor = new TypedExtractor {
    def unapply(x: Term): Option[(Term, TypeTree)] = x match {
      case Trees.Typed(expr, tpt) => Some((expr, tpt))
      case _ => None
    }
  }

  val Assign: AssignExtractor = new AssignExtractor {
    def unapply(x: Term): Option[(Term, Term)] = x match {
      case Trees.Assign(lhs, rhs) => Some((lhs, rhs))
      case _ => None
    }
  }

  val Block: BlockExtractor = new BlockExtractor {
    def unapply(x: Term): Option[(List[Statement], Term)] = x match {
      case Trees.Block(stats, expr) => Some((stats, expr))
      case _ => None
    }
  }

  val Inlined: InlinedExtractor = new InlinedExtractor {
    def unapply(x: Term): Option[(Term, List[Statement], Term)] = x match {
      case Trees.Inlined(call, bindings, expansion) =>
        Some((call, bindings, expansion))
      case _ => None
    }
  }

  val Lambda: LambdaExtractor = new LambdaExtractor {
    def unapply(x: Term): Option[(Term, Option[TypeTree])] = x match {
      case Trees.Closure(_, meth, tpt) => Some((meth, if (tpt.isEmpty) None else Some(tpt)))
      case _ => None
    }
  }

  val If: IfExtractor = new IfExtractor {
    def unapply(x: Term): Option[(Term, Term, Term)] = x match {
      case Trees.If(cond, thenp, elsep) => Some((cond, thenp, elsep))
      case _ => None
    }
  }

  val Match: MatchExtractor = new MatchExtractor {
    def unapply(x: Term): Option[(Term, List[CaseDef])] = x match {
      case Trees.Match(selector, cases) => Some((selector, cases))
      case _ => None
    }
  }

  val Try: TryExtractor = new TryExtractor {
    def unapply(x: Term): Option[(Term, List[CaseDef], Option[Term])] = x match {
      case Trees.Try(body, catches, finalizer) => Some((body, catches, if (finalizer.isEmpty) None else Some(finalizer)))
      case _ => None
    }
  }

  val Return: ReturnExtractor = new ReturnExtractor {
    def unapply(x: Term): Option[Term] = x match {
      case Trees.Return(expr, from) => Some(expr) // TODO use `from` or remove it
      case _ => None
    }
  }

  val Repeated: RepeatedExtractor = new RepeatedExtractor {
    def unapply(x: Term): Option[List[Term]] = x match {
      case Trees.SeqLiteral(args, elemtpt) => Some(args) // TODO use `elemtpt`?
      case _ => None
    }
  }

  val SelectOuter: SelectOuterExtractor = new SelectOuterExtractor {
    def unapply(x: Term): Option[(Term, Int, Type)] = x match {
      case sel@Trees.Select(qual, NameKinds.OuterSelectName(_, levels)) => Some((qual, levels, sel.tpe))
      case _ => None
    }
  }

  // ----- CaseDef --------------------------------------------------

  type CaseDef = tpd.CaseDef

  val CaseDef: CaseDefExtractor = new CaseDefExtractor {
    def unapply(x: CaseDef): Option[(Pattern, Option[Term], Term)] = x match {
      case Trees.CaseDef(pat, guard, body) =>
        Some(pat, if (guard.isEmpty) None else Some(guard), body)
      case _ => None
    }
  }

  // ----- Patterns -------------------------------------------------

  type Pattern = tpd.Tree

  override implicit def PatternDeco(x: Pattern): Typed = typed(x)

  val Value: ValueExtractor = new ValueExtractor {
    def unapply(x: Term): Option[Term] = x match {
      case lit: tpd.Literal => Some(lit)
      case ident: tpd.Ident => Some(ident)
      case _ => None
    }
  }

  val Bind: BindExtractor = new BindExtractor {
    def unapply(x: Term): Option[(TermName, Pattern)] = x match {
      case Trees.Bind(name: Names.TermName, body) => Some(name, body)
      case _ => None
    }
  }

  val Unapply: UnapplyExtractor = new UnapplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term], List[Pattern])] = x match {
      case Trees.UnApply(fun, implicits, patterns) => Some(fun, implicits, patterns)
      case _ => None
    }
  }

  val Alternative: AlternativeExtractor = new AlternativeExtractor {
    def unapply(x: Term): Option[List[Pattern]] = x match {
      case Trees.Alternative(patterns) => Some(patterns)
      case _ => None
    }
  }

  val TypeTest: TypeTestExtractor = new TypeTestExtractor {
    def unapply(x: Term): Option[TypeTree] = x match {
      case Trees.Typed(_, tpt) => Some(tpt)
      case _ => None
    }
  }

  // ----- TypeTrees ------------------------------------------------

  type TypeTree = tpd.Tree

  implicit def TypeTreeDeco(x: TypeTree): AbstractTypeTree = new AbstractTypeTree {
    def tpe: Types.Type = x.tpe
  }

  val Synthetic: SyntheticExtractor = new SyntheticExtractor {
    def unapply(x: TypeTree): Boolean = x match {
      case Trees.TypeTree() => true
      case _ => false
    }
  }

  val TypeIdent: TypeIdentExtractor = new TypeIdentExtractor {
    def unapply(x: TypeTree): Option[TypeName] = x match {
      case id@Trees.Ident(name: Names.TypeName) if id.isType => Some(name)
      case _ => None
    }
  }

  val TypeSelect: TypeSelectExtractor = new TypeSelectExtractor {
    def unapply(x: TypeTree): Option[(Term, TypeName)] = x match {
      case id@Trees.Select(qual, name: Names.TypeName) if id.isType => Some(qual, name)
      case _ => None
    }
  }

  val Singleton: SingletonExtractor = new SingletonExtractor {
    def unapply(x: TypeTree): Option[Term] = x match {
      case Trees.SingletonTypeTree(ref) => Some(ref)
      case _ => None
    }
  }

  val Refined: RefinedExtractor = new RefinedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, List[Definition])] = x match {
      case Trees.RefinedTypeTree(tpt, refinements) => Some(tpt, refinements)
      case _ => None
    }
  }

  val Applied: AppliedExtractor = new AppliedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, List[TypeTree])] = x match {
      case Trees.AppliedTypeTree(tycon, args) => Some(tycon, args)
      case _ => None
    }
  }

  val Annotated: AnnotatedExtractor = new AnnotatedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, Term)] = x match {
      case Trees.Annotated(argument, annot) => Some(argument, annot)
      case _ => None
    }
  }

  val And: AndExtractor = new AndExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, TypeTree)] = x match {
      case Trees.AndTypeTree(left, right) => Some(left, right)
      case _ => None
    }
  }

  val Or: OrExtractor = new OrExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, TypeTree)] = x match {
      case Trees.OrTypeTree(left, right) => Some(left, right)
      case _ => None
    }
  }

  val ByName: ByNameExtractor = new ByNameExtractor {
    def unapply(x: TypeTree): Option[TypeTree] = x match {
      case Trees.ByNameTypeTree(tpt) => Some(tpt)
      case _ => None
    }
  }

  // ----- TypeBoundsTrees ------------------------------------------------

  type TypeBoundsTree = tpd.TypeBoundsTree

  implicit def TypeBoundsTreeDeco(x: TypeBoundsTree): AbstractTypeBoundsTree = ???

  val TypeBoundsTree: TypeBoundsTreeExtractor = new TypeBoundsTreeExtractor {
    def unapply(x: TypeBoundsTree)(implicit ctx: Context): Option[(TypeTree, TypeTree)] = x match {
      case Trees.TypeBoundsTree(lo, hi) => Some(lo, hi)
      case _ => None
    }
  }

  // ===== Types ====================================================

  type MaybeType = Types.Type

  // ----- Types ----------------------------------------------------

  type Type = Types.Type

  val ConstantType: ConstantTypeExtractor = new ConstantTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Constant] = x match {
      case Types.ConstantType(value) => Some(value)
      case _ => None
    }
  }

  val SymRef: SymRefExtractor = new SymRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Definition, MaybeType /* Type | NoPrefix */)] = x  match {
      case tp: Types.NamedType =>
        tp.designator match {
          case sym: Symbol => Some((Definition(sym), tp.prefix))
          case _ => None
        }
      case _ => None
    }
  }

  val NameRef: NameRefExtractor = new NameRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Name, MaybeType /* Type | NoPrefix */)] = x match {
      case tp: Types.NamedType =>
        tp.designator match {
          case name: Names.Name => Some(name, tp.prefix)
          case _ => None
        }
      case _ => None
    }
  }

  val SuperType: SuperTypeExtractor = new SuperTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.SuperType(thistpe, supertpe) => Some(thistpe, supertpe)
      case _ => None
    }
  }

  val Refinement: RefinementExtractor = new RefinementExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Name, MaybeType /* Type | TypeBounds */)] = x match {
      case Types.RefinedType(parent, name, info) => Some(parent, name, info)
      case _ => None
    }
  }

  val AppliedType: AppliedTypeExtractor = new AppliedTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, List[MaybeType /* Type | TypeBounds */])] = x match {
      case Types.AppliedType(tycon, args) => Some((tycon, args))
      case _ => None
    }
  }

  val AnnotatedType: AnnotatedTypeExtractor = new AnnotatedTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Term)] = x match {
      case Types.AnnotatedType(underlying, annot) => Some((underlying, annot.tree))
      case _ => None
    }
  }

  val AndType: AndTypeExtractor = new AndTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.AndType(left, right) => Some(left, right)
      case _ => None
    }
  }

  val OrType: OrTypeExtractor = new OrTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.OrType(left, right) => Some(left, right)
      case _ => None
    }
  }

  val ByNameType: ByNameTypeExtractor = new ByNameTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Type] = x match {
      case Types.ExprType(resType) => Some(resType)
      case _ => None
    }
  }

  val ParamRef: ParamRefExtractor = new ParamRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(LambdaType[_, _], Int)] = x match {
      case Types.TypeParamRef(binder, idx) => Some(binder, idx)
      case _ => None
    }
  }

  val ThisType: ThisTypeExtractor = new ThisTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Type] = x match {
      case Types.ThisType(tp) => Some(tp)
      case _ => None
    }
  }

  val RecursiveThis: RecursiveThisExtractor = new RecursiveThisExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[RecursiveType] = x match {
      case Types.RecThis(binder) => Some(binder)
      case _ => None
    }
  }

  type RecursiveType = Types.RecType
  val RecursiveType: RecursiveTypeExtractor = new RecursiveTypeExtractor {
    def unapply(x: RecursiveType)(implicit ctx: Context): Option[Type] = x match {
      case tp: Types.RecType => Some(tp.underlying)
      case _ => None
    }
  }

  // ----- Methodic Types -------------------------------------------

  type LambdaType[ParamName <: Name, ParamInfo <: MaybeType] =
    Types.LambdaType { type ThisName = ParamName; type PInfo = ParamInfo }

  type MethodType = Types.MethodType

  override implicit def MethodTypeDeco(x: MethodType): AbstractMethodType = new AbstractMethodType {
    def isErased: Boolean = x.isErasedMethod
    def isImplicit: Boolean = x.isImplicitMethod
  }

  val MethodType: MethodTypeExtractor = new MethodTypeExtractor {
    def unapply(x: MethodType): Option[(List[TermName], List[Type], Type)] = x match {
      case x: MethodType => Some(x.paramNames, x.paramInfos, x.resType)
      case _ => None
    }
  }

  type PolyType = Types.PolyType
  val PolyType: PolyTypeExtractor = new PolyTypeExtractor {
    def unapply(x: PolyType): Option[(List[TypeName], List[TypeBounds], Type)] = x match {
      case x: PolyType => Some(x.paramNames, x.paramInfos, x.resType)
      case _ => None
    }
  }

  type TypeLambda = Types.TypeLambda
  val TypeLambda: TypeLambdaExtractor = new TypeLambdaExtractor {
    def unapply(x: TypeLambda): Option[(List[TypeName], List[TypeBounds], Type)] = x match {
      case x: TypeLambda => Some(x.paramNames, x.paramInfos, x.resType)
      case _ => None
    }
  }

  // ----- TypeBounds ------------------------------------------------

  type TypeBounds = Types.TypeBounds

  val TypeBounds: TypeBoundsExtractor = new TypeBoundsExtractor {
    def unapply(x: TypeBounds): Option[(Type, Type)] = x match {
      case x: Types.TypeBounds => Some(x.lo, x.hi)
      case _ => None
    }
  }

  // ----- NoPrefix --------------------------------------------------

  type NoPrefix = Types.NoPrefix.type

  val NoPrefix: NoPrefixExtractor = new NoPrefixExtractor {
    def unapply(x: NoPrefix): Boolean = x == Types.NoPrefix
  }

  // ===== Constants ================================================

  type Constant = Constants.Constant

  implicit def ConstantDeco(x: Constant): AbstractConstant = new AbstractConstant {
    def value: Any = x.value
  }

  val UnitConstant: UnitExtractor = _.tag == Constants.UnitTag

  val NullConstant: NullExtractor = _.tag == Constants.NullTag

  val BooleanConstant: BooleanExtractor = const =>
    if (const.tag == Constants.BooleanTag) Some(const.booleanValue)
    else None

  val ByteConstant: ByteExtractor = const =>
    if (const.tag == Constants.ByteTag) Some(const.byteValue)
    else None

  val ShortConstant: ShortExtractor = const =>
    if (const.tag == Constants.ShortTag) Some(const.shortValue)
    else None

  val CharConstant: CharExtractor = const =>
    if (const.tag == Constants.CharTag) Some(const.charValue)
    else None

  val IntConstant: IntExtractor = const =>
    if (const.tag == Constants.IntTag) Some(const.intValue)
    else None

  val LongConstant: LongExtractor = const =>
    if (const.tag == Constants.LongTag) Some(const.longValue)
    else None

  val FloatConstant: FloatExtractor = const =>
    if (const.tag == Constants.FloatTag) Some(const.floatValue)
    else None

  val DoubleConstant: DoubleExtractor = const =>
    if (const.tag == Constants.DoubleTag) Some(const.doubleValue)
    else None

  val StringConstant: StringExtractor = const =>
    if (const.tag == Constants.StringTag) Some(const.stringValue)
    else None

  // ===== Constants ================================================

  type Modifier = ModImpl // TODO

  trait ModImpl
  case class ModAnnot(tree: Term) extends ModImpl
  case class ModFlags(flags: FlagSet) extends ModImpl
  case class ModQual(tp: Type, protect: Boolean) extends ModImpl


  val Annotation: AnnotationExtractor = new AnnotationExtractor {
    def unapply(x: Modifier): Option[Term] = x match {
      case ModAnnot(tree) => Some(tree)
      case _ => None
    }
  }

  val Flags: FlagsExtractor = new FlagsExtractor {
    def unapply(x: Modifier): Option[FlagSet] = x match {
      case ModFlags(flags) => Some(flags)
      case _ => None
    }
  }

  val QualifiedPrivate: QualifiedPrivateExtractor = new QualifiedPrivateExtractor {
    def unapply(x: Modifier): Option[Type] = x match {
      case ModQual(tp, false) => Some(tp)
      case _ => None
    }
  }

  val QualifiedProtected: QualifiedProtectedExtractor = new QualifiedProtectedExtractor {
    def unapply(x: Modifier): Option[Type] = x match {
      case ModQual(tp, true) => Some(tp)
      case _ => None
    }
  }

  // ===== Private Methids ==========================================

  private implicit def context(implicit tctx: Context): Contexts.Context =
    tctx.asInstanceOf[TastyContext].ctx

  def typed(tree: tpd.Tree): Typed = new Typed {
    def tpe: Types.Type = tree.tpe
  }
}
