package tasty4scalac

import java.io._
import java.net.URL
import java.nio.file._

import scala.reflect.internal.util.{BatchSourceFile, NoFile, SourceFile}
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.NoReporter

final class Scalac(settings: Settings) extends Global(settings, NoReporter) with Compiler {
  override def compile(code: String): Unit = {
    val file = new BatchSourceFile(NoFile, code.toCharArray)
    compile(file)
  }

  private def compile(files: SourceFile*): Unit = {
    new Run().compileSources(files.toList)
  }
}

object Scalac extends Compiler.Factory {
  private val classpath = System.getProperty("scalac.classpath")
  private val pluginPath = System.getProperty("scalac.plugin.classpath")

  def apply(): Scalac = {
    val settings = new Settings()
    settings.classpath.value = classpath
    settings.plugin.value = List(pluginPath)
    settings.stopAfter.value = List("tasty")
    settings.d.value = {
      val path = Files.createTempDirectory("tasty-generation")
      Files.createDirectories(path).toString
    }

    new Scalac(settings)
  }
}