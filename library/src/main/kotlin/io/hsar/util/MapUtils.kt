package io.hsar.util

fun <K, V1, V2, R> Map<K, V1>.mergeReduce(other: Map<K, V2>, reduce: (key: K, value1: V1?, value2: V2?) -> R): Map<K, R> =
    (this.keys + other.keys).associateWith { reduce(it, this[it], other[it]) }