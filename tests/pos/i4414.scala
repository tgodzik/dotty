import scala.quoted._

object Test {

  def a[A: Type]()(implicit ctx: QuoteContext): Unit = {
    b[Expr[A]]()
    a[A]()
  }

  def b[A: Type](): Unit = ???
}
