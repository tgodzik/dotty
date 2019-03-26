package tasty.binary

import dotty.tools.dotc.core.tasty.TastyBuffer
import tasty.Writer

class SectionWriter extends BinaryWriter {
  final def write[A](value: A)(implicit writer: Writer[A]): Unit = writer.write(value)

  final def writeSequence[A](sequence: Seq[A])(implicit writer: Writer[A]): Unit =
    writeSubsection(writeTerminalSequence(sequence))

  final def writeTerminalSequence[A](sequence: Seq[A])(implicit writer: Writer[A]): Unit =
    sequence.foreach(write(_))

  final def writeSubsection(op: => Unit): Unit = {
    val start = length
    val subsectionOffset = start + TastyBuffer.AddrWidth

    // write subsection
    length = subsectionOffset
    op
    val subsectionLength = length - subsectionOffset

    // write subsection length
    length = start
    writeNat(subsectionLength)
    val padding = subsectionOffset - length

    // strip zeros
    // TODO given N nested sections it will move bytes N times. Maybe compact when growing?
    // TODO still needs an index of first padding
    if (padding > 0) {
      // leaves some trash after the length - but it should be of no concern
      System.arraycopy(buffer, subsectionOffset, buffer, length, subsectionLength)
    }

    // set pointer after the subsection
    length += subsectionLength
  }
}