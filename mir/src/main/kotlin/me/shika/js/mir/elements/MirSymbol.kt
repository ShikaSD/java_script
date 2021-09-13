package me.shika.js.mir.elements

interface MirSymbol<Element : MirElement> {
    val owner: Element

    val isBound: Boolean
    fun bind(owner: Element)
}

abstract class MirBindableSymbol<Element : MirElement> : MirSymbol<Element> {
    private var _owner: Element? = null

    override val owner: Element
        get() = _owner ?: throw IllegalStateException("Owner is unbound")

    override val isBound: Boolean
        get() = _owner != null

    override fun bind(owner: Element) {
        _owner = owner
    }
}

class MirFunctionSymbol : MirBindableSymbol<MirFunction>()
class MirParameterSymbol : MirBindableSymbol<MirParameter>()
class MirVariableSymbol : MirBindableSymbol<MirVariable>()
class MirClassSymbol : MirBindableSymbol<MirClass>()
