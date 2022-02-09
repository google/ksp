package com.google.devtools.ksp

// TODO: garbage collect underlying sequence after exhaust.
class MemoizedSequence<T>(sequence: Sequence<T>) : Sequence<T> {

    private val cache = arrayListOf<T>()

    private val iter: Iterator<T> by lazy {
        sequence.iterator()
    }

    private inner class CachedIterator() : Iterator<T> {
        var idx = 0
        override fun hasNext(): Boolean {
            return idx < cache.size || iter.hasNext()
        }

        override fun next(): T {
            if (idx == cache.size) {
                cache.add(iter.next())
            }
            val value = cache[idx]
            idx += 1
            return value
        }
    }

    override fun iterator(): Iterator<T> {
        return CachedIterator()
    }
}
