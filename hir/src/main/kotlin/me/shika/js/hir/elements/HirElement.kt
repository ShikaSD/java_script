package me.shika.js.hir.elements

import me.shika.js.ConstValue
import me.shika.js.SourceOffset
import me.shika.js.SourceOffset.Companion.NO_SOURCE

interface HirElement {
    val source: SourceOffset

    fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context): Data
    fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context)
}

class HirFile(val fileName: String, val statements: List<HirElement>, override val source: SourceOffset) : HirElement {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirFile(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class HirFunction(
    val name: String,
    val parameters: List<HirParameter>,
    val body: HirBody,
    override val source: SourceOffset = NO_SOURCE
) : HirElement {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirFunction(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        parameters.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }

    companion object {
        fun builtin(name: String): HirFunction =
            HirFunction(
                name = name,
                parameters = emptyList(),
                body = HirBody(emptyList())
            )
    }
}

class HirParameter(val name: String, override val source: SourceOffset = NO_SOURCE) : HirElement {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirParameter(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        // no-op
    }
}

class HirBody(val statements: List<HirElement>, override val source: SourceOffset = NO_SOURCE) : HirElement {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirBody(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class HirVariable(val name: String, val value: HirExpression?, override val source: SourceOffset) : HirElement {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirVariable(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        value?.accept(visitor, data)
    }
}

/**
 * Expressions
 */
interface HirExpression : HirElement

class HirConst(val value: ConstValue, override val source: SourceOffset = NO_SOURCE) : HirExpression {


    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirConst(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        // no-op
    }
}

class HirReference(val name: String, override val source: SourceOffset = NO_SOURCE) : HirExpression {
    var candidate: HirElement? = null

    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirReference(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        // no-op
    }
}

class HirCall(
    val name: String,
    val arguments: List<HirExpression?>,
    override val source: SourceOffset = NO_SOURCE
) : HirExpression {
    var candidate: HirElement? = null

    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirCall(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        arguments.forEach { it?.accept(visitor, data) }
    }
}
