package com.example.data.lab

data class ExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String = "",
    val executionTimeMs: Long = 28,
    val exitCode: Int = 0,
    val engineName: String = "Virtual Sandboxed Runtime",
    val isSimulated: Boolean = true
)

interface CodeExecutor {
    suspend fun execute(code: String, language: String): ExecutionResult
}

class SafeSandboxedExecutionEngine : CodeExecutor {

    override suspend fun execute(code: String, language: String): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val lang = language.lowercase().trim()

        // 1. Basic syntax sanity check
        val syntaxCheck = checkSyntax(code, lang)
        if (!syntaxCheck.success) {
            val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(14)
            return ExecutionResult(
                success = false,
                stdout = "",
                stderr = syntaxCheck.stderr,
                executionTimeMs = duration,
                exitCode = 1,
                engineName = "Virtual Sandboxed Runtime",
                isSimulated = true
            )
        }

        // 2. Parse & simulate execution
        val outputLines = mutableListOf<String>()
        val variables = mutableMapOf<String, Any>()

        try {
            val lines = code.lines()
            var inForLoop = false
            var forVarName = ""
            var forStart = 0
            var forEnd = 0
            var forStep = 1
            val loopBodyLines = mutableListOf<String>()

            var idx = 0
            while (idx < lines.size) {
                val rawLine = lines[idx].trim()
                idx++

                // Skip preprocessor / package / imports / comments / empty
                if (rawLine.isEmpty() || rawLine.startsWith("//") || rawLine.startsWith("#") ||
                    rawLine.startsWith("package ") || rawLine.startsWith("import ") ||
                    rawLine.startsWith("/*") || rawLine.startsWith("*")
                ) {
                    continue
                }

                // Detect for loop: for (int i = 1; i <= 5; i++) {
                val forMatch = Regex("""for\s*\(\s*(?:int\s+)?(\w+)\s*=\s*(\d+)\s*;\s*\1\s*(<=|<)\s*(\d+)\s*;\s*\1(?:\+\+|\s*\+=\s*(\d+))\s*\)""").find(rawLine)
                if (forMatch != null) {
                    forVarName = forMatch.groupValues[1]
                    forStart = forMatch.groupValues[2].toInt()
                    val isLessOrEqual = forMatch.groupValues[3] == "<="
                    val limit = forMatch.groupValues[4].toInt()
                    forEnd = if (isLessOrEqual) limit else limit - 1
                    forStep = forMatch.groupValues.getOrNull(5)?.toIntOrNull() ?: 1

                    // Collect loop body
                    loopBodyLines.clear()
                    var braceCount = if (rawLine.contains("{")) 1 else 0
                    while (idx < lines.size && braceCount > 0) {
                        val bodyLine = lines[idx].trim()
                        idx++
                        if (bodyLine.contains("{")) braceCount++
                        if (bodyLine.contains("}")) braceCount--
                        if (braceCount > 0) {
                            loopBodyLines.add(bodyLine)
                        }
                    }

                    // Execute loop
                    var loopVal = forStart
                    var safetyCounter = 0
                    while (loopVal <= forEnd && safetyCounter < 1000) {
                        variables[forVarName] = loopVal
                        for (bodyLine in loopBodyLines) {
                            processStatement(bodyLine, lang, variables, outputLines)
                        }
                        loopVal += forStep
                        safetyCounter++
                    }
                    continue
                }

                // Normal statement processing
                processStatement(rawLine, lang, variables, outputLines)
            }

            val finalOutput = if (outputLines.isEmpty()) {
                "Program finished with no printed output."
            } else {
                outputLines.joinToString("\n")
            }

            val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(18)
            return ExecutionResult(
                success = true,
                stdout = finalOutput,
                stderr = "",
                executionTimeMs = duration,
                exitCode = 0,
                engineName = "Virtual Sandboxed Runtime",
                isSimulated = true
            )

        } catch (e: Exception) {
            val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(15)
            return ExecutionResult(
                success = false,
                stdout = outputLines.joinToString("\n"),
                stderr = "Runtime Diagnostic: ${e.message ?: "Evaluation exception"}",
                executionTimeMs = duration,
                exitCode = 2,
                engineName = "Virtual Sandboxed Runtime",
                isSimulated = true
            )
        }
    }

    private fun processStatement(
        line: String,
        lang: String,
        variables: MutableMap<String, Any>,
        outputLines: MutableList<String>
    ) {
        val trimmed = line.trim()

        // 1. Variable Assignment: int width = 12; or var count = 5
        val varAssignMatch = Regex("""(?:int|float|double|val|var|auto|String)\s+([a-zA-Z_]\w*)\s*=\s*(.+?);?$""").find(trimmed)
        if (varAssignMatch != null) {
            val varName = varAssignMatch.groupValues[1]
            val expr = varAssignMatch.groupValues[2].trim()
            val evaluated = evaluateExpression(expr, variables)
            if (evaluated != null) {
                variables[varName] = evaluated
            }
            return
        }

        // 2. Direct assignment: count += 5 or x = x * 2;
        val reassignment = Regex("""^([a-zA-Z_]\w*)\s*(\+=|-=|\*=|=)\s*(.+?);?$""").find(trimmed)
        if (reassignment != null) {
            val varName = reassignment.groupValues[1]
            val op = reassignment.groupValues[2]
            val expr = reassignment.groupValues[3].trim()
            val evaluated = evaluateExpression(expr, variables)
            if (evaluated != null) {
                when (op) {
                    "=" -> variables[varName] = evaluated
                    "+=" -> {
                        val curr = (variables[varName] as? Number)?.toDouble() ?: 0.0
                        val add = (evaluated as? Number)?.toDouble() ?: 0.0
                        variables[varName] = (curr + add).toInt()
                    }
                    "*=" -> {
                        val curr = (variables[varName] as? Number)?.toDouble() ?: 1.0
                        val mult = (evaluated as? Number)?.toDouble() ?: 1.0
                        variables[varName] = (curr * mult).toInt()
                    }
                }
            }
            return
        }

        // 3. Print Statements:
        // C: printf(...)
        if (trimmed.startsWith("printf(") || trimmed.contains("printf(")) {
            val content = extractCallArguments(trimmed, "printf")
            if (content != null) {
                val formatted = parsePrintf(content, variables)
                outputLines.add(formatted)
            }
            return
        }

        // C++: std::cout << ... or cout << ...
        if (trimmed.contains("cout") && trimmed.contains("<<")) {
            val output = parseCout(trimmed, variables)
            outputLines.add(output)
            return
        }

        // Java: System.out.println(...) / System.out.print(...)
        if (trimmed.contains("System.out.println(") || trimmed.contains("System.out.print(")) {
            val isPrintln = trimmed.contains("println")
            val content = extractCallArguments(trimmed, if (isPrintln) "System.out.println" else "System.out.print")
            if (content != null) {
                val parsed = parseJavaPrint(content, variables)
                outputLines.add(parsed)
            }
            return
        }

        // Kotlin / Android: println(...)
        if (trimmed.startsWith("println(") || trimmed.contains("println(")) {
            val content = extractCallArguments(trimmed, "println")
            if (content != null) {
                val parsed = parseKotlinPrint(content, variables)
                outputLines.add(parsed)
            }
            return
        }
    }

    private fun checkSyntax(code: String, lang: String): SyntaxCheckResult {
        var openBraces = 0
        var openParens = 0
        var inString = false
        var prevChar = ' '

        for (ch in code) {
            if (ch == '"' && prevChar != '\\') {
                inString = !inString
            }
            if (!inString) {
                if (ch == '{') openBraces++
                if (ch == '}') openBraces--
                if (ch == '(') openParens++
                if (ch == ')') openParens--
            }
            prevChar = ch
        }

        if (inString) {
            return SyntaxCheckResult(false, "Syntax Error: Unterminated string literal.")
        }
        if (openBraces != 0) {
            return SyntaxCheckResult(false, "Syntax Error: Mismatched curly braces '{ }' (difference: $openBraces).")
        }
        if (openParens != 0) {
            return SyntaxCheckResult(false, "Syntax Error: Mismatched parentheses '( )' (difference: $openParens).")
        }

        // Check for missing main in C/C++/Java if non-trivial
        if (code.length > 50) {
            if (lang == "c" && !code.contains("main(")) {
                return SyntaxCheckResult(false, "Linker Error: undefined reference to 'main'. C programs require an int main() entry point.")
            }
            if (lang == "cpp" && !code.contains("main(")) {
                return SyntaxCheckResult(false, "Linker Error: in function '_start': undefined reference to 'main'.")
            }
            if (lang == "java" && !code.contains("main(String[]")) {
                return SyntaxCheckResult(false, "Runtime Error: Main method not found in class, please define the main method as:\n   public static void main(String[] args)")
            }
        }

        return SyntaxCheckResult(true, "")
    }

    private fun extractCallArguments(line: String, fnName: String): String? {
        val startIdx = line.indexOf(fnName)
        if (startIdx == -1) return null
        val parenStart = line.indexOf('(', startIdx)
        if (parenStart == -1) return null
        val parenEnd = line.lastIndexOf(')')
        if (parenEnd <= parenStart) return null
        return line.substring(parenStart + 1, parenEnd).trim()
    }

    private fun parsePrintf(args: String, vars: Map<String, Any>): String {
        // Example: "Age: %d, Score: %.1f\n", age, score
        val parts = splitArgs(args)
        if (parts.isEmpty()) return ""
        val formatStr = unquote(parts[0]).replace("\\n", "")
        if (parts.size == 1) return formatStr

        var result = formatStr
        val specRegex = Regex("""%[0-9.]*[difsScf]""")
        val matches = specRegex.findAll(formatStr).toList()

        for (i in matches.indices) {
            if (i + 1 < parts.size) {
                val argExpr = parts[i + 1].trim()
                val evaluated = evaluateExpression(argExpr, vars) ?: argExpr
                val match = matches[i]
                result = result.replaceFirst(match.value, evaluated.toString())
            }
        }
        return result
    }

    private fun parseCout(line: String, vars: Map<String, Any>): String {
        // cout << "Count: " << i << std::endl;
        val parts = line.split("<<")
        val builder = StringBuilder()
        for (i in 1 until parts.size) {
            var token = parts[i].trim().removeSuffix(";").trim()
            if (token == "endl" || token == "std::endl") {
                continue
            }
            if (token.startsWith("\"") && token.endsWith("\"")) {
                builder.append(unquote(token))
            } else {
                val evaluated = evaluateExpression(token, vars)
                builder.append(evaluated ?: token)
            }
        }
        return builder.toString()
    }

    private fun parseJavaPrint(content: String, vars: Map<String, Any>): String {
        return evaluateStringConcatenation(content, vars)
    }

    private fun parseKotlinPrint(content: String, vars: Map<String, Any>): String {
        if (content.startsWith("\"") && content.endsWith("\"")) {
            var raw = unquote(content)
            // Handle $var interpolation
            val varRegex = Regex("""\$([a-zA-Z_]\w*)""")
            raw = varRegex.replace(raw) { matchResult ->
                val vName = matchResult.groupValues[1]
                vars[vName]?.toString() ?: matchResult.value
            }
            return raw
        }
        return evaluateStringConcatenation(content, vars)
    }

    private fun evaluateStringConcatenation(expr: String, vars: Map<String, Any>): String {
        val tokens = expr.split("+")
        val sb = StringBuilder()
        for (token in tokens) {
            val t = token.trim()
            if (t.startsWith("\"") && t.endsWith("\"")) {
                sb.append(unquote(t))
            } else {
                val evaluated = evaluateExpression(t, vars)
                sb.append(evaluated ?: t)
            }
        }
        return sb.toString()
    }

    private fun evaluateExpression(expr: String, vars: Map<String, Any>): Any? {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return null

        // Number literal
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // Boolean
        if (trimmed == "true") return true
        if (trimmed == "false") return false

        // String literal
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return unquote(trimmed)
        }

        // Variable lookup
        if (vars.containsKey(trimmed)) {
            return vars[trimmed]
        }

        // Simple binary arithmetic: a * b, a + b, etc.
        for (op in listOf("*", "/", "%", "+", "-")) {
            if (trimmed.contains(op)) {
                val parts = trimmed.split(op, limit = 2)
                if (parts.size == 2) {
                    val left = evaluateExpression(parts[0], vars)
                    val right = evaluateExpression(parts[1], vars)
                    if (left is Number && right is Number) {
                        val l = left.toDouble()
                        val r = right.toDouble()
                        val res = when (op) {
                            "*" -> l * r
                            "/" -> if (r != 0.0) l / r else 0.0
                            "%" -> l % r
                            "+" -> l + r
                            "-" -> l - r
                            else -> 0.0
                        }
                        return if (res % 1.0 == 0.0) res.toInt() else res
                    }
                }
            }
        }

        return trimmed
    }

    private fun splitArgs(args: String): List<String> {
        val list = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (ch in args) {
            if (ch == '"') inQuotes = !inQuotes
            if (ch == ',' && !inQuotes) {
                list.add(current.toString().trim())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            list.add(current.toString().trim())
        }
        return list
    }

    private fun unquote(s: String): String {
        return s.removePrefix("\"").removeSuffix("\"")
    }

    private data class SyntaxCheckResult(val success: Boolean, val stderr: String)

    companion object {
        fun getTemplateForLanguage(lang: String): String {
            return when (lang.lowercase()) {
                "c" -> """#include <stdio.h>

int main() {
    printf("DevLearn Virtual Sandbox\n");
    int status = 200;
    printf("Status Code: %d\n", status);
    return 0;
}"""
                "cpp", "c++" -> """#include <iostream>

int main() {
    std::cout << "DevLearn C++ Sandbox" << std::endl;
    int cores = 8;
    std::cout << "Active Cores: " << cores << std::endl;
    return 0;
}"""
                "java" -> """public class Main {
    public static void main(String[] args) {
        System.out.println("DevLearn Java Platform");
        int memoryMB = 512;
        System.out.println("Allocated Heap: " + memoryMB + "MB");
    }
}"""
                else -> """fun main() {
    val app = "DevLearn Android"
    val version = "3.2"
    println("Welcome to ${'$'}app v${'$'}version")
}"""
            }
        }
    }
}
