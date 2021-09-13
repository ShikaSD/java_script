package me.shika.js.mir.elements

import me.shika.js.ConstValue
import me.shika.js.SourceOffset

interface MirElement {
    val source: SourceOffset

    fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context): Result
    fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context)
}

interface MirSymbolOwner<Owner : MirElement> {
    val symbol: MirSymbol<Owner>
}

interface MirElementWithParent : MirElement {
    var parent: MirElement?
}

class MirFile(val fileName: String, var statements: List<MirElement>, override val source: SourceOffset) : MirElementWithParent {
    override var parent: MirElement? = null

    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirFile(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class MirFunction(
    override val symbol: MirFunctionSymbol,
    val name: String,
    var parameters: List<MirParameter>,
    var body: MirBody,
    var isNative: Boolean = false,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirElementWithParent, MirSymbolOwner<MirFunction> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirFunction(this, data)
    
    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
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

    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirParameter(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        // no-op
    }
}

class MirBody(var statements: List<MirElement>, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirElement {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirBody(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class MirVariable(
    override val symbol: MirVariableSymbol,
    val name: String,
    var value: MirExpression?,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirElementWithParent, MirSymbolOwner<MirVariable> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirVariable(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        value?.accept(visitor, data)
    }
}

class MirClass(
    override val symbol: MirClassSymbol,
    val name: String,
    var statements: List<MirElement>,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirElementWithParent, MirSymbolOwner<MirClass> {
    init {
        symbol.bind(this)
    }

    override var parent: MirElement? = null

    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirClass(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        statements.forEach { it.accept(visitor, data) }
    }
}

/**
 * Expressions
 */
interface MirExpression : MirElement

class MirConst(val value: ConstValue, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirConst(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        // no-op
    }
}

class MirGetValue(val symbol: MirSymbol<*>, override val source: SourceOffset = SourceOffset.NO_SOURCE) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirGetValue(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        // no-op
    }
}

class MirSetValue(
    val symbol: MirSymbol<*>,
    var value: MirExpression,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirSetValue(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        value.accept(visitor, data)
    }
}

class MirGetProperty(val receiver: MirExpression, val name: String, override val source: SourceOffset) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirGetProperty(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        receiver.accept(visitor, data)
    }
}

class MirSetProperty(
    var receiver: MirExpression,
    val name: String,
    var value: MirExpression,
    override val source: SourceOffset
) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirSetProperty(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        receiver.accept(visitor, data)
        value.accept(visitor, data)
    }
}

class MirObjectExpression(
    var entries: Map<String, MirExpression>,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirObjectExpression(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        entries.values.forEach {
            it.accept(visitor, data)
        }
    }
}

class MirCall(
    var receiver: MirExpression,
    var arguments: List<MirExpression?>,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirCall(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        receiver.accept(visitor, data)
        arguments.forEach { it?.accept(visitor, data) }
    }
}

class MirNewInstance(
    val classSymbol: MirClassSymbol,
    var arguments: List<MirExpression?>,
    override val source: SourceOffset = SourceOffset.NO_SOURCE
) : MirExpression {
    override fun <Context, Result> accept(visitor: MirVisitor<Context, Result>, data: Context) =
        visitor.visitMirNewInstance(this, data)

    override fun <Context, Result> acceptChildren(visitor: MirVisitor<Context, Result>, data: Context) {
        arguments.forEach { it?.accept(visitor, data) }
    }
}
