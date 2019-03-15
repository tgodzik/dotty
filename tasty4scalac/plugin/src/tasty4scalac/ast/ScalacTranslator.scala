package tasty4scalac.ast

import scala.tools.nsc.Global




object ScalacTranslator {

  def apply(global: Global) = {

    val ScalacAST = new GlobalBasedAST(global)

    object ScalacConstants extends ASTConstants[ScalacAST.Constant] {
      override def isUnit(constant: ScalacAST.Constant): Boolean = ???

      override def isBoolean(constant: ScalacAST.Constant): Boolean = ???

      override def getBoolean(constant: ScalacAST.Constant): Boolean = ???

      override def isByte(constant: ScalacAST.Constant): Boolean = ???

      override def getByte(constant: ScalacAST.Constant): Byte = ???
    }

    object ScalacNames extends ASTNames[ScalacAST.Name, ScalacAST.TermName, ScalacAST.TypeName]{
      override def copyFromChrs(start: Int, length: Int): Array[Byte] = ???

      override def isNotWildcardName(name: ScalacAST.Name): Boolean = ???

      override def toTermName(name: ScalacAST.Name): ScalacAST.TermName = ???

      override def toTermName(name: String): ScalacAST.TermName = ???

      override def start(name: ScalacAST.Name): Int = ???

      override def length(name: ScalacAST.Name): Int = ???

      override def isNotEmpty(name: ScalacAST.Name): Boolean = ???

      override def getTermKindTag(termName: ScalacAST.TermName): Int = ???

      override protected def signedName(termName: ScalacAST.TermName): (ScalacAST.TermName, List[ScalacAST.TypeName], ScalacAST.TypeName) = ???

      override protected def isSignedName(termName: ScalacAST.TermName): Boolean = ???

      override def isSimpleName(termName: ScalacAST.TermName): Boolean = ???

      override protected def anyQualifiedName(termName: ScalacAST.TermName): (ScalacAST.TermName, ScalacAST.Name) = ???

      override protected def isAnyQualifiedName(termName: ScalacAST.TermName): Boolean = ???

      override protected def anyUniqueName(termName: ScalacAST.TermName): (ScalacAST.TermName, String, Int) = ???

      override protected def isAnyUniqueName(termName: ScalacAST.TermName): Boolean = ???

      override protected def derivedName(termName: ScalacAST.TermName): ScalacAST.TermName = ???

      override protected def isDerivedName(termName: ScalacAST.TermName): Boolean = ???

      override protected def anyNumberedName(termName: ScalacAST.TermName): (ScalacAST.TermName, Int) = ???

      override protected def isAnyNumberedName(termName: ScalacAST.TermName): Boolean = ???
    }

  object ScalacSymbols extends ASTSymbols[ScalacAST.Symbol, Nothing, ScalacAST.Name]{
    override def isPackage(symbol: ScalacAST.Symbol)(implicit context: Nothing): Boolean = ???

    override def fullName(symbol: ScalacAST.Symbol)(implicit context: Nothing): ScalacAST.Name = ???

    override def name(symbol: ScalacAST.Symbol)(implicit context: Nothing): ScalacAST.Name = ???

    override def isEffectiveRoot(symbol: ScalacAST.Symbol)(implicit context: Nothing): Boolean = ???
  }

    object ScalacTypes extends ASTTypes[ScalacAST.Type, Nothing, ScalacAST.Constant, ScalacAST.Symbol, ScalacAST.Annotation, ScalacAST.TypeRef]{
      override def stripTypeVar(tpe: ScalacAST.Type)(implicit ctx: Nothing): ScalacAST.Type = ???

      override def binder(tpe: ScalacAST.TypeRef): ScalacAST.Type = ???

      override def paramNum(tpe: ScalacAST.TypeRef): Int = ???

      override protected def isConstant(tpe: ScalacAST.Type): Boolean = ???

      override protected def getConstant(tpe: ScalacAST.Type): ScalacAST.Constant = ???

      override protected def isThisType(tpe: ScalacAST.Type): Boolean = ???

      override protected def getThisType(tpe: ScalacAST.Type): (ScalacAST.Type, ScalacAST.Symbol) = ???

      override protected def isAnnotatedType(tpe: ScalacAST.Type): Boolean = ???

      override protected def getAnnotatedType(tpe: ScalacAST.Type): (ScalacAST.Type, ScalacAST.Annotation) = ???

      override def isMethodType(tpe: ScalacAST.Type): Boolean = ???

      override def isContextualMethod(tpe: ScalacAST.Type): Boolean = ???

      override def isImplicitMethod(tpe: ScalacAST.Type): Boolean = ???

      override def isErasedMethod(tpe: ScalacAST.Type): Boolean = ???

      override def isParamRef(tpe: ScalacAST.Type): Boolean = ???

      override def paramRef(tpe: ScalacAST.global.Type): ScalacAST.global.TypeRef = ???
    }

    new ASTTranslator[ScalacAST.type] {

      override def isIdent(t: ScalacAST.Tree): Boolean = t.isInstanceOf[global.Ident]

      override def getIdentName(t: ScalacAST.Tree): ScalacAST.Name = ???

      override def isType(tree: ScalacAST.Tree): Boolean = tree.isType

      override def isTermRef(tpe: ScalacAST.Type): Boolean = ???

      override def withTypeRef(t: ScalacAST.Tree, tpe: ScalacAST.Type)(implicit ctx: Nothing): ScalacAST.Tree = ???

      override def getTpe(t: ScalacAST.Tree): ScalacAST.Type = t.tpe

      override def isEmpty(t: ScalacAST.Tree): Boolean = ???

      override protected def isThis(t: ScalacAST.Tree): Boolean = ???

      override protected def getThisQual(t: ScalacAST.Tree): ScalacAST.Tree = ???

      override val constants: ASTConstants[ScalacAST.Constant] = ScalacConstants
      override val names: ASTNames[ScalacAST.Name, ScalacAST.TermName, ScalacAST.TypeName] = ScalacNames
      override val symbols: ASTSymbols[ScalacAST.Symbol, Nothing, ScalacAST.Name] = ScalacSymbols
      override val types: ASTTypes[ScalacAST.Type, Nothing, ScalacAST.Constant, ScalacAST.Symbol, ScalacAST.Annotation, ScalacAST.TypeRef] = ScalacTypes

      override def emptyTree: ScalacAST.Tree = ???

      override def getTree(annotation: ScalacAST.Annotation)(implicit nt: ScalacAST.Context): ScalacAST.Tree = ???

      override def shouldPickleTree(tree: ScalacAST.Tree): Boolean = ???

      override def getSymbol(tree: ScalacAST.Tree)(implicit nt: ScalacAST.Context): ScalacAST.Symbol = ???

      override protected def isMemberDef(tree: ScalacAST.Tree): Boolean = ???

      override def isValDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getValDef(tpe: ScalacAST.Tree)(implicit nt: ScalacAST.Context): (ScalacAST.Symbol, ScalacAST.Tree, ScalacAST.Tree) = ???

      override def isDefDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getDefDef(tpe: ScalacAST.Tree)(implicit nt: ScalacAST.Context): (ScalacAST.Symbol, ScalacAST.Tree, ScalacAST.Tree, List[ScalacAST.Tree], List[List[ScalacAST.Tree]]) = ???

      override def isTypeDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getTypeDef(tpe: ScalacAST.Tree)(implicit nt: ScalacAST.Context): (ScalacAST.Symbol, ScalacAST.Tree) = ???

      override def isPackageDef(tree: ScalacAST.Tree): Boolean = ???

      override def getPackageDef(tree: ScalacAST.Tree): (ScalacAST.Type, List[ScalacAST.Tree]) = ???
    }
  }
}
