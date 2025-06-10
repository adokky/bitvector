package dev.dokky.bitvector

fun BitVector.firstZero(): Int = first(bit = false)

fun BitVector.firstZero(start: Int, endExclusive: Int = words.size shl WORD_INDEX_SHIFT): Int =
    first(start = start, endExclusive = endExclusive, bit = false)

fun BitVector.firstOrElse(default: Int): Int = first().let { if (it < 0) default else it }

fun BitVector.firstZeroOrElse(default: Int): Int = firstZero().let { if (it < 0) default else it }

fun BitVector.lastOrElse(default: Int): Int = last().let { if (it < 0) default else it }