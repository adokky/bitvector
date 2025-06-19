package dev.dokky.bitvector

import kotlin.test.*

class MutableBitVectorTest {
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
    fun bitwise_operations() {
        val a = mutableBitsOf(0, 1, 2, 3, 120,                130)
        val b = mutableBitsOf(0, 1, 2,    120, 121, 122, 123, 130)

        a.copy().apply { mutateAnd(b) } assertEquals bitsOf(0, 1, 2, 120, 130)

        a.copy().apply { mutateAndNot(b) } assertEquals bitsOf(3)

        a.copy().apply { mutateOr(b) } assertEquals bitsOf(
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

        a.copy().apply { mutateXor(b) } assertEquals bitsOf(3, 121, 122, 123)

        // check the original is not mutated
        a assertEquals bitsOf(0, 1, 2, 3, 120, 130)
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