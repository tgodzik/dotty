package tasty.names

import tasty.binary.BinaryOutput

import scala.io.Codec
import scala.tools.nsc.Global

final class ScalacNamePool(val output: BinaryOutput)(implicit g: Global) extends NamePool[Global#Name] {

  override protected def pickle(name: Global#Name): Unit = {
    val bytes =
      if (name.length == 0) new Array[Byte](0)
      else Codec.toUTF8(g.chrs, name.start, name.length)
    pickleUtf8(bytes)
  }
}