package dotty.tools.dotc
package quoted

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Mode
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.tasty.Reflector

/** Compiler that takes the contents of a quoted expression (or type) and outputs it's tree. */
abstract class ExprReflector extends Reflector {

  override protected def frontendPhases: List[List[Phase]] = List(
    List(new QuotedFrontend)
  )

  override def newRun(implicit ctx: Context): ExprRun = {
    reset()
    new ExprRun(this, ctx.addMode(Mode.ReadPositions))
  }

}
