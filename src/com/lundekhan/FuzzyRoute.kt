package com.lundekhan

import com.londogard.fuzzymatch.FuzzyMatcher
import com.lundekhan.gui.HtmlTemplates.Shell
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.html.*
import org.koin.ktor.ext.inject

fun Route.fuzzyRoute(lines: List<String>): Route = route("/fuzsearch") {
    val fuzzyMatcher by inject<FuzzyMatcher>()

    fun MAIN.fuzzyForm(text: String?): Unit = section {
        form(method = FormMethod.post) {
            header {
                style = "padding:0"
                h3 { +"fuzsearch." }
                p { +"Fuzzy search C-files." }
            }
            section {
                textInput(name="keyword") {
                    placeholder = "Search (try IDK out!)"
                    if (text != null) value = text

                }
                postButton { +"Submit" }
            }
        }
    }

    get {
        call.respondHtml {
            Shell { fuzzyForm(null) }
        }
    }

    post {
        val keyword = call.receiveParameters()["keyword"]
        val fuzzyMatches = fuzzyMatcher
            .fuzzyMatch(lines, keyword ?: "")
            .mapNotNull { it.text }
        call.respondHtml {
            Shell {
                fuzzyForm(keyword)
                section {
                    aside {
                        fuzzyMatches.forEach { p { +it } }
                    }
                }
            }
        }
    }

    route("/api") {
        post {
            val keyword = call.receive<SearchPost>().keyword
            val fuzzyMatches = fuzzyMatcher
                .fuzzyMatch(lines, keyword)
                .mapNotNull { it.text }

            call.respond(ResultResponseArray(fuzzyMatches))
        }
    }
}

data class SearchPost(val keyword: String)