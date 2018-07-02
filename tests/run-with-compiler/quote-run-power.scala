
import dotty.tools.dotc.quoted.Toolbox._

import scala.quoted._

object Test {
  def main(args: Array[String]): Unit = {
    implicit val toolbox: scala.quoted.Toolbox = dotty.tools.dotc.quoted.Toolbox.make

    val powerCodeFor5 = '{ (x:Double )=> ~powerCode1('(x), 5) }
    println(powerCodeFor5.run)
    println(powerCodeFor5.show)
    println()
    println(powerCode1('(1.2), 5).run)
    println(powerCode1('(1.2), 5).show)
    println()
    println(powerCode2('(1.2), 5).run)
    println(powerCode2('(1.2), 5).show)
    println()
    println(powerCode3('(1.2), BigInt(5)).run)
    println(powerCode3('(1.2), BigInt(5)).show)
    println()
    println(powerCode3('(1.2), BigInt(Int.MaxValue)).run)
    println(powerCode3('(1.2), BigInt(Int.MaxValue)).show)
    println()
    println()
    implicit val doubleRing = new ExprRing[Double]('(1.0), (x, y) => '{ ~x * ~y })
    // println(powerCode4('(1.2), 5).run)
    // println(powerCode4('(1.2), 5).show)
    println()
    implicit val intRing = new ExprRing[Int]('(1), (x, y) => '{ ~x * ~y })
    println(powerCode5('(2), 5).run)
    println(powerCode5('(2), 5).show)
    println()
    implicit val stringOps = new ExprRing[String]('(""), (x, y) => '{ ~x + ~y })
    println(powerCode5('("a"), 5).run)
    println(powerCode5('("a"), 5).show)
    println()
  }

  def powerCode1(x: Expr[Double], n: Int): Expr[Double] =
    if (n == 0) '(1.0)
    else if (n % 2 == 1) '{ ~x * ~powerCode1(x, n - 1) }
    else '{ val y: Double = ~x * ~x; ~powerCode1('(y), n / 2) }


  def powerCode2(x: Expr[Double], n: Int): Expr[Double] =
    if (n == 0) 1.0.toExpr
    else if (n % 2 == 1) '{ ~x * ~powerCode2(x, n - 1) }
    else '{ val y: Double = ~x * ~x; ~powerCode2('(y), n / 2) }


  def powerCode3(x: Expr[BigDecimal], n: BigInt, inlinedOps: Int = 0)(implicit l: Liftable[BigInt]): Expr[BigDecimal] =
    if (inlinedOps > 32) '(power(~x, ~n.toExpr))
    else if (n == 0) '(BigDecimal(1.0))
    else if (n % 2 == 1) '{ ~x * ~powerCode3(x, n - 1, inlinedOps + 1) }
    else '{ val y = ~x * ~x; ~powerCode3('(y), n / 2, inlinedOps + 1) }

  def power(x: BigDecimal, n: BigInt): BigDecimal =
    if (n == 0) 1.0
    else if (n % 2 == 1) x * power(x, n - 1)
    else { val y: BigDecimal = x * x; power(y, n / 2) }

  implicit def BigIntIsLifable: Liftable[BigInt] = new Liftable[BigInt] {
    def toExpr(x: BigInt): Expr[BigInt] = '(BigInt(~x.toString.toExpr))
  }

  case class ExprRing[T](one: Expr[T], mult: (Expr[T], Expr[T]) => Expr[T])

  // def powerCode4[T](x: Expr[T], n: Int)(implicit r: ExprRing[T], t: Type[T]): Expr[T] =
  //   if (n == 0) r.one
  //   else if (n % 2 == 1) r.mult(x, powerCode4(x, n - 1))
  //   else '{ val y: ~t = ~r.mult(x, x); ~powerCode4('(y), n / 2) }

  def powerCode5[T: Type](x: Expr[T], n: Int)(implicit r: ExprRing[T]): Expr[T] =
    if (n == 0) r.one
    else if (n % 2 == 1) r.mult(x, powerCode5(x, n - 1))
    else '{ val y: T = ~r.mult(x, x); ~powerCode5('(y), n / 2) }

}
