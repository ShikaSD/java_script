package me.shika.js.mir.lowerings

import me.shika.js.mir.elements.MirBody
import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirFunctionSymbol
import me.shika.js.mir.elements.MirTransformer

class MirClinitLowering : MirTransformer<Nothing?> {
    override fun visitMirClass(cls: MirClass, data: Nothing?): MirElement {
        val statements = cls.statements

        // fixme maybe vars should be allowed on the top level, but it is too complicated atm
        val expressions = statements.filter { it !is MirFunction && it !is MirClass }
        if (expressions.isNotEmpty()) {
            cls.statements -= expressions

            val clinit = MirFunction(
                symbol = MirFunctionSymbol(),
                name = "<clinit>",
                parameters = emptyList(),
                body = MirBody(expressions),
                isNative = true,
                isStatic = true
            )
            cls.statements += clinit
        }

        return super.visitMirClass(cls, data)
    }
}
