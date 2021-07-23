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

val HirFile.functions get() = statements.asSequence().filterIsInstance<HirFunction>()

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
        fun builtin(name: String, parameters: List<HirParameter> = emptyList()): HirFunction =
            HirFunction(
                name = name,
                parameters = parameters,
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

class HirGetValue(val name: String, override val source: SourceOffset = NO_SOURCE) : HirExpression {
    var candidate: HirElement? = null

    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirGetValue(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        // no-op
    }
}

class HirObjectExpression(val entries: Map<String, HirExpression>, override val source: SourceOffset) : HirExpression {
    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirObjectExpression(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        entries.values.forEach {
            it.accept(visitor, data)
        }
    }
}

class HirSetValue(
    val name: String,
    val argument: HirExpression,
    override val source: SourceOffset = NO_SOURCE
) : HirExpression {
    var candidate: HirElement? = null

    override fun <Context, Data> accept(visitor: HirVisitor<Context, Data>, data: Context) =
        visitor.visitHirSetValue(this, data)

    override fun <Context, Data> acceptChildren(visitor: HirVisitor<Context, Data>, data: Context) {
        argument.accept(visitor, data)
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
