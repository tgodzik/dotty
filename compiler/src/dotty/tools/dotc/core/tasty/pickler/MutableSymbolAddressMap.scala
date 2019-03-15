package dotty.tools.dotc.core.tasty.pickler

import dotty.tools.dotc.core.tasty.translator.AST

import scala.collection.JavaConverters._

/* Mutable map from symbols any T */
class MutableSymbolAddressMap[S <: AST#Symbol, T](private val value: java.util.IdentityHashMap[S, T]) extends AnyVal {

  def apply(sym: S): T = value.get(sym)

  def get(sym: S): Option[T] = Option(value.get(sym))

  def getOrElse[U >: T](sym: S, default: => U): U = {
    val v = value.get(sym)
    if (v != null) v else default
  }

  def getOrElseUpdate(sym: S, op: => T): T = {
    val v = value.get(sym)
    if (v != null) v
    else {
      val v = op
      assert(v != null)
      value.put(sym, v)
      v
    }
  }

  def update(sym: S, x: T): Unit = {
    assert(x != null)
    value.put(sym, x)
  }

  def put(sym: S, x: T): T = {
    assert(x != null)
    value.put(sym, x)
  }

  def -=(sym: S): Unit = value.remove(sym)

  def remove(sym: S): Option[T] = Option(value.remove(sym))

  def contains(sym: S): Boolean = value.containsKey(sym)

  def isEmpty: Boolean = value.isEmpty

  def clear(): Unit = value.clear()

  def filter(p: ((S, T)) => Boolean): Map[S, T] =
    value.asScala.toMap.filter(p)

  def iterator: Iterator[(S, T)] = value.asScala.iterator

  def keysIterator: Iterator[S] = value.keySet().asScala.iterator

  def toMap: Map[S, T] = value.asScala.toMap

  override def toString: String = value.asScala.toString()
}

object MutableSymbolAddressMap{
  @forceInline def newMutableSymbolMap[S <: AST#Symbol, T]: MutableSymbolAddressMap[S, T] =
    new MutableSymbolAddressMap(new java.util.IdentityHashMap[S, T]())
}
