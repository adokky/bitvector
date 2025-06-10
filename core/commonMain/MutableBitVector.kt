package dev.dokky.bitvector

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Performance optimized bitset implementation. Certain operations are
 * prefixed with `unsafe`; these methods perform no validation.
 */
class MutableBitVector private constructor(words: IntArray): BitVector(words) {
    constructor(initialCapacity: Int = WORD_SIZE): this(IntArray((initialCapacity / WORD_SIZE).coerceAtLeast(1)))

    /** Creates a bit set based off another bit vector. */
    constructor(copyFrom: BitVector): this(
        IntArray(copyFrom.words.size).also {
            for (i in it.indices) {
                it[i] = copyFrom.words[i]
            }
        }
    )

    override fun copy(): MutableBitVector = MutableBitVector(this)

    /** @param index the index of the bit to set */
    fun set(index: Int) {
        require(index >= 0)

        val word = index.toWordIdx()
        checkCapacity(word)
        words[word] = words[word] or (1 shl index)
    }

    /**
     * @param index the index of the bit to set
     * @return previous value of the bit at [index]
     */
    @JvmOverloads
    fun put(index: Int, value: Boolean = true): Boolean {
        require(index >= 0)

        val wi = index.toWordIdx()
        checkCapacity(wi)

        val old = unsafeGet(wi, index)

        words[wi] = if (value) {
            words[wi] or (1 shl index)
        } else {
            words[wi] and (1 shl index).inv()
        }

        return old
    }

    /** @param index the index of the bit to set */
    operator fun set(index: Int, value: Boolean) {
        if (value)
            set(index)
        else
            unset(index)
    }

    fun fill(range: IntRange) {
        require(range.start >= 0)

        val fromIndex = range.first
        val toIndex = range.last + 1

        val startWordIndex: Int = fromIndex.toWordIdx()
        val endWordIndex: Int = (toIndex - 1).toWordIdx()

        checkCapacity(endWordIndex)

        val firstWordMask = -1 shl fromIndex
        val lastWordMask = -1 ushr -toIndex

        if (startWordIndex == endWordIndex) {
            words[startWordIndex] = words[startWordIndex] or (firstWordMask and lastWordMask)
        } else {
            words[startWordIndex] = words[startWordIndex] or firstWordMask
            for (i in startWordIndex + 1 until endWordIndex) {
                words[i] = -1
            }
            words[endWordIndex] = words[endWordIndex] or lastWordMask
        }
    }

    fun clear(range: IntRange) {
        clear(range.first, range.last + 1)
    }

    fun clear(fromIndex: Int, toIndexExclusive: Int) {
        require(fromIndex >= 0)
        require(toIndexExclusive >= fromIndex)

        if (fromIndex == toIndexExclusive) return

        var toIndex = toIndexExclusive
        val startWordIndex: Int = fromIndex.toWordIdx()
        val wordsInUse = words.size
        if (startWordIndex < wordsInUse) {
            var endWordIndex: Int = (toIndex - 1).toWordIdx()
            if (endWordIndex >= wordsInUse) {
                toIndex = length()
                endWordIndex = wordsInUse - 1
            }

            val firstWordMask = -1 shl fromIndex
            val lastWordMask = -1 ushr -toIndex
            if (startWordIndex == endWordIndex) {
                words[startWordIndex] = words[startWordIndex] and (firstWordMask and lastWordMask).inv()
            } else {
                words[startWordIndex] = words[startWordIndex] and firstWordMask.inv()
                for (i in startWordIndex + 1 until endWordIndex) {
                    words[i] = 0
                }
                words[endWordIndex] = words[endWordIndex] and lastWordMask.inv()
            }
        }
    }

    /** @param index the index of the bit to set */
    fun unsafeSet(index: Int) {
        val word = index.toWordIdx()
        words[word] = words[word] or (1 shl index)
    }

    /**
     * Sets the bit at [index] and returns its previous value.
     *
     * @param index the index of the bit to set
     */
    fun unsafeGetAndSet(index: Int): Boolean {
        val wordIdx = index.toWordIdx()

        val oldWord = words[wordIdx]
        val newWord = oldWord or (1 shl index)

        if (oldWord == newWord) return true
        words[wordIdx] = newWord
        return false
    }

    /**
     * Clears the bit at [index] and returns its previous value.
     *
     * @param index the index of the bit to set
     */
    fun unsafeGetAndUnset(index: Int): Boolean {
        val wordIdx = index.toWordIdx()

        val oldWord = words[wordIdx]
        val newWord = oldWord and ((1 shl index).inv())

        if (oldWord == newWord) return false
        words[wordIdx] = newWord
        return true
    }

    /** @param index the index of the bit to set */
    fun unsafeSet(index: Int, value: Boolean) {
        if (value)
            unsafeSet(index)
        else
            unsafeUnset(index)
    }

    /** @param index the index of the bit to flip */
    fun flip(index: Int) {
        val word = index.toWordIdx()
        checkCapacity(word)
        words[word] = words[word] xor (1 shl index)
    }

    /**
     * Grows the backing array so that it can hold the requested
     * bits. Mostly applicable when relying on the `unsafe` methods,
     * including [unsafeGet] and [unsafeUnset].
     *
     * @param bits number of bits to accommodate
     */
    fun ensureCapacity(bits: Int) {
        checkCapacity(bits.toWordIdx())
    }

    private fun checkCapacity(wordIndex: Int) {
        if (wordIndex >= words.size) {
            words = IntArray(wordIndex + 1).also { a ->
                words.forEachIndexed { idx, bits -> a[idx] = bits }
            }
        }
    }

    /**
     * @param index the index of the bit to clear
     */
    fun unset(index: Int) {
        val word = index.toWordIdx()
        if (word >= words.size) return
        words[word] = words[word] and (1 shl index).inv()
    }

    /** @param index the index of the bit to clear */
    fun unsafeUnset(index: Int) {
        val word = index.toWordIdx()
        words[word] = words[word] and (1 shl index).inv()
    }

    /** Clears the entire bitset  */
    fun clear() {
        for (i in words.indices) {
            words[i] = 0
        }
    }

    /** Inverts first [nbits] */
    fun invert(nbits: Int) {
        if (nbits <= 0) return

        val wn = nbits / WORD_SIZE

        if (wn >= words.size) words = words.copyOf(newSize = wn + 1)

        for (i in 0 ..< wn) {
            words[i] = words[i].inv()
        }

        val mask = (0.inv() shl (nbits % WORD_SIZE)).inv()
        words[wn] = words[wn] xor mask
    }

    /**
     * Performs a logical **AND** of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the
     * value true and the corresponding bit in the bit set argument
     * also had the value true.
     */
    fun and(other: BitVector) {
        val commonWords = minOf(words.size, other.words.size)
        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] and other.words[i]
                i++
            }
        }

        if (words.size > commonWords) {
            var i = commonWords
            val s = words.size
            while (s > i) {
                words[i] = 0
                i++
            }
        }
    }

    /**
     * Clears all the bits in this bit set whose corresponding
     * bit is set in the specified bit set.
     *
     * @param other a bit set
     */
    fun andNot(other: BitVector) {
        val commonWords = minOf(words.size, other.words.size)
        var i = 0
        while (commonWords > i) {
            words[i] = words[i] and other.words[i].inv()
            i++
        }
    }

    /**
     * Performs a logical **OR** of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value `true` if and only if it either already had the value `true`
     * or the corresponding bit in the bit set argument has the
     * value `true`.
     *
     * @param other a bit set
     */
    fun or(other: BitVector) {
        val commonWords = minOf(words.size, other.words.size)
        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] or other.words[i]
                i++
            }
        }

        if (commonWords < other.words.size) {
            checkCapacity(other.words.size)
            var i = commonWords
            val s = other.words.size
            while (s > i) {
                words[i] = other.words[i]
                i++
            }
        }
    }

    /**
     * Performs a logical **XOR** of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     *
     *  * The bit initially has the value true, and the corresponding bit in
     *    the argument has the value `false`.
     *  * The bit initially has the value false, and the corresponding bit in
     *    the argument has the value `true`.
     *
     * @param other
     */
    fun xor(other: BitVector) {
        val commonWords = minOf(words.size, other.words.size)

        run {
            var i = 0
            while (commonWords > i) {
                words[i] = words[i] xor other.words[i]
                i++
            }
        }

        if (commonWords < other.words.size) {
            checkCapacity(other.words.size)
            var i = commonWords
            val s = other.words.size
            while (s > i) {
                words[i] = other.words[i]
                i++
            }
        }
    }

    companion object {
        @JvmStatic
        fun wrap(bits: IntArray): MutableBitVector = MutableBitVector(bits)

        @JvmField
        internal val Empty: BitVector = MutableBitVector(IntArray(0))
    }
}

