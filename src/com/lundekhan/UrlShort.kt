package com.lundekhan

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.Hash
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.*
import org.koin.ktor.ext.inject
import java.nio.charset.StandardCharsets

@InternalAPI
fun Route.UrlShort(redirections: MutableMap<String, String>) { // , db: Database
    val db by inject<Database>()

    get("/url/{short}") {
        val shortened = call.parameters["short"] ?: ""
        val fullUrl = redirections[shortened]
        application.log.info("$redirections .. $fullUrl .. $shortened")
        call.respondRedirect(fullUrl ?: "/url-short")
    }

    get("/url-short") {
        call.respondHtml {
            head { title("Londogard: Url-Short") }
            body {
                h1 { + "Url-Short" }
                p { + "Add url to shorten" }
                form(encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                    input {
                        type = InputType.url
                        name = "url"
                    }
                    br
                    input {
                        type = InputType.submit
                        value = "submit"
                    }
                }
            }
        }
    }
    // TODO add database integration for true save
    post("/url-short") {
        val url = call.receiveParameters().get("url") ?: return@post call.respond(HttpStatusCode.BadRequest, "URL must be supplied.")
        val hash = url.hashHexify()
        redirections.putIfAbsent(hash, url)
        launch(Dispatchers.IO) { db.urlQueries.select(url).executeAsOneOrNull() ?: db.urlQueries.insert(url, hash) }

        call.respondHtml {
            head { title("Londogard: Url-Short") }
            body {
                h1 { + "Url-Short" }
                p { + "Shortend URL:" }
                input {
                    type = InputType.url
                    name = "shortened-url"
                    value = "londogard.hopto.org/url/$hash"
                }
            }
        }
    }
}