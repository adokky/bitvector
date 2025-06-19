package dev.dokky.bitvector

internal fun and(words: IntArray, destWords: IntArray, otherWords: IntArray) {
    val commonWords = minOf(words.size, otherWords.size)
    run {
        var i = 0
        while (commonWords > i) {
            destWords[i] = words[i] and otherWords[i]
            i++
        }
    }

    if (words.size > commonWords) {
        var i = commonWords
        val s = words.size
        while (s > i) {
            destWords[i] = 0
            i++
        }
    }
}

internal fun or(words: IntArray, destWords: IntArray, otherWords: IntArray) {
    val commonWords = minOf(words.size, otherWords.size)
    run {
        var i = 0
        while (commonWords > i) {
            destWords[i] = words[i] or otherWords[i]
            i++
        }
    }

    if (commonWords < otherWords.size) {
        var i = commonWords
        val s = otherWords.size
        while (s > i) {
            destWords[i] = otherWords[i]
            i++
        }
    }
}

internal fun xor(words: IntArray, destWords: IntArray, otherWords: IntArray) {
    val commonWords = minOf(words.size, otherWords.size)

    run {
        var i = 0
        while (commonWords > i) {
            destWords[i] = words[i] xor otherWords[i]
            i++
        }
    }

    if (commonWords < otherWords.size) {
        var i = commonWords
        val s = otherWords.size
        while (s > i) {
            destWords[i] = otherWords[i]
            i++
        }
    }
}