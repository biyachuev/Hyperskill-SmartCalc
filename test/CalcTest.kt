import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CalcTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun executeExpr() {
        Calc.executeExpr("2 * 6")
        assertEquals("12", Calc.result)

        Calc.executeExpr("2 - 2 + 3")
        assertEquals("3", Calc.result)

        Calc.executeExpr("8 * 3 + 12 * (4 - 2)")
        assertEquals("48", Calc.result)

        Calc.executeExpr("-10")
        assertEquals("-10", Calc.result)

        Calc.executeExpr("3 + 8 * ((4 + 3) * 2 + 1) - 6 / (2 + 1)")
        assertEquals("121", Calc.result)

        Calc.executeExpr("-1+ 2 ^ 5 / ( 5 * 4 --+1 ) + 10 - 11 * ( 2 - 4 ) --- 7 +++ - 9")
        assertEquals("16", Calc.result)

        Calc.executeExpr("2^2")
        assertEquals("4", Calc.result)

        Calc.executeExpr("2*2^3")
        assertEquals("16", Calc.result)

        Calc.executeExpr("2^(-2)")
        assertEquals("0", Calc.result)

        Calc.executeExpr("4+ 3)")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("4*(2+3")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("3 *** 5")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("1 +++ 2 * 3 -- 4")
        assertEquals("11", Calc.result)

        Calc.executeExpr("1 +++ 2 * 3 -- 4")
        assertEquals("11", Calc.result)

        Calc.executeExpr("a=-2")
        assertEquals("", Calc.result)
        Calc.executeExpr("-2*(-a)")
        assertEquals("-4", Calc.result)

        Calc.executeExpr("2 / (-4)")
        assertEquals("0", Calc.result)

        Calc.executeExpr("0/5")
        assertEquals("0", Calc.result)

        Calc.executeExpr("12/06")
        assertEquals("2", Calc.result)

        Calc.executeExpr("12/-06")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("6/08")
        assertEquals("0", Calc.result)

        Calc.executeExpr("a=0")
        Calc.executeExpr("0/a")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("a/0")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("ABC=2")
        Calc.executeExpr("abc")
        assertEquals("Unknown variable", Calc.result)

        Calc.executeExpr("AB C")
        assertEquals("Invalid expression", Calc.result)

        Calc.executeExpr("a=4")
        Calc.executeExpr("b = 5 ")
        Calc.executeExpr("c =6  ")
        Calc.executeExpr(" -12+   12 + -4 + 5 -   -6 - -- 7 ++++ -9 + a + b + 9 --- c")
        assertEquals("3", Calc.result)

        Calc.executeExpr("a=4")
        Calc.executeExpr("b = 5 ")
        Calc.executeExpr("c =6  ")
        Calc.executeExpr("a*2+b*3+c*(2+3)")
        assertEquals("53", Calc.result)

        Calc.executeExpr(" - 1 + 2 ^ 5 / ( 5 * 4 + 1)")
        assertEquals("0", Calc.result)

        Calc.executeExpr("a = -2")
        Calc.executeExpr("-1 + 2 ^ 5 / ( 5 * 4 + 1 ) + a")
        assertEquals("-2", Calc.result)

        Calc.executeExpr("( 5 * 4 + 1)")
        assertEquals("21", Calc.result)
    }
}
