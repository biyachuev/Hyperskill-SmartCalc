import java.util.logging.*
import java.math.BigInteger

const val ERROR = "ERROR"
const val SUCCESSFUL = "SUCCESS"

const val UNKNOWN_CMD = "Unknown command"
const val UNKNOWN_VAR = "Unknown variable"
const val INVALID_EXPR = "Invalid expression"
const val INVALID_ASSIGNMENT = "Invalid assignment"
const val INVALID_ID = "Invalid identifier"
const val HELP_MSG = """Smart Calculator for [Big]integer values. Supported operations: +-/*^. Variable are supported (only letters).
|Examples:
|> a = 2
|> -12+   12 + -4 + 5 -   -6 - -- 7 ++++ -9 + a
|-7
|> b = a
|> b
| 2
|type /exit to exit"""

object Calc {
    private var isExit: Boolean = false
    private var varList: MutableMap<String, BigInteger> = mutableMapOf()    // list of variables created by user
    private val priority: Map<String, Int> = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "^" to 3)

    var result: String = ""
    private val logger = Logger.getLogger(Calc::class.qualifiedName)
    private val fileHandler = FileHandler("/Users/biyachuev/Documents/Projects_Kotlin/HyperSkill_SmartCalc/calc.log")

    init {
        logger.addHandler(fileHandler)
        logger.useParentHandlers = false
        fileHandler.formatter = SimpleFormatter()
    }

    object Stk {
        private val myStack: MutableList<String> = mutableListOf()
        fun clear() = myStack.clear()
        fun isEmpty(): Boolean = myStack.isEmpty()
        fun push(item: String) { myStack.add(item) }
        fun pop(): String? = if (!isEmpty()) myStack.removeAt(myStack.size - 1) else myStack.lastOrNull()
        fun peek(): String? = myStack.lastOrNull()
    }

    private fun printResult() {
        if (result.isNotEmpty()) println(result)
        logger.info("result = $result")
    }

    fun executeExpr(s: String) {
        result = ""
        when {
            s == "" -> { }
            s.startsWith('/') -> executeCmd(s)                                              // "/exit" or "/help"
            s.contains('=') -> processVariables(s)                                          // " a = -1 " or "a=b"
            s.filter { it in listOf('+', '-', '/', '*', '^') } == "" -> processVariables(s) // "a" or "2 3" or "a 2"
            else -> calculate(s)                                                            // expression or simple negative number e.g. "-2"
        }
        printResult()
    }

    private fun executeCmd(s: String) {
        when (s) {
            "/exit" -> isExit = true
            "/help" -> result = HELP_MSG
            else -> result = UNKNOWN_CMD
        }
    }

    fun isExit(): Boolean = isExit

    private fun processVariables(s: String) {
        var expr = s.trim()

        when (expr.filter { it == '=' }.count()) {
            // no "=" -> simply need to print value of variable/number e.g. "a" or "-2"
            0 -> when {
                expr.any { it.isWhitespace() } -> {
                    result = INVALID_EXPR
                    logger.warning("""Expression has zero equal signs and there spaces which are not allowed, e.g. "2 0 2""""")
                    return
                }
                expr.any { it.isDigit() } -> {
                    result = expr
                    logger.info("Expression is the number because there are no equal signs and expression contains at least 1 digit")
                    return
                }
                expr.any { !it.isDigit() && !it.isLetter() } -> {
                    result = INVALID_EXPR
                    logger.warning("Unacceptable characters in expression")
                    return
                }
                expr in varList.keys -> {
                    result = varList[expr].toString()
                    logger.info("variable $expr is found")
                    return
                }
                else -> {
                    result = UNKNOWN_VAR
                    logger.warning("Expression contains 0 equal signs and the variable in expression is not in the list of known variables - varList")
                    return
                }
            }
            1 -> { }   // the assignment "=" operation is processed below
            else -> {
                result = INVALID_ASSIGNMENT
                logger.warning("Expression contains more than one equal sign.")
                return
            }
        }

        expr = expr.replace(" ", "")
        val left = expr.substringBefore('=')
        val right = expr.substringAfter('=')

        if (right == "" || left == "") {
            result = INVALID_ASSIGNMENT
            logger.warning("Expression to the right or left of the equal sign contains an empty value")
            return
        }

        if (left.filter { !it.isLetter() } != "") {
            result = INVALID_ID
            logger.warning("The left side of the equal sign contains invalid characters")
            return
        }

        if (right.filter { !it.isLetter() } != "")
            try {
                right.toBigInteger()
            } catch (e: Exception) {
                result = INVALID_ASSIGNMENT
                logger.warning("On the right side of the expression can only be letters (e.g. a = b) or a negative number (e.g. a = -2)")
                return
            }

        // so we have correct syntax here e.g. a=-2 or a=b
        val res: BigInteger
        res = when {
            "\\d".toRegex().containsMatchIn(right) -> right.toBigInteger()
            right in varList.keys -> varList[right]!!
            else -> {
                result = INVALID_EXPR
                logger.warning("On the right side of the expression is an unknown variable")
                return
            }
        }
        varList[left] = res
    }

    private fun infixToPostfix(expr: String):String {
        var res = ""
        for (part in expr.split(" ")) {
            logger.fine("part = $part")
            try {
//                val stkPeekValue = Stk?.peek() ?: -1
                when {
                    part[0].isDigit() -> res += "$part "
                    Stk.isEmpty() || Stk.peek() == "(" -> Stk.push(part)
                    part == "(" -> Stk.push(part)
                    part == ")" -> {
                        do {
                            res += Stk.pop() + " "
                        } while (Stk.peek() != "(")
                        Stk.pop()       // discard left parentheses
                    }
                    priority.getValue(part) > priority[Stk.peek()]!! -> Stk.push(part)
                    priority.getValue(part) <= priority[Stk.peek()]!! -> {
                        loop@ do {
                            res += Stk.pop() + " "
                            if (!Stk.isEmpty())
                                when {
                                    Stk.peek() == "(" -> break@loop
                                    priority[Stk.peek()]!! < priority.getValue(part) -> break@loop
                                }
                        } while (!Stk.isEmpty())
                        Stk.push(part)
                    }
                }
            }
            catch (e: Exception) {
                logger.warning("Some error")
                return ERROR
            }
        }

        while (!Stk.isEmpty()) res += Stk.pop() + " "
        logger.info("result = ${res.trim()}")
        return res.trim()
    }

    private fun validateAndTransform (s: String): String {
        var expr = s.replace(" ", "")
        logger.info("Transform: whitespaces removed: \"$expr\"")

        try {
            result = s.toBigInteger().toString()
            logger.info("Validate: this is a valid negative number")
            return SUCCESSFUL
        }
        catch (e: Exception) {
            logger.info("Validate: Unsuccessful validation with toBigInteger() if the expr is a negative number e.g. -5")
        }

        if ("""(.*(\*{2,}).*)|(.*(/{2,}).*)|(.*(\^{2,}).*)""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.warning("Validate: *..* or /../ or ^..^ not allowed")
            return ERROR
        }

        while ("""(\+\+)|(--)""".toRegex().containsMatchIn(expr)) expr = expr.replace("""(\+\+)|(--)""".toRegex(), "+")
        while (expr.contains("+-")) expr = expr.replace("+-", "-")
        logger.info("Transform: ++/--   ->   +,    +-   ->   -   : \"$expr\"")

        while (expr.contains("(-")) expr = expr.replace("(-","(0-")
        logger.info("""Transform: "(-" -> "(0-" just to present "-a" as "(0-a)"""")

        if (""".*([+\-*/^])([+\-*/^]).*""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.warning("Validate: restricted sequence of operands")
            return ERROR
        }

        if ("""(.*(\()([+*/^]).*)|(.*([+\-*/^])(\)).*)""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.warning("""Validate: Brackets. There can be no operand after the left bracket "(" e.g. (*. There can be no operand before the right bracket ")" e.g. *)""")
            return ERROR
        }

        var balance = 0
        for (i in expr.indices) {
            when {
                expr[i] == '(' -> balance++
                expr[i] == ')' -> balance--
            }
            if (balance < 0) {
                result = INVALID_EXPR
                logger.warning("Balance for \"()\" < 0")
                return ERROR
            }
        }
        if (balance != 0) {
            result = INVALID_EXPR
            logger.warning("Balance for \"()\" != 0")
            return ERROR
        }

        val valid = """[\w\d+\-*/^()]+""".toRegex().matches(expr) &&   // in expr only 0-9,+,-,*,/,^ and letters allowed
                """.+[\w\d)]$""".toRegex().matches(expr)               // expr always ends with a digit or right parenthesis or letter

        if (!valid) {
            result = INVALID_EXPR
            logger.warning("""Validate: troubles with conditions in "valid" variable""")
            return ERROR
        }

        // Transform: insertion of whitespaces near operands
        var temp = ""
        for (i in expr.indices) {
            if (expr[i].toString() in listOf("+", "-", "/", "*", "^", ")", "(")) temp += " " + expr[i] + " " else temp += expr[i]
        }
        expr = temp.trim()
        while (expr.contains("  ")) expr = expr.replace("  ", " ")

        if (expr[0] == '-') expr = "( 0 " + expr.replace("""^([-]\s\d+)""".toRegex(), "$1 )")
        logger.info("Transform: \"- 2 + ...\" -> \"(0 - 2) + ...")

        // Validate: for correct variables and insert values instead of variables
        temp = ""
        for (part in expr.split(" ")) {
            if (part[0].isLetter())
                if (part in varList) {
                    temp += if (varList[part]!! >= BigInteger.ZERO) varList[part]!!.toString() + " "
                    else "( 0 - " + varList[part]!!.toString().drop(1) + " ) "
                }
                else {
                    result = INVALID_EXPR
                    logger.warning("Validate: unknown variable")
                    return ERROR
                }
            else temp += "$part "
        }

        logger.info("Validate: all variables are known: \"${temp.trim()}\"")
        return temp.trim()
    }

    private fun compute(s2: String, s1: String, op: String): String {
        try {
            val a = s1.toBigInteger()
            val b = s2.toBigInteger()
            var res: BigInteger
            when (op) {
                "+" -> res = a + b
                "-" -> res = a - b
                "/" -> {
                    if (b != BigInteger.ZERO) {
                        res = a / b
                    } else {
                        logger.warning("Division by zero: $a / $b")
                        return ERROR
                    }
                }
                "*" -> res = a * b
                "^" -> {
                    var i = BigInteger.ONE
                    res = a
                    when {
                        b == BigInteger.ZERO -> res = BigInteger.ONE
                        b < BigInteger.ZERO -> res = BigInteger.ZERO
                        else -> {
                            while (i != b) {
                                res *= a
                                i++
                            }
                        }
                    }
                }
                else -> {
                    logger.warning("Unsupported operand. Only +-/*^ are supported.")
                    return ERROR
                }
            }
            return res.toString()
        } catch (e: Exception) {
            logger.warning("Cannot convert params to BigInteger")
            return ERROR
        }
    }

    private fun calculate(s: String) {
        var expr = validateAndTransform(s)  // // e.g. s = "-12+   12 + -4 + 5 -   -6 - -- 7 ++++ -9 + a + b + 9 --- c"
        if (expr == ERROR || expr == SUCCESSFUL) return
        expr = infixToPostfix(expr)

        for (part in expr.split(" ")) {
            logger.fine("part = \"$part\"")
            when {
                part[0].isDigit() ->  Stk.push(part)
                else -> {
                    val temp = compute(Stk.pop()!!, Stk.pop()!!, part)
                    if (temp != "ERROR") Stk.push(temp)
                    else {
                        Stk.clear()
                        result = INVALID_EXPR
                        return
                    }
                }
            }
        }
        result = Stk.pop().toString()
    }
}

fun main() {
    while (!Calc.isExit()) Calc.executeExpr(readLine()!!)
    println("Bye!")
}