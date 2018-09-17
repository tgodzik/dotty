/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package dotty.tools.sbtplugin.codec
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait CompileBuildsResultFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val CompileBuildsResultFormat: JsonFormat[dotty.tools.sbtplugin.CompileBuildsResult] = new JsonFormat[dotty.tools.sbtplugin.CompileBuildsResult] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): dotty.tools.sbtplugin.CompileBuildsResult = {
    jsOpt match {
      case Some(js) =>
      unbuilder.beginObject(js)
      val compilationSucceeded = unbuilder.readField[Boolean]("compilationSucceeded")
      unbuilder.endObject()
      dotty.tools.sbtplugin.CompileBuildsResult(compilationSucceeded)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: dotty.tools.sbtplugin.CompileBuildsResult, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("compilationSucceeded", obj.compilationSucceeded)
    builder.endObject()
  }
}
}
