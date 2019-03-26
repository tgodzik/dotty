package tasty.names

import dotty.tools.dotc.core.tasty.TastyFormat

import scala.tools.nsc.Global


sealed trait ScalacName {
  def kind: Int
}

case class SimpleName(name: String) extends ScalacName {
  override def kind: Int = TastyFormat.NameTags.UTF8
}

case class QualifiedName(prefix: ScalacName, suffix: ScalacName) extends ScalacName {
  override def kind: Int = TastyFormat.NameTags.QUALIFIED
}

case class SignedName(name: ScalacName, returnName: ScalacName, parameterNames: List[ScalacName]) extends ScalacName {
  override def kind: Int = TastyFormat.NameTags.SIGNED
}

trait ScalacNameConversions {

  implicit def nameToScalacName(name: Global#Name): ScalacName = {
    nameToScalacName(name.toString)
  }

  implicit def nameToScalacName(names: List[Global#Name]): List[ScalacName] = {
    names.map(nameToScalacName)
  }


  implicit def nameToScalacName(name: String): ScalacName = {
    val stringName = name.toString
    if (stringName.contains('.')) {
      val (prefix, qualifier) = splitAtLastChar(stringName, '.')
      QualifiedName(nameToScalacName(prefix), nameToScalacName(qualifier))
    } else {
      SimpleName(stringName)
    }
  }

  private def splitAtLastChar(str: String, char: Char): (String, String) = {
    val (prefix, qual) = str.splitAt(str.lastIndexOf(char))
    (prefix, qual.drop(1))
  }
}