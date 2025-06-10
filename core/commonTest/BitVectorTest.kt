package dev.dokky.bitvector

import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class BitVectorTest {
    @Test
    fun test_equality() {
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
    fun get_and_set() {
        val bv = MutableBitVector()
        bv[0] = true
        bv[2] = true
        bv.unsafeSet(14)
        bv[49] = true

        assertTrue(bv[0])
        assertTrue(1 !in bv)
        assertTrue(2 in bv)
        assertTrue(14 in bv)
        assertTrue(bv.unsafeGet(49))
        assertFalse(bv[128])

        bv.cardinality() assertEquals 4
    }

    @Test
    fun pushing_bits_to_function_faster_iterator_alternative() {
        val bv = bitsOf(1, 2, 56, 64, 128, 129, 130, 131, 420)
        val other = MutableBitVector()

        bv.forEachBit { other.set(it) }
        other assertEquals bv
    }

    @Test
    fun fill_then_clear() {
        val bv = mutableBitsOf(23, 4, 5, 123, 467, 10)
        assertFalse(bv.isEmpty())
        bv.clear()
        assertTrue(bv.isEmpty())

        @Suppress("UNUSED_PARAMETER")
        for (bit in bv) fail()
    }

    @Test
    fun fill() {
        val bv = mutableBitsOf(1, 2, 3, 31, 32, 33, 63, 64, 65, 70, 95, 96)

        bv.clear(0..1)
        assertEquals(
            listOf(2, 3, 31, 32, 33, 63, 64, 65, 70, 95, 96),
            bv.toList()
        )

        bv.clear(4..32)
        assertEquals(
            listOf(2, 3, 33, 63, 64, 65, 70, 95, 96),
            bv.toList()
        )

        bv.clear(0..95)
        assertEquals(
            listOf(96),
            bv.toList()
        )

        bv.fill(2..2)
        assertEquals(
            listOf(2, 96),
            bv.toList()
        )

        bv.fill(0..98)
        assertEquals(
            (0..98).toList(),
            bv.toList()
        )
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
    fun bitwise_operations() {
        val a = mutableBitsOf(0, 1, 2, 3, 120,                130)
        val b = mutableBitsOf(0, 1, 2,    120, 121, 122, 123, 130)

        a.copy().apply { and(b) } assertEquals bitsOf(0, 1, 2, 120, 130)

        a.copy().apply { andNot(b) } assertEquals bitsOf(3)

        a.copy().apply { or(b) } assertEquals bitsOf(
            0,
            1,
            2,
            3,
            120,
            121,
            122,
            123,
            130
        )

        a.copy().apply { xor(b) } assertEquals bitsOf(3, 121, 122, 123)
    }

    @Test
    fun clear_range() {
        var nums = listOf(2, 3, 6, 7, 8, 9, 10, 124, 125, 127, 129)
        val bits = mutableBitsOf(*nums.toIntArray())

        fun removeRange(range: IntRange) {
            bits.clear(range)
            nums = nums.filter { it !in range}
            assertEquals(nums, bits.toList())
        }

        removeRange(3..6)
        removeRange(3..6)
        removeRange(0..1)
        removeRange(0..0)
        removeRange(130..1000)
        removeRange(25..120)
        removeRange(7..7)
        removeRange(127..127)
        removeRange(124..127)
        removeRange(0..130)

        assertTrue(bits.isEmpty())
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

    @Test
    fun invert_test_1() {
        val bits = mutableBitsOf(1, 2, 4)

        bits.invert(0)
        assertEquals(bitsOf(1, 2, 4), bits)

        bits.invert(2)
        assertEquals(bitsOf(0, 2, 4), bits)

        bits.invert(3)
        assertEquals(bitsOf(1, 4), bits)

        bits.invert(7)
        assertEquals(bitsOf(0, 2, 3, 5, 6), bits)
    }

    @Test
    fun invert_test_2() {
        val bits = mutableBitsOf(105, 107)

        bits.invert(109)
        assertEquals(
            bitsOf(*((0..104).toList()
                .plus(106)
                .plus(108)
                .toIntArray())),
            bits
        )
    }

    @Test
    fun invert_test_3() {
        val bits = mutableBitsOf(3)

        bits.invert(130)
        assertEquals(
            bitsOf(*((0..129).toList().minus(3).toIntArray())),
            bits
        )
    }
}


infix fun <T> T.assertEquals(expected: T) = assertEquals(expected, this)