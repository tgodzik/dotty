package dotty.tools.sbtplugin

import sbt._
import sbt.Def.Initialize
import sbt.Keys._

import sbt.protocol.testing.TestResult

import sbt.internal.langserver._
import sbt.internal.langserver.codec.JsonProtocol._
import sbt.internal.protocol._
import sbt.internal.server._

import dotty.tools.sbtplugin.codec.JsonProtocol._

import sbt.dottyplugin.Restricted

// ~ no event when an individual test start
// - with junit-interface, no event when an individual test stop
class BspTestsListener(buildIdentifier: String) extends TestsListener {
  override def doInit(): Unit = {
    // println(s"doInit()")
  }
  override def startGroup(name: String): Unit = {
    // println(s"startGroup($name)")
  }

  private def broadcastTestStatus(testPath: List[String], hasChildrenTests: Boolean,
      kind: TestStatusKind, shortDescription: String = "", longDescription: String = ""): Unit = {
    val status =
      TestStatus(
        TestIdentifier(
          BuildIdentifier(buildIdentifier, hasTests = true),
          testPath.toVector,
          hasChildrenTests,
        ),
        kind,
        shortDescription,
        longDescription
      )
    Restricted.exchange.channels.collect {
      case c: NetworkChannel  =>
        c
    }.foreach { c =>
      Restricted.jsonRpcNotify(c, "dotty/testStatus", status)
    }
  }

  private def convertStatus(status: sbt.testing.Status): TestStatusKind = {
    import sbt.testing.{Status => S}
    import dotty.tools.sbtplugin.{TestStatusKind => TSK}
    status match {
      case S.Success => TSK.Success
      case S.Failure => TSK.Failure
      case S.Error => TSK.Failure
      case _ => TSK.Ignored
    }
  }

  private def convertResult(status: sbt.protocol.testing.TestResult): TestStatusKind = {
    import sbt.protocol.testing.{TestResult => TR}
    import dotty.tools.sbtplugin.{TestStatusKind => TSK}
    status match {
      case TR.Passed => TSK.Success
      case TR.Failed => TSK.Failure
      case TR.Error => TSK.Failure
    }
  }

  private def mkShortDescription(t: Throwable): String = t.toString

  private def mkLongDescription(t: Throwable): String = {
    val sw = new java.io.StringWriter
    sw.write(t.toString)
    sw.write("\nStacktrace:\n")
    t.printStackTrace(new java.io.PrintWriter(sw))
    sw.toString
  }

  private def convertTestName(test: String): List[String] = {
    // For now, we only handle tests of the from `prefix.name` where
    // `prefix` is a fully qualified class name.
    val fullPath = test.split("\\.")
    val prefix = fullPath.init.mkString(".")
    val name = fullPath.last
    List(prefix, name)
  }

  override def testEvent(event: TestEvent): Unit = {

    event.detail.foreach { e =>
      val (short, long) =
        if (e.throwable.isDefined)
          (mkShortDescription(e.throwable.get), mkLongDescription(e.throwable.get))
        else
          ("", "")

      broadcastTestStatus(
        convertTestName(e.fullyQualifiedName),
        hasChildrenTests = false,
        convertStatus(e.status),
        short,
        long
      )
    }
  }

  override def endGroup(name: String, result: sbt.protocol.testing.TestResult): Unit = {
    broadcastTestStatus(
      List(name),
      hasChildrenTests = true,
      convertResult(result),
      ""
    )
  }

  override def endGroup(name: String, t: Throwable): Unit = {
    broadcastTestStatus(
      List(name),
      hasChildrenTests = true,
      TestStatusKind.Failure,
      mkShortDescription(t),
      mkLongDescription(t)
    )
  }

  override def doComplete(finalResult: sbt.protocol.testing.TestResult): Unit = {
    broadcastTestStatus(
      Nil,
      hasChildrenTests = true,
      convertResult(finalResult),
      ""
    )
  }
}
