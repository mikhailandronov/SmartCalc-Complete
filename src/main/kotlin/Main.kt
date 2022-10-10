package calculator

import java.math.BigInteger

typealias ValueType = BigInteger

fun main() {
    while (true) {
        val input = readln().trim()

        // if input is empty string then just skip it
        if (input == "") continue

        // if input is command then execute it
        val cmd = Command.parse(input)
        if (cmd != null){
            cmd.execute()
            if (cmd.isFinalCommand()) break
        }

        // if input is expression then calculate it
        val expr = Expression.parse(input)
        if (expr?.getValue() != null)
            println(expr.getValue())
    }
}

abstract class Command (private val isFinal: Boolean = false) {
    companion object {
        fun parse(input: String): Command? {
            if (!input.matches(Regex("""/[a-zA-Z]+""")))
                return null
            else
                return when (input) {
                    "/help" -> HelpCommand()
                    "/exit" -> ExitCommand()
                    else -> {
                        println("Unknown command")
                        null
                    }
                }
        }
    }
    abstract fun execute()

    fun isFinalCommand() = isFinal
}

class HelpCommand: Command(){
    override fun execute() {
        println("The program calculates the value of expression.\n" +
                "Allowed commands are /help and /exit.\n" +
                "Allowed operations are +, -, *, / .\n" +
                "Variables are also allowed that must be named with letters only.")
    }
}

class ExitCommand: Command(true){
    override fun execute() {
        println("Bye!")
    }
}

abstract class Expression {
    companion object {
        fun parse(input: String, isAssignment: Boolean = false): Expression? {
            if (input.matches(Regex("""/[a-zA-Z]+"""))) // commands must be ignored
                return null


            else if (input.matches(Regex("""\b[a-zA-Z]+\b"""))) // just a variable name
                if (Variable.isKnown(input))
                    return Variable(input)
                else {
                    println("Unknown variable")
                    return null
                }


            else if (input.matches(Regex("""[+-]?\b\d+\b"""))) // just a number
                return Number(input.toBigInteger())



            else if (!input.contains("="))                           // not an assignment
                // look for invalid identifiers
                if (Regex("""\b[a-zA-Z]+\d+[a-zA-Z]*\b""").find(input) != null ||
                    Regex("""\b\d+[a-zA-Z]+\d*\b""").find(input) != null )
                {
                    if (!isAssignment) println("Invalid identifier")
                    return null
                }
                // look for mismatched parenthesis
                else if (input.count{it == '('} != input.count{it == ')'} )
                {
                    if (!isAssignment) println("Invalid expression")
                    return null
                }
                // look for **** or /////
                else if (Regex("""\*\*+|//+""").find(input) != null)
                {
                    if (!isAssignment) println("Invalid expression")
                    return null
                }
                else return CompoundExpression(convertToPostfix(input))


            else if (input.contains("=")) {                       // an assignment
                val varName = input.split("=")[0]

                if (!varName.trim().matches(Regex("""\b[a-zA-Z]+\b"""))) { // variable to assign isn't correct
                    println("Invalid identifier")
                }
                else {                                    // variable to assign is correct => assign expression value

                    val exprToAssign = input.drop(varName.length + 1)
                    val varExpr = parse(exprToAssign.trim(), isAssignment = true)

                    if (varExpr?.getValue() != null)
                        Variable.assign(varName.trim(), varExpr.getValue()!!)
                    else
                        println("Invalid assignment")
                }
                return null
            }

            else {
                println("Invalid expression")
                return null
            }
        }


        // refer to [Calculation algorithm.docx] file for algorithm description
        private fun convertToPostfix(infixExpression: String): MutableList<String>{
            val operationsPrecedenceMap = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)
            val cleanedExpression = infixExpression
                .replace(Regex("""--"""), "+")
                .replace(Regex("""\++"""), "+")
                .replace(Regex("""\s"""), "")
                .replace(Regex("""\+-"""), "-")

            val tokens = tokenizeExpression(cleanedExpression)
            val stack = ArrayDeque<String>()
            val res = mutableListOf<String>()
            for (token in tokens)
                when (token){
                    "(" -> stack.addFirst(token)
                    ")" -> {
                        while (!stack.isEmpty() && stack.first() != "(")
                            res.add(stack.removeFirst())
                        if (!stack.isEmpty()) stack.removeFirst() // skip "("
                    }
                    in arrayOf("*", "/", "+", "-") ->
                    {
                        if (stack.isEmpty())
                            stack.addFirst(token)
                        else // !stack.isEmpty()
                            when (stack.first()) {
                                "(" -> stack.addFirst(token)
                                in arrayOf("*", "/", "+", "-") ->
                                {
                                    while (!stack.isEmpty() &&
                                            stack.first() != "(" &&
                                            (operationsPrecedenceMap[stack.first()]?: 10) >= (operationsPrecedenceMap[token] ?: 0)) {
                                        res.add(stack.removeFirst())
                                    }

                                    stack.addFirst(token)
                                }
                                else -> throw IllegalArgumentException("Invalid expression")
                            }
                    }
                    else -> res.add(token)
                }
            while (!stack.isEmpty()) res.add(stack.removeFirst())
            return res
        }

        private fun tokenizeExpression(infixExpression: String): MutableList<String> {
            val res = mutableListOf<String>()
            // Try to find a number, or a variable or some of allowed operands: (, ), +, -, *, /

            var i = 0
            var unaryOpAllowed = true
            while (i < infixExpression.length){
                if (infixExpression[i] in arrayOf('(', '*', '/')) {
                    res.add(infixExpression[i].toString())
                    unaryOpAllowed = true
                }

                if (infixExpression[i] == ')') {
                    res.add(infixExpression[i].toString())
                    unaryOpAllowed = false
                }

                if (infixExpression[i] in arrayOf('+', '-')){
                    if (unaryOpAllowed) {
                        res.add("0")
                        unaryOpAllowed = false
                    }
                    res.add(infixExpression[i].toString())
                }

                if (infixExpression[i].isDigit()){
                    var number = ""
                    while (i < infixExpression.length && infixExpression[i].isDigit())
                        number += infixExpression[i++]
                    res.add(number)
                    i--
                    unaryOpAllowed = false
                }

                if (infixExpression[i].isLetter()){
                    var name = ""
                    while (i < infixExpression.length && infixExpression[i].isLetter())
                        name += infixExpression[i++]
                    res.add(name)
                    i--
                    unaryOpAllowed = false
                }
                i++
            }

//            println(res)
            return res
        }
    }

    abstract fun getValue(): ValueType?
}

class Variable(private val name: String): Expression(){
    companion object{
        private val memory = mutableMapOf(
            "one" to BigInteger.valueOf(1),
            "two" to BigInteger.valueOf(2),
            "three" to BigInteger.valueOf(3),
            "four" to BigInteger.valueOf(4),
            "five" to BigInteger.valueOf(5)
        )

        fun isKnown(name: String) = memory.containsKey(name)

        fun assign(name: String, value: ValueType){
                memory[name] = value
        }
    }
    override fun getValue(): ValueType?{
        return memory[name]
    }
}

class Number(private val value: ValueType): Expression(){
    override fun getValue(): ValueType?{
        return value
    }
}

class CompoundExpression(private val postfixExpressionTokens: MutableList<String>): Expression(){
    override fun getValue(): ValueType?{
//        println(postfixExpressionTokens)
        val stack = ArrayDeque<ValueType>()

        for (token in postfixExpressionTokens){
            if (token.matches(Regex("""\b\d+\b""")) or // number or variable
                token.matches(Regex("""\b[a-zA-Z]+\b"""))
            ) {
                val literal = parse(token)
                if (literal?.getValue() != null)
                    stack.addFirst(literal.getValue()!!)
                else
                    return null
            }

            if (token in arrayOf("*", "/", "+", "-")){
                val op2 = stack.removeFirst()
                val op1 = stack.removeFirst()
                val res = when(token){
                    "-" -> op1.minus(op2)
                    "+" -> op1.plus(op2)
                    "/" -> op1.divide(op2)
                    "*" -> op1.multiply(op2)
                    else -> BigInteger.valueOf(0)
                }
                stack.addFirst(res)
            }
        }

        if (stack.count() == 1)
            return stack.removeFirst()
        else
            return null
    }
}

