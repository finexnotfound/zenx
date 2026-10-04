package com.example.zenx.interpreter

import com.example.zenx.filesystem.ZenFileSystem
import java.io.File

class SimplePythonInterpreter(private val fs: ZenFileSystem) {
    private val variables = mutableMapOf<String, Any>()

    fun executeScript(fileArg: String): String {
        val file = fs.resolveFile(fileArg)
        if (!file.exists()) {
            return "python: can't open file '$fileArg': [Errno 2] No such file or directory"
        }
        val lines = file.readLines()
        val output = StringBuilder()
        for (line in lines) {
            val res = evaluateLine(line.trim())
            if (res.isNotBlank()) output.append(res).append("\n")
        }
        return output.toString().trimEnd()
    }

    fun evaluateLine(line: String): String {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return ""

        // Print statement
        if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
            val content = trimmed.substring(6, trimmed.length - 1).trim()
            return evaluatePrintContent(content)
        }

        // Variable assignment: x = 10 or msg = "hello"
        if (trimmed.contains("=") && !trimmed.contains("==")) {
            val parts = trimmed.split("=", limit = 2)
            val varName = parts[0].trim()
            val expr = parts[1].trim()
            val value = evaluateExpression(expr)
            variables[varName] = value
            return ""
        }

        // Direct expression evaluation
        return evaluateExpression(trimmed).toString()
    }

    private fun evaluatePrintContent(raw: String): String {
        // String literal: "..." or '...'
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length - 1)
        }

        // f-string: f"..."
        if (raw.startsWith("f\"") && raw.endsWith("\"")) {
            var text = raw.substring(2, raw.length - 1)
            for ((k, v) in variables) {
                text = text.replace("{$k}", v.toString())
            }
            return text
        }

        // Variable or expression
        if (variables.containsKey(raw)) {
            return variables[raw].toString()
        }

        return evaluateExpression(raw).toString()
    }

    private fun evaluateExpression(expr: String): Any {
        val trimmed = expr.trim()
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        if (variables.containsKey(trimmed)) {
            return variables[trimmed] ?: ""
        }
        val intVal = trimmed.toIntOrNull()
        if (intVal != null) return intVal
        val doubleVal = trimmed.toDoubleOrNull()
        if (doubleVal != null) return doubleVal

        // Basic arithmetic: a + b, a - b, a * b, a / b
        for (op in listOf("+", "-", "*", "/")) {
            if (trimmed.contains(op)) {
                val parts = trimmed.split(op, limit = 2)
                val leftVal = evaluateExpression(parts[0].trim()).toString().toDoubleOrNull()
                val rightVal = evaluateExpression(parts[1].trim()).toString().toDoubleOrNull()
                if (leftVal != null && rightVal != null) {
                    val result = when (op) {
                        "+" -> leftVal + rightVal
                        "-" -> leftVal - rightVal
                        "*" -> leftVal * rightVal
                        "/" -> if (rightVal != 0.0) leftVal / rightVal else Double.NaN
                        else -> 0.0
                    }
                    return if (result % 1.0 == 0.0) result.toLong() else result
                }
            }
        }
        return trimmed
    }
}

class SimpleNodeInterpreter(private val fs: ZenFileSystem) {
    fun executeScript(fileArg: String): String {
        val file = fs.resolveFile(fileArg)
        if (!file.exists()) {
            return "node: internal/modules/cjs/loader.js: Cannot find module '$fileArg'"
        }
        val lines = file.readLines()
        val output = StringBuilder()
        for (line in lines) {
            val res = evaluateLine(line.trim())
            if (res.isNotBlank()) output.append(res).append("\n")
        }
        return output.toString().trimEnd()
    }

    fun evaluateLine(line: String): String {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("//")) return ""

        if (trimmed.startsWith("console.log(") && trimmed.endsWith(")")) {
            val content = trimmed.substring(12, trimmed.length - 1).trim()
            if ((content.startsWith("\"") && content.endsWith("\"")) || (content.startsWith("'") && content.endsWith("'")) || (content.startsWith("`") && content.endsWith("`"))) {
                return content.substring(1, content.length - 1)
            }
            return content
        }
        return "undefined"
    }
}
