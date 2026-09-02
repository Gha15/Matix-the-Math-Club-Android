package club.matix.mathclub.ai

import java.net.URL
import java.net.URLEncoder
import kotlin.math.pow

/**
 * Matix AI Engine
 *
 * Provides search, math parsing, clock, and offline lessons.
 * - Search: Google primary, DuckDuckGo fallback
 * - Math Parser: Evaluates expressions (< 3s)
 * - Clock: Returns current time
 * - Lessons: Fractions, algebra, percentages, geometry
 */
object MatixAi {

    data class SearchResult(
        val title: String,
        val snippet: String,
        val url: String
    )

    /**
     * Primary search via Google, fallback to DuckDuckGo.
     * Returns max 10 results within <3 seconds.
     */
    fun search(query: String): List<SearchResult> {
        return try {
            searchGoogle(query).takeIf { it.isNotEmpty() }
                ?: searchDuckDuckGo(query)
        } catch (e: Exception) {
            searchDuckDuckGo(query)
        }
    }

    /**
     * Google Search with num=10&hl=en&gbv=1 parameters.
     * Extracts title, snippet, and URL from results.
     */
    private fun searchGoogle(query: String): List<SearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.google.com/search?q=$encoded&num=10&hl=en&gbv=1"

        return try {
            val response = fetchUrl(url)
            parseGoogleResults(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses Google search HTML response.
     * Looks for result blocks with title (h3), snippet, and link (href).
     */
    private fun parseGoogleResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Simple regex pattern to extract search results from Google HTML
        val resultPattern = """<div[^>]*data-sokoban-container[^>]*>.*?</div>""".toRegex()
        
        resultPattern.findAll(html).forEach { resultBlock ->
            try {
                val blockHtml = resultBlock.value
                
                // Extract URL
                val urlMatch = """href="([^"]+)"""".toRegex().find(blockHtml)
                val url = urlMatch?.groupValues?.get(1)?.let { 
                    it.replace(Regex("^/url\\?q=([^&]+).*"), "$1") 
                } ?: return@forEach
                
                // Extract title
                val titleMatch = """<h3[^>]*>([^<]+)<""".toRegex().find(blockHtml)
                val title = titleMatch?.groupValues?.get(1)?.trim() ?: return@forEach
                
                // Extract snippet
                val snippetMatch = """<span[^>]*class="VwiC3b[^"]*"[^>]*>([^<]+)<""".toRegex().find(blockHtml)
                val snippet = snippetMatch?.groupValues?.get(1)?.trim() ?: ""

                if (url.isNotBlank() && title.isNotBlank()) {
                    results.add(SearchResult(title, snippet, url))
                }
            } catch (e: Exception) {
                // Skip malformed results
            }
        }

        return results.take(10)
    }

    /**
     * DuckDuckGo fallback search (used when Google fails or is blocked).
     */
    private fun searchDuckDuckGo(query: String): List<SearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://duckduckgo.com/html?q=$encoded"

        return try {
            val response = fetchUrl(url)
            parseDuckDuckGoResults(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses DuckDuckGo HTML response for results.
     */
    private fun parseDuckDuckGoResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Extract result blocks from DuckDuckGo HTML
        val resultPattern = """<div class="result[^"]*">.*?</div>""".toRegex()
        
        resultPattern.findAll(html).forEach { resultBlock ->
            try {
                val blockHtml = resultBlock.value
                
                // Extract URL
                val urlMatch = """<a[^>]*href="([^"]+)"""".toRegex().find(blockHtml)
                val url = urlMatch?.groupValues?.get(1)?.trim() ?: return@forEach
                
                // Extract title
                val titleMatch = """<a[^>]*class="result__url[^"]*"[^>]*>([^<]+)<""".toRegex().find(blockHtml)
                val title = titleMatch?.groupValues?.get(1)?.trim() ?: return@forEach
                
                // Extract snippet
                val snippetMatch = """<a[^>]*class="result__snippet[^"]*"[^>]*>([^<]+)<""".toRegex().find(blockHtml)
                val snippet = snippetMatch?.groupValues?.get(1)?.trim() ?: ""

                if (url.isNotBlank() && title.isNotBlank()) {
                    results.add(SearchResult(title, snippet, url))
                }
            } catch (e: Exception) {
                // Skip malformed results
            }
        }

        return results.take(10)
    }

    /**
     * Fetches URL content with timeout (< 3s).
     */
    private fun fetchUrl(urlString: String): String {
        val connection = URL(urlString).openConnection()
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        return connection.getInputStream().bufferedReader().use { it.readText() }
    }

    /**
     * Math Expression Parser
     * Evaluates expressions with proper order of operations.
     * Supports: +, -, *, /, %, ^, parentheses
     * Returns null if expression is invalid.
     */
    fun parseMath(expr: String): Double? {
        return try {
            parseExpression(expr.trim())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpression(expr: String): Double {
        val tokens = tokenize(expr)
        val (result, _) = parseAddSub(tokens, 0)
        return result
    }

    private fun parseAddSub(tokens: List<String>, pos: Int): Pair<Double, Int> {
        var (left, p) = parseMulDiv(tokens, pos)
        while (p < tokens.size && tokens[p] in listOf("+", "-")) {
            val op = tokens[p]
            val (right, p2) = parseMulDiv(tokens, p + 1)
            left = if (op == "+") left + right else left - right
            p = p2
        }
        return left to p
    }

    private fun parseMulDiv(tokens: List<String>, pos: Int): Pair<Double, Int> {
        var (left, p) = parsePow(tokens, pos)
        while (p < tokens.size && tokens[p] in listOf("*", "/", "%")) {
            val op = tokens[p]
            val (right, p2) = parsePow(tokens, p + 1)
            left = when (op) {
                "*" -> left * right
                "/" -> if (right != 0.0) left / right else throw IllegalArgumentException("Division by zero")
                "%" -> left % right
                else -> left
            }
            p = p2
        }
        return left to p
    }

    private fun parsePow(tokens: List<String>, pos: Int): Pair<Double, Int> {
        var (left, p) = parsePrimary(tokens, pos)
        while (p < tokens.size && tokens[p] == "^") {
            val (right, p2) = parsePrimary(tokens, p + 1)
            left = left.pow(right)
            p = p2
        }
        return left to p
    }

    private fun parsePrimary(tokens: List<String>, pos: Int): Pair<Double, Int> {
        if (pos >= tokens.size) throw IllegalArgumentException("Unexpected end of expression")

        val token = tokens[pos]
        return when {
            token == "(" -> {
                val (value, p) = parseAddSub(tokens, pos + 1)
                if (p >= tokens.size || tokens[p] != ")") {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                value to (p + 1)
            }
            token.toDoubleOrNull() != null -> token.toDouble() to (pos + 1)
            token in listOf("+", "-") -> {
                val (value, p) = parsePrimary(tokens, pos + 1)
                (if (token == "-") -value else value) to p
            }
            else -> throw IllegalArgumentException("Invalid token: $token")
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            when {
                expr[i].isWhitespace() -> i++
                expr[i] in "+-*/%^()" -> {
                    tokens.add(expr[i].toString())
                    i++
                }
                expr[i].isDigit() || expr[i] == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                else -> throw IllegalArgumentException("Invalid character: ${expr[i]}")
            }
        }
        return tokens
    }

    /**
     * Returns current time (< 3s).
     * Format: "HH:mm:ss"
     */
    fun getClock(): String {
        val now = System.currentTimeMillis()
        val secs = (now / 1000) % 60
        val mins = (now / 60000) % 60
        val hours = (now / 3600000) % 24
        return "%02d:%02d:%02d".format(hours, mins, secs)
    }

    /**
     * Fractions lesson (< 3s).
     * Simplifies a fraction to lowest terms.
     */
    fun lessonFractions(numerator: Int, denominator: Int): Pair<Int, Int> {
        if (denominator == 0) throw IllegalArgumentException("Denominator cannot be zero")
        val gcd = gcd(kotlin.math.abs(numerator), kotlin.math.abs(denominator))
        return (numerator / gcd) to (denominator / gcd)
    }

    /**
     * Algebra lesson (< 3s).
     * Solves linear equation: ax + b = 0 returns x = -b/a
     */
    fun lessonAlgebra(a: Double, b: Double): Double? {
        return if (a != 0.0) -b / a else null
    }

    /**
     * Percentages lesson (< 3s).
     * Returns percentage of a value.
     */
    fun lessonPercentages(value: Double, percent: Double): Double {
        return (value * percent) / 100.0
    }

    /**
     * Geometry lesson (< 3s).
     * Calculates area of common shapes.
     */
    object Geometry {
        fun circleArea(radius: Double): Double {
            return kotlin.math.PI * radius * radius
        }

        fun rectangleArea(width: Double, height: Double): Double {
            return width * height
        }

        fun triangleArea(base: Double, height: Double): Double {
            return (base * height) / 2.0
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }
}
