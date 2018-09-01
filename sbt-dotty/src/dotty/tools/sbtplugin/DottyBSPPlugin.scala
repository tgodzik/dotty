package dotty.tools.sbtplugin

import sbt._
import sbt.Def.Initialize
import sbt.Keys._

import sbt.internal.langserver._
import sbt.internal.langserver.codec.JsonProtocol._
import sbt.internal.protocol._
import sbt.internal.server._
import sbt.complete.DefaultParsers._

import dotty.tools.sbtplugin.codec.JsonProtocol._

import DottyPlugin.autoImport._

import sbt.dottyplugin.Restricted

object DottyBSPPlugin extends AutoPlugin {
  object autoImport {
    private[dotty] val bspTest = inputKey[Unit]("Run the tests")
  }
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings: Seq[Setting[_]] = Seq(
    bspTest := {
      val Seq(name, id) = spaceDelimited("<arg>").parsed
      println("name: " + name)
      println("id: " + id)

      val params = TestResult("foo")

      println("channels: " + Restricted.exchange.channels)
      Restricted.exchange.channels.collectFirst {
        case c: NetworkChannel if c.name == name =>
          c
      }.foreach { c =>
        println("c: " + c)
        Restricted.jsonRpcRespond(c, params, Some(id))
      }
    },

    serverHandlers += ServerHandler({ callback =>
      import callback._
      import sjsonnew.BasicJsonProtocol._
       ServerIntent(
        {
          case r: JsonRpcRequestMessage if r.method == "dotty/test" =>
            val params = SbtExecParams("foo")
            appendExec(Exec(s"Global/bspTest $name ${r.id}", None, Some(CommandSource(name))))
        },
        PartialFunction.empty
      )
    })
  )
}
