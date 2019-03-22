package tasty.binary

import dotty.tools.dotc.core.tasty.{TastyBuffer, TastyHash}
import dotty.tools.dotc.util.Util.dble
import tasty.names.NameRef

class BinaryOutput(initialSize: Int = 32) {
  private var buffer = new Array[Byte](initialSize)
  private var length = 0

  final def size: Int = length

  final def bytes: Array[Byte] = buffer.take(length)

  final def write(subsection: BinaryOutput): Unit = {
    writeNat(subsection.size)
    writeBytes(subsection.bytes, subsection.size)
  }

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

  final def writeByte(b: Int): Unit = {
    ensureCapacity(1)
    buffer(length) = b.toByte
    length += 1
  }

  final def writeBytes(bytes: Array[Byte], amount: Int): Unit = {
    ensureCapacity(amount)
    System.arraycopy(bytes, 0, buffer, length, amount)
    length += amount
  }


  final def writeLongNat(x: Long): Unit = {
    def writePrefix(x: Long): Unit = {
      val y = x >>> 7
      if (y != 0L) writePrefix(y)
      writeByte((x & 0x7f).toInt)
    }

    val y = x >>> 7
    if (y != 0L) writePrefix(y)
    writeByte(((x & 0x7f) | 0x80).toInt)
  }

  final def writeLongInt(x: Long): Unit = {
    def writePrefix(x: Long): Unit = {
      val y = x >> 7
      if (y != 0L - ((x >> 6) & 1)) writePrefix(y)
      writeByte((x & 0x7f).toInt)
    }

    val y = x >> 7
    if (y != 0L - ((x >> 6) & 1)) writePrefix(y)
    writeByte(((x & 0x7f) | 0x80).toInt)
  }

  final def writeUncompressedLong(x: Long): Unit = {
    var y = x
    val bytes = new Array[Byte](8)
    for (i <- 7 to 0 by -1) {
      bytes(i) = (y & 0xff).toByte
      y = y >>> 8
    }
    writeBytes(bytes)
  }

  final def writeNat(x: Int): Unit = writeLongNat(x.toLong & 0x00000000FFFFFFFFL)

  final def writeInt(x: Int): Unit = writeLongInt(x)

  final def writeBytes(bytes: Array[Byte]): Unit = writeBytes(bytes, bytes.length)

  private def ensureCapacity(n: Int): Unit = {
    while (length + n >= buffer.length) buffer = dble(buffer)
  }
}


object BinaryOutput {
  def hashOf(byteSection: BinaryOutput): Long = TastyHash.pjwHash64(byteSection.bytes)
}