package me.shika.js.mir.lowerings

import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirClassSymbol
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirVisitor

class MirFileClassWrapLowering : MirVisitor<Nothing?, Unit> {
    override fun visitMirElement(element: MirElement, data: Nothing?) {
        // no-op, we only need top-level
    }

    override fun visitMirFile(file: MirFile, data: Nothing?) {
        if (file.statements.size > 1) {
            file.statements = listOf(file.wrapWithClass())
        }
    }

    private fun MirFile.wrapWithClass(): MirClass =
        MirClass(
            MirClassSymbol(),
            className(),
            statements,
            source
        ).also {
            it.parent = this
        }

    private fun MirFile.className(): String =
        fileName.replace('.', '_')
}
