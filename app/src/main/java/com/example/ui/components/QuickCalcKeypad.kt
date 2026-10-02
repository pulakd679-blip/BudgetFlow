package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun QuickCalcKeypad(
    currentExpression: String,
    onExpressionChanged: (String) -> Unit,
    onCalculateDone: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyRows = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        label = key,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("keypad_$key"),
                        isAction = key in listOf("÷", "×", "-", "+", "="),
                        isEqual = key == "=",
                        isClear = key == "C"
                    ) {
                        when (key) {
                            "C" -> onExpressionChanged("")
                            "⌫" -> {
                                if (currentExpression.isNotEmpty()) {
                                    onExpressionChanged(currentExpression.dropLast(1))
                                }
                            }
                            "=" -> {
                                val result = evaluateSimpleExpression(currentExpression)
                                if (result != null) {
                                    val formatted = if (result % 1.0 == 0.0) {
                                        result.toLong().toString()
                                    } else {
                                        String.format(Locale.US, "%.2f", result)
                                    }
                                    onExpressionChanged(formatted)
                                    onCalculateDone(result)
                                }
                            }
                            "÷" -> onExpressionChanged(currentExpression + "/")
                            "×" -> onExpressionChanged(currentExpression + "*")
                            else -> onExpressionChanged(currentExpression + key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isEqual: Boolean = false,
    isClear: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = when {
        isEqual -> MaterialTheme.colorScheme.primary
        isAction -> MaterialTheme.colorScheme.primaryContainer
        isClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isEqual -> MaterialTheme.colorScheme.onPrimary
        isAction -> MaterialTheme.colorScheme.onPrimaryContainer
        isClear -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (label == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = contentColor
                )
            } else {
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = if (isAction || isEqual) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Basic safe expression evaluator for finance keypad
 */
fun evaluateSimpleExpression(expr: String): Double? {
    if (expr.isBlank()) return null
    return try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        // Tokenize and evaluate using recursive descent parser
        val parser = SimpleExpressionParser(sanitized)
        parser.parse()
    } catch (e: Exception) {
        null
    }
}

private class SimpleExpressionParser(private val text: String) {
    private var pos = 0

    fun parse(): Double {
        val res = parseExpression()
        return res
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (pos < text.length) {
            when (text[pos]) {
                '+' -> {
                    pos++
                    x += parseTerm()
                }
                '-' -> {
                    pos++
                    x -= parseTerm()
                }
                else -> return x
            }
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (pos < text.length) {
            when (text[pos]) {
                '*' -> {
                    pos++
                    x *= parseFactor()
                }
                '/' -> {
                    pos++
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("Division by zero")
                    x /= d
                }
                else -> return x
            }
        }
        return x
    }

    private fun parseFactor(): Double {
        while (pos < text.length && text[pos].isWhitespace()) pos++
        if (pos >= text.length) return 0.0

        if (text[pos] == '+') {
            pos++
            return parseFactor()
        }
        if (text[pos] == '-') {
            pos++
            return -parseFactor()
        }

        if (text[pos] == '(') {
            pos++
            val res = parseExpression()
            if (pos < text.length && text[pos] == ')') pos++
            return res
        }

        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) {
            pos++
        }
        val numStr = text.substring(start, pos)
        return numStr.toDoubleOrNull() ?: 0.0
    }
}
