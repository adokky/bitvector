package dev.dokky.bitvector

import kotlin.collections.iterator
import kotlin.experimental.and

fun <T> List<T>.filterWith(mask: MutableBitVector, maskOffset: Int = 0): List<T> =
    buildList(minOf(mask.cardinality(), size)) {
        mask.forEachBitBreakable { bitIndex ->
            if (bitIndex >= this@filterWith.size) true else {
                add(this@filterWith[maskOffset + bitIndex])
                false
            }
        }
    }

fun <T> List<T>.filterWithNot(mask: MutableBitVector, maskOffset: Int = 0): List<T> =
    buildList(minOf(mask.cardinality(), size)) {
        mask.forEachZeroBit { bitIndex ->
            add(this@filterWithNot[maskOffset + bitIndex])
        }
    }

inline fun <T, R> List<T>.filterAndMap(mask: MutableBitVector, mapper: (T) -> R): List<R> =
    buildList(minOf(mask.cardinality(), size)) {
        for (bitIndex in mask.iterator()) {
            if (bitIndex >= this@filterAndMap.size) break
            add(mapper(this@filterAndMap[bitIndex]))
        }
    }

inline fun iterateBits(
    mask: ByteArray,
    body: (fieldIndex: Int) -> Unit
) {
    for (wordIndex in mask.indices) {
        var word = mask[wordIndex]
        var index: Int
        while (true) {
            index = word.countTrailingZeroBits()
            if (index >= 8) break
            word = word and (0xff shl (index + 1)).toByte()
            body((wordIndex shl 3) + index)
        }
    }
}