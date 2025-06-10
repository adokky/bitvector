package dev.dokky.bitvector

fun mutableBitsOf(vararg bits: Int) : MutableBitVector {
    return MutableBitVector().apply { bits.forEach{ set(it) } }
}

fun bitsOf(vararg bits: Int) : BitVector = mutableBitsOf(*bits)

internal fun Int.toWordIdx() = this ushr 5
internal fun Int.bitCapacity() = this shl 5