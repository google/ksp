package com.google.devtools.ksp

class IdKey<T>(private val k: T) {
    override fun equals(other: Any?): Boolean = if (other is IdKey<*>) k === other.k else false
    override fun hashCode(): Int = k.hashCode()
}

class IdKeyPair<T, P>(private val k1: T, private val k2: P) {
    override fun equals(other: Any?): Boolean = if (other is IdKeyPair<*, *>) k1 === other.k1 &&
        k2 === other.k2 else false
    override fun hashCode(): Int = k1.hashCode() * 31 + k2.hashCode()
}

class IdKeyTriple<T, P, Q>(private val k1: T, private val k2: P, private val k3: Q) {
    override fun equals(other: Any?): Boolean = if (other is IdKeyTriple<*, *, *>) k1 === other.k1 &&
        k2 === other.k2 && k3 === other.k3 else false
    override fun hashCode(): Int = k1.hashCode() * 31 * 31 + k2.hashCode() * 31 + k3.hashCode()
}
