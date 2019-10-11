package com.lundekhan.htmltemplates

import io.ktor.application.ApplicationCall
import io.ktor.html.respondHtml
import kotlinx.html.*

suspend fun ApplicationCall.respondHtmlDefault(title: String, index: Int, content: BODY.() -> Unit) =
    this.respondHtml {
        head {
            title("Londogard: $title")
            link(href = "http://fonts.googleapis.com/css?family=Roboto", rel = "stylesheet")
            getStyle()
        }
        body {
            bodyHeader(index)
            content()
        }
    }

fun BODY.bodyHeader(selectedIdx: Int) {
    h3 {
        listOf("londogard", "url", "smry", "textgen", "billsplit", "apps", "github").forEachIndexed { index, tab ->
            val href = when (tab) {
                "londogard" -> "/"
                "url" -> "/url-short"
                else -> "/$tab"
            }
            a(href) { +(if (index == selectedIdx) ">$tab." else "$tab.") }
            + " "
        }
    }
    hr { }
}