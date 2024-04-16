package kpring.test.restdoc.dsl

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.ResultActionsDsl

fun WebTestClient.BodyContentSpec.restDoc(
    identifier: String,
    description: String,
    config: RestDocBuilder.() -> Unit,
): WebTestClient.BodyContentSpec {
    val builder = RestDocBuilder()
    builder.config()

    return this.consumeWith(
        WebTestClientRestDocumentationWrapper.document(
            identifier = identifier,
            description = description,
            snippets = builder.snippets.toTypedArray()
        )
    )
}

fun ResultActionsDsl.restDoc(
    identifier: String,
    description: String,
    config: RestDocBuilder.() -> Unit,
) {
    val builder = RestDocBuilder()
    builder.config()

    this.andDo {
        handle(
            MockMvcRestDocumentationWrapper.document(
                identifier = identifier,
                description = description,
                snippets = builder.snippets.toTypedArray()
            )
        )
    }
}

class RestDocBuilder {

    val snippets = mutableListOf<Snippet>()
    fun request(config: RestDocRequestBuilder.() -> Unit) {
        val builder = RestDocRequestBuilder()
        builder.config()
        if (builder.headerSnippet != null) snippets.add(builder.headerSnippet!!)
        if (builder.bodySnippet != null) snippets.add(builder.bodySnippet!!)
        if (builder.querySnippet != null) snippets.add(builder.querySnippet!!)
    }

    fun response(config: RestDocResponseBuilder.() -> Unit) {
        val builder = RestDocResponseBuilder()
        builder.config()
        if (builder.headerSnippet != null) snippets.add(builder.headerSnippet!!)
        if (builder.bodySnippet != null) snippets.add(builder.bodySnippet!!)
    }
}