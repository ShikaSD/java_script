package me.shika.js

data class SourceOffset(val startOffset: Int, val endOffset: Int) {
    companion object {
        const val NO_OFFSET = -1
        val NO_SOURCE: SourceOffset = SourceOffset(NO_OFFSET, NO_OFFSET)
    }
}

sealed class ConstValue {
    data class Number(val value: Double) : ConstValue()
    data class Str(val value: String) : ConstValue()
    data class Bool(val value: Boolean) : ConstValue()
}
