package me.shika.js.mir.elements

import me.shika.js.ConstValue
import me.shika.js.SourceOffset

interface MirElement {
    val source: SourceOffset

    fun <Context> accept(visitor: MirVisitor<Context>, data: Context)
    fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context)
}

interface MirSymbolOwner<Owner : MirElement> {
    val symbol: MirSymbol<Owner>
}

interface MirElementWithParent : MirElement {
    var parent: MirElement?
}

class MirFile(val fileName: String, val statements: List<MirElement>, override val source: SourceOffset) : MirElement {
    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirFile(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class MirFunction(
    override val symbol: MirFunctionSymbol,
    val name: String,
    val parameters: List<MirParameter>,
    val body: MirBody,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirElementWithParent, MirSymbolOwner<MirFunction> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirFunction(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        parameters.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }
}

class MirParameter(
    override val symbol: MirParameterSymbol,
    val name: String,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirElementWithParent, MirSymbolOwner<MirParameter> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirParameter(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        // no-op
    }
}

class MirBody(val statements: List<MirElement>, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirElement {
    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirBody(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class MirVariable(
    override val symbol: MirVariableSymbol,
    val name: String,
    val value: MirExpression?,
    override val source: SourceOffset
) : MirElementWithParent, MirSymbolOwner<MirVariable> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirVariable(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        value?.accept(visitor, data)
    }
}

/**
 * Expressions
 */
interface MirExpression : MirElement

class MirConst(val value: ConstValue, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirExpression {
    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirConst(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        // no-op
    }
}

class MirReference(val symbol: MirSymbol<*>, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirExpression {
    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirReference(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        // no-op
    }
}

class MirCall(
    val symbol: MirFunctionSymbol,
    val arguments: List<MirExpression?>,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirExpression {
    override fun <Context> accept(visitor: MirVisitor<Context>, data: Context) {
        visitor.visitMirCall(this, data)
    }

    override fun <Context> acceptChildren(visitor: MirVisitor<Context>, data: Context) {
        arguments.forEach { it?.accept(visitor, data) }
    }
}
