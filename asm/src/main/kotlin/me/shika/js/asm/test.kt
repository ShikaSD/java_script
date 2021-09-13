package me.shika.js.asm

fun test(name: Any) {
    var something: JsFunction = object : JsObject(), JsFunction {
        override fun invoke() {
            TODO("Not yet implemented")
        }
    }

    something.invoke()
}

interface JsFunction {
    fun invoke()
}

open class JsObject
