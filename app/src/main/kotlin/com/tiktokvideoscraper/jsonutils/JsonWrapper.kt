package com.tiktokvideoscraper.jsonutils

import com.google.gson.JsonParser
import com.google.gson.JsonElement


class JsonWrapper(val element: JsonElement) {
    operator fun get(key: String): JsonWrapper {
        return when {
            element.isJsonObject -> JsonWrapper(element.asJsonObject[key] ?: throw NoSuchElementException("Key not found: $key"))
            else -> throw IllegalArgumentException("Not a JSON object")
        }
    }

    operator fun get(index: Int): JsonWrapper {
        return when {
            element.isJsonArray -> JsonWrapper(element.asJsonArray[index])
            else -> throw IllegalArgumentException("Not a JSON array")
        }
    }

    fun asString(): String = element.asString
    fun asInt(): Int = element.asInt
    fun asDouble(): Double = element.asDouble
    fun asBoolean(): Boolean = element.asBoolean

    override fun toString(): String = element.toString()
}

fun parseJson(jsonString: String): JsonWrapper {
    return JsonWrapper(JsonParser.parseString(jsonString))
}
