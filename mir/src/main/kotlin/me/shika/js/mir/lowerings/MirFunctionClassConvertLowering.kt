package me.shika.js.mir.lowerings

import me.shika.js.mir.debug.dump
import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirClassSymbol
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirElementWithParent
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirFunctionSymbol
import me.shika.js.mir.elements.MirGetValue
import me.shika.js.mir.elements.MirNewInstance
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirTransformer
import me.shika.js.mir.elements.MirVariable
import me.shika.js.mir.elements.MirVariableSymbol

class MirFunctionClassConvertLowering : MirTransformer<Nothing?> {
    // todo convert to scope
    private val addedClasses = mutableListOf<MirClass>()
    private val symbolTable = mutableMapOf<MirSymbol<*>, MirSymbol<*>>()

    override fun visitMirFile(file: MirFile, data: Nothing?): MirElement {
        val updatedFile = super.visitMirFile(file, data)

        addedClasses.forEach { cls ->
            when (val parent = cls.parent) {
                is MirFile -> parent.statements += cls
                is MirClass -> parent.statements += cls
            }
        }

        addedClasses.clear()
        symbolTable.clear()

        return updatedFile
    }

    override fun visitMirFunction(function: MirFunction, data: Nothing?): MirElement {
        super.visitMirFunction(function, data)

        val classParent = function.closestClassContainer()
        require(classParent != null) {
            "Couldn't find a container for class based on ${function.dump()}"
        }

        val functionClass = MirClass(
            symbol = MirClassSymbol(),
            name = function.name,
            statements = listOf(
                MirFunction(
                    symbol = MirFunctionSymbol(),
                    name = "invoke",
                    parameters = function.parameters,
                    body = function.body,
                    isNative = true,
                    source = function.source
                )
            )
        )
        functionClass.parent = classParent

        val variableDeclaration = MirVariable(
            symbol = MirVariableSymbol(),
            name = function.name,
            value = MirNewInstance(
                functionClass.symbol,
                emptyList()
            ),
        )

        symbolTable[function.symbol] = variableDeclaration.symbol
        addedClasses += functionClass

        return variableDeclaration
    }

    override fun visitMirGetValue(getValue: MirGetValue, data: Nothing?): MirElement =
        if (getValue.symbol in symbolTable) {
            MirGetValue(
                symbol = symbolTable[getValue.symbol]!!,
                source = getValue.source
            )
        } else {
            getValue
        }

    private fun MirElementWithParent.closestClassContainer(): MirElement? {
        var element = parent as? MirElementWithParent
        while (element != null && !element.isClassContainer()) {
            element = element.parent as? MirElementWithParent
        }
        return element
    }

    private fun MirElement.isClassContainer() =
        this is MirClass || this is MirFile
}
