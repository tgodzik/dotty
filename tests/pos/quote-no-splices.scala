import scala.quoted.QuoteContext
class Foo {
  def foo: Unit = {
    def expr(implicit ctx: QuoteContext) = '{
      val a = 3
      println("foo")
      2 + a
    }
  }
}
