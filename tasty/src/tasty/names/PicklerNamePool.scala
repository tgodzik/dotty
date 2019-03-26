package tasty.names

import dotty.tools.dotc.core.tasty.TastyFormat.NameTags.{QUALIFIED, UTF8}
import tasty.binary.SectionWriter

import scala.collection.mutable

abstract class WriterNamePool[Name](output: SectionWriter) {
  private val pool = mutable.Map[Name, NameRef]()

  protected def write(name: Name): Unit

  protected def unifyName(name: Name): Name

  final def writeName(name: Name): NameRef = {
    val unified = unifyName(name)
    pool.getOrElseUpdate(unified, insert(unified))
  }

  private def insert(name: Name): NameRef = {
    write(name)
    new NameRef(pool.size)
  }

  protected final def writeUtf8(bytes: Array[Byte]): Unit = tagged(UTF8) {
    output.writeBytes(bytes)
  }

  protected final def writeQualified(qualifier: NameRef, name: NameRef): Unit = tagged(QUALIFIED) {
    writeNameRef(qualifier)
    writeNameRef(name)
  }

  private final def tagged(tag: Int)(op: => Unit): Unit = {
    output.writeByte(tag)
    output.writeSubsection(op)
  }

  private def writeNameRef(ref: NameRef): Unit = output.writeNat(ref.index)
}