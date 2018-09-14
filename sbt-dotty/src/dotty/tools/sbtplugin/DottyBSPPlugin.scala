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
    val buildTargetIdentifier = settingKey[String]("A unique identifier for this target")
    // private[dotty] val listTests = inputKey[Unit]("List all test classes")
  }
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger = allRequirements

  val buildTargetIdentifierSetting =
    buildTargetIdentifier := s"${thisProject.value.id}/${configuration.value}"

  val testRunStarted = "Test run started"
  val testRunFinishedPrefix = "Test run finished"
  val testStarted = raw"Test (.*) started".r
  val testIgnored = raw"Test (.*) ignored".r
  val testFailed = raw"Test (.*) failed: (.*), took .*sec".r

  def stripAnsiColors(m: String): String = m.replaceAll("\u001B\\[[;\\d]*m", "")

  // FIXME: Should be replaced by sbt testListeners, but right now not good enough:
  // ~ no event when a test start
  // - with junit-interface, no event when a test stop
  def logger(target: String) = {
    val appender = new AbstractAppender(s"DottyBSPPlugin-$target", null, Restricted.dummyLayout, true)  {
      var runningTest: Option[String] = None

      def append(x: LogEvent): Unit = {

        def broadcastTestStatus(target: String, test: String, kind: TestStatusKind,
          details: Option[String] = None): Unit = {
          val status = 
            TestStatus(
              TestIdentifier(BuildTargetIdentifier(target), test),
              kind, details
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

        def runningTestFinished(kind: TestStatusKind, details: Option[String] = None) = {
          runningTest foreach { prevTest =>
            broadcastTestStatus(target, prevTest, kind, details)
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
                      broadcastTestStatus(target, startedTest, TestStatusKind.Running)
                    case testIgnored(ignoredTest) =>
                      runningTestFinished(TestStatusKind.Success)
                      broadcastTestStatus(target, ignoredTest, TestStatusKind.Ignored)
                    case testFailed(failedTest, details) =>
                      if (runningTest != Some(failedTest)) {
                        // println(s"!!! $runningTest != $failedTest")
                        assert(false, s"!!! $runningTest != $failedTest")
                      }
                      runningTestFinished(TestStatusKind.Failure, details = Some(details))
                    case _ =>
                      println("#MSG: " + msg)
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
    inConfig(Compile)(Seq(buildTargetIdentifierSetting)) ++
    inConfig(Test)(Seq(
      buildTargetIdentifierSetting,
      // We use JUnit native support for listeners instead of sbt testListeners
      // because the latter is not good enough currently (e.g. it only reports
      // the status of a test once all tests in a class have finished).
      // testOptions += Tests.Argument(TestFrameworks.JUnit, "--run-listener=dotty.tools.sbtplugin.oJUnitListener")
    )) ++
    Seq(
      // testListeners += new BspTestsListener(streams.value.log),
      extraLoggers := {
        val currentFunction = extraLoggers.value
        val target = (buildTargetIdentifier in Test).value

        if (isDotty.value) {
          (key: ScopedKey[_]) => {
            // println("key: " + key)
            // println("scope: " + key.scope)
            logger(target) +: currentFunction(key)
          }
        } else {
          currentFunction
        }
      }
    )
  }

  type TargetId = String
  type Target = (ProjectRef, Configuration)

  private[dotty] def runTaskInTargets[T](key: TaskKey[T], targetIds: Seq[TargetId], state: State): (State, Seq[(TargetId, T)]) = {
    val structure = Project.structure(state)
    val settings = structure.data
    val joinedTask = structure.allProjectRefs.flatMap { projRef =>
      val project = Project.getProjectForReference(projRef, structure).get
      project.configurations.flatMap { config =>
        buildTargetIdentifier.in(projRef, config).get(settings).flatMap { id =>
          if (targetIds.contains(id))
            key.in(projRef, config).get(settings).map(_.map(v => id -> v))
          else
            None
        }
      }
    }.join

    DottyIDEPlugin.runTask(joinedTask, state)
  }

  private[dotty] def runInputTaskInTargets[T](key: InputKey[T], input: String, targetIds: Seq[TargetId], state: State): (State, Seq[(TargetId, T)]) = {
    val structure = Project.structure(state)
    val settings = structure.data
    val joinedTask = structure.allProjectRefs.flatMap { projRef =>
      val project = Project.getProjectForReference(projRef, structure).get
      project.configurations.flatMap { config =>
        buildTargetIdentifier.in(projRef, config).get(settings).flatMap { id =>
          if (targetIds.contains(id))
            Some(key.in(projRef, config).toTask(input).evaluate(settings).map(v => id -> v))
          else
            None
        }
      }
    }.join

    DottyIDEPlugin.runTask(joinedTask, state)
  }

  // def targetsOf(targetIds: Seq[String], state: State): Seq[Target] = {
  //   val structure = Project.structure(state)
  //   val settings = structure.data

  //   structure.allProjectRefs.flatMap { projRef =>
  //     val project = Project.getProjectForReference(projRef, structure).get
  //     project.configurations.flatMap { config =>
  //       buildTargetIdentifier.in(projRef, config).get(settings).flatMap { id =>
  //         if (targetIds.contains(id))
  //           Some((projRef, config))
  //         else
  //           None
  //       }
  //     }
  //   }
  // }

  def listTests =
    Command.args("listTests", "<channelName> <requestId> <targets>*") { (state0, args) =>
      val channelName +: requestId +: targets = args

      // val refs = targetsOf(targetId, origState)
      val (state1, tests) = runTaskInTargets(definedTestNames, targets, state0)

      val res = ListTestsResults(tests.flatMap { case (target, names) =>
        names.map(name =>
          ListTestsItem(
            id = TestIdentifier(BuildTargetIdentifier(target), name),
            subTests = Vector.empty
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
    Command.args("runTests", "<channelName> <requestId> <buildTarget> <testTargets>*") { (state0, args) =>
      val channelName +: requestId +: buildTarget +: testTargets = args

      val testArgs = testTargets.mkString(" ")
      println("buildTarget: " + buildTarget)
      println("testArgs: " + testArgs)
      // val (state1, _) = runTaskInTargets(testOnly.toTask(s" $testArgs"), Seq(buildTarget), origState)
      val state1 = try {
        runInputTaskInTargets(testOnly, s" $testArgs", Seq(buildTarget), state0)._1
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
            val params = Converter.fromJson[ListTestsParams](json(r)).get
            val targets = params.targets.map(_.name).mkString(" ")
            
           appendExec(Exec(s"listTests $name ${r.id} $targets", None, Some(CommandSource(name))))

          case r: JsonRpcRequestMessage if r.method == "dotty/runTests" =>
            val params = Converter.fromJson[RunTestsParams](json(r)).get
            val targets = params.tests.groupBy(_.target).foreach { case (target, tests) =>
              val testNames = tests.map(_.name).mkString(" ")
              appendExec(Exec(s"runTests $name ${r.id} ${target.name} $testNames", None, Some(CommandSource(name))))
            }
       },
        PartialFunction.empty
      )
    })
  )
}
