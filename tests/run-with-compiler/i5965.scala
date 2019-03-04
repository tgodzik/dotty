import scala.quoted._

import scala.tasty._

object Test {

  implicit val toolbox: scala.quoted.Toolbox = scala.quoted.Toolbox.make

  def main(args: Array[String]): Unit = {
    '[List]
    val list = bound('{List(1, 2, 3)})
    println(list.show)
    println(list.run)

    val opt = bound('{Option(4)})
    println(opt.show)
    println(opt.run)

    val map = bound('{Map(4 -> 1)})
    println(map.show)
    println(map.run)
  }

  def bound[T: Type, S[_]: Type](x: Expr[S[T]]): Expr[S[T]] = '{
    val y: S[T] = $x
    y
  }
}
