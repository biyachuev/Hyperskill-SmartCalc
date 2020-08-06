import java.util.logging.*
import java.math.BigInteger

// TODO убрать cтремный код
// TODO try/catch 100000000000 + 100000000000000000000, та же история с Double и Float
// TODO поисследовать varList = mutableMapOf<String, Int>("" to 0) по аналогии с Stk может можно убрать !! и prio (Null to 0)
// TODO убрать русские комменты
// TODO где можно try, например, просто проверять toInt вместо сложных проверок на /d
// TODO BigInteger mode только когда он действительно нужен
// TODO BigInteger as class

const val VALIDATION_ERROR = "ERROR"
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
    private var varList = mutableMapOf<String, BigInteger>("" to BigInteger.ZERO)    // list of variables created by user
    private val prio = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "^" to 3)
    var result: String = ""
    private val logger = Logger.getLogger(Calc::class.qualifiedName)
    private val fileHandler = FileHandler("calc.log")

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

    fun printResult() {
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
            else -> calculate(s)                                                            // выражение или просто отрицательное число, например, "-2"
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
            // no "=" -> need to print value of variable/number e.g. "a" or "-2"
            0 -> when {
                expr.filter { it.isWhitespace() }.count() > 0 -> { // e.g. "2 0 2"
                    result = INVALID_EXPR
                    logger.info("0 знаков =, есть пробелы, которых быть не должно")
                    return
                }
                expr.filter { it.isDigit() }.count() > 0 -> {
                    result = expr
                    logger.info("0 знаков =, и есть хотя бы одна цифра -> значит имеем дело с числом")
                    return
                }
                expr in varList.keys -> {
                    result = varList[expr].toString()
                    return
                }
                else -> {
                    result = UNKNOWN_VAR
                    logger.info("0 знаков =, и не нашли переменную в varList")
                    return
                }
            }
            // операция присваивания разбирается ниже
            1 -> { }
            // several "=" found
            else -> {
                result = INVALID_ASSIGNMENT
                logger.info("More than 1 equal \"=\" sign")
                return
            }
        }

        expr = expr.replace(" ", "")
        val left = expr.substringBefore('=')
        val right = expr.substringAfter('=')

        if (right == "" || left == "") {
            result = INVALID_ASSIGNMENT
            logger.info("справа или слева пусто")
            return
        }

        if (left.filter { !it.isLetter() } != "") { // only letters must be before "="
            result = INVALID_ID
            logger.info("слева от равно, содержит что-то помимо букв - что запрещено для переменной")
            return
        }

        // проверяем на a = -2 или a = b
        if (right.filter { !it.isLetter() } != "") // проверяем условие справа от равно только буквы
            try {
                right.toBigInteger() // result = BigInteger
            }
            catch (e: Exception) {
                result = INVALID_ASSIGNMENT
                logger.info("Справа от присваивания что-то неправильное")
                return
            }

        // so we have correct syntax here e.g. a=-2 or a=b
        var res = BigInteger.ZERO
        res = when {
            "\\d".toRegex().containsMatchIn(right) -> right.toBigInteger() // легко меняется на toBigInteger
            right in varList.keys -> varList[right]!!
            else -> {
                result = INVALID_EXPR
                logger.info("Не нашли такую переменную после =")
                return
            }
        }

        //  это что??? стремный код
        when (left) {
            in varList.keys -> varList[left] = res // проверить можно ли иметь mutableList с BigInteger
            else -> varList[left] = res
        }
    }

    private fun infixToPostfix(expr: String):String {
        var res = ""
        for (part in expr.split(" ")) {
            logger.fine("part = $part")
            when {
                part[0].isDigit() -> res += "$part "
                Stk.isEmpty() || Stk.peek() == "(" -> Stk.push(part)
                part == "(" -> Stk.push(part)
                part == ")" -> {
                    do {
                        res += Stk.pop() + " "
                    } while (Stk.peek() != "(")
                    Stk.pop() // discard left parentheses
                }
                prio[part]!! > prio[Stk.peek()]!! -> Stk.push(part)
                prio[part]!! <= prio[Stk.peek()]!! -> {
                    loop@ do {
                        res += Stk.pop() + " "
                        if (!Stk.isEmpty())
                            when {
                                Stk.peek() == "(" -> break@loop
                                prio[Stk.peek()]!! < prio[part]!! -> break@loop // подумать как сделать лучше
                            }
                    } while (!Stk.isEmpty()) // наверняка это можно отрефакторить
                    Stk.push(part)
                }
            }
        }

        while (!Stk.isEmpty()) res += Stk.pop() + " "
        logger.info("result = ${res.trim()}")
        return res.trim()
    }

    private fun validateAndTransform (s: String): String {
        var temp = ""
        var expr = s

        // Transform: //combinations like "88 22" are not valid
        if ("""\d\s+\d""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.info("Validate: combinations like \"88 22\" are not valid")
            return VALIDATION_ERROR
        }

        expr = expr.replace(" ", "")
        logger.info("Transform: whitespaces removed: \"$expr\"")

        // Validate: if expr is a negative number only
        try {
            result = s.toBigInteger().toString() // проверить
            logger.info("this is number")
            return SUCCESSFUL
        }
        catch (e: Exception) {
            null
        }

        // Validate:  *..* or /../ or ^..^ not allowed
        if ("""(.*(\*{2,}).*)|(.*(/{2,}).*)|(.*(\^{2,}).*)""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.info("Validate: *..* or /../ or ^..^ not allowed")
            return VALIDATION_ERROR
        }

        // Transform: ++/-- -> +
        while ("""(\+\+)|(--)""".toRegex().containsMatchIn(expr)) {
            expr = expr.replace("""(\+\+)|(--)""".toRegex(), "+")
        }
        // Transform: +- -> - & "  " -> " "
        while (expr.contains("+-")) expr = expr.replace("+-", "-")
        logger.info("Transform: ++/-- -> +, +- -> -: \"$expr\"")

        // "(-" -> "(0-" just to present "-a" as "(0-a)"
        while (expr.contains("(-")) expr = expr.replace("(-","(0-")

        // Validate: restricted sequence of operands
        if (""".*([+\-*/^])([+\-*/^]).*""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.info("validate: restricted sequence of operands")
            return VALIDATION_ERROR
        }

        // Validate: скобки. После ( не может быть операнд кроме минуса, перед ) не может быть операнд e.g. (* or *)
        if ("""(.*(\()([+*/^]).*)|(.*([+\-*/^])(\)).*)""".toRegex().containsMatchIn(expr)) {
            result = INVALID_EXPR
            logger.info("Validate: скобки. После ( не может быть операнд, перед ) не может быть операнд e.g. (* or *)")
            return VALIDATION_ERROR
        }

        // Validate: баланс скобок
        var balance = 0
        for (i in expr.indices) {
            when {
                expr[i] == '(' -> balance++
                expr[i] == ')' -> balance--
            }
            if (balance < 0) {
                result = INVALID_EXPR
                logger.info("Balance for \"()\" < 0")
                return VALIDATION_ERROR
            }
        }
        if (balance != 0) {
            result = INVALID_EXPR
            logger.info("Balance for \"()\" != 0")
            return VALIDATION_ERROR
        }

        val valid = """[\w\d+\-*/^()]+""".toRegex().matches(expr) &&   // in expr only 0-9,+,-,*,/,^ and letters allowed
                """.+[\w\d)]$""".toRegex().matches(expr)               // expr always ends with a digit or right parenthesis or letter

        if (!valid) {
            result = INVALID_EXPR
            logger.info("Validate: нельзя иметь сложные условия")
//                logger.info("""[\w\d+\-*/^()]+""".toRegex().matches(expr))
//                logger.info(""".+[\w\d)]$""".toRegex().matches(expr))
            return VALIDATION_ERROR
        }

        // Transform: insertion of whitespaces near operands
        temp = ""
        for (i in expr.indices) {
            if (expr[i].toString() in listOf("+", "-", "/", "*", "^", ")", "(")) temp += " " + expr[i] + " " else temp += expr[i]
        }
        expr = temp.trim()
        while (expr.contains("  ")) expr = expr.replace("  ", " ")

        // Transform: "- 2 + ..." -> "(0 - 2) + ..."
        if (expr[0] == '-') {
            expr = "( 0 " + expr.replace("""^([-]\s\d+)""".toRegex(), "$1 )")
        }

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
                    logger.info("unknown variable")
                    return VALIDATION_ERROR
                }
            else temp += "$part "
        }

        logger.info("Validate: all variables are known: \"${temp.trim()}\"")
        return temp.trim()
    }

    private fun compute(s2: String, s1: String, op: String): String {
        val a = s1.toBigInteger()
        val b = s2.toBigInteger()
        var res = BigInteger.ZERO
        when (op) {
            "+" -> res = a + b
            "-" -> res = a - b
            "/" -> {
                if (b != BigInteger.ZERO) {
                    res = a / b
                }
                else {
                    logger.info("Division by zero")
                    return VALIDATION_ERROR
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
                logger.info("Unsupported operand. Only +-/*^ are supported.")
                return VALIDATION_ERROR
            }
        }
        return res.toString()
    }

    private fun calculate(s: String) { // подумать потом вывести подстановку значений переменных выше
        var expr = validateAndTransform(s)  // // e.g. "-12+   12 + -4 + 5 -   -6 - -- 7 ++++ -9 + a + b + 9 --- c"
        if (expr == VALIDATION_ERROR || expr == SUCCESSFUL) return
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
                } // здесь можно вставить проверки try
            }
        }
        result = Stk.pop().toString()
    }
}

fun main() {
    while (!Calc.isExit()) Calc.executeExpr(readLine()!!)
    println("Bye!")
}