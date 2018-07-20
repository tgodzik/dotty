import scala.quoted.Expr
object Macro {
  import quoted.Liftable.{IntIsLiftable => _}
  transparent def foo(inline n: Int): Int = ~{
    '(n)
  }
}