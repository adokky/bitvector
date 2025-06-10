## BitVector

Uncompressed, dynamically resizeable bitset, similar to `java.util.BitSet`.
- Allocation-free enumeration of set bits 
- 4 byte words, avoids [`Long` emulation][long-emu] in JS

Based on Adrian Papari initial [implementation][original] and has been enhanced to add new functionality and modern Kotlin support:
* introduce new operations (`first`, `last`, `clear`, `invert` and more)
* split into 2 implementations: `BitVector` and `MutableBitVector`
* make use of kotlin stdlib `countOneBits`, `countLeadingZeroBits`

 [original]: https://github.com/junkdog/bitvector
 [long-emu]: https://kotlinlang.org/docs/reference/js-to-kotlin-interop.html#representing-kotlin-types-in-javascript
 
## Usage

#### Constructing
```kotlin
val bv: BitVector = bitsOf(1, 2, 56, 64, 128, 129, 130, 131, 420)
```


#### Individual bits
```kotlin
val bv = BitVector()
bv[142] = true // or bv.set(142)
assert(142 in bv)

bv.clear(142)  // or bv[142] = false
assert(142 !in bv)
```


#### `or`, `and`, `andNot`, `xor`
As with `java.util.BitSet`, these operations mutate the callee. Do a `copy()` if the original BitVector needs to stick around.

These functions are not infix functions, as such syntax would suggest a value copy.
  
```kotlin
val a = bitsOf(0, 1, 2, 3, 120,                130)
val b = bitsOf(0, 1, 2,    120, 121, 122, 123, 130)

assert(a.and(b) == bitsOf(0, 1, 2, 120, 130))

// caveat: bitvector was mutated above
assert(a == bitsOf(0, 1, 2, 120, 130))
```


#### vs other BitVectors
```kotlin
// 1, 4 contained in other 
bitsOf(1, 4) in bitsOf(0, 1, 4, 5, 6) 
```

#### Looping over bits: fast way
```kotlin
bitsOf(*bits).forEachBit { println("bit $it says hi") }
```


#### Looping over bits: Iterable<Int>
```kotlin
// can be combined with filter/map etc
bitsOf(*bits).forEach { println("bit $it says hi") }
```

## JVM Benchmarks / enumerating set bits

Mapping `true` bit positions into integers, inserting each into an `IntBag` (thin wrapper around `int[]`).
 
[artemis-odb](https://github.com/junkdog/artemis-odb)'s `BitVector` was the basis for this implementation. The benchmark setup favors the artemis implementation, as it provides an optimized `toIntBag` method: it serves as a reference for best possible performance.

See [jmh-logs](https://github.com/junkdog/bitvector/tree/master/jmh-logs) for the full logs.

![benchmark.png](http://junkdog.github.io/images/bitvector-jmh.png)

Discrepancy to artemis' `BitVector` is unwelcome. The implementation is for the most part the same, except that this implementation uses `int` for words, instead of `long`. 4 or 8 byte words did not have a significant impact on performance.

The for-loop performs poorly due to all the `Integer` boxing, extra indirection and allocation, compared to `forEachBit`.   

`java.util.BitSet` suffers from not having a way of enumerating all bits at once, instead relying on repeatedly calling `nextSetBit`.