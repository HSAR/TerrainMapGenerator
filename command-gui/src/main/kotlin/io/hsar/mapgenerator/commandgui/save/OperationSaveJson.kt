package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal object OperationSaveJson {
    val objectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
}
