package dev.dokky.bitvector

internal expect fun IntArray.startsWith(other: IntArray): Boolean

internal fun IntArray.startsWithCommon(other: IntArray): Boolean {
    for (i in other.indices) {
        if (this[i] != other[i]) return false
    }
    return true
}