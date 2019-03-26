package tasty.tree

import dotty.tools.dotc.core.tasty.TastyFormat
import tasty.TastySectionWriter
import tasty.binary.SectionWriter
import tasty.names.WriterNamePool

protected[tree] abstract class TreeSectionWriter[A, Name](nameSection: WriterNamePool[Name], output: SectionWriter)
  extends TastySectionWriter[A, Name](nameSection, output) {

  protected final def tagged(tag: Int)(op: => Unit): Unit = {
    output.writeByte(tag)
    if (tag >= TastyFormat.firstLengthTreeTag) writeSubsection(op)
    else op
  }

  protected final def writeRef(value: Int): Unit = output.writeNat(value)
}
