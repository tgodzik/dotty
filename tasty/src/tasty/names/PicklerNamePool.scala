package tasty.names

import scala.collection.mutable

abstract class PicklerNamePool[Name] extends NameSectionPickler[Name] {
  private val pool = mutable.Map[Name, NameRef]()

  final def pickleName(name: Name): NameRef = {
    def newNameRef = new NameRef(pool.size)

    if (pool.contains(name)) pool(name)
    else {
      pickle(name)
      val ref = newNameRef
      pool += name -> ref
      ref
    }
  }
}
