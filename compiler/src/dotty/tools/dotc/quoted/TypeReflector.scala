package dotty.tools.dotc
package quoted

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Mode
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.tasty.Reflector

/** Compiler that takes the contents of a quoted expression (or type) and outputs it's tree. */
abstract class TypeReflector extends Reflector {

  override protected def frontendPhases: List[List[Phase]] = List(
    List(new QuotedFrontend)
  )

  override def newRun(implicit ctx: Context): TypeRun = {
    reset()
    new TypeRun(this, ctx.addMode(Mode.ReadPositions))
  }

}
