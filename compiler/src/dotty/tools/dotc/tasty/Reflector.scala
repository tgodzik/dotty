package dotty.tools.dotc.tasty

import dotty.tools.dotc.Compiler
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Phases.Phase

/** Compiler that takes the contents of a quoted expression (or type) and outputs it's tree. */
abstract class Reflector extends Compiler {

  def reflect(tree: tpd.Tree)(implicit ctx: Context): Unit

  override protected def frontendPhases: List[List[Phase]]

  override protected def picklerPhases: List[List[Phase]] = Nil

  override protected def transformPhases: List[List[Phase]] = Nil

  override protected def backendPhases: List[List[Phase]] = List(
    List(new Reflector)
  )

  class Reflector extends Phase {
    override def phaseName: String = "reflector"
    override def run(implicit ctx: Context): Unit = reflect(ctx.compilationUnit.tpdTree)
  }
}
