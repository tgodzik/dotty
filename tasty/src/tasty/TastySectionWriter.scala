package tasty

import tasty.binary.SectionWriter
import tasty.names.WriterNamePool

abstract class TastySectionWriter[A, Name](nameSection: WriterNamePool[Name], underlying: SectionWriter) extends Writer[A] {
  final def writeSequence(sequence: Seq[A]): Unit =
    underlying.writeSequence(sequence)(writer = this)

  final def writeTerminalSequence(sequence: Seq[A]): Unit =
    underlying.writeTerminalSequence(sequence)(writer = this)

  protected final def currentOffset: Int = underlying.size

  protected final def writeName(name: Name): Unit = {
    val ref = nameSection.writeName(name)
    underlying.writeNat(ref.index)
  }

  protected final def writeSubsection(op: => Unit): Unit =
    underlying.writeSubsection(op)

  protected final def writeNat(value: Int): Unit =
    underlying.writeNat(value)

  protected final def writeInteger(value: Int): Unit =
    underlying.writeInteger(value)

  protected final def writeLong(value: Long): Unit =
    underlying.writeLong(value)
}