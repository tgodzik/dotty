package tasty.names

import dotty.tools.dotc.core.tasty.TastyFormat.NameTags._
import tasty.binary.TaggedSectionPickler

abstract class NameSectionPickler[Name] extends TaggedSectionPickler {
  protected def pickle(name: Name): Unit

  override protected final def startsSubsection(tag: Int): Boolean = true

  protected final def pickleUtf8(bytes: Array[Byte]): Unit = tagged(UTF8) {
    output.writeBytes(bytes)
  }
}
