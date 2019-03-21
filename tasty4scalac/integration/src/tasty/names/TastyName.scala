package tasty.names

sealed trait TastyName

object TastyName {

  case class UTF8(value: String) extends TastyName


}
