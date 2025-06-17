package dev.dokky.bitvector

import java.util.Arrays

internal actual fun IntArray.startsWith(other: IntArray): Boolean {
    assert(other.size <= size)
    return Arrays.equals(
      //array  fromIndex  toIndex (exclusive)
        this,  0,         other.size,
        other, 0,         other.size
    )
}