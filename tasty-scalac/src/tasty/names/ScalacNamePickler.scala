package tasty.names

import tasty.binary.{BinaryOutput, SectionPickler}

import scala.io.Codec
import scala.tools.nsc.Global


final class ScalacNamePickler(output: SectionPickler)(implicit g: Global) extends PicklerNamePool[ScalacName] {

  protected def pickleSimpleName(name: String): Unit = {
    val bytes = Codec.toUTF8(name.toCharArray, 0, name.length)
    pickleUtf8(bytes)
  }

  override protected def pickle(name: ScalacName): Unit = name match {
    case SimpleName(name) =>
      pickleSimpleName(name)
    case QualifiedName(prefix, suffix) =>
      pickleQualifiedName(pickleName(prefix), pickleName(suffix))
    case SignedName(name, returnName, parameterNames) =>
      val paramNames = parameterNames.map(param => pickleName(param))
      pickleSignedName(pickleName(name), pickleName(returnName), paramNames)
  }

}