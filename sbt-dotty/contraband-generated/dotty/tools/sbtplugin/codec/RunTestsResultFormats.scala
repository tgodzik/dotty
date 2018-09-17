/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait RunTestsResultFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val RunTestsResultFormat: JsonFormat[dotty.tools.sbtplugin.RunTestsResult] = new JsonFormat[dotty.tools.sbtplugin.RunTestsResult] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.RunTestsResult = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      
      unbuilder.endObject()
      dotty.tools.sbtplugin.RunTestsResult()
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.RunTestsResult, builder: Builder[J]): Unit = {
    builder.beginObject()
    
    builder.endObject()
  }
}
}
