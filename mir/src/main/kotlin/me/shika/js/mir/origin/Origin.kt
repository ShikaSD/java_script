package me.shika.js.mir.origin

abstract class Origin(val debugName: String) {
    override fun toString(): String = debugName
}

object JsFunctionOrigin : Origin("JsFunction")
