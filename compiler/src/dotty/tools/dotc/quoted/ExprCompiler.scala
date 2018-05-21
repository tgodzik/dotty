package dotty.tools.dotc
package quoted

import dotty.tools.backend.jvm.GenBCode
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.Mode
import dotty.tools.dotc.core.Names.TypeName
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.transform.ReifyQuotes
import dotty.tools.io.AbstractFile

/** Compiler that takes the contents of a quoted expression `expr` and produces
 *  a class file with `class ' { def apply: Object = expr }`.
 */
class ExprCompiler(directory: AbstractFile) extends Compiler {

  /** A GenBCode phase that outputs to a virtual directory */
  private class ExprGenBCode extends GenBCode {
    override def phaseName = "genBCode"
    override def outputDir(implicit ctx: Context) = directory
  }

  override protected def frontendPhases: List[List[Phase]] =
    List(List(new ExprCompilationFrontend(outputClassName)))

  override protected def picklerPhases: List[List[Phase]] =
    List(List(new ReifyQuotes))

  override protected def backendPhases: List[List[Phase]] =
    List(List(new ExprGenBCode))

  override def newRun(implicit ctx: Context): ExprRun = {
    reset()
    new ExprRun(this, ctx.addMode(Mode.ReadPositions))
  }

  def outputClassName: TypeName = "Quoted".toTypeName

}
