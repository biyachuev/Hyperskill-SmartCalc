import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CalcBigIntegerTest {

    @Test
    fun parseCmdLine() {
        // https://www.calculator.net/big-number-calculator.html
        Calc.executeExpr("112234567890 + 112234567890 * (10000000999 - 999)")
        assertEquals("1122345679012234567890", Calc.result)

        Calc.executeExpr("a = 800000000000000000000000")
        Calc.executeExpr("b = 100000000000000000000000")
        Calc.executeExpr("a + b")
        assertEquals("900000000000000000000000", Calc.result)

        Calc.executeExpr("10 ^ 100")
        assertEquals("10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000", Calc.result)

        Calc.executeExpr("100000000000000000000000 ^ 2")
        assertEquals("10000000000000000000000000000000000000000000000", Calc.result)

        Calc.executeExpr("990000000000000000000000099 ^ (-2)")
        assertEquals("0", Calc.result)

        Calc.executeExpr("112234567890112234567890 ^ 1")
        assertEquals("112234567890112234567890", Calc.result)

        Calc.executeExpr("112234567890112234567890 ^ 0")
        assertEquals("1", Calc.result)

        // отрицательная степень
        // нулевая степень
    }
}