/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait TestResultFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val TestResultFormat: JsonFormat[dotty.tools.sbtplugin.TestResult] = new JsonFormat[dotty.tools.sbtplugin.TestResult] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.TestResult = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val res = unbuilder.readField[String]("res")
      unbuilder.endObject()
      dotty.tools.sbtplugin.TestResult(res)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.TestResult, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("res", obj.res)
    builder.endObject()
  }
}
}
