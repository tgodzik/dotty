package dotty.tools.dotc.tasty

import dotty.tools.dotc.Run
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Mode
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.fromtasty.{ReadTastyTreesFromClasses, TASTYRun}

/** Compiler that takes the contents of a quoted expression (or type) and outputs it's tree. */
abstract class TASTYReflector extends Reflector {

  override protected def frontendPhases: List[List[Phase]] = List(
    List(new ReadTastyTreesFromClasses)
  )

  override def newRun(implicit ctx: Context): Run = {
    reset()
    new TASTYRun(this, ctx.addMode(Mode.ReadPositions))
  }

}
