package tasty.names

import dotty.tools.dotc.core.tasty.TastyFormat.NameTags._
import tasty.binary.{BinaryInput, TaggedSectionUnpickler}

abstract class NameUnpickler[Name](names: Seq[Name]) extends TaggedSectionUnpickler[Name] {

  override protected final def startsSubsection(tag: Int): Boolean = true

  override final def unpickle(tag: Int)(input: BinaryInput): Name = tag match {
    case UTF8 => unpickleUTF8(new String(input.readBytes()))
    case QUALIFIED => unpickleQualified(readName(input), readName(input))
  }

  protected def unpickleUTF8(value: String): Name

  protected def unpickleQualified(name: Name, name1: Name): Name

  private def readName(input: BinaryInput): Name = names(input.readNat())
}
