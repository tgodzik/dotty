package tasty4scalac.ast

import dotty.tools.dotc.ast.Trees.{Ident, MemberDef, This}
import dotty.tools.dotc.ast.{Trees, tpd}
import dotty.tools.dotc.core.Constants._
import dotty.tools.dotc.core.NameKinds._
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Types.{TermRef, ThisType}
import dotty.tools.dotc.core.{Contexts, _}
import Decorators._
import dotty.tools.dotc.ast.tpd.Template
import dotty.tools.dotc.core.NameKinds.SignedName.SignedInfo
import dotty.tools.dotc.core.Names.{DerivedName, SimpleName}
import dotty.tools.dotc.core.tasty.TreePickler.Hole


class DottyNames extends ASTNames[Names.Name, Names.TermName, Names.TypeName] {

  override def isNotWildcardName(name: Names.Name): Boolean = name != nme.WILDCARD

  override def toTermName(name: Names.Name) = name.toTermName

  override protected def isSignedName(termName: Names.TermName): Boolean =
    termName.isInstanceOf[DerivedName] && termName.asInstanceOf[DerivedName].info.isInstanceOf[SignedInfo]

  override protected def signedName(termName: Names.TermName): (Names.TermName, List[Names.TypeName], Names.TypeName) = termName match {
    case Names.DerivedName(underlying, info: SignedInfo) => (underlying, info.sig.paramsSig, info.sig.resSig)
  }

  override protected def anyQualifiedName(termName: Names.TermName): (Names.TermName, Names.Name) = termName match {
    case name@Names.DerivedName(qual, info: QualifiedInfo) =>
      (name.underlying, info.name)
  }

  override protected def isAnyQualifiedName(termName: Names.TermName): Boolean = {
    termName.isInstanceOf[DerivedName] &&
      termName.asInstanceOf[DerivedName].info.isInstanceOf[QualifiedInfo] &&
      termName.asInstanceOf[DerivedName].info.asInstanceOf[QualifiedInfo].kind.isInstanceOf[UniqueNameKind]
  }

  override protected def anyUniqueName(termName: Names.TermName): (Names.TermName, String, Int) = termName match {
    case Names.DerivedName(qual, info: NumberedInfo) =>
      info.kind match {
        case unique: UniqueNameKind => (qual, unique.separator, info.num)
      }
  }

  override protected def isAnyUniqueName(termName: Names.TermName): Boolean =
    termName.isInstanceOf[DerivedName] && termName.asInstanceOf[DerivedName].info.isInstanceOf[NumberedInfo]

  override protected def derivedName(termName: Names.TermName): Names.TermName = termName match {
    case Names.DerivedName(term, _) => term
  }

  override protected def isDerivedName(termName: Names.TermName): Boolean = termName.isInstanceOf[DerivedName]

  override def copyFromChrs(start: Int, length: Int): Array[Byte] = Names.copyFromChrs(start, length)

  override def toTermName(name: String): Names.TermName = name.toTermName

  override def isNotEmpty(name: Names.Name): Boolean = !name.isEmpty

  override def getTermKindTag(termName: Names.TermName): Int = termName.toTermName.info.kind.tag

  override def isSimpleName(termName: Names.TermName): Boolean = termName.isInstanceOf[SimpleName]

  override protected def anyNumberedName(termName: Names.TermName): (Names.TermName, Int) = termName match {
    case AnyNumberedName(termName, infoNum) => (termName, infoNum)
  }

  override protected def isAnyNumberedName(termName: Names.TermName): Boolean =
    termName.isInstanceOf[DerivedName] && termName.asInstanceOf[DerivedName].info.isInstanceOf[NumberedInfo]

  override def start(name: Names.Name): Int = name.asInstanceOf[SimpleName].start

  override def length(name: Names.Name): Int = name.asInstanceOf[SimpleName].length
}

class DottyConstants extends ASTConstants[Constants.Constant] {

  override def isUnit(c: Constant): Boolean = c.tag == UnitTag

  override def isBoolean(c: Constant): Boolean = c.tag == BooleanTag

  override def getBoolean(c: Constant): Boolean = c.booleanValue

  override def isByte(c: Constant): Boolean = c.tag == ByteTag

  override def getByte(c: Constant): Byte = c.byteValue
}

class DottySymbols extends ASTSymbols[Symbols.Symbol, Contexts.Context, Names.Name] {
  override def isPackage(symbol: Symbols.Symbol)(implicit context: Contexts.Context): Boolean = {
    symbol.is(Flags.Package)
  }

  override def fullName(symbol: Symbols.Symbol)(implicit context: Contexts.Context): Names.Name = symbol.fullName

  override def isEffectiveRoot(symbol: Symbols.Symbol)(implicit context: Contexts.Context): Boolean = symbol.isEffectiveRoot

  override def name(symbol: Symbols.Symbol)(implicit context: Contexts.Context): Names.Name = symbol.name
}

class DottyTypes extends ASTTypes[Types.Type, Contexts.Context, Constants.Constant, Symbols.Symbol, Annotations.Annotation, Types.ParamRef] {
  override def stripTypeVar(tpe: Types.Type)(implicit ctx: Contexts.Context): Types.Type = tpe.stripTypeVar

  override protected def isConstant(tpe: Types.Type): Boolean = tpe.isInstanceOf[Types.ConstantType]

  override protected def getConstant(tpe: Types.Type): Constant = tpe match {
    case constantTpe: Types.ConstantType => constantTpe.value
  }

  override def isThisType(tpe: Types.Type): Boolean = tpe.isInstanceOf[Types.ThisType]

  override def isAnnotatedType(tpe: Types.Type): Boolean = tpe.isInstanceOf[Types.AnnotatedType]

  override def isMethodType(tpe: Types.Type): Boolean = tpe.isInstanceOf[Types.MethodType]

  override def isParamRef(tpe: Types.Type): Boolean = tpe.isInstanceOf[Types.ParamRef]

  override protected def getThisType(tpe: Types.Type): (Types.Type, Symbols.Symbol) = tpe match {
    case ThisType(tp, sym) => (tp, sym)
  }

  override protected def getAnnotatedType(tpe: Types.Type): (Types.Type, Annotations.Annotation) = tpe match{
    case AnnotatedType(tp, annon) => (tp, annon)
  }

  override def isContextualMethod(tpe: Types.Type): Boolean = tpe.isContextual

  override def isImplicitMethod(tpe: Types.Type): Boolean = tpe.isImplicitMethod

  override def isErasedMethod(tpe: Types.Type): Boolean = tpe.isErasedMethod

  override def binder(tpe: Types.ParamRef): Types.Type = tpe.binder

  override def paramNum(tpe: Types.ParamRef): Int = tpe.paramNum

  override def paramRef(tpe: Types.Type): Types.ParamRef = tpe.asInstanceOf[Types.ParamRef]
}

// TODO - might need to drop the unapplies for the sake of optymization
class DottyTranslator extends ASTTranslator[DottyAST.type] {

  override val constants: ASTConstants[Constant] = new DottyConstants

  override val names: ASTNames[Names.Name, Names.TermName, Names.TypeName] = new DottyNames

  override val symbols: ASTSymbols[Symbols.Symbol, Contexts.Context, Names.Name] = new DottySymbols

  override val types: ASTTypes[Types.Type, Contexts.Context, Constant, Symbols.Symbol, Annotations.Annotation, Types.ParamRef] = new DottyTypes

  override def isIdent(t: tpd.Tree): Boolean = t.isInstanceOf[Ident[_]]

  override def getIdentName(t: tpd.Tree): Names.Name = t match {
    case Trees.Ident(n) => n
  }

  override def isType(tree: tpd.Tree): Boolean = tree.isType

  override def isTermRef(tpe: Types.Type): Boolean = tpe.isInstanceOf[TermRef]

  override def withTypeRef(t: tpd.Tree, tpe: Types.Type)(implicit cts: Contexts.Context): tpd.Tree = {
    val ThisType(tpeRef) = tpe
    t.withType(tpeRef)
  }

  override def getTpe(t: tpd.Tree): Types.Type = t.tpe

  override def isEmpty(t: tpd.Tree): Boolean = t.isEmpty

  override protected def isThis(t: tpd.Tree): Boolean = t.isInstanceOf[This[_]]


  override protected def getThisQual(t: tpd.Tree): tpd.Tree = t match {
    case This(qual) => qual
  }

  override def getSymbol(tree: DottyAST.Tree)(implicit ctx: Contexts.Context): Symbols.Symbol = tree.symbol


  override protected def isMemberDef(tree: DottyAST.Tree): Boolean = tree.isInstanceOf[tpd.MemberDef]

  override def emptyTree: DottyAST.Tree = tpd.EmptyTree

  override def getTree(annotation: Annotations.Annotation)(implicit ctx: Contexts.Context): DottyAST.Tree = annotation.tree

  override def shouldPickleTree(tree: DottyAST.Tree): Boolean =
    tree.isInstanceOf[Template] || tree.isInstanceOf[Hole] || tree.isType


  override def isValDef(tpe: DottyAST.Tree): Boolean = tpe.isInstanceOf[tpd.ValDef]

  override def getValDef(tpe: DottyAST.Tree)(implicit ctx: Contexts.Context): (Symbols.Symbol, DottyAST.Tree, DottyAST.Tree) = tpe match {
    case tpe: tpd.ValDef => (tpe.symbol, tpe.tpt, tpe.rhs)
  }

  override def isDefDef(tpe: DottyAST.Tree): Boolean = tpe.isInstanceOf[tpd.DefDef]

  override def getDefDef(tpe: DottyAST.Tree)(implicit ctx: Contexts.Context): (Symbols.Symbol, DottyAST.Tree, DottyAST.Tree, List[tpd.Tree], List[List[tpd.Tree]]) = tpe match {
    case tpe: tpd.DefDef => (tpe.symbol, tpe.tpt, tpe.rhs, tpe.tparams, tpe.vparamss)
  }

  override def isTypeDef(tpe: DottyAST.Tree): Boolean = tpe.isInstanceOf[tpd.TypeDef]

  override def getTypeDef(tpe: DottyAST.Tree)(implicit ctx: Contexts.Context): (Symbols.Symbol, DottyAST.Tree) = tpe match {
    case tpe: tpd.TypeDef => (tpe.symbol, tpe.rhs)
  }

  override def isPackageDef(tree: DottyAST.Tree): Boolean = tree.isInstanceOf[tpd.PackageDef]

  override def getPackageDef(tree: DottyAST.Tree): (DottyAST.Type, List[tpd.Tree]) = tree match {
    case packageDef: tpd.PackageDef => (packageDef.pid.tpe, packageDef.stats)
  }
}