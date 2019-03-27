package tasty.names

import tasty.binary.SectionPickler

import scala.io.Codec
import scala.tools.nsc.Global

final class ScalacPicklerNamePool(output: SectionPickler)(implicit g: Global) extends PicklerNamePool[Global#Name](output) {
  override protected def pickle(name: Global#Name): Unit = {
    val stringName = name.toString
    if (stringName.contains('.')) {
      val (prefix, qualifier) = splitAtLastChar(stringName, '.')
      val prefixName = g.TermName(prefix)
      val qualifierName = g.TermName(qualifier)
      pickleQualified(pickleName(prefixName), pickleName(qualifierName))
    } else {
      val bytes =
        if (name.length() == 0) new Array[Byte](0)
        else Codec.toUTF8(g.chrs, name.start, name.length())
      pickleUtf8(bytes)
    }
  }

  override def unifyName(name: Global#Name): Global#TermName = name.toTermName

  private def splitAtLastChar(str: String, char: Char): (String, String) = {
    val (prefix, qual) = str.splitAt(str.lastIndexOf(char))
    (prefix, qual.drop(1))
  }
}