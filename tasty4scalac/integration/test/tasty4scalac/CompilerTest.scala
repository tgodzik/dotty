package tasty4scalac

import java.util

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import tasty.binary.BinaryInput
import tasty.{TastyADT, TastyADTUnpickler}

@RunWith(classOf[Parameterized])
class CompilerTest(scalacOutput: Map[String, TastyADT], dottyOutput: Map[String, TastyADT]) {
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
}

object CompilerTest {

  import collection.JavaConverters._

  private val scalac = Scalac()
  private val dotty = Dotty()

  @Parameters()
  def parameters(): util.Collection[Array[Any]] = {

    val testCases = for {
      code <- ScalaTestCaseSource.testCases()
      scalacOutput = scalac.compile(code).mapValues(convert)
      dottyOutput = dotty.compile(code).mapValues(convert)
    } yield Array[Any](scalacOutput, dottyOutput)

    testCases.asJavaCollection
  }

  private def convert(binary: BinaryTasty): TastyADT = {
    val input = new BinaryInput(binary.bytes)
    TastyADTUnpickler.unpickle(input)
  }
}
