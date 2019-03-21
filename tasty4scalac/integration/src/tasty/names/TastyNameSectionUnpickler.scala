package tasty.names

import tasty.names.TastyName.UTF8

object TastyNameSectionUnpickler extends NameSectionUnpickler[TastyName] {
  override protected def createUnpickler(names: Seq[TastyName]): NameUnpickler[TastyName] = new TastyNameUnpickler(names)

  private final class TastyNameUnpickler(names: Seq[TastyName]) extends NameUnpickler[TastyName](names) {
    override protected def unpickleUTF8(value: String): TastyName = UTF8(value)

    override protected def unpickleQualified(name: TastyName, name1: TastyName): TastyName = ???
  }

}
