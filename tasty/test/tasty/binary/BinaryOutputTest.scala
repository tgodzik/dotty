package tasty.binary

import org.junit.runner.RunWith
import org.junit.{Assert, Test}

@RunWith(classOf[org.junit.runners.JUnit4])
class BinaryOutputTest {
  @Test
  def writesSubSectionWithoutWastingSpace(): Unit = {
    val section = new BinaryOutput()

    // when
    section.writeSubsection {
      section.writeByte(5)
    }

    // then
    Assert.assertEquals(section.size, 2)
  }

  @Test
  def writingContinuesImmediatelyAfterSubsection(): Unit = {
    val section = new BinaryOutput()
    section.writeSubsection(section.writeByte(5))

    // when
    section.writeByte(10)

    // then
    Assert.assertEquals(section.bytes(2), 10)
  }

}
