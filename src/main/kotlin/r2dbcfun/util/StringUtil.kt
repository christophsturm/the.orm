package r2dbcfun

internal fun String.toSnakeCase(): String =
    this
        .foldIndexed(StringBuilder(this.length + 5)) { idx, target, char ->
            when {
                char.isLowerCase() -> target.append(char)
                idx > 0 -> target.append('_').append(char.toLowerCase())
                else -> target.append(char.toLowerCase())
            }
        }
        .toString()

internal fun String.toIndexedPlaceholders(): String {
    var idx = 1
    return this
        .fold(StringBuilder(this.length + 10)) { target, char ->
            when (char) {
                '?' -> target.append("${'$'}${idx++}")
                else -> target.append(char)
            }
        }
        .toString()
}
