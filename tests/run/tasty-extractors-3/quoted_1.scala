import scala.quoted._
import dotty.tools.dotc.quoted.Toolbox._

import scala.tasty.Universe
import scala.tasty.util.{TastyPrinter, TreeTraverser}

object Macros {

  implicit inline def printTypes[T](x: => T): Unit =
    ~impl('(x))(Universe.compilationUniverse) // FIXME infer Universe.compilationUniverse within top level ~

  def impl[T](x: Expr[T])(implicit u: Universe): Expr[Unit] = {
    import u._
    import tasty._
    val buff = new StringBuilder
    val traverser = new TreeTraverser {

      override def traverseTypeTree(tree: MaybeTypeTree)(implicit ctx: this.tasty.Context): Unit = {
        buff.append(TastyPrinter.stringOfType(tasty)(tree.tpe))
        buff.append("\n\n")
        traverseTypeTreeChildren(tree)
      }
    }

    val tree = x.toTasty
    traverser.traverse(tree)(ctx)
    '(print(~buff.result().toExpr))
  }
}
