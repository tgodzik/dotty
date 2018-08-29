package dotty.tools.sbtplugin

import sbt._
import sbt.Def.Initialize
import sbt.Keys._

import sbt.internal.langserver._
import sbt.internal.langserver.codec.JsonProtocol._
import sbt.internal.protocol._
import sbt.internal.server._

import DottyPlugin.autoImport._

import sbt.dottyplugin.Restricted

object DottyBSPPlugin extends AutoPlugin {
  object autoImport {
    private[dotty] val bspTest = taskKey[Int]("Run the tests")
  }
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  // override def projectSettings: Seq[Setting[_]] = Seq(
  // )

  override def buildSettings: Seq[Setting[_]] = Seq(
    bspTest := {
      val params = SbtExecParams("foo")
      Restricted.notifyEvent("dotty/testResponse", params)
      42
    },

    serverHandlers in Global += ServerHandler({ callback =>
      import callback._
      import sjsonnew.BasicJsonProtocol._
       ServerIntent(
        {
          case r: JsonRpcRequestMessage if r.method == "dotty/test" =>
            val params = SbtExecParams("foo")
            // Restricted.notifyEvent("dotty/testResponse", params)
            appendExec(Exec("bspTest", None, Some(CommandSource(name))))
            // jsonRpcNotify("lunar/oleh", "")
            ()
        },
        PartialFunction.empty
      )
    })
  )
}
