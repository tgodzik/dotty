package tasty4scalac.ast

import scala.tools.nsc.Global

object ScalacTranslator {

  def apply(global: Global) = {

    val ScalacAST = new GlobalBasedAST(global)

    new ASTTranslator[ScalacAST.type] {

      override def isUnit(c: ScalacAST.Constant): Boolean = c.tag == global.UnitTag

      override def isBoolean(c: ScalacAST.Constant): Boolean = c.tag == global.BooleanTag

      override def getBoolean(c: ScalacAST.Constant): Boolean = c.booleanValue

      override def isByte(c: ScalacAST.Constant): Boolean = c.tag == global.ByteTag

      override def getByte(c: ScalacAST.Constant): Byte = c.byteValue

      override def isIdent(t: ScalacAST.Tree): Boolean = t.isInstanceOf[global.Ident]

      override def getIdentName(t: ScalacAST.Tree): ScalacAST.Name = t match {
        case global.Ident(n) => n
      }

      override def isType(tree: ScalacAST.Tree): Boolean = tree.isType

      override def isTermRef(tpe: ScalacAST.Type): Boolean = ???

      override def withTypeRef(t: ScalacAST.Tree, tpe: ScalacAST.Type)(implicit ctx: Nothing): ScalacAST.Tree = ??? // tpe.underlyinScalacAST.typeConstructor.asInstanceOf[ScalacAST.TypeRef]

      override def getTpe(t: ScalacAST.Tree): ScalacAST.Type = t.tpe

      override def isEmpty(t: ScalacAST.Tree): Boolean = ???

      override def isNotWildcardName(n: ScalacAST.Name): Boolean = ???
      override protected def isThis(t: ScalacAST.Tree): Boolean = ???

      override protected def getThisQual(t: ScalacAST.Tree): ScalacAST.Tree = ???

    }
  }
}
