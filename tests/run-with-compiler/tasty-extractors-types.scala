
import scala.quoted._

import dotty.tools.dotc.quoted.Toolbox._
import dotty.tools.dotc.tasty.ContextProvider._

import scala.tasty.util.{TastyPrinter, TreeTraverser}
import scala.tasty.trees._
import scala.tasty.Context

object Test {
  def main(args: Array[String]): Unit = {
    Context.provided { implicit ctx =>
      def test(tpe: Type[_]) = {
        val tree = tpe.toTasty
        println(TastyPrinter.stringOf(tree))
        println(TastyPrinter.stringOf(tree.tpe))
        println()
      }
      val i = '[Int]

      val tests = List(
        '[Int],
        '[List[String]],
        '[Map[String, Int]],
        '[Map[String, ~i]],
      )
      tests.foreach(test)
    }
  }
}
