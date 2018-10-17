import scala.quoted._

object Test {
  def step(k: (String => Expr[Unit])): Staged[Unit] = '()
  def meth(implicit ctx: QuoteContext): Unit = '{
    (i: Int) => ~step(el => '() )
  }
}
