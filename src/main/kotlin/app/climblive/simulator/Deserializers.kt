package app.climblive.simulator

import app.climblive.simulator.dto.CompClassDto
import app.climblive.simulator.dto.ContenderDto
import app.climblive.simulator.dto.ProblemDto
import app.climblive.simulator.dto.TickDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable

object Deserializers {
    val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(KotlinModule())

    object ContenderDeserializer : ResponseDeserializable<ContenderDto> {
        override fun deserialize(content: String) =
            objectMapper.readValue<ContenderDto>(content)
    }

    object CompClassListDeserializer : ResponseDeserializable<List<CompClassDto>> {
        override fun deserialize(content: String) =
            objectMapper.readValue<List<CompClassDto>>(content)
    }

    object TickDeserializer : ResponseDeserializable<TickDto> {
        override fun deserialize(content: String) =
            objectMapper.readValue<TickDto>(content)
    }

    object TickListDeserializer : ResponseDeserializable<List<TickDto>> {
        override fun deserialize(content: String) =
            objectMapper.readValue<List<TickDto>>(content)
    }

    object ProblemListDeserializer : ResponseDeserializable<List<ProblemDto>> {
        override fun deserialize(content: String) =
            objectMapper.readValue<List<ProblemDto>>(content)
    }
}

