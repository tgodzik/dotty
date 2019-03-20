package tasty.tree

import dotty.tools.dotc.core.tasty.TastyFormat
import tasty.binary.TaggedSectionPickler
import tasty.names.NamePool

trait TreeSectionPickler extends TaggedSectionPickler {
  protected type Name

  def namePool: NamePool[Name]

  protected final def pickleName(name: Name): Unit = {
    val ref = namePool.pickleName(name)
    output.writeNat(ref.value)
  }

  override protected final def startsSubsection(tag: Int): Boolean = tag >= TastyFormat.firstLengthTreeTag
}
