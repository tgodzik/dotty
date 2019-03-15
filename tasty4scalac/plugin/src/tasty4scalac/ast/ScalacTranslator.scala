package tasty4scalac.ast

import scala.tools.nsc.Global

object ScalacTranslator {

  def apply(global: Global) = {

    val ScalacAST = new GlobalBasedAST(global)

    new ASTTranslator[ScalacAST.type] {


      override def isIdent(t: ScalacAST.Tree): Boolean = t.isInstanceOf[global.Ident]

      override def getIdentName(t: ScalacAST.Tree): ScalacAST.Name = t match {
        case global.Ident(n) => n
      }

      override def isType(tree: ScalacAST.Tree): Boolean = tree.isType

      override def isTermRef(tpe: ScalacAST.Type): Boolean = ???

      override def withTypeRef(t: ScalacAST.Tree, tpe: ScalacAST.Type)(implicit ctx: Nothing): ScalacAST.Tree = ??? // tpe.underlyinScalacAST.typeConstructor.asInstanceOf[ScalacAST.TypeRef]

      override def getTpe(t: ScalacAST.Tree): ScalacAST.Type = t.tpe

      override def isEmpty(t: ScalacAST.Tree): Boolean = ???

      override protected def isThis(t: ScalacAST.Tree): Boolean = ???

      override protected def getThisQual(t: ScalacAST.Tree): ScalacAST.Tree = ???

      override val constants: ASTConstants[ScalacAST.Constant] = _
      override val names: ASTNames[ScalacAST.Name, ScalacAST.TermName, ScalacAST.TypeName] = _
      override val symbols: ASTSymbols[ScalacAST.Symbol, Nothing, ScalacAST.Name] = _
      override val types: ASTTypes[ScalacAST.Type, Nothing, ScalacAST.Constant, ScalacAST.Symbol, ScalacAST.Annotation, ScalacAST.TypeRef] = _

      override def emptyTree: ScalacAST.Tree = ???

      override def getTree(annotation: ScalacAST.Annotation): ScalacAST.Tree = ???

      override def shouldPickleTree(tree: ScalacAST.Tree): Boolean = ???

      override def getSymbol(tree: ScalacAST.Tree): ScalacAST.Symbol = ???

      override protected def isMemberDef(tree: ScalacAST.Tree): Boolean = ???

      override def isValDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getValDef(tpe: ScalacAST.Tree): (ScalacAST.Symbol, ScalacAST.Tree, ScalacAST.Tree) = ???

      override def isDefDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getDefDef(tpe: ScalacAST.Tree): (ScalacAST.Symbol, ScalacAST.Tree, ScalacAST.Tree, List[ScalacAST.Tree], List[List[ScalacAST.Tree]]) = ???

      override def isTypeDef(tpe: ScalacAST.Tree): Boolean = ???

      override def getTypeDef(tpe: ScalacAST.Tree): (ScalacAST.Symbol, ScalacAST.Tree) = ???

      override def isPackageDef(tree: ScalacAST.Tree): Boolean = ???

      override def getPackageDef(tree: ScalacAST.Tree): (ScalacAST.Tree, List[ScalacAST.Tree]) = ???
    }
  }
}
