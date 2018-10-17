package dotty.tools.dotc.quoted

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.util.NoSource

import scala.quoted.{Expr, QuoteContext}

/* Compilation unit containing the contents of a quoted expression */
class ExprCompilationUnit(val expr: QuoteContext => Expr[_]) extends CompilationUnit(NoSource)
