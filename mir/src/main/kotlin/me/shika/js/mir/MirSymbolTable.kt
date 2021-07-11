package me.shika.js.mir

import me.shika.js.hir.debug.dump
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirVariable
import me.shika.js.mir.elements.MirFunctionSymbol
import me.shika.js.mir.elements.MirParameterSymbol
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirVariableSymbol

class MirSymbolTable {
    private val symbols = hashMapOf<HirElement, MirSymbol<*>>()

    fun declareFunctionSymbol(function: HirFunction): MirFunctionSymbol =
        symbols.getOrPut(function) { MirFunctionSymbol() } as MirFunctionSymbol

    fun declareParameterSymbol(parameter: HirParameter): MirParameterSymbol =
        symbols.getOrPut(parameter) { MirParameterSymbol() } as MirParameterSymbol

    fun declareVariableSymbol(variable: HirVariable): MirVariableSymbol =
        symbols.getOrPut(variable) { MirVariableSymbol() } as MirVariableSymbol

    fun referenceSymbol(element: HirElement): MirSymbol<*> =
        symbols.getOrElse(element) {
            throw IllegalStateException("Symbol not defined: ${element.dump()}")
        }
}
