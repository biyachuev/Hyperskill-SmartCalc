import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CalcBigIntegerTest {

    @Test
    fun parseCmdLine() {
        Calc.executeExpr("112234567890 + 112234567890 * (10000000999 - 999)")
        assertEquals("1122345679012234567890", Calc.result)

        Calc.executeExpr("a = 800000000000000000000000")
        Calc.executeExpr("b = 100000000000000000000000")
        Calc.executeExpr("a + b")
        assertEquals("900000000000000000000000", Calc.result)
    }
}