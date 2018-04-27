import scala.quoted._

import dotty.tools.dotc.quoted.Toolbox._
import dotty.tools.dotc.tasty.ContextProvider._

import scala.tasty.Context

object Test {

  def main(args: Array[String]): Unit = {
    Context.provided { implicit cxt =>
      3.toExpr match { case Constant(n) => println(n) }
      '(4) match { case Constant(n) => println(n) }
      '("abc") match { case Constant(n) => println(n) }
      '(null) match { case Constant(n) => println(n) }

      '(new Object) match { case Constant(n) => println(n); case _ => println("OK") }
    }
  }
}
