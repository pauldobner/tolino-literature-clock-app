package com.example.literatureclock

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class QuoteRepository(private val context: Context) {

    // Map "HH:MM" -> List of Quotes
    private val quotesEn = HashMap<String, MutableList<Quote>>()
    private val quotesDe = HashMap<String, MutableList<Quote>>()
    
    var isLoaded = false
        private set

    suspend fun loadQuotes() = withContext(Dispatchers.IO) {
        parseCsv("quotes/quotes.en-US.csv", quotesEn)
        parseCsv("quotes/quotes.de-DE.csv", quotesDe)
        isLoaded = true
    }

    private fun parseCsv(assetPath: String, destination: HashMap<String, MutableList<Quote>>) {
        try {
            val inputStream = context.assets.open(assetPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header
            reader.readLine()

            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val parts = line.split("|")
                    if (parts.size >= 6) {
                        val time = parts[0]
                        val quoteTimePhrase = parts[2]
                        val quoteText = parts[3]
                        val title = parts[4]
                        val author = parts[5]
                        val sfw = if (parts.size > 6) parts[6] else ""

                        val quote = Quote(time, quoteTimePhrase, quoteText, title, author, sfw)
                        
                        if (!destination.containsKey(time)) {
                            destination[time] = ArrayList()
                        }
                        destination[time]?.add(quote)
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getQuote(time: String, lang: String): Quote? {
        // lang: "en", "de", "both"
        val candidates = ArrayList<Quote>()
        
        if (lang == "en" || lang == "both") {
            quotesEn[time]?.let { candidates.addAll(it) }
        }
        if (lang == "de" || lang == "both") {
            quotesDe[time]?.let { candidates.addAll(it) }
        }

        if (candidates.isEmpty()) return null
        return candidates.random()
    }
}
