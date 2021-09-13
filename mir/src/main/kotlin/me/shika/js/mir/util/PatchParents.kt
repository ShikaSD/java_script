package me.shika.js.mir.util

import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirElementWithParent
import me.shika.js.mir.elements.MirVisitor

fun <T : MirElement> T.patchParents(parent: MirElement? = null): T {
    accept(PatchMirParents, parent)
    return this
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private object PatchMirParents : MirVisitor<MirElement?, Unit> {
    override fun visitMirElement(element: MirElement, parent: MirElement?) {
        if (element is MirElementWithParent) {
            element.parent = parent
            element.acceptChildren(this, element)
        } else {
            element.acceptChildren(this, parent)
        }
    }
}
