package tasty4scalac

import java.nio.file.{Files, Path, Paths}

import scala.collection.JavaConverters._


object ScalaTestCaseSource {
  private val scalaExtension = ".scala"
  private val root = Paths.get(System.getProperty("test.root.directory"))

  def testCases(): Seq[String] = findTestCases(root)

  private def findTestCases(path: Path): Seq[String] = {
    if (!Files.exists(path)) Nil
    else Files.walk(path)
      .iterator()
      .asScala
      .filter(_.getFileName.toString.endsWith(scalaExtension))
      .map(readContent)
      .toSeq
  }

  private def readContent(path: Path): String = new String(Files.readAllBytes(path))
}
