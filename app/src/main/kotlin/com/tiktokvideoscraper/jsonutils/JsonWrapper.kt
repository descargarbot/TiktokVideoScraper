package com.tiktokvideoscraper.jsonutils

import com.google.gson.JsonParser
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonArray

class JsonWrapper(val element: JsonElement) : Iterable<JsonWrapper>, Map<String, JsonWrapper> {
    override fun iterator(): Iterator<JsonWrapper> {
        return when {
            element.isJsonArray -> element.asJsonArray.map { JsonWrapper(it) }.iterator()
            element.isJsonObject -> element.asJsonObject.entrySet().map { JsonWrapper(it.value) }.iterator()
            else -> listOf(this).iterator()
        }
    }

    override val entries: Set<Map.Entry<String, JsonWrapper>>
        get() = when {
            element.isJsonObject -> element.asJsonObject.entrySet().map { entry ->
                object : Map.Entry<String, JsonWrapper> {
                    override val key: String = entry.key
                    override val value: JsonWrapper = JsonWrapper(entry.value)
                }
            }.toSet()
            else -> emptySet()
        }

    override val keys: Set<String>
        get() = when {
            element.isJsonObject -> element.asJsonObject.keySet()
            else -> emptySet()
        }

    override val size: Int
        get() = when {
            element.isJsonObject -> element.asJsonObject.size()
            element.isJsonArray -> element.asJsonArray.size()
            else -> 1
        }

    override val values: Collection<JsonWrapper>
        get() = when {
            element.isJsonObject -> element.asJsonObject.entrySet().map { JsonWrapper(it.value) }
            else -> emptyList()
        }

    override fun containsKey(key: String): Boolean =
        element.isJsonObject && element.asJsonObject.has(key)

    override fun containsValue(value: JsonWrapper): Boolean =
        element.isJsonObject && element.asJsonObject.entrySet().any { it.value == value.element }

    override fun get(key: String): JsonWrapper {
        return when {
            element.isJsonObject -> element.asJsonObject[key]?.let { JsonWrapper(it) }
                ?: throw NoSuchElementException("Key not found: $key")
            else -> throw IllegalArgumentException("Not a JSON object")
        }
    }

    override fun isEmpty(): Boolean = size == 0

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
