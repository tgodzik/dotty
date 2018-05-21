package dotty.tools.dotc.quoted

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.{Compiler, Run}

class TypeRun(comp: Compiler, ictx: Context) extends Run(comp, ictx) {
  def compileType(tpe: scala.quoted.Type[_]): Unit = {
    val units = new TypeCompilationUnit(tpe) :: Nil
    compileUnits(units)
  }
}
