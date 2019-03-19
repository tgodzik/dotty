package tasty.names

import tasty.binary.BinaryOutput

import scala.io.Codec
import scala.tools.nsc.Global

final class ScalacNamePickler(val output: BinaryOutput)(implicit g: Global) extends PicklerNamePool[Global#Name] {

  override type UnifiedName = Global#TermName

  override protected def pickle(name: Global#Name): Unit = {
    val stringName = name.toString
    if (stringName.contains('.')) {
      val (prefix, qualifier) = splitAtLastChar(stringName, '.')
      val prefixName = g.TermName(prefix)
      val qualifierName = g.TermName(qualifier)
      pickleQualifiedName(pickleName(prefixName), pickleName(qualifierName))
    } else {
      pickleSimpleName(name.start, name.length)
    }
  }

  protected def pickleSimpleName(start: Int, length: Int): Unit = {
    val bytes =
      if (length == 0) new Array[Byte](0)
      else Codec.toUTF8(g.chrs, start, length)
    pickleUtf8(bytes)
  }

  override def unifyName(name: Global#Name): Global#TermName = name.toTermName

  private def splitAtLastChar(str: String, char: Char): (String, String) = {
    val (prefix, qual) = str.splitAt(str.lastIndexOf('.'))
    (prefix, qual.drop(1))
  }

}