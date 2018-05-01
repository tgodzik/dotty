package dotty.tools.dotc.tasty

import dotty.tools.dotc.ast.{Trees, tpd, untpd}
import dotty.tools.dotc.core
import dotty.tools.dotc.core._
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.quoted.PickledQuotes
import dotty.tools.dotc.util.SourcePosition

import scala.quoted

object TastyImpl extends scala.tasty.Tasty {

  // ===== Quotes ===================================================

  implicit def QuotedExprDeco[T](x: quoted.Expr[T]): AbstractQuotedExpr = new AbstractQuotedExpr {
    def toTasty(implicit ctx: Context): Term = PickledQuotes.quotedExprToTree(x)
  }

  implicit def QuotedTypeDeco[T](x: quoted.Type[T]): AbstractQuotedType = new AbstractQuotedType {
    def toTasty(implicit ctx: Context): TypeTree = PickledQuotes.quotedTypeToTree(x)
  }

  // ===== Contexts =================================================

  type Context = Contexts.Context

  implicit def ContextDeco(ctx: Context): AbstractContext = new AbstractContext {
    def owner: Definition = FromSymbol.definition(ctx.owner)(ctx)
  }

  // ===== Id =======================================================

  type Id = untpd.Ident

  implicit def IdDeco(x: Id): Positioned =  new Positioned {
    def pos(implicit ctx: Context): Position = new TastyPosition(x.pos)
  }

  val Id: IdExtractor = new IdExtractor {
    def unapply(x: Any) = x match {
      case x: untpd.Ident => Some(x.name.toString) // TODO how to make sure it is not a Ident or TypeIdent? Check x.tpe?
      case _ => None
    }
  }

  // ===== Trees ====================================================


  // ----- Top Level Statements -----------------------------------------------

  type TopLevelStatement = tpd.Tree

  implicit def TopLevelStatementDeco(t: TopLevelStatement): Positioned = new Positioned {
    def pos(implicit ctx: Context): Position = new TastyPosition(t.pos)
  }

  type PackageClause = tpd.PackageDef

  val PackageClause: PackageClauseExtractor = new PackageClauseExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[TopLevelStatement])] = x match {
      case x: tpd.PackageDef @unchecked => Some((x.pid, x.stats))
      case _ => None
    }
  }

  implicit def PackageClauseDeco(x: PackageClause): AbstractPackageClause = new AbstractPackageClause {
    override def definition: Definition = ???
  }

  // ----- Statements -----------------------------------------------

  type Statement = tpd.Tree

  type Import = tpd.Import
  val Import: ImportExtractor = new ImportExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[ImportSelector])] = x match {
      case x: tpd.Import @unchecked => Some((x.expr, x.selectors))
      case _ => None
    }
  }

  type ImportSelector = untpd.Tree
  val SimpleSelector: SimpleSelectorExtractor = new SimpleSelectorExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Id] = x match {
      case x: untpd.Ident => Some(x) // TODO make sure it will not match other idents
      case _ => None
    }
  }

  val RenameSelector: RenameSelectorExtractor = new RenameSelectorExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Id, Id)] = x match {
      case Trees.Thicket((id1: untpd.Ident) :: (id2: untpd.Ident) :: Nil) if id2.name != nme.WILDCARD => Some(id1, id2)
      case _ => None
    }
  }

  val OmitSelector: OmitSelectorExtractor = new OmitSelectorExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Id] = x match {
      case Trees.Thicket((id: untpd.Ident) :: Trees.Ident(nme.WILDCARD) :: Nil) => Some(id)
      case _ => None
    }
  }

  // ----- Definitions ----------------------------------------------

  type Definition = tpd.Tree

  override val Definition: DefinitionExtractor = new DefinitionExtractor {
    override def unapply(x: Any)(implicit ctx: Context): Option[Definition] = x match {
      case x: tpd.MemberDef @unchecked => Some(x)
      case _ => None
    }
  }

  implicit def DefinitionDeco(x: Definition): AbstractDefinition = new AbstractDefinition {

    def owner(implicit ctx: Context): Definition = FromSymbol.definition(x.symbol.owner)

    def mods(implicit ctx: Context): List[Modifier] = {
      val privateWithin = x.symbol.privateWithin
      val isProtected = x.symbol.is(core.Flags.Protected)
      ModFlags(new FlagSet(x.symbol.flags)) ::
      (if (privateWithin.exists) List(ModQual(privateWithin.typeRef, isProtected)) else Nil) :::
      x.symbol.annotations.map(t => ModAnnot(t.tree))
    }

    def localContext(implicit ctx: Context): Context =
      if (x.hasType && x.symbol.exists) ctx.withOwner(x.symbol)
      else ctx
  }

  type Parent = tpd.Tree

  type ClassDef = tpd.TypeDef
  val ClassDef: ClassDefExtractor = new ClassDefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, DefDef, List[Parent] /* List[Term | TypeTree] */,  Option[ValDef], List[Statement])] = x match {
      case x: tpd.TypeDef @unchecked if x.isClassDef =>
        val temp @ Trees.Template(constr, parents, self, _) = x.rhs
        val selfVal = if (self.isEmpty) None else Some(self)
        Some((x.name.toString, constr, parents, selfVal, temp.body))
      case _ => None
    }
  }

  type DefDef = tpd.DefDef
  val DefDef: DefDefExtractor = new DefDefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, List[TypeDef],  List[List[ValDef]], TypeTree, Option[Term])] = x match {
      case x: tpd.DefDef @unchecked =>
        Some((x.name.toString, x.tparams, x.vparamss, x.tpt, if (x.rhs.isEmpty) None else Some(x.rhs)))
      case _ => None
    }
  }

  type ValDef = tpd.ValDef
  val ValDef: ValDefExtractor = new ValDefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, TypeTree, Option[Term])] = x match {
      case x: tpd.ValDef @unchecked =>
        Some((x.name.toString, x.tpt, if (x.rhs.isEmpty) None else Some(x.rhs)))
      case _ => None
    }
  }

  type TypeDef = tpd.TypeDef
  val TypeDef: TypeDefExtractor = new TypeDefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, MaybeTypeTree /* TypeTree | TypeBoundsTree */)] = x match {
      case x: tpd.TypeDef @unchecked if !x.symbol.isClass => Some((x.name.toString, x.rhs))
      case _ => None
    }
  }

//  type PackageDef = Symbol
//  val PackageDef: PackageDefExtractor = new PackageDefExtractor {
//    def unapply(x: PackageDef)(implicit ctx: Context): Option[(Name, List[Statement])] = ???
//  }

  // ----- Terms ----------------------------------------------------

  type Term = tpd.Tree

  implicit def TermDeco(t: Term): Typed = new Typed {
    def tpe: Types.Type = t.tpe
  }

  val Ident: IdentExtractor = new IdentExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[String] = x match {
      case x @ Trees.Ident(name: Names.TermName) if x.isTerm => Some(name.show)
      case _ => None
    }
  }

  val Select: SelectExtractor = new SelectExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, String)] = x match {
      case x: tpd.Select @unchecked if x.isTerm => Some((x.qualifier, x.name.toString))
      case _ => None
    }
  }

  val Literal: LiteralExtractor = new LiteralExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Constant] = x match {
      case Trees.Literal(const) => Some(const)
      case _ => None
    }
  }

  val This: ThisExtractor = new ThisExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Option[Id]] = x match {
      case Trees.This(qual) => Some(if (qual.isEmpty) None else Some(qual))
      case _ => None
    }
  }

  val New: NewExtractor = new NewExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[TypeTree] = x match {
      case x: tpd.New @unchecked => Some(x.tpt)
      case _ => None
    }
  }

  val NamedArg: NamedArgExtractor = new NamedArgExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, Term)] = x match {
      case x: tpd.NamedArg @unchecked if x.name.isInstanceOf[Names.TermName] => Some((x.name.toString, x.arg))
      case _ => None
    }
  }

  val Apply: ApplyExtractor = new ApplyExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[Term])] = x match {
      case x: tpd.Apply @unchecked => Some((x.fun, x.args))
      case _ => None
    }
  }

  val TypeApply: TypeApplyExtractor = new TypeApplyExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[Term])] = x match {
      case x: tpd.TypeApply @unchecked => Some((x.fun, x.args))
      case _ => None
    }
  }

  val Super: SuperExtractor = new SuperExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, Option[Id])] = x match {
      case x: tpd.Super @unchecked => Some((x.qual, if (x.mix.isEmpty) None else Some(x.mix)))
      case _ => None
    }
  }

  val Typed: TypedExtractor = new TypedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, TypeTree)] = x match {
      case x: tpd.Typed @unchecked => Some((x.expr, x.tpt))
      case _ => None
    }
  }

  val Assign: AssignExtractor = new AssignExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, Term)] = x match {
      case x: tpd.Assign @unchecked => Some((x.lhs, x.rhs))
      case _ => None
    }
  }

  val Block: BlockExtractor = new BlockExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(List[Statement], Term)] = x match {
      case x: tpd.Block @unchecked => Some((x.stats, x.expr))
      case _ => None
    }
  }

  val Inlined: InlinedExtractor = new InlinedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[Statement], Term)] = x match {
      case x: tpd.Inlined @unchecked =>
        Some((x.call, x.bindings, x.expansion))
      case _ => None
    }
  }

  val Lambda: LambdaExtractor = new LambdaExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, Option[TypeTree])] = x match {
      case x: tpd.Closure @unchecked => Some((x.meth, if (x.tpt.isEmpty) None else Some(x.tpt)))
      case _ => None
    }
  }

  val If: IfExtractor = new IfExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, Term, Term)] = x match {
      case x: tpd.If @unchecked => Some((x.cond, x.thenp, x.elsep))
      case _ => None
    }
  }

  val Match: MatchExtractor = new MatchExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[CaseDef])] = x match {
      case x: tpd.Match @unchecked => Some((x.selector, x.cases))
      case _ => None
    }
  }

  val Try: TryExtractor = new TryExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[CaseDef], Option[Term])] = x match {
      case x: tpd.Try @unchecked => Some((x.expr, x.cases, if (x.finalizer.isEmpty) None else Some(x.finalizer)))
      case _ => None
    }
  }

  val Return: ReturnExtractor = new ReturnExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Term] = x match {
      case x: tpd.Return @unchecked => Some(x.expr)
      case _ => None
    }
  }

  val Repeated: RepeatedExtractor = new RepeatedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[List[Term]] = x match {
      case x: tpd.SeqLiteral @unchecked => Some(x.elems)
      case _ => None
    }
  }

  val SelectOuter: SelectOuterExtractor = new SelectOuterExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, Int, Type)] = x match {
      case x: tpd.Select @unchecked =>
        x.name match {
          case NameKinds.OuterSelectName(_, levels) => Some((x.qualifier, levels, x.tpe))
          case _ => None
        }
      case _ => None
    }
  }

  // ----- CaseDef --------------------------------------------------

  type CaseDef = tpd.CaseDef

  val CaseDef: CaseDefExtractor = new CaseDefExtractor {
    def unapply(x: Any): Option[(Pattern, Option[Term], Term)] = x match {
      case x: tpd.CaseDef @unchecked =>
        Some(x.pat, if (x.guard.isEmpty) None else Some(x.guard), x.body)
      case _ => None
    }
  }

  // ----- Patterns -------------------------------------------------

  type Pattern = tpd.Tree

  implicit def PatternDeco(x: Pattern): Typed = new Typed {
    def tpe: Types.Type = x.tpe
  }

  val Value: ValueExtractor = new ValueExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Term] = x match {
      case lit: tpd.Literal @unchecked => Some(lit)
      case ident: tpd.Ident @unchecked => Some(ident)
      case _ => None
    }
  }

  val Bind: BindExtractor = new BindExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, Pattern)] = x match {
      case x: tpd.Bind @unchecked if x.name.isInstanceOf[Names.TermName] => Some(x.name.toString, x.body)
      case _ => None
    }
  }

  val Unapply: UnapplyExtractor = new UnapplyExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, List[Term], List[Pattern])] = x match {
      case x: tpd.UnApply @unchecked => Some(x.fun, x.implicits, x.patterns)
      case _ => None
    }
  }

  val Alternative: AlternativeExtractor = new AlternativeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[List[Pattern]] = x match {
      case x: tpd.Alternative @unchecked => Some(x.trees)
      case _ => None
    }
  }

  val TypeTest: TypeTestExtractor = new TypeTestExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[TypeTree] = x match {
      case x: tpd.Typed @unchecked => Some(x.tpt)
      case _ => None
    }
  }

  // ----- MaybeTypeTree ------------------------------------------------

  type MaybeTypeTree = tpd.Tree

  implicit def MaybeTypeTreeDeco(x: MaybeTypeTree): AbstractMaybeTypeTree = new AbstractMaybeTypeTree {
    def tpe: Type = x.tpe
  }

  // ----- TypeTrees ------------------------------------------------

  type TypeTree = tpd.Tree

  implicit def TypeTreeDeco(x: TypeTree): Typed = new Typed {
    def tpe: Types.Type = x.tpe
  }

  val Synthetic: SyntheticExtractor = new SyntheticExtractor {
    def unapply(x: Any)(implicit ctx: Context): Boolean = x match {
      case Trees.TypeTree() => true
      case _ => false
    }
  }

  val TypeIdent: TypeIdentExtractor = new TypeIdentExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[String] = x match {
      case x: tpd.Ident @unchecked if x.isType => Some(x.name.toString)
      case _ => None
    }
  }

  val TypeSelect: TypeSelectExtractor = new TypeSelectExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Term, String)] = x match {
      case x: tpd.Select @unchecked if x.isType => Some(x.qualifier, x.name.toString)
      case _ => None
    }
  }

  val Singleton: SingletonExtractor = new SingletonExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Term] = x match {
      case x: tpd.SingletonTypeTree @unchecked => Some(x.ref)
      case _ => None
    }
  }

  val Refined: RefinedExtractor = new RefinedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, List[Definition])] = x match {
      case x: tpd.RefinedTypeTree @unchecked => Some(x.tpt, x.refinements)
      case _ => None
    }
  }

  val Applied: AppliedExtractor = new AppliedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, List[TypeTree])] = x match {
      case x: tpd.AppliedTypeTree @unchecked => Some(x.tpt, x.args)
      case _ => None
    }
  }

  val Annotated: AnnotatedExtractor = new AnnotatedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, Term)] = x match {
      case x: tpd.Annotated @unchecked => Some(x.arg, x.annot)
      case _ => None
    }
  }

  val And: AndExtractor = new AndExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, TypeTree)] = x match {
      case x: tpd.AndTypeTree @unchecked => Some(x.left, x.right)
      case _ => None
    }
  }

  val Or: OrExtractor = new OrExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, TypeTree)] = x match {
      case x: tpd.OrTypeTree @unchecked => Some(x.left, x.right)
      case _ => None
    }
  }

  val ByName: ByNameExtractor = new ByNameExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[TypeTree] = x match {
      case x: tpd.ByNameTypeTree @unchecked => Some(x.result)
      case _ => None
    }
  }

  // ----- TypeBoundsTrees ------------------------------------------------

  type TypeBoundsTree = tpd.TypeBoundsTree

  implicit def TypeBoundsTreeDeco(x: TypeBoundsTree): AbstractTypeBoundsTree = ???

  val TypeBoundsTree: TypeBoundsTreeExtractor = new TypeBoundsTreeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(TypeTree, TypeTree)] = x match {
      case x: tpd.TypeBoundsTree @unchecked => Some(x.lo, x.hi)
      case _ => None
    }
  }

  // ===== Types ====================================================

  type MaybeType = Types.Type

  // ----- Types ----------------------------------------------------

  type Type = Types.Type

  val ConstantType: ConstantTypeExtractor = new ConstantTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Constant] = x match {
      case Types.ConstantType(value) => Some(value)
      case _ => None
    }
  }

  val SymRef: SymRefExtractor = new SymRefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Definition, MaybeType /* Type | NoPrefix */)] = x  match {
      case tp: Types.NamedType =>
        tp.designator match {
          case sym: Symbol => Some((FromSymbol.definition(sym), tp.prefix))
          case _ => None
        }
      case _ => None
    }
  }

  val NameRef: NameRefExtractor = new NameRefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(String, MaybeType /* Type | NoPrefix */)] = x match {
      case tp: Types.NamedType =>
        tp.designator match {
          case name: Names.Name => Some(name.toString, tp.prefix)
          case _ => None
        }
      case _ => None
    }
  }

  val SuperType: SuperTypeExtractor = new SuperTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.SuperType(thistpe, supertpe) => Some(thistpe, supertpe)
      case _ => None
    }
  }

  val Refinement: RefinementExtractor = new RefinementExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, String, MaybeType /* Type | TypeBounds */)] = x match {
      case Types.RefinedType(parent, name, info) => Some(parent, name.toString, info)
      case _ => None
    }
  }

  val AppliedType: AppliedTypeExtractor = new AppliedTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, List[MaybeType /* Type | TypeBounds */])] = x match {
      case Types.AppliedType(tycon, args) => Some((tycon, args))
      case _ => None
    }
  }

  val AnnotatedType: AnnotatedTypeExtractor = new AnnotatedTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, Term)] = x match {
      case Types.AnnotatedType(underlying, annot) => Some((underlying, annot.tree))
      case _ => None
    }
  }

  val AndType: AndTypeExtractor = new AndTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.AndType(left, right) => Some(left, right)
      case _ => None
    }
  }

  val OrType: OrTypeExtractor = new OrTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case Types.OrType(left, right) => Some(left, right)
      case _ => None
    }
  }

  val ByNameType: ByNameTypeExtractor = new ByNameTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Type] = x match {
      case Types.ExprType(resType) => Some(resType)
      case _ => None
    }
  }

  val ParamRef: ParamRefExtractor = new ParamRefExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(LambdaType[_], Int)] = x match {
      case Types.TypeParamRef(binder, idx) => Some(binder, idx)
      case _ => None
    }
  }

  val ThisType: ThisTypeExtractor = new ThisTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Type] = x match {
      case Types.ThisType(tp) => Some(tp)
      case _ => None
    }
  }

  val RecursiveThis: RecursiveThisExtractor = new RecursiveThisExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[RecursiveType] = x match {
      case Types.RecThis(binder) => Some(binder)
      case _ => None
    }
  }

  type RecursiveType = Types.RecType
  val RecursiveType: RecursiveTypeExtractor = new RecursiveTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Type] = x match {
      case tp: Types.RecType => Some(tp.underlying)
      case _ => None
    }
  }

  // ----- Methodic Types -------------------------------------------

  type LambdaType[ParamInfo <: MaybeType] = Types.LambdaType { type PInfo = ParamInfo }

  type MethodType = Types.MethodType

  implicit def MethodTypeDeco(x: MethodType): AbstractMethodType = new AbstractMethodType {
    def isErased: Boolean = x.isErasedMethod
    def isImplicit: Boolean = x.isImplicitMethod
  }

  val MethodType: MethodTypeExtractor = new MethodTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(List[String], List[Type], Type)] = x match {
      case x: MethodType => Some(x.paramNames.map(_.toString), x.paramInfos, x.resType)
      case _ => None
    }
  }

  type PolyType = Types.PolyType
  val PolyType: PolyTypeExtractor = new PolyTypeExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(List[String], List[TypeBounds], Type)] = x match {
      case x: PolyType => Some(x.paramNames.map(_.toString), x.paramInfos, x.resType)
      case _ => None
    }
  }

  type TypeLambda = Types.TypeLambda
  val TypeLambda: TypeLambdaExtractor = new TypeLambdaExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(List[String], List[TypeBounds], Type)] = x match {
      case x: TypeLambda => Some(x.paramNames.map(_.toString), x.paramInfos, x.resType)
      case _ => None
    }
  }

  // ----- TypeBounds ------------------------------------------------

  type TypeBounds = Types.TypeBounds

  val TypeBounds: TypeBoundsExtractor = new TypeBoundsExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[(Type, Type)] = x match {
      case x: Types.TypeBounds => Some(x.lo, x.hi)
      case _ => None
    }
  }

  // ----- NoPrefix --------------------------------------------------

  type NoPrefix = Types.NoPrefix.type

  val NoPrefix: NoPrefixExtractor = new NoPrefixExtractor {
    def unapply(x: Any)(implicit ctx: Context): Boolean = x == Types.NoPrefix
  }

  // ===== Constants ================================================

  type Constant = Constants.Constant

  implicit def ConstantDeco(x: Constant): AbstractConstant = new AbstractConstant {
    def value: Any = x.value
  }

  val UnitConstant: UnitExtractor = new UnitExtractor {
    def unapply(x: Any): Boolean = x match {
      case x: Constants.Constant => x.tag == Constants.UnitTag
      case _ => false
    }
  }

  val NullConstant: NullExtractor = new NullExtractor {
    def unapply(x: Any): Boolean =  x match {
      case x: Constants.Constant => x.tag == Constants.NullTag
      case _ => false
    }
  }

  val BooleanConstant: BooleanExtractor = new BooleanExtractor {
    def unapply(x: Any): Option[Boolean] = x match {
      case x: Constants.Constant if x.tag == Constants.BooleanTag => Some(x.booleanValue)
      case _ => None
    }
  }

  val ByteConstant: ByteExtractor = new ByteExtractor {
    def unapply(x: Any): Option[Byte] = x match {
      case x: Constants.Constant if x.tag == Constants.ByteTag => Some(x.byteValue)
      case _ => None
    }
  }

  val ShortConstant: ShortExtractor = new ShortExtractor {
    def unapply(x: Any): Option[Short] = x match {
      case x: Constants.Constant if x.tag == Constants.ShortTag => Some(x.shortValue)
      case _ => None
    }
  }

  val CharConstant: CharExtractor = new CharExtractor {
    def unapply(x: Any): Option[Char] = x match {
      case x: Constants.Constant if x.tag == Constants.CharTag => Some(x.charValue)
      case _ => None
    }
  }

  val IntConstant: IntExtractor = new IntExtractor {
    def unapply(x: Any): Option[Int] = x match {
      case x: Constants.Constant if x.tag == Constants.IntTag => Some(x.intValue)
      case _ => None
    }
  }

  val LongConstant: LongExtractor = new LongExtractor {
    def unapply(x: Any): Option[Long] = x match {
      case x: Constants.Constant if x.tag == Constants.LongTag => Some(x.longValue)
      case _ => None
    }
  }

  val FloatConstant: FloatExtractor = new FloatExtractor {
    def unapply(x: Any): Option[Float] = x match {
      case x: Constants.Constant if x.tag == Constants.FloatTag => Some(x.floatValue)
      case _ => None
    }
  }

  val DoubleConstant: DoubleExtractor = new DoubleExtractor {
    def unapply(x: Any): Option[Double] = x match {
      case x: Constants.Constant if x.tag == Constants.DoubleTag => Some(x.doubleValue)
      case _ => None
    }
  }

  val StringConstant: StringExtractor = new StringExtractor {
    def unapply(x: Any): Option[String] = x match {
      case x: Constants.Constant if x.tag == Constants.StringTag => Some(x.stringValue)
      case _ => None
    }
  }

  // ===== Constants ================================================

  type Modifier = ModImpl // TODO

  trait ModImpl
  case class ModAnnot(tree: Term) extends ModImpl
  case class ModFlags(flags: FlagSet) extends ModImpl
  case class ModQual(tp: Type, protect: Boolean) extends ModImpl


  val Annotation: AnnotationExtractor = new AnnotationExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Term] = x match {
      case ModAnnot(tree) => Some(tree)
      case _ => None
    }
  }

  val Flags: FlagsExtractor = new FlagsExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[FlagSet] = x match {
      case ModFlags(flags) => Some(flags)
      case _ => None
    }
  }

  val QualifiedPrivate: QualifiedPrivateExtractor = new QualifiedPrivateExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Type] = x match {
      case ModQual(tp, false) => Some(tp)
      case _ => None
    }
  }

  val QualifiedProtected: QualifiedProtectedExtractor = new QualifiedProtectedExtractor {
    def unapply(x: Any)(implicit ctx: Context): Option[Type] = x match {
      case ModQual(tp, true) => Some(tp)
      case _ => None
    }
  }

  // ===== Private Methods ==========================================

  private class TastyPosition(val pos: SourcePosition) extends Position {
    def start = pos.start
    def end = pos.end

    def sourceFile = pos.source.file.jpath

    def startLine = pos.startLine
    def endLine = pos.endLine

    def startColumn = pos.startColumn
    def endColumn = pos.endColumn

    override def toString: String = s"Position(${pos.line}, ${pos.column})"
  }
}
