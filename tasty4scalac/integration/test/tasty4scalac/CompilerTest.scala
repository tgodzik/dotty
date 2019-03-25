package tasty4scalac

import java.nio.file.Path

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import tasty.binary.BinaryInput
import tasty.{TastyADT, TastyADTUnpickler}

import scala.io.Source

@RunWith(classOf[Parameterized])
class CompilerTest(name: String, unused: String) {

  private val scalac = Scalac()
  private val dotty = Dotty()
  private val code = Source.fromFile(CompilerTest.root.resolve(name).toFile).mkString
  private val scalacOutput = scalac.compile(code).mapValues(convert)
  private val dottyOutput = dotty.compile(code).mapValues(convert)

  @Test
  def scalacEmitsSameAmountOfTastyFiles(): Unit =
    assertEquals(dottyOutput.keySet, scalacOutput.keySet)

  @Test
  def scalacEmitsSubsetOfDottyEmittedNames(): Unit = {
    for {
      (name, scalacTasty) <- scalacOutput
      dottyTasty = dottyOutput(name)
      scalacEmittedNames = scalacTasty.names.toSet
      dottyEmittedNames = dottyTasty.names.toSet
      missingNames = scalacEmittedNames -- dottyEmittedNames
    } assertTrue(s"scalac emitted non-matching names for: $name. Those are: [$missingNames]", missingNames.isEmpty)
  }

  private def convert(binary: BinaryTasty): TastyADT = {
    val input = new BinaryInput(binary.bytes)
    TastyADTUnpickler.unpickle(input)
  }
}

object CompilerTest {

  import collection.JavaConverters._

  private val testCases = ScalaTestCaseSource.testCases().map(_.getFileName.toString)

  def root: Path = ScalaTestCaseSource.root

  @Parameters(name = "{0}")
  def data(): java.util.Collection[Array[String]] = {
    testCases.zip(testCases).map { case (code1, code2) => Array(code1, code2) }.asJavaCollection
  }
}
