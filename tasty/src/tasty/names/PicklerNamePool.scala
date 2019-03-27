package tasty.names

import dotty.tools.dotc.core.tasty.TastyFormat.NameTags.{QUALIFIED, UTF8}
import tasty.binary.SectionPickler

import scala.collection.mutable

abstract class PicklerNamePool[Name](output: SectionPickler) {
  private val pool = mutable.Map[Name, NameRef]()

  protected def pickle(name: Name): Unit

  protected def unifyName(name: Name): Name

  final def pickleName(name: Name): NameRef = {
    val unified = unifyName(name)
    pool.getOrElseUpdate(unified, insert(unified))
  }

  private def insert(name: Name): NameRef = {
    pickle(name)
    new NameRef(pool.size)
  }

  protected final def pickleUtf8(bytes: Array[Byte]): Unit = tagged(UTF8) {
    output.pickleBytes(bytes)
  }

  protected final def pickleQualified(qualifier: NameRef, name: NameRef): Unit = tagged(QUALIFIED) {
    pickleNameRef(qualifier)
    pickleNameRef(name)
  }

  private final def tagged(tag: Int)(op: => Unit): Unit = {
    output.pickleByte(tag)
    output.pickleSubsection(op)
  }

  private def pickleNameRef(ref: NameRef): Unit = output.pickleNat(ref.index)
}