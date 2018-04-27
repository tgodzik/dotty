package dotty.tools.dotc.tasty
package internal

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context

import scala.tasty.trees

private[dotc] class TastyContext(val ctx: Context) extends scala.tasty.Context {
  def owner: trees.Definition = Definition(ctx.owner)(ctx)

  def toTasty[T](expr: quoted.Expr[T]) = expr match {
    case expr: quoted.Exprs.TreeExpr[tpd.Tree, Context] @unchecked => internal.Term(expr.tree)
    case _ =>
      // new QuoteDriver().withTree(expr, (tree, ctx) => internal.Term(tree), Settings.run())
      ???
  }

  def toTasty[T](tpe: quoted.Type[T]) = tpe match {
    case typeTree: quoted.Types.TreeType[tpd.TypeTree, Context] @unchecked => internal.TypeTree(typeTree.typeTree)
    case _ =>
      // new QuoteDriver().withTypeTree(tpe, (tpt, ctx) => internal.TypeTree(tpt), Settings.run())
      ???
  }

  def toolbox: scala.runtime.tasty.Toolbox = Toolbox
}
