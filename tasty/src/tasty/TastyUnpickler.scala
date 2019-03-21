package tasty

import tasty.binary.BinaryInput

trait Tasty

abstract class TastyUnpickler[Name, T <: Tasty] {
  final def unpickle(input: BinaryInput): T = {
    val raw = RawTasty(input)
    val names = unpickleNames(raw.nameSection)
    val sections = raw.sections.map {
      case (nameRef, section) => names(nameRef.value) -> section
    }

    unpickle(names, sections)
  }

  protected def unpickleNames(input: BinaryInput): Seq[Name]

  protected def unpickle(names: Seq[Name], sections: Map[Name, BinaryInput]): T
}
