import scala.quoted._

object Test {

  implicit def dummyCtx: QuoteContext = ???

  '{ ~implicitly[Liftable[Int]].toExpr(1) }

  {
    import Liftable._

    '{ ~IntIsLiftable.toExpr(1) }

    '{ ~1.toExpr }

  }

}
