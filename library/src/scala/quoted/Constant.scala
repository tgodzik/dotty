package scala.quoted

import scala.tasty.Tasty

class Constant(implicit tasty: Tasty) {
  import tasty._

  def unapply[T](expr: Expr[T])(implicit ctx: Context): Option[T] = {
    def const(tree: Term): Option[T] = tree match {
      case Literal(c) => Some(c.value.asInstanceOf[T])
      case Block(Nil, e) => const(e)
      case Inlined(_, Nil, e) => const(e)
      case _  => None
    }
    const(expr.toTasty)
  }
}
