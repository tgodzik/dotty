package scala

package object quoted {

  type Staged[T] = QuoteContext => Expr[T]

  type StagedType[T] = QuoteContext => Type[T]

  implicit class LiftExprOps[T](val x: T) extends AnyVal {
    def toExpr(implicit ev: Liftable[T], ctx: QuoteContext): Expr[T] = ev.toExpr(x)
  }

}
