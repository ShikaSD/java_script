package me.shika.js

data class SourceOffset(val startOffset: Int, val endOffset: Int) {
    companion object {
        const val NO_OFFSET = -1
        val NO_SOURCE: SourceOffset = SourceOffset(NO_OFFSET, NO_OFFSET)
    }
}

sealed class ConstValue {
    abstract val value: Any

    data class Number(override val value: Double) : ConstValue()
    data class Str(override val value: String) : ConstValue()
    data class Bool(override val value: Boolean) : ConstValue()
}
