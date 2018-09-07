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

  override def projectSettings: Seq[Setting[_]] = {
    inConfig(Compile)(Seq(buildTargetIdentifierSetting)) ++
    inConfig(Test)(Seq(buildTargetIdentifierSetting))
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
