package tasty.binary

import dotty.tools.dotc.core.tasty.TastyHash
import dotty.tools.dotc.util.Util.dble

class BinaryWriter(initialSize: Int = 64) {
  protected var buffer = new Array[Byte](initialSize)
  protected var length = 0

  final def size: Int = length

  final def bytes: Array[Byte] = buffer.take(length)

  final def write(subsection: BinaryWriter): Unit = {
    writeNat(subsection.size)
    writeBytes(subsection.bytes, subsection.size)
  }


  final def writeByte(b: Int): Unit = {
    ensureCapacity(1)
    buffer(length) = b.toByte
    length += 1
  }

  final def writeBytes(bytes: Array[Byte]): Unit = writeBytes(bytes, bytes.length)

  final def writeBytes(bytes: Array[Byte], amount: Int): Unit = {
    ensureCapacity(amount)
    System.arraycopy(bytes, 0, buffer, length, amount)
    length += amount
  }

  final def writeNat(value: Int): Unit = writeNat(value.toLong & 0x00000000FFFFFFFFL)

  final def writeNat(value: Long): Unit = {
    def writePrefix(x: Long): Unit = {
      val y = x >>> 7
      if (y != 0L) writePrefix(y)
      writeByte((x & 0x7f).toInt)
    }

    val y = value >>> 7
    if (y != 0L) writePrefix(y)
    writeByte(((value & 0x7f) | 0x80).toInt)
  }

  final def writeInteger(value: Int): Unit = writeLong(value.toLong)

  final def writeLong(value: Long): Unit = {
    def writePrefix(x: Long): Unit = {
      val y = x >> 7
      if (y != 0L - ((x >> 6) & 1)) writePrefix(y)
      writeByte((x & 0x7f).toInt)
    }

    val y = value >> 7
    if (y != 0L - ((value >> 6) & 1)) writePrefix(y)
    writeByte(((value & 0x7f) | 0x80).toInt)
  }

  final def writeUncompressedLong(value: Long): Unit = {
    var y = value
    val bytes = new Array[Byte](8)
    for (i <- 7 to 0 by -1) {
      bytes(i) = (y & 0xff).toByte
      y = y >>> 8
    }
    writeBytes(bytes)
  }

  private def ensureCapacity(n: Int): Unit = {
    while (length + n >= buffer.length) buffer = dble(buffer)
  }
}

object BinaryWriter {
  def hashOf(writer: BinaryWriter): Long = TastyHash.pjwHash64(writer.bytes)
}