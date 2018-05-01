package scala.tasty

abstract class Tasty {

  type Id

  trait Positioned {
    def pos(implicit ctx: Context): Position
  }

  trait Typed {
    def tpe: Type
  }

  // ===== Names ====================================================

  type Name

  type PossiblySignedName

  // ----- TermName -------------------------------------------------

  type TermName <: Name with PossiblySignedName

  val Simple: SimpleExtractor
  abstract class SimpleExtractor {
    def unapply(x: TermName): Option[String]
  }

  // s"$prefix.$name"
  val Qualified: QualifiedExtractor
  abstract class QualifiedExtractor {
    def unapply(x: TermName): Option[(TermName, String)]
  }

  // s"$methodName${"$default$"}${idx+1}"
  val DefaultGetter: DefaultGetterExtractor
  abstract class DefaultGetterExtractor {
    def unapply(x: TermName): Option[(TermName, String)]
  }

  // s"${if (covariant) "+" else "-"}$underlying"
  val Variant: VariantExtractor
  abstract class VariantExtractor {
    def unapply(x: TermName): Option[(TermName, Boolean)]
  }

  // s"${"super$"}$underlying"
  val SuperAccessor: SuperAccessorExtractor
  abstract class SuperAccessorExtractor {
    def unapply(x: TermName): Option[TermName]
  }

  // s"${"protected$"}$underlying"
  val ProtectedAccessor: ProtectedAccessorExtractor
  abstract class ProtectedAccessorExtractor {
    def unapply(x: TermName): Option[TermName]
  }

  // s"${"protected$set"}$underlying"
  val ProtectedSetter: ProtectedSetterExtractor
  abstract class ProtectedSetterExtractor {
    def unapply(x: TermName): Option[TermName]
  }

  // s"$underlying${"$"}"
  val ObjectClass: ObjectClassExtractor
  abstract class ObjectClassExtractor {
    def unapply(x: TermName): Option[TermName]
  }

  // ----- SignedName -------------------------------------------------

  type SignedName <: PossiblySignedName

  val SignedName: SignedNameExtractor
  abstract class SignedNameExtractor {
    def unapply(x: SignedName): Option[(TermName, TypeName, List[TypeName])]
  }

  // ----- TypeName -------------------------------------------------

  type TypeName <: Name

  val TypeName: TypeNameExtractor
  abstract class TypeNameExtractor {
    def unapply(x: TypeName): Option[TermName]
  }

  // ===== Trees ====================================================

  type Tree
  implicit def TreeDeco(t: Tree): Positioned

  // ----- Top Level Statements -----------------------------------------------

  type TopLevelStatement <: Tree

  val PackageClause: PackageClauseExtractor
  abstract class PackageClauseExtractor {
    def unapply(x: TopLevelStatement): Option[(Term, List[TopLevelStatement])]
  }

  // ----- Statements -----------------------------------------------

  type Statement <: TopLevelStatement

  type Import <: Statement

  val Import: ImportExtractor
  abstract class ImportExtractor {
    def unapply(x: Import): Option[(Term, List[ImportSelector])]
  }

  type ImportSelector

  val SimpleSelector: SimpleSelectorExtractor
  abstract class SimpleSelectorExtractor {
    def unapply(x: ImportSelector): Option[Id]
  }

  val RenameSelector: RenameSelectorExtractor
  abstract class RenameSelectorExtractor {
    def unapply(x: ImportSelector): Option[(Id, Id)]
  }

  val OmitSelector: OmitSelectorExtractor
  abstract class OmitSelectorExtractor {
    def unapply(x: ImportSelector): Option[Id]
  }

  // ----- Definitions ----------------------------------------------

  type Definition <: Statement

  trait AbstractDefinition {
    def mods(implicit ctx: Context): List[Modifier]
    def owner(implicit ctx: Context): Definition
    def localContext(implicit ctx: Context): Context
  }
  implicit def DefinitionDeco(x: Definition): AbstractDefinition

  type ClassDef <: Definition
  val ClassDef: ClassDefExtractor
  abstract class ClassDefExtractor {
    def unapply(x: ClassDef)(implicit ctx: Context): Option[(TypeName, DefDef, List[Tree] /* List[Term | TypeTree] */,  Option[ValDef], List[Statement])]
  }

  type DefDef <: Definition
  val DefDef: DefDefExtractor
  abstract class DefDefExtractor {
    def unapply(x: DefDef)(implicit ctx: Context): Option[(TermName, List[TypeDef],  List[List[ValDef]], TypeTree, Option[Term])]
  }

  type ValDef <: Definition
  val ValDef: ValDefExtractor
  abstract class ValDefExtractor {
    def unapply(x: ValDef)(implicit ctx: Context): Option[(TermName, TypeTree, Option[Term])]
  }

  type TypeDef <: Definition
  val TypeDef: TypeDefExtractor
  abstract class TypeDefExtractor {
    def unapply(x: TypeDef)(implicit ctx: Context): Option[(TypeName, Tree /* Type | TypeBounds */)]
  }

//  type PackageDef <: Definition
//  val PackageDef: PackageDefExtractor
//  abstract class PackageDefExtractor {
//    def unapply(x: PackageDef)(implicit ctx: Context): Option[(Name, List[Statement])]
//  }

  // ----- Terms ----------------------------------------------------

  type Term <: Statement
  implicit def TermDeco(t: Term): Typed

  val Ident: IdentExtractor
  abstract class IdentExtractor {
    def unapply(x: Term): Option[TermName]
  }

  val Select: SelectExtractor
  abstract class SelectExtractor {
    def unapply(x: Term): Option[(Term, PossiblySignedName)]
  }

  val Literal: LiteralExtractor
  abstract class LiteralExtractor {
    def unapply(x: Term): Option[Constant]
  }

  val This: ThisExtractor
  abstract class ThisExtractor {
    def unapply(x: Term): Option[Option[Id]]
  }

  val New: NewExtractor
  abstract class NewExtractor {
    def unapply(x: Term): Option[TypeTree]
  }

  val NamedArg: NamedArgExtractor
  abstract class NamedArgExtractor {
    def unapply(x: Term): Option[(TermName, Term)]
  }

  val Apply: ApplyExtractor
  abstract class ApplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term])]
  }

  val TypeApply: TypeApplyExtractor
  abstract class TypeApplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term])]
  }

  val Super: SuperExtractor
  abstract class SuperExtractor {
    def unapply(x: Term): Option[(Term, Option[Id])]
  }

  val Typed: TypedExtractor
  abstract class TypedExtractor {
    def unapply(x: Term): Option[(Term, TypeTree)]
  }

  val Assign: AssignExtractor
  abstract class AssignExtractor {
    def unapply(x: Term): Option[(Term, Term)]
  }

  val Block: BlockExtractor
  abstract class BlockExtractor {
    def unapply(x: Term): Option[(List[Statement], Term)]
  }

  val Inlined: InlinedExtractor
  abstract class InlinedExtractor {
    def unapply(x: Term): Option[(Term, List[Definition], Term)]
  }

  val Lambda: LambdaExtractor
  abstract class LambdaExtractor {
    def unapply(x: Term): Option[(Term, Option[TypeTree])]
  }

  val If: IfExtractor
  abstract class IfExtractor {
    def unapply(x: Term): Option[(Term, Term, Term)]
  }

  val Match: MatchExtractor
  abstract class MatchExtractor {
    def unapply(x: Term): Option[(Term, List[CaseDef])]
  }

  val Try: TryExtractor
  abstract class TryExtractor {
    def unapply(x: Term): Option[(Term, List[CaseDef], Option[Term])]
  }

  val Return: ReturnExtractor
  abstract class ReturnExtractor {
    def unapply(x: Term): Option[Term]
  }

  val Repeated: RepeatedExtractor
  abstract class RepeatedExtractor {
    def unapply(x: Term): Option[List[Term]]
  }

  val SelectOuter: SelectOuterExtractor
  abstract class SelectOuterExtractor {
    def unapply(x: Term): Option[(Term, Int, Type)]
  }

  // ----- CaseDef --------------------------------------------------

  type CaseDef <: Tree

  val CaseDef: CaseDefExtractor
  abstract class CaseDefExtractor {
    def unapply(x: CaseDef): Option[(Pattern, Option[Term], Term)]
  }

  // ----- Patterns -------------------------------------------------

  type Pattern <: Tree

  implicit def PatternDeco(x: Pattern): Typed

  val Value: ValueExtractor
  abstract class ValueExtractor {
    def unapply(x: Term): Option[Term]
  }

  val Bind: BindExtractor
  abstract class BindExtractor {
    def unapply(x: Term): Option[(TermName, Pattern)]
  }

  val Unapply: UnapplyExtractor
  abstract class UnapplyExtractor {
    def unapply(x: Term): Option[(Term, List[Term], List[Pattern])]
  }

  val Alternative: AlternativeExtractor
  abstract class AlternativeExtractor {
    def unapply(x: Term): Option[List[Pattern]]
  }

  val TypeTest: TypeTestExtractor
  abstract class TypeTestExtractor {
    def unapply(x: Term): Option[TypeTree]
  }

  // ----- TypeTrees ------------------------------------------------

  type TypeTree <: Tree

  trait AbstractTypeTree {
    def tpe: Type
  }
  implicit def TypeTreeDeco(x: TypeTree): AbstractTypeTree

  val Synthetic: SyntheticExtractor
  abstract class SyntheticExtractor {
    def unapply(x: TypeTree): Boolean
  }

  val TypeIdent: TypeIdentExtractor
  abstract class TypeIdentExtractor {
    def unapply(x: TypeTree): Option[TypeName]
  }

  val TypeSelect: TypeSelectExtractor
  abstract class TypeSelectExtractor {
    def unapply(x: TypeTree): Option[(Term, TypeName)]
  }

  val Singleton: SingletonExtractor
  abstract class SingletonExtractor {
    def unapply(x: TypeTree): Option[Term]
  }

  val Refined: RefinedExtractor
  abstract class RefinedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, List[Definition])]
  }

  val Applied: AppliedExtractor
  abstract class AppliedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, List[TypeTree])]
  }

  val Annotated: AnnotatedExtractor
  abstract class AnnotatedExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, Term)]
  }

  val And: AndExtractor
  abstract class AndExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, TypeTree)]
  }

  val Or: OrExtractor
  abstract class OrExtractor {
    def unapply(x: TypeTree): Option[(TypeTree, TypeTree)]
  }

  val ByName: ByNameExtractor
  abstract class ByNameExtractor {
    def unapply(x: TypeTree): Option[TypeTree]
  }

  // ----- TypeBoundsTrees ------------------------------------------------

  type TypeBoundsTree <: Tree

  trait AbstractTypeBoundsTree {
    def tpe: TypeBounds
  }
  implicit def TypeBoundsTreeDeco(x: TypeBoundsTree): AbstractTypeBoundsTree

  val TypeBoundsTree: TypeBoundsTreeExtractor
  abstract class TypeBoundsTreeExtractor {
    def unapply(x: TypeBoundsTree)(implicit ctx: Context): Option[(TypeTree, TypeTree)]
  }

  // ===== Types ====================================================

  type MaybeType // TODO: To represent Or types in scala2. Remove when bootstrapped

  // ----- Types ----------------------------------------------------

  type Type <: MaybeType

  val ConstantType: ConstantTypeExtractor
  abstract class ConstantTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Constant]
  }

  val SymRef: SymRefExtractor
  abstract class SymRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Definition, MaybeType /* Type | NoPrefix */)]
  }

  val NameRef: NameRefExtractor
  abstract class NameRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Name, MaybeType /* Type | NoPrefix */)]
  }

  val SuperType: SuperTypeExtractor
  abstract class SuperTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)]
  }

  val Refinement: RefinementExtractor
  abstract class RefinementExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Name, MaybeType /* Type | TypeBounds */)]
  }

  val AppliedType: AppliedTypeExtractor
  abstract class AppliedTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, List[MaybeType /* Type | TypeBounds */])]
  }

  val AnnotatedType: AnnotatedTypeExtractor
  abstract class AnnotatedTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Term)]
  }

  val AndType: AndTypeExtractor
  abstract class AndTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)]
  }

  val OrType: OrTypeExtractor
  abstract class OrTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(Type, Type)]
  }

  val ByNameType: ByNameTypeExtractor
  abstract class ByNameTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Type]
  }

  val ParamRef: ParamRefExtractor
  abstract class ParamRefExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[(LambdaType[_, _], Int)]
  }

  val ThisType: ThisTypeExtractor
  abstract class ThisTypeExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[Type]
  }

  val RecursiveThis: RecursiveThisExtractor
  abstract class RecursiveThisExtractor {
    def unapply(x: Type)(implicit ctx: Context): Option[RecursiveType]
  }

  type RecursiveType <: Type
  val RecursiveType: RecursiveTypeExtractor
  abstract class RecursiveTypeExtractor {
    def unapply(x: RecursiveType)(implicit ctx: Context): Option[Type]
  }

  // ----- Methodic Types -------------------------------------------

  type LambdaType[ParamName <: Name, ParamInfo <: MaybeType] <: Type

  type MethodType <: LambdaType[TermName, Type]

  trait AbstractMethodType {
    def isImplicit: Boolean
    def isErased: Boolean
  }
  implicit def MethodTypeDeco(x: MethodType): AbstractMethodType

  val MethodType: MethodTypeExtractor
  abstract class MethodTypeExtractor {
    def unapply(x: MethodType): Option[(List[TermName], List[Type], Type)]
  }


  type PolyType <: LambdaType[TypeName, TypeBounds]

  val PolyType: PolyTypeExtractor
  abstract class PolyTypeExtractor {
    def unapply(x: PolyType): Option[(List[TypeName], List[TypeBounds], Type)]
  }

  type TypeLambda <: LambdaType[TypeName, TypeBounds]

  val TypeLambda: TypeLambdaExtractor
  abstract class TypeLambdaExtractor {
    def unapply(x: TypeLambda): Option[(List[TypeName], List[TypeBounds], Type)]
  }

  // ----- TypeBounds -----------------------------------------------

  type TypeBounds <: MaybeType

  val TypeBounds: TypeBoundsExtractor
  abstract class TypeBoundsExtractor {
    def unapply(x: TypeBounds): Option[(Type, Type)]
  }

  // ----- NoPrefix -------------------------------------------------

  type NoPrefix <: MaybeType

  val NoPrefix: NoPrefixExtractor
  abstract class NoPrefixExtractor {
    def unapply(x: NoPrefix): Boolean
  }

  // ===== Constants ================================================

  type Constant
  trait AbstractConstant {
    def value: Any
  }
  implicit def ConstantDeco(x: Constant): AbstractConstant

  val UnitConstant: UnitExtractor
  abstract class UnitExtractor {
    def unapply(x: Constant): Boolean
  }

  val NullConstant: NullExtractor
  abstract class NullExtractor {
    def unapply(x: Constant): Boolean
  }

  val BooleanConstant: BooleanExtractor
  abstract class BooleanExtractor {
    def unapply(x: Constant): Option[Boolean]
  }

  val ByteConstant: ByteExtractor
  abstract class ByteExtractor {
    def unapply(x: Constant): Option[Byte]
  }

  val ShortConstant: ShortExtractor
  abstract class ShortExtractor {
    def unapply(x: Constant): Option[Short]
  }

  val CharConstant: CharExtractor
  abstract class CharExtractor {
    def unapply(x: Constant): Option[Char]
  }

  val IntConstant: IntExtractor
  abstract class IntExtractor {
    def unapply(x: Constant): Option[Int]
  }

  val LongConstant: LongExtractor
  abstract class LongExtractor {
    def unapply(x: Constant): Option[Long]
  }

  val FloatConstant: FloatExtractor
  abstract class FloatExtractor {
    def unapply(x: Constant): Option[Float]
  }

  val DoubleConstant: DoubleExtractor
  abstract class DoubleExtractor {
    def unapply(x: Constant): Option[Double]
  }

  val StringConstant: StringExtractor
  abstract class StringExtractor {
    def unapply(x: Constant): Option[String]
  }

  // ===== Modifier =================================================

  type Modifier

  val Annotation: AnnotationExtractor
  abstract class AnnotationExtractor {
    def unapply(x: Modifier): Option[Term]
  }

  val Flags: FlagsExtractor
  abstract class FlagsExtractor {
    def unapply(x: Modifier): Option[FlagSet]
  }

  val QualifiedPrivate: QualifiedPrivateExtractor
  abstract class QualifiedPrivateExtractor {
    def unapply(x: Modifier): Option[Type]
  }

  val QualifiedProtected: QualifiedProtectedExtractor
  abstract class QualifiedProtectedExtractor {
    def unapply(x: Modifier): Option[Type]
  }

}
