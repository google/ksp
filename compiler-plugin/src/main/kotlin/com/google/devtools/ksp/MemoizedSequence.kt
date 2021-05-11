package com.google.devtools.ksp

// TODO: garbage collect underlying sequence after exhaust.
class MemoizedSequence<T>(sequence: Sequence<T>) : Sequence<T> {

    private val cache = mutableListOf<T>()

    private val iter: Iterator<T> by lazy {
        sequence.iterator()
    }

    private inner class CachedIterator(val iterCache: Iterator<T>, var buffer: Int) : Iterator<T> {
        override fun hasNext(): Boolean {
            return buffer > 0 || iter.hasNext()
        }

        override fun next(): T {
            if (buffer > 0) {
                buffer --
                return iterCache.next()
            }
            val value = iter.next()
            cache.add(value)
            return value
        }
    }

    override fun iterator(): Iterator<T> {
        return CachedIterator(cache.iterator(), cache.size)
    }
}
