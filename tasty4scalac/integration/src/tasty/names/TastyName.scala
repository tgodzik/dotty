package tasty.names

sealed trait TastyName

object TastyName {

  case class UTF8(value: String) extends TastyName {
    override def toString: String = value
  }

  case class Qualified(qualifier: TastyName, name: TastyName) extends TastyName

  case class Expanded(qualifier: TastyName, name: TastyName) extends TastyName

  case class ExpandPrefix(qualifier: TastyName, name: TastyName) extends TastyName

  case class Unique(separator: String, id: Int, underlying: Option[TastyName]) extends TastyName

  case class DefaultGetter(underlying: TastyName, id: Int) extends TastyName

  case class Variance(underlying: TastyName, variance: Int) extends TastyName

  case class SuperAccessor(underlying: TastyName) extends TastyName

  case class InlineAccessor(underlying: TastyName) extends TastyName

  case class ObjectClass(underlying: TastyName) extends TastyName

  case class Signed(original: TastyName, parameters: Seq[TastyName], result: TastyName) extends TastyName

}
