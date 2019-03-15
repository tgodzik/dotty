package dotty.tools.dotc.core.tasty.pickler

import dotty.tools.dotc.core.tasty.TastyBuffer
import dotty.tools.dotc.core.tasty.TastyBuffer.NameRef
import dotty.tools.dotc.core.tasty.translator.{AST, ASTTranslator}

import scala.collection.mutable

class NameBufferT[A <: AST](translator: ASTTranslator[A]) extends TastyBuffer(10000) {

  private val nameRefs = new mutable.LinkedHashMap[A#Name, NameRef]

  import translator._

  def nameIndex(name: A#Name): NameRef = {
    val name1 = names.toTermName(name)
    nameRefs.get(name1) match {
      case Some(ref) =>
        ref
      case None =>
        name1 match {
          case names.SignedName(original,  params, result) =>
            nameIndex(original); nameIndex(result); params.foreach(nameIndex)
          case names.AnyQualifiedName(prefix, name) =>
            nameIndex(prefix); nameIndex(name)
          case names.AnyUniqueName(original, separator, num) =>
            nameIndex(names.toTermName(separator))
            if (names.isNotEmpty(original)) nameIndex(original)
          case names.DerivedName(original) =>
            nameIndex(original)
          case _ =>
        }
        val ref = NameRef(nameRefs.size)
        nameRefs(name1) = ref
        ref
    }
  }

  private def withLength(op: => Unit, lengthWidth: Int = 1): Unit = {
    val lengthAddr = currentAddr
    for (i <- 0 until lengthWidth) writeByte(0)
    op
    val length = currentAddr.index - lengthAddr.index - lengthWidth
    putNat(lengthAddr, length, lengthWidth)
  }

  def writeNameRef(ref: NameRef): Unit = writeNat(ref.index)

  def writeNameRef(name: A#Name): Unit = writeNameRef(nameRefs(names.toTermName(name)))

  def pickleNameContents(name: A#Name): Unit = {
    val tag = names.getTermKindTag(names.toTermName(name))
    writeByte(tag)
    names.toTermName(name) match {
      case simpleName if names.isSimpleName(simpleName) =>
        val bytes =
          if (names.length(simpleName) == 0) new Array[Byte](0)
          else names.copyFromChrs(names.start(simpleName), names.length(simpleName))
        writeNat(bytes.length)
        writeBytes(bytes, bytes.length)
      case names.AnyQualifiedName(prefix, name) =>
        withLength {
          writeNameRef(prefix); writeNameRef(name)
        }
      case names.AnyUniqueName(original, separator, num) =>
        withLength {
          writeNameRef(names.toTermName(separator))
          writeNat(num)
          if (names.isNotEmpty(original)) writeNameRef(original)
        }
      case names.AnyNumberedName(original, num) =>
        withLength {
          writeNameRef(original); writeNat(num)
        }
      case names.SignedName(original, params, result) =>
        withLength(
          {
            writeNameRef(original); writeNameRef(result); params.foreach(writeNameRef)
          },
          if ((params.length + 2) * NameBufferT.maxIndexWidth <= NameBufferT.maxNumInByte) 1 else 2)
      case names.DerivedName(original) =>
        withLength {
          writeNameRef(original)
        }
    }
  }

  override def assemble(): Unit = {
    var i = 0
    for ((name, ref) <- nameRefs) {
      assert(ref.index == i)
      i += 1
      pickleNameContents(name)
    }
  }
}

object NameBufferT {
  private val maxIndexWidth = 3  // allows name indices up to 2^21.
  private val payloadBitsPerByte = 7 // determined by nat encoding in TastyBuffer
  private val maxNumInByte = (1 << payloadBitsPerByte) - 1
}


