package cloud.kosch.aiandroid.ai

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.pow

sealed interface LocalSearchUtilityResult {
    val display: String

    data class Calculation(
        val expression: String,
        val value: Double,
        override val display: String,
    ) : LocalSearchUtilityResult

    data class Conversion(
        val inputValue: Double,
        val fromUnit: String,
        val toUnit: String,
        val value: Double,
        override val display: String,
    ) : LocalSearchUtilityResult
}

/**
 * Fully offline utility evaluator for Universal Search.
 *
 * It intentionally supports only a bounded arithmetic grammar and an allow-listed unit table. No script engine,
 * reflection, network lookup, locale-sensitive code execution or arbitrary function names are involved.
 */
object LocalSearchUtilityEngine {
    fun evaluate(rawQuery: String): LocalSearchUtilityResult? {
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isBlank()) return null
        parseConversion(query)?.let { return it }
        if (!looksLikeArithmetic(query)) return null
        val normalized = query.replace(',', '.')
        val value = runCatching { ArithmeticParser(normalized).parse() }.getOrNull() ?: return null
        if (!value.isFinite()) return null
        return LocalSearchUtilityResult.Calculation(
            expression = query,
            value = value,
            display = formatNumber(value),
        )
    }

    private fun parseConversion(query: String): LocalSearchUtilityResult.Conversion? {
        val match = CONVERSION.matchEntire(query.lowercase(Locale.ROOT).replace(',', '.')) ?: return null
        val input = match.groupValues[1].toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
        val fromAlias = normalizeUnitAlias(match.groupValues[2])
        val toAlias = normalizeUnitAlias(match.groupValues[3])
        val from = UNITS[fromAlias] ?: return null
        val to = UNITS[toAlias] ?: return null
        if (from.dimension != to.dimension) return null

        val value = when (from.dimension) {
            UnitDimension.TEMPERATURE -> convertTemperature(input, from.canonical, to.canonical)
            else -> input * from.toBaseFactor / to.toBaseFactor
        }
        if (!value.isFinite()) return null
        return LocalSearchUtilityResult.Conversion(
            inputValue = input,
            fromUnit = from.canonical,
            toUnit = to.canonical,
            value = value,
            display = "${formatNumber(value)} ${to.canonical}",
        )
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> error("Unsupported temperature unit")
        }
        return when (to) {
            "°C" -> celsius
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            "K" -> celsius + 273.15
            else -> error("Unsupported temperature unit")
        }
    }

    private fun looksLikeArithmetic(query: String): Boolean {
        if (query.length > MAX_QUERY_LENGTH) return false
        if (!query.any(Char::isDigit) && !query.contains("pi", ignoreCase = true)) return false
        return query.all { character ->
            character.isDigit() || character.isWhitespace() || character in ".,+-*/^()%" ||
                character.lowercaseChar() in setOf('p', 'i', 'e')
        }
    }

    private fun normalizeUnitAlias(raw: String): String = raw
        .trim()
        .lowercase(Locale.ROOT)
        .replace("°", "")
        .replace(" ", "")

    private fun formatNumber(value: Double): String {
        val magnitude = kotlin.math.abs(value)
        if (magnitude != 0.0 && (magnitude >= 1e12 || magnitude < 1e-8)) {
            return String.format(Locale.ROOT, "%.8g", value)
        }
        return BigDecimal(value, MathContext(12, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()
    }

    private enum class UnitDimension { LENGTH, MASS, VOLUME, TIME, DATA, TEMPERATURE }

    private data class UnitDefinition(
        val canonical: String,
        val dimension: UnitDimension,
        val toBaseFactor: Double = 1.0,
    )

    private fun unit(
        canonical: String,
        dimension: UnitDimension,
        factor: Double,
        vararg aliases: String,
    ): List<Pair<String, UnitDefinition>> {
        val definition = UnitDefinition(canonical, dimension, factor)
        return (aliases.toList() + canonical)
            .map(::normalizeUnitAlias)
            .distinct()
            .map { it to definition }
    }

    private val UNITS: Map<String, UnitDefinition> = buildMap {
        fun add(canonical: String, dimension: UnitDimension, factor: Double, vararg aliases: String) {
            putAll(unit(canonical, dimension, factor, *aliases))
        }

        add("mm", UnitDimension.LENGTH, 0.001, "millimeter", "millimetre")
        add("cm", UnitDimension.LENGTH, 0.01, "centimeter", "centimetre")
        add("m", UnitDimension.LENGTH, 1.0, "meter", "metre")
        add("km", UnitDimension.LENGTH, 1_000.0, "kilometer", "kilometre")
        add("in", UnitDimension.LENGTH, 0.0254, "inch", "inches")
        add("ft", UnitDimension.LENGTH, 0.3048, "foot", "feet")
        add("yd", UnitDimension.LENGTH, 0.9144, "yard", "yards")
        add("mi", UnitDimension.LENGTH, 1_609.344, "mile", "miles")

        add("mg", UnitDimension.MASS, 0.000001, "milligram", "milligrams")
        add("g", UnitDimension.MASS, 0.001, "gram", "grams")
        add("kg", UnitDimension.MASS, 1.0, "kilogram", "kilograms")
        add("oz", UnitDimension.MASS, 0.028349523125, "ounce", "ounces")
        add("lb", UnitDimension.MASS, 0.45359237, "lbs", "pound", "pounds")

        add("ml", UnitDimension.VOLUME, 0.001, "milliliter", "millilitre")
        add("cl", UnitDimension.VOLUME, 0.01, "centiliter", "centilitre")
        add("l", UnitDimension.VOLUME, 1.0, "liter", "litre")
        add("cup", UnitDimension.VOLUME, 0.2365882365, "cups", "us cup")
        add("gal", UnitDimension.VOLUME, 3.785411784, "gallon", "gallons", "us gallon")

        add("ms", UnitDimension.TIME, 0.001, "millisecond", "milliseconds")
        add("s", UnitDimension.TIME, 1.0, "sec", "second", "seconds")
        add("min", UnitDimension.TIME, 60.0, "minute", "minutes")
        add("h", UnitDimension.TIME, 3_600.0, "hr", "hour", "hours")
        add("d", UnitDimension.TIME, 86_400.0, "day", "days")

        add("B", UnitDimension.DATA, 1.0, "byte", "bytes")
        add("KB", UnitDimension.DATA, 1_000.0, "kilobyte", "kilobytes")
        add("MB", UnitDimension.DATA, 1_000_000.0, "megabyte", "megabytes")
        add("GB", UnitDimension.DATA, 1_000_000_000.0, "gigabyte", "gigabytes")
        add("TB", UnitDimension.DATA, 1_000_000_000_000.0, "terabyte", "terabytes")
        add("KiB", UnitDimension.DATA, 1_024.0, "kibibyte", "kibibytes")
        add("MiB", UnitDimension.DATA, 1_048_576.0, "mebibyte", "mebibytes")
        add("GiB", UnitDimension.DATA, 1_073_741_824.0, "gibibyte", "gibibytes")

        add("°C", UnitDimension.TEMPERATURE, 1.0, "c", "celsius")
        add("°F", UnitDimension.TEMPERATURE, 1.0, "f", "fahrenheit")
        add("K", UnitDimension.TEMPERATURE, 1.0, "kelvin")
    }

    private class ArithmeticParser(private val source: String) {
        private var index = 0

        fun parse(): Double {
            require(source.length <= MAX_QUERY_LENGTH) { "Expression too long" }
            val result = expression()
            skipWhitespace()
            require(index == source.length) { "Unexpected token" }
            return result
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('+') -> value + term()
                    consume('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('*') -> value * power()
                    consume('/') -> {
                        val divisor = power()
                        require(divisor != 0.0) { "Division by zero" }
                        value / divisor
                    }
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            var value = unary()
            skipWhitespace()
            if (consume('^')) {
                val exponent = power()
                require(kotlin.math.abs(exponent) <= MAX_EXPONENT_ABS) { "Exponent too large" }
                value = value.pow(exponent)
            }
            return value
        }

        private fun unary(): Double {
            skipWhitespace()
            return when {
                consume('+') -> unary()
                consume('-') -> -unary()
                else -> postfix()
            }
        }

        private fun postfix(): Double {
            var value = primary()
            skipWhitespace()
            while (consume('%')) {
                value /= 100.0
                skipWhitespace()
            }
            return value
        }

        private fun primary(): Double {
            skipWhitespace()
            if (consume('(')) {
                val value = expression()
                skipWhitespace()
                require(consume(')')) { "Missing closing parenthesis" }
                return value
            }
            readConstant()?.let { return it }
            return number()
        }

        private fun readConstant(): Double? {
            val remaining = source.substring(index).lowercase(Locale.ROOT)
            return when {
                remaining.startsWith("pi") -> {
                    index += 2
                    Math.PI
                }
                remaining.startsWith("e") -> {
                    index += 1
                    Math.E
                }
                else -> null
            }
        }

        private fun number(): Double {
            skipWhitespace()
            val start = index
            var decimalSeen = false
            while (index < source.length) {
                val current = source[index]
                if (current.isDigit()) {
                    index += 1
                } else if (current == '.' && !decimalSeen) {
                    decimalSeen = true
                    index += 1
                } else {
                    break
                }
            }
            require(index > start) { "Number expected" }
            return source.substring(start, index).toDouble()
        }

        private fun consume(character: Char): Boolean {
            if (index < source.length && source[index] == character) {
                index += 1
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index += 1
        }
    }

    private const val MAX_QUERY_LENGTH = 200
    private const val MAX_EXPONENT_ABS = 100.0
    private val CONVERSION = Regex(
        "^\\s*([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*([a-z°]+(?:\\s+cup|\\s+gallon)?)\\s*(?:to|in|into|nach|zu|->|>)\\s*([a-z°]+(?:\\s+cup|\\s+gallon)?)\\s*$",
        RegexOption.IGNORE_CASE,
    )
}
