package dotty.tools.dotc.tasty

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.quoted.PickledQuotes

import scala.tasty.Tasty

private[dotc] class TastyContext(val ctx: Context) extends scala.tasty.Context {
  def owner: impl.Definition = TastyImpl.Definition(ctx.owner)(this).asInstanceOf[impl.Definition]

  def toTasty[T](expr: quoted.Expr[T]): impl.Term =
    PickledQuotes.quotedExprToTree(expr)(ctx).asInstanceOf[impl.Term]

  def toTasty[T](tpe: quoted.Type[T]): impl.TypeTree =
    PickledQuotes.quotedTypeToTree(tpe)(ctx).asInstanceOf[impl.TypeTree]

  val impl: Tasty = TastyImpl
}
