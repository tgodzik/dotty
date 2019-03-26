package tasty.tree

import tasty.binary.SectionWriter
import tasty.names.WriterNamePool

abstract class ModifierWriter[Modifier, Name](nameSection: WriterNamePool[Name],
                                              underlying: SectionWriter)
  extends TreeSectionWriter[Modifier, Name](nameSection, underlying) {

}
