package dev.dokky.bitvector

import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

@PublishedApi
internal const val WORD_INDEX_SHIFT: Int = 5
internal const val WORD_SIZE: Int = 32

/**
 * Uncompressed, dynamically resizeable bitset, similar to `java.util.BitSet`
 */
sealed class BitVector(words: IntArray): Iterable<Int> {
    var words: IntArray = words
        protected set

    /**
     * @param index the index of the bit
     * @return whether the bit is set
     */
    fun unsafeGet(index: Int): Boolean {
        return words[index.toWordIdx()] and (1 shl index) != 0
    }

    /**
     * @param index the index of the bit
     * @return whether the bit is set
     */
    operator fun get(index: Int): Boolean {
        require(index >= 0)

        val word = index.toWordIdx()
        return unsafeGet(word, index)
    }

    protected fun unsafeGet(wordIndex: Int, index: Int): Boolean =
        wordIndex < words.size && words[wordIndex] and (1 shl index) != 0

    open fun copy(): BitVector = MutableBitVector(this)

    override fun toString(): String {
        val cardinality = cardinality()
        return if (cardinality <= 0) "[]" else {
            buildString(cardinality * 4) {
                append('[')
                forEachBit { bit ->
                    append(bit)
                    append(", ")
                }
                setLength(length - 2)
                append("]")
            }
        }
    }

    /**
     * Enumerates over all `true` bits sequentially. This function
     * performs better than [forEach] and any other functions
     * from `Iterable<Int>`.
     */
    inline fun forEachBit(action: (Int) -> Unit) {
        forEachBitBreakable { action(it); false }
    }

    /**
     * Enumerates over all `false` bits sequentially up until number of 32-bit words.
     */
    inline fun forEachZeroBit(action: (Int) -> Unit) {
        forEachZeroBitBreakable { action(it); false }
    }

    /**
     * Simmilar to [forEachBit] but stops iteration if [action] returns `true`.
     */
    inline fun forEachBitBreakable(bit: Boolean = true, action: (Int) -> Boolean) {
        val words = words
        val size = words.size
        var wordIdx = 0

        while (size > wordIdx) {
            var word = words[wordIdx]
            if (!bit) word = word.inv()
            while (word != 0) {
                val t = word and -word
                word = word xor t
                if (action((wordIdx shl WORD_INDEX_SHIFT) + (t - 1).countOneBits())) return
            }

            wordIdx++
        }
    }

    /**
     * Enumerates over all `false` bits sequentially up until number of 32-bit words.
     */
    inline fun forEachZeroBitBreakable(f: (Int) -> Boolean) {
        forEachBitBreakable(bit = false, f)
    }

    /** @return index of first [bit] */
    fun first(bit: Boolean = true): Int {
        var wordIdx = 0

        while (wordIdx < words.size) {
            var word = words[wordIdx]
            if (!bit) word = word.inv()
            while (word != 0) {
                val t = word and -word
                return (wordIdx shl WORD_INDEX_SHIFT) + (t - 1).countOneBits()
            }

            wordIdx++
        }

        return if (bit) -1 else (words.size shl WORD_INDEX_SHIFT)
    }

    /** @return index of first [bit] in range starting from [start] until [endExclusive] */
    fun first(start: Int, endExclusive: Int = words.size shl WORD_INDEX_SHIFT, bit: Boolean = true): Int {
        val wordStart = start.toWordIdx()
        val wordEnd = (endExclusive.toWordIdx() + 1).coerceAtMost(words.size)
        if (start >= endExclusive) return -1

        for (wordIdx in wordStart ..< wordEnd) {
            var word = words[wordIdx]
            if (!bit) word = word.inv()
            while (word != 0) {
                val t = word and -word
                word = word xor t
                val idx = (wordIdx shl WORD_INDEX_SHIFT) + (t - 1).countOneBits()
                if (idx >= endExclusive) return -1
                if (idx >= start) return idx
            }
        }

        return if (bit) -1 else {
            val zerosStart = words.size shl WORD_INDEX_SHIFT
            when {
                zerosStart <= start -> start
                zerosStart >= endExclusive -> -1
                else -> zerosStart
            }
        }
    }

    /** @return last index of `true` bit */
    fun last(): Int {
        var wordIdx = words.lastIndex
        while (wordIdx >= 0) {
            val word = words[wordIdx]
            val idx = WORD_SIZE - word.countLeadingZeroBits() - 1
            if (idx >= 0) return (wordIdx shl WORD_INDEX_SHIFT) + idx
            wordIdx--
        }
        return -1
    }

    override fun iterator(): IntIterator = BitVectorIterator(this)

    operator fun contains(index: Int): Boolean = get(index)

    /** Returns the count of `true` bits */
    fun cardinality(): Int {
        var count = 0
        for (i in words.indices)
            count += words[i].countOneBits()

        return count
    }

    /**
     * Returns the "logical size" of this bitset: the index of the
     * highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     *
     * @return the logical size of this bitset
     */
    fun length(): Int {
        val bits = this.words
        for (word in bits.indices.reversed()) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0)
                return word.bitCapacity() + WORD_SIZE - bitsAtWord.countLeadingZeroBits()
        }

        return 0
    }

    /** @return `true` if this bitset contains no set bits */
    fun isEmpty(): Boolean = words.all { it == 0 }

    /**
     * Returns `true` if the specified BitVector has at least one bit set to `true`
     * that is also set to `true` in this BitVector.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set intersects the specified bit set
     */
    fun intersects(other: BitVector): Boolean {
        val bits = this.words
        val otherBits = other.words
        var i = 0
        val s = minOf(bits.size, otherBits.size)
        while (s > i) {
            if (bits[i] and otherBits[i] != 0) {
                return true
            }
            i++
        }
        return false
    }

    /**
     * Returns `true` if this bit set is a super set of the specified set,
     * i.e. it has all bits set to `true` that are also set to `true`
     * in the specified BitVector.
     *
     * @param other a bit set
     * @return boolean indicating whether this bit set is a super set of the specified set
     */
    operator fun contains(other: BitVector): Boolean {
        val bits = this.words
        val otherBits = other.words
        val otherBitsLength = otherBits.size
        val bitsLength = bits.size

        for (i in bitsLength..otherBitsLength - 1) {
            if (otherBits[i] != 0) {
                return false
            }
        }

        var i = 0
        val s = minOf(bitsLength, otherBitsLength)
        while (s > i) {
            if (bits[i] and otherBits[i] != otherBits[i]) {
                return false
            }
            i++
        }

        return true
    }

    override fun hashCode(): Int {
        val word = length().toWordIdx()
        var hash = 0
        var i = 0
        while (word >= i) {
            hash = 127 * hash + words[i]
            i++
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BitVector) return false

        val otherBits = other.words

        val commonWords = minOf(words.size, otherBits.size)
        var i = 0
        while (commonWords > i) {
            if (words[i] != otherBits[i])
                return false
            i++
        }

        if (words.size == otherBits.size)
            return true

        return length() == other.length()
    }

    companion object {
        @JvmStatic
        val Empty: BitVector @JvmName("empty") get() = MutableBitVector.Empty
    }
}