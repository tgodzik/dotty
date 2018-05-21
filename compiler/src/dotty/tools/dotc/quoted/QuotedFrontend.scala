package dotty.tools.dotc.quoted

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.quoted.PickledQuotes.{quotedExprToTree, quotedTypeToTree}
import dotty.tools.dotc.typer.FrontEnd
import dotty.tools.dotc.util.SourceFile

//** Frontend that receives a scala.quoted.Expr or scala.quoted.Type as input */
class QuotedFrontend extends FrontEnd {

  override def isTyper = false

  override def runOn(units: List[CompilationUnit])(implicit ctx: Context): List[CompilationUnit] = {
    units.map {
      case exprUnit: ExprCompilationUnit => inCompilationUnit(quotedExprToTree(exprUnit.expr))
      case typeUnit: TypeCompilationUnit => inCompilationUnit(quotedTypeToTree(typeUnit.tpe))
    }
  }

  private def inCompilationUnit(tree: tpd.Tree)(implicit ctx: Context): CompilationUnit = {
    val source = new SourceFile("", Seq())
    CompilationUnit.mkCompilationUnit(source, tree, forceTrees = true)
  }

}
