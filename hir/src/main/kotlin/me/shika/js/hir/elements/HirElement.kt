package me.shika.js.hir.elements

import me.shika.js.hir.elements.HirSource.Companion.NoSource

interface HirElement {
    val source: HirSource

    fun <Context> accept(visitor: HirVisitor<Context>, data: Context)
    fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context)
}

class HirSource(val startOffset: Int, val endOffset: Int) {
    companion object {
        val NoSource: HirSource = HirSource(-1, -1)
    }
}

class HirFile(val fileName: String, val statements: List<HirElement>, override val source: HirSource) : HirElement {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirFile(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class HirFunction(
    val name: String,
    val parameters: List<HirParameter>,
    val body: HirBody,
    override val source: HirSource = NoSource
) : HirElement {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirFunction(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        parameters.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }
}

class HirParameter(val name: String, override val source: HirSource = NoSource) : HirExpression {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirParameter(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        // no-op
    }
}

class HirBody(val statements: List<HirElement>, override val source: HirSource = NoSource) : HirElement {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirBody(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class HirVariable(val name: String, val value: HirExpression?, override val source: HirSource) : HirElement {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirVariable(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        value?.accept(visitor, data)
    }
}

/**
 * Expressions
 */
interface HirExpression : HirElement

class HirConst(val value: Value, override val source: HirSource = NoSource) : HirExpression {
    sealed class Value {
        data class Number(val value: Double) : Value()
        data class Str(val value: String) : Value()
        data class Bool(val value: Boolean) : Value()
    }

    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirConst(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        // no-op
    }
}

class HirReference(val name: String, override val source: HirSource = NoSource) : HirExpression {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirReference(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        // no-op
    }
}

class HirCall(
    val name: String,
    val arguments: List<HirExpression?>,
    override val source: HirSource = NoSource
) : HirExpression {
    override fun <Context> accept(visitor: HirVisitor<Context>, data: Context) {
        visitor.visitHirCall(this, data)
    }

    override fun <Context> acceptChildren(visitor: HirVisitor<Context>, data: Context) {
        arguments.forEach { it?.accept(visitor, data) }
    }
}
