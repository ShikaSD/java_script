package me.shika.js.hir.resolve

import me.shika.js.hir.builtins.BuiltIns
import me.shika.js.hir.elements.HirElement

val RootScope = Scope(mapOf(BuiltIns.Print.name to BuiltIns.Print))

class Scope(declarations: Map<String, HirElement> = emptyMap()) {
    private val declarations: MutableMap<String, HirElement> = declarations.toMutableMap()

    fun addElement(name: String, element: HirElement) {
        declarations[name] = element
    }

    fun subScope(): Scope = Scope(declarations)

    fun named(name: String): HirElement? =
        declarations[name]
}
