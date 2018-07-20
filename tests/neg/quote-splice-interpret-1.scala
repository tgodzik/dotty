import dotty.tools.dotc.ast.Trees.Import

import scala.quoted._

object Macros {
  inline def isZero(inline n: Int): Boolean = ~{ // error
    if (n == 0) '(true)
    else '(false)
  }
}
