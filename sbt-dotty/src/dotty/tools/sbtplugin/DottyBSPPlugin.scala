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
    // private[dotty] val listTests = inputKey[Unit]("List all test classes")
  }
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  val buildIdentifierSetting =
    buildIdentifier := s"${thisProject.value.id}/${configuration.value}"

  val testRunStarted = "Test run started"
  val testRunFinishedPrefix = "Test run finished"
  val testStarted = raw"Test (.*) started".r
  val testIgnored = raw"Test (.*) ignored".r
  val testFailed = raw"Test (.*) failed: (.*), took .*sec".r

  def stripAnsiColors(m: String): String = m.replaceAll("\u001B\\[[;\\d]*m", "")

  def testPath(test: String): List[String] = {
    // For now, we only handle tests of the from `prefix.name` where
    // `prefix` is a fully qualified class name.
    val fullPath = test.split("\\.")
    val prefix = fullPath.init.mkString(".")
    val name = fullPath.last
    List(prefix, name)
  }

  // FIXME: Should be replaced by sbt testListeners, but right now not good enough:
  // ~ no event when a test start
  // - with junit-interface, no event when a test stop
  def logger(build: String) = {
    val appender = new AbstractAppender(s"DottyBSPPlugin-$build", null, Restricted.dummyLayout, true)  {
      var runningTest: Option[String] = None

      def append(x: LogEvent): Unit = {

        def broadcastTestStatus(build: String, test: String, kind: TestStatusKind,
          details: String = ""): Unit = {
          val status = 
            TestStatus(
              TestIdentifier(
                BuildIdentifier(build, hasTests = true),
                testPath(test).toVector,
                hasChildrenTests = false
              ),
              kind,
              details
            )
          println(s"sending testStatus $status")
          Restricted.exchange.channels.collect {
            case c: NetworkChannel  =>
              c
          }.foreach { c =>
            println(s"sending testStatus $status to $c")
            Restricted.jsonRpcNotify(c, "dotty/testStatus", status)
          }
        }

        def runningTestFinished(kind: TestStatusKind, details: String = "") = {
          runningTest foreach { prevTest =>
            broadcastTestStatus(build, prevTest, kind, details)
          }
          runningTest = None
        }

        x.getMessage match {
          case e: org.apache.logging.log4j.message.ObjectMessage =>
            e.getParameter match {
              case se: sbt.internal.util.StringEvent =>
                val msg = stripAnsiColors(se.message)

                if (msg == testRunStarted)
                  runningTest = None
                else if (msg.startsWith(testRunFinishedPrefix)) {
                  runningTestFinished(TestStatusKind.Success)
                }
                else {
                  msg match {
                    case testStarted(startedTest) =>
                      runningTestFinished(TestStatusKind.Success)
                      runningTest = Some(startedTest)
                      broadcastTestStatus(build, startedTest, TestStatusKind.Running)
                    case testIgnored(ignoredTest) =>
                      runningTestFinished(TestStatusKind.Success)
                      broadcastTestStatus(build, ignoredTest, TestStatusKind.Ignored)
                    case testFailed(failedTest, details) =>
                      if (runningTest != Some(failedTest)) {
                        // println(s"!!! $runningTest != $failedTest")
                        assert(false, s"!!! $runningTest != $failedTest")
                      }
                      runningTestFinished(TestStatusKind.Failure, details)
                    case _ =>
                      // println("#MSG: " + msg)
                  }
                }
                // Test funsets.FunSetSuite.union contains all elements of each set finished, took 0.001 sec


              case _ =>
            }
        //   case _ =>
        // }
        }
      }
    }
    appender.start
    appender
  }

  override def projectSettings: Seq[Setting[_]] = {
    inConfig(Compile)(Seq(
      buildIdentifierSetting,
      collectAnalyses := { () }
    )) ++
    inConfig(Test)(Seq(
      buildIdentifierSetting,
      // We use JUnit native support for listeners instead of sbt testListeners
      // because the latter is not good enough currently (e.g. it only reports
      // the status of a test once all tests in a class have finished).
      // testOptions += Tests.Argument(TestFrameworks.JUnit, "--run-listener=dotty.tools.sbtplugin.oJUnitListener")
    )) ++
    Seq(
      // testListeners += new BspTestsListener(streams.value.log),
      extraLoggers := {
        val currentFunction = extraLoggers.value
        val build = (buildIdentifier in Test).value

        if (isDotty.value) {
          (key: ScopedKey[_]) => {
            // println("key: " + key)
            // println("scope: " + key.scope)
            logger(build) +: currentFunction(key)
          }
        } else {
          currentFunction
        }
      }
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

  def listTests =
    Command.args("listTests", "<channelName> <requestId> <tests>*") { (state0, args) =>
      val channelName +: requestId +: builds = args

      println("##builds: " + builds)
      val (state1, tests) = runTaskInBuilds(definedTestNames, builds, state0)

      println("testsSBT: " + tests)
      val res = ListTestsResult(tests.flatMap { case (build, names) =>
        names.map(name =>
          TestIdentifier(
            BuildIdentifier(build, hasTests = true),
            path = testPath(name).toVector,
            hasChildrenTests = true
          )
        )
      }.toVector)

      println("res: " + res)

      Restricted.exchange.channels.collectFirst {
        case c: NetworkChannel if c.name == channelName =>
          c
      }.foreach { c =>
        println("c: " + c)
        Restricted.jsonRpcRespond(c, res, Some(requestId))
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
    commands ++= Seq(listTests, runTests),

    serverHandlers += ServerHandler({ callback =>
      import callback._
      import sjsonnew.BasicJsonProtocol._
       ServerIntent(
        {
          case r: JsonRpcRequestMessage if r.method == "dotty/listTests" =>
            val j = json(r)
            println("j: " + j)
            val params = Converter.fromJson[ListTestsParams](j).get
            val builds = params.parents.map(_.build.name).mkString(" ")
            appendExec(Exec(s"listTests $name ${r.id} $builds", None, Some(CommandSource(name))))

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
