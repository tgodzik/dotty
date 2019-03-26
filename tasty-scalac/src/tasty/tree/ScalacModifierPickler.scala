package tasty.tree

import tasty.binary.SectionWriter
import tasty.names.ScalacWriterNamePool

import scala.tools.nsc.Global

final class ScalacModifierWriter(nameSection: ScalacWriterNamePool,
                                  underlying: SectionWriter)
                                 (implicit val g: Global)
  extends ModifierWriter[Global#Symbol, Global#Name](nameSection, underlying) {

  override def write(value: Global#Symbol): Unit = {}
}
