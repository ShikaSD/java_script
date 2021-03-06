package js

open class JsObject {
    private val storage = mutableMapOf<String, Any?>()

    fun put(key: String, value: Any?) {
        storage[key] = value
    }

    fun get(key: String): Any? =
        storage[key]

    override fun toString(): String =
        storage.toString()
}
