package dev.dokky.bitvector

import dev.adokky.testEquality
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class BitVectorTest {
    @Test
    fun get_single_bit() {
        listOf(
            intArrayOf(0),
            intArrayOf(0, 2, 5, 31),
            intArrayOf(300)
        ).forEach { nums ->
            val bits = bitsOf(*nums)
            for (i in 0..(nums.max() + 70)) {
                assertEquals(i in nums, bits[i])
            }
        }
    }

    @Test
    fun bitwise_operations() {
        val a = bitsOf(0, 1, 2, 3, 120,                130)
        val b = bitsOf(0, 1, 2,    120, 121, 122, 123, 130)

        a.and(b) assertEquals bitsOf(0, 1, 2, 120, 130)

        a.or(b) assertEquals bitsOf(0, 1, 2, 3, 120, 121, 122, 123, 130)

        a.xor(b) assertEquals bitsOf(3, 121, 122, 123)
    }

    @Test
    fun test_equality_simple() {
        bitsOf(
            1, 2, 4, 1230, 1323, 1324
        ) assertEquals bitsOf(
            1, 2, 4, 1230, 1323, 1324
        )

        val bv = mutableBitsOf(0, 1, 2, 120, 420)
        bv[120] = false

        bitsOf(0, 1, 2, 420) assertEquals bv
    }

    @Test
    fun test_equality_auto() {
        testEquality {
            requireNonIdentical = true
            checkToString = true

            group { bitsOf() }
            group { bitsOf(1, 2, 4, 1230) }
            group(listOf(bitsOf(1, 2, 4, 1230, 1323)))
            group(listOf(bitsOf(1, 2, 4, 1230, 1323, 1324)))
            group(listOf(bitsOf(1, 2, 4, 1230, 1323, 1420)))
            group(listOf(bitsOf(0, 1, 2, 120, 420)))
        }
    }

    @Test
    fun pushing_bits_to_function_faster_iterator_alternative() {
        val bv = bitsOf(1, 2, 56, 64, 128, 129, 130, 131, 420)
        val other = MutableBitVector()

        bv.forEachBit { other.set(it) }
        other assertEquals bv
    }

    @Test
    fun fundamental_bitwise_checks() {
        val bits = bitsOf(0, 1, 2, 3, 7, 8, 9)

        assertTrue((bitsOf(1, 8, 9) in bits))
        assertTrue(bits !in bitsOf(1, 8, 9))
        assertTrue(bits.intersects(bitsOf(3, 4, 5, 6, 7)))

        assertFalse(bitsOf(100) in bits)
        assertFalse(100 in bits)
    }

    @Test
    fun find_first_bit() {
        for (i1 in 0..100) {
            for (i2 in 0..2) {
                val bits = mutableBitsOf(*(intArrayOf(i1) + (i1 until (i1 + i2)).toList().toIntArray()))
                val actual = bits.first()
                if (actual != i1) fail("expected: $i1, actual: $actual. Bits: $bits")
            }
        }

        assertEquals(-1, bitsOf().first())
        assertEquals(0, bitsOf().first(bit = false))
        assertEquals(31, MutableBitVector.wrap(intArrayOf(0.inv() ushr 1)).first(bit = false))
        assertEquals(32, MutableBitVector.wrap(intArrayOf(0.inv())).first(bit = false))
    }

    @Test
    fun find_last_bit_random() {
        val MAX_SIZE = 150
        val ITERATIONS = 10_000

        repeat(ITERATIONS) {
            val bits = MutableBitVector()
            var expected = -1
            repeat(3) {
                val start = Random.nextInt(MAX_SIZE)
                val end = Random.nextInt(start..MAX_SIZE)
                for (i in start .. end) bits.set(i)
                if (end > expected) expected = end
            }
            val actual = bits.last()
            if (actual != expected) fail("expected: $expected, actual: $actual. Bits: $bits")
        }
    }

    @Test
    fun find_last_bit_simple() {
        assertEquals(-1, bitsOf().last())
        assertEquals(0, bitsOf(0).last())
        assertEquals(31, bitsOf(31).last())
        assertEquals(63, bitsOf(31, 63).last())
        assertEquals(29, bitsOf(0, 14, 29).last())

        assertEquals(-1, mutableBitsOf(3, 6, 31).apply { clear() }.last())
        assertEquals(2, mutableBitsOf(1, 2, 3).apply { unset(3) }.last())
        assertEquals(31, mutableBitsOf(31, 63).apply { unset(63) }.last())
    }

    @Test
    fun find_first_bit_in_range() {
        for (i1 in 0..100) {
            for (i2 in 0..2) {
                val bits = bitsOf(*(intArrayOf(i1) + (i1 until (i1 + i2)).toList().toIntArray()))
                val actual = bits.first(start = i1)
                if (actual != i1) fail("expected: $i1, actual: $actual. Bits: $bits")
            }
        }

        val bits = mutableBitsOf(*(34..69).toList().toIntArray())
        bits[47] = false
        bits[60] = false
        bits[61] = false

        assertEquals(34, bits.first(start = 0))
        assertEquals(-1, bits.first(start = 1, endExclusive = 2))
        assertEquals(-1, bits.first(start = 33, endExclusive = 34))
        assertEquals(-1, bits.first(start = 34, endExclusive = 34))
        assertEquals(34, bits.first(start = 34, endExclusive = 35))
        assertEquals(46, bits.first(start = 46, endExclusive = 50))
        assertEquals(46, bits.first(start = 46))
        assertEquals(48, bits.first(start = 47))
        assertEquals(59, bits.first(start = 59))
        assertEquals(62, bits.first(start = 60))
        assertEquals(-1, bits.first(start = 60, endExclusive = 62))
        assertEquals(-1, bits.first(start = 100, endExclusive = 1000000))
        assertEquals(69, bits.first(start = 69, endExclusive = 1000000))
    }

    @Test
    fun find_first_zero_random() {
        val bits = BooleanArray(70)

        repeat(1_000) {
            bits.fill(false)
            val bv = MutableBitVector()

            repeat(1_000) {
                val idx = Random.nextInt(bits.size)
                if (Random.nextBoolean()) {
                    bits[idx] = true
                    bv[idx] = true
                } else {
                    bits[idx] = false
                    bv[idx] = false
                }

                assertEquals(
                    bits.indices.filter { bits[it] },
                    bv.toList()
                )

                repeat(10) {
                    val start = Random.nextInt(bits.size - 1)
                    val end = Random.nextInt(start, bits.size)

                    assertEquals(
                        expected = bits.sliceArray(start ..< end).indexOf(true)
                            .let { if (it < 0) -1 else (it + start) },
                        actual = bv.first(start, end)
                    )

                    assertEquals(
                        expected = bits.sliceArray(start ..< end).indexOf(false)
                            .let { if (it < 0) -1 else (it + start) },
                        actual = bv.firstZero(start, end)
                    )
                }
            }
        }
    }
}


infix fun <T> T.assertEquals(expected: T) = assertEquals(expected, this)