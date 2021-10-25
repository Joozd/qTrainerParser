package extensions

/**
 * Get all indices of an occurrence in an iterable
 * eg. [1,2,3,1,2,3].indicesOf(1) == [0,3]
 */
fun <E> Iterable<E>.indicesOf(e: E)
        = mapIndexedNotNull{ index, elem -> index.takeIf{ elem == e } }


/**
 * Get all indices of elements that conform to [predicate]
 * [1,2,3,4,5].indicesOf { it %2 == 1 } == [0,2,4]
 */
fun <E> Iterable<E>.indicesOf(predicate: (E) -> Boolean) =
    mapIndexedNotNull {  index, elem -> index.takeIf { predicate(elem) } }