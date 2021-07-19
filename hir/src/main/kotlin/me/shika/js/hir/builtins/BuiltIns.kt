package me.shika.js.hir.builtins

import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirParameter

object BuiltIns {
    val Print = HirFunction.builtin("print", parameters = listOf(HirParameter("value")))
}
