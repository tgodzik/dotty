package dotty.tools.sbtplugin

import sbt._
import sbt.Def.Initialize
import sbt.Keys._

import sbt.internal.langserver._
import sbt.internal.langserver.codec.JsonProtocol._
import sbt.internal.protocol._
import sbt.internal.server._
import sbt.complete.DefaultParsers._

import sjsonnew.support.scalajson.unsafe.Converter

import dotty.tools.sbtplugin.codec.JsonProtocol._

import DottyPlugin.autoImport._

import sbt.dottyplugin.Restricted

import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.LogEvent

object DottyBSPPlugin extends AutoPlugin {
  object autoImport {
    val buildIdentifier = settingKey[String]("A unique identifier for this build")
  }
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  val buildIdentifierSetting =
    buildIdentifier := s"${thisProject.value.id}/${configuration.value}"

  override def projectSettings: Seq[Setting[_]] = {
    inConfig(Compile)(Seq(
      buildIdentifierSetting,
      collectAnalyses := { () }
    )) ++
    inConfig(Test)(Seq(
      buildIdentifierSetting,
      testListeners += new BspTestsListener(buildIdentifier.value)
      // We use JUnit native support for listeners instead of sbt testListeners
      // because the latter is not good enough currently (e.g. it only reports
      // the status of a test once all tests in a class have finished).
      // testOptions += Tests.Argument(TestFrameworks.JUnit, "--run-listener=dotty.tools.sbtplugin.oJUnitListener")
    )) ++
    Seq(
      // testListeners += new BspTestsListener,
    )
  }

  type BuildId = String
  type Build = (ProjectRef, Configuration)

  private[dotty] def runTaskInBuilds[T](key: TaskKey[T], buildIds: Seq[BuildId], state: State): (State, Seq[(BuildId, T)]) = {
    val structure = Project.structure(state)
    val settings = structure.data
    val joinedTask = structure.allProjectRefs.flatMap { projRef =>
      val project = Project.getProjectForReference(projRef, structure).get
      project.configurations.flatMap { config =>
        buildIdentifier.in(projRef, config).get(settings).flatMap { id =>
          if (buildIds.contains(id))
            key.in(projRef, config).get(settings).map(_.map(v => id -> v))
          else
            None
        }
      }
    }.join

    DottyIDEPlugin.runTask(joinedTask, state)
  }

  private[dotty] def runInputTaskInBuilds[T](key: InputKey[T], input: String, buildIds: Seq[BuildId], state: State): (State, Seq[(BuildId, T)]) = {
    val structure = Project.structure(state)
    val settings = structure.data
    val joinedTask = structure.allProjectRefs.flatMap { projRef =>
      val project = Project.getProjectForReference(projRef, structure).get
      project.configurations.flatMap { config =>
        buildIdentifier.in(projRef, config).get(settings).flatMap { id =>
          if (buildIds.contains(id))
            Some(key.in(projRef, config).toTask(input).evaluate(settings).map(v => id -> v))
          else
            None
        }
      }
    }.join

    DottyIDEPlugin.runTask(joinedTask, state)
  }

  // def buildsOf(buildIds: Seq[String], state: State): Seq[Build] = {
  //   val structure = Project.structure(state)
  //   val settings = structure.data

  //   structure.allProjectRefs.flatMap { projRef =>
  //     val project = Project.getProjectForReference(projRef, structure).get
  //     project.configurations.flatMap { config =>
  //       buildIdentifier.in(projRef, config).get(settings).flatMap { id =>
  //         if (buildIds.contains(id))
  //           Some((projRef, config))
  //         else
  //           None
  //       }
  //     }
  //   }
  // }

  def compileBuilds =
    Command.args("compileBuilds", "<channelName> <requestId> <builds>*") { (state0, args) =>
      val channelName +: requestId +: builds = args

      val (state1, success) = try {
        (runTaskInBuilds(compile, builds, state0)._1, true)
      } catch {
        case i: Incomplete => // At least one compilation failed
          (state0, false)
      }

      Restricted.exchange.channels.collectFirst {
        case c: NetworkChannel if c.name == channelName =>
          c
      }.foreach { c =>
        println("c: " + c)
        Restricted.jsonRpcRespond(c, CompileBuildsResult(success), Some(requestId))
      }

      state1
    }

  def runTests =
    Command.args("runTests", "<channelName> <requestId> <build> <tests>*") { (state0, args) =>
      val channelName +: requestId +: build +: tests = args

      val testArgs = tests.mkString(" ")
      println("build: " + build)
      println("testArgs: " + testArgs)
      // val (state1, _) = runTaskInBuilds(testOnly.toTask(s" $testArgs"), Seq(build), origState)
      val state1 = try {
        runInputTaskInBuilds(testOnly, s""" -- \"$testArgs*\"""", Seq(build), state0)._1
      } catch {
        case i: Incomplete => // The tests failed
          state0
      }

      Restricted.exchange.channels.collectFirst {
        case c: NetworkChannel if c.name == channelName =>
          c
      }.foreach { c =>
        println("c: " + c)
        Restricted.jsonRpcRespond(c, RunTestsResult(), Some(requestId))
      }

      state1
    }

  private final case class BuildServerError(code: Long, message: String)
      extends Throwable(message)
  // Copy-pasted from sbt.internal.server.LanguageServerProtocol
  private def json(r: JsonRpcRequestMessage) =
    r.params.getOrElse(
      throw BuildServerError(
        ErrorCodes.InvalidParams,
        s"param is expected on '${r.method}' method."
      )
    )

  override def globalSettings: Seq[Setting[_]] = Seq(
    // listTests := {
    //   val Seq(name, id) = spaceDelimited("<arg>").parsed
    //   println("name: " + name)
    //   println("id: " + id)

    //   val params = TestResult("foo")

    //   println("channels: " + Restricted.exchange.channels)
    //   Restricted.exchange.channels.collectFirst {
    //     case c: NetworkChannel if c.name == name =>
    //       c
    //   }.foreach { c =>
    //     println("c: " + c)
    //     Restricted.jsonRpcRespond(c, params, Some(id))
    //   }
    // },
    commands ++= Seq(compileBuilds, runTests),

    serverHandlers += ServerHandler({ callback =>
      import callback._
      import sjsonnew.BasicJsonProtocol._
       ServerIntent(
        {
          case r: JsonRpcRequestMessage if r.method == "dotty/compileBuilds" =>
            val params = Converter.fromJson[CompileBuildsParams](json(r)).get
            val builds = params.builds.map(_.name).mkString(" ")
            appendExec(Exec(s"compileBuilds $name ${r.id} $builds", None, Some(CommandSource(name))))

          case r: JsonRpcRequestMessage if r.method == "dotty/runTests" =>
            val params = Converter.fromJson[RunTestsParams](json(r)).get
            val builds = params.tests.groupBy(_.build).foreach { case (build, tests) =>
              val testNames = tests.map(_.path.mkString(".")).mkString(" ")
              appendExec(Exec(s"runTests $name ${r.id} ${build.name} $testNames", None, Some(CommandSource(name))))
            }
       },
        PartialFunction.empty
      )
    })
  )
}
