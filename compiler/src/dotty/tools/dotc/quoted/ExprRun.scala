package dotty.tools.dotc.quoted

import dotty.tools.dotc.{Compiler, Run}
import dotty.tools.dotc.core.Contexts.Context

import scala.quoted.{Expr, Type}

class ExprRun(comp: Compiler, ictx: Context) extends Run(comp, ictx) {
  def compileExpr(expr: Expr[_]): Unit = {
    val units = new ExprCompilationUnit(expr) :: Nil
    compileUnits(units)
  }
}
