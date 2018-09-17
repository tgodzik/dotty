/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait RunTestsParamsFormats { self: dotty.tools.sbtplugin.codec.TestIdentifierFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val RunTestsParamsFormat: JsonFormat[dotty.tools.sbtplugin.RunTestsParams] = new JsonFormat[dotty.tools.sbtplugin.RunTestsParams] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.RunTestsParams = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val tests = unbuilder.readField[Vector[dotty.tools.sbtplugin.TestIdentifier]]("tests")
      unbuilder.endObject()
      dotty.tools.sbtplugin.RunTestsParams(tests)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.RunTestsParams, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("tests", obj.tests)
    builder.endObject()
  }
}
}
