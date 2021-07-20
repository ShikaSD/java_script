package js

fun test() {
    JsObject().add("test", 0)
}

class JsObject {
    private val storage = mutableMapOf<String, Any?>()

    fun add(key: String, value: Any?) {
        storage[key] = value
    }

    fun get(key: String): Any? =
        storage[key]

    override fun toString(): String =
        storage.toString()
}
