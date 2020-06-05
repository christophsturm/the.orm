package r2dbcfun

internal fun String.toSnakeCase(): String = this.foldIndexed(StringBuilder(this.length + 5)) { idx, target, char ->
    when {
        char.isLowerCase() -> target.append(char)
        idx > 0 -> target.append('_').append(char.toLowerCase())
        else -> target.append(char.toLowerCase())
    }
}.toString()
