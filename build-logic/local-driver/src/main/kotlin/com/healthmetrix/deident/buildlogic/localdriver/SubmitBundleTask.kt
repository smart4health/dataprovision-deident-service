package com.healthmetrix.deident.buildlogic.localdriver

import groovy.json.JsonOutput
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.http4k.base64Encode
import org.http4k.client.ApacheClient
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.io.File
import java.time.ZonedDateTime
import java.util.UUID

const val APPLICATION_FHIR_JSON_VALUE = "application/fhir+json"

open class SubmitBundleTask : DefaultTask() {

    @Input
    @set:Option(option = "base-url", description = "base url for deident service")
    var baseUrl: String = "http://localhost:8080"

    @Input
    @Optional
    @set:Option(option = "user-id", description = "userId to use")
    var userId: String = randomId()

    @Input
    @set:Option(option = "user-secret", description = "Secret to use, will be base64 encoded")
    var userSecret: String = randomId()

    @Input
    @set:Option(option = "d4l-id", description = "(UU)ID of the D4L account, which is used for contextualization when Patient resource is absent")
    var d4lId: String = UUID.randomUUID().toString()

    @Input
    @Optional
    @set:Option(option = "file", description = "File to send through the deident pipeline")
    var bodyFile: String? = null

    @Input
    @set:Option(option = "no-browse", description = "Add this flag to not open the html in the default browser")
    var noBrowse: Boolean = false

    @Input
    @set:Option(option = "submit-only", description = "Do not start the local RP server")
    var submitOnly: Boolean = false

    @Input
    @Optional
    @set:Option(option = "basic-auth", description = "basic auth credentials to use, in user:pass form")
    var basicAuth: String? = null

    @TaskAction
    fun taskAction() {
        val auth = basicAuth?.let {
            val (user, pass) = it.split(":")
            ClientFilters.BasicAuth(user, pass)
        } ?: Filter.NoOp

        val deidentClient = auth.then(ApacheClient())

        val url = bodyFile?.let {
            project.file(it).let { f -> if (f.exists() && f.isFile) f.toURI().toURL() else null }
                ?: error("Could not load $it from directory")
        } ?: ResourceLoader.Classpath(muteWarning = true).load("submit-bundle/anna-smith.bundle.json")
            ?: error("Could not load anna-smith.bundle.json")

        val body = url.readText()

        println("    User id is $userId")
        println("User secret is ${userSecret.base64Encode()}")

        val req = Request(Method.POST, "${baseUrl.trimEnd('/')}/v1/downloader/$userId")
            .header("Content-Type", APPLICATION_FHIR_JSON_VALUE)
            .header("X-Deident-User-Secret", userSecret.base64Encode())
            .header("X-Deident-Fetched-At", ZonedDateTime.now().toString())
            .header("X-Deident-D4l-Id", d4lId.base64Encode())
            .body(body)

        var output: String? = null

        val app = routes(
            "/rp" bind Method.POST to {
                output = it.bodyString()
                Response(Status.OK)
            },
        )

        if (submitOnly) {
            val response = req.let(deidentClient::invoke)

            "${response.version} ${response.status}"
                .let(::println)
            response.headers
                .joinToString("\n") { "${it.first}: ${it.second}" }
                .let(::println)
            response.bodyString()
                .toPrettyJson()
                .let(::println)

            return
        }

        val server = app.asServer(Netty(4040)).start()
        println("Server started")

        try {
            val res = deidentClient(req)
            println("Request sent")

            if (!res.status.successful) {
                server.stop()
                logger.error(res.toString())
                throw GradleException("Submission of bundle failed")
            }

            println(res.bodyString().toPrettyJson())

            val seconds = 20L
            val sleepFactor = 4L
            var counter = seconds * sleepFactor
            while (output == null && counter > 0) {
                if (counter.rem(sleepFactor) == 0L) {
                    println("waiting for ${counter / sleepFactor} seconds")
                }

                Thread.sleep(1000 / sleepFactor)
                counter -= 1
            }
        } finally {
            server.stop()
            println("Server stopped")
        }

        val outputFile = File(project.buildDir, "output.html").also {
            it.writeText(
                template(
                    body.toPrettyJson(),
                    (output ?: "NOTHING").toPrettyJson(),
                ),
            )
        }

        println("HTML written to ${outputFile.absolutePath}")

        if (!noBrowse) {
            java.awt.Desktop.getDesktop()
                ?.browse(outputFile.toURI())
                ?: logger.error("Failed to getDesktop")
        }
    }

    private fun randomId(): String =
        (1 until 10).map { "abcdefghijklmnopqrstuvwxyz".random() }.joinToString(separator = "")

    private fun String.toPrettyJson(): String = try {
        JsonOutput.prettyPrint(this)
    } catch (ex: Exception) {
        this
    }

    private fun template(left: String, right: String): String = buildString {
        appendHTML().html {
            body {
                div {
                    style = "display: flex"

                    val cardStyle = "background-color: #eee; padding: 8px; margin: 8px; border-radius: 8px"

                    div {
                        style = "flex: 1;"

                        p {
                            style = "margin-left: auto; margin-right: auto; text-align: center;"
                            +"Before"
                        }

                        pre {
                            style = cardStyle
                            +left
                        }
                    }

                    div {
                        style = "flex: 1;"

                        p {
                            style = "margin-left: auto; margin-right: auto; text-align: center;"
                            +"After"
                        }
                        pre {
                            style = cardStyle
                            +right
                        }
                    }
                }
            }
        }
    }
}
