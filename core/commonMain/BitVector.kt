package dev.dokky.bitvector

import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

@PublishedApi
internal const val WORD_INDEX_SHIFT: Int = 5
internal const val WORD_SIZE: Int = 32
internal const val WORD_MASK: Int = 0.inv().ushr(WORD_SIZE - WORD_INDEX_SHIFT)

/**
 * Uncompressed, dynamically resizeable bitset, similar to `java.util.BitSet`
 */
sealed class BitVector(words: IntArray): Iterable<Int> {
    var words: IntArray = words
        protected set

    /**
     * Retrieves the value of the bit at the specified index without bounds checking.
     *
     * @param index the index of the bit
     * @return `true` if the bit is set, `false` otherwise
     */
    fun unsafeGet(index: Int): Boolean {
        return words[index.toWordIdx()] and (1 shl index) != 0
    }

    /**
     * Retrieves the value of the bit at the specified index.
     *
     * @param index the index of the bit
     * @return `true` if the bit is set, `false` otherwise
     * @throws IllegalArgumentException if `index` is negative
     */
    operator fun get(index: Int): Boolean {
        require(index >= 0)

        val word = index.toWordIdx()
        return unsafeGet(word, index)
    }

    protected fun unsafeGet(wordIndex: Int, index: Int): Boolean =
        wordIndex < words.size && words[wordIndex] and (1 shl index) != 0

    /** Creates a copy of this bitset. */
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
     * Similar to [forEachBit] but stops iteration if [action] returns `true`.
     *
     * @param bit the bit value to iterate over (`true` or `false`)
     * @param action the action to be performed for each bit
     * @return `true` if iteration was stopped early, `false` otherwise
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
     *
     * @param f the action to be performed for each bit
     * @return `true` if iteration was stopped early, `false` otherwise
     */
    inline fun forEachZeroBitBreakable(f: (Int) -> Boolean) {
        forEachBitBreakable(bit = false, f)
    }

    /**
     * Returns the index of the first [bit] or -1 if there is no such [bit].
     *
     * @param bit the bit value to find (`true` or `false`)
     */
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

    /**
     * Returns the index of the first [bit] that occurs in the specified range
     * starting from [start] until [endExclusive] or -1 if there is no such [bit].
     *
     * @param start the start index (inclusive)
     * @param endExclusive the end index (exclusive)
     * @param bit the bit value to find (`true` or `false`)
     */
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

    /**
     * Returns the index of the last `true` bit or -1 if all bits are zero.
     */
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

    /**
     * Returns an iterator over the indices of the `true` bits in this bitset.
     */
    override fun iterator(): IntIterator = BitVectorIterator(this)

    /**
     * Checks if the bit at the specified [index] is set.
     */
    operator fun contains(index: Int): Boolean = get(index)

    /** Returns the count of `true` bits */
    fun cardinality(): Int {
        var count = 0
        for (i in words.indices)
            count += words[i].countOneBits()

        return count
    }

    /**
     * Returns the count of `true` bits in the specified range starting from [start] until [endExclusive].
     *
     * @param start the start index (inclusive)
     * @param endExclusive the end index (exclusive)
     * @return the number of `true` bits in the specified range
     */
    fun cardinality(start: Int, endExclusive: Int = Int.MAX_VALUE): Int {
        if (start >= endExclusive) return 0

        val wordStart = start.toWordIdx()
        val wordEndIndex = (endExclusive - 1).toWordIdx()
        val wordEnd = wordEndIndex.coerceAtMost(words.size - 1)

        val firstMask = 0.inv().shl(start)
        val endMask = 0.inv().ushr(WORD_SIZE - endExclusive)

        var count = 0
        for (widx in wordStart..wordEnd) {
            var w = words[widx]
            if (widx == wordStart) w = w and firstMask
            if (widx == wordEndIndex) w = w and endMask
            count += w.countOneBits()
        }
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

    /**
     * Returns the result of logical **AND** of this target bit set with the [other] bit set.
     * Each bit in the result set is `true` if and only if
     * it both initially had the value `true` and the corresponding bit
     * in the bit set argument also had the value true.
     */
    infix fun and(other: BitVector): BitVector {
        val newWords = IntArray(minOf(words.size, other.words.size))
        and(words = words, destWords = newWords, otherWords = other.words)
        return MutableBitVector(newWords)
    }

    /**
     * Returns the result of logical **OR** of this bit set with the [other] bit set.
     * Each bit in the result set is `true` if and only if it either
     * already had the value `true` or the corresponding bit in the bit
     * set argument has the value `true`.
     */
    infix fun or(other: BitVector): BitVector {
        val newWords = IntArray(maxOf(words.size, other.words.size))
        or(words = words, destWords = newWords, otherWords = other.words)
        return MutableBitVector(newWords)
    }

    /**
     * Returns the result of logical **XOR** of this bit set with the [other] bit set.
     * Each result bit is `true` if and only if one of the following statements holds:
     *  * The bit initially has the value `true`, and the corresponding bit in
     *    the argument has the value `false`.
     *  * The bit initially has the value `false`, and the corresponding bit in
     *    the argument has the value `true`.
     */
    infix fun xor(other: BitVector): BitVector {
        val newWords = IntArray(maxOf(words.size, other.words.size))
        xor(words = words, destWords = newWords, otherWords = other.words)
        return MutableBitVector(newWords)
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
        if (other !is BitVector) return false

        val otherWords = other.words

        val short: IntArray
        val long: IntArray
        if (words.size > otherWords.size) {
            short = otherWords
            long = words
        } else {
            short = words
            long = otherWords
        }

        if (!long.startsWith(short)) return false

        for (i in short.size ..< long.size) {
            if (long[i] != 0) return false
        }

        return true
    }

    companion object {
        @JvmStatic
        val Empty: BitVector @JvmName("empty") get() = MutableBitVector.Empty
    }
}