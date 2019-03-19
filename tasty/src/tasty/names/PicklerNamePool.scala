package tasty.names


import dotty.tools.dotc.core.tasty.TastyFormat.NameTags

import scala.collection.mutable
import scala.io.Codec

abstract class PicklerNamePool[Name] extends NameSectionPickler[Name] {
  type UnifiedName <: Name

  private val pool = mutable.Map[UnifiedName, NameRef]()

  def unifyName(name: Name): UnifiedName

  final def pickleName(name: Name): NameRef = {
    val unifiedName = unifyName(name)
    pool.getOrElseUpdate(unifiedName, insert(unifiedName))
  }

  protected def pickleQualifiedName(prefix: NameRef, name: NameRef): Unit = tagged(NameTags.QUALIFIED) {
    writeNameRef(prefix)
    writeNameRef(name)
  }

  private def insert(name: UnifiedName): NameRef = {
    pickle(name)
    new NameRef(pool.size)
  }

  private def writeNameRef(nameRef: NameRef): Unit = {
    output.writeNat(nameRef.index)
  }

}
