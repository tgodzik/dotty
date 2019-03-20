package tasty.tree

import tasty.binary.BinaryOutput
import tasty.names.ScalacNamePool

import scala.tools.nsc.Global

final class ScalacModifierPickler(val namePool: ScalacNamePool, val output: BinaryOutput) extends ModifierPickler {
  override type Modifier = Global#Symbol
  override protected type Name = Global#Name

  override def pickleModifier(modifier: Global#Symbol): Unit = {}
}
