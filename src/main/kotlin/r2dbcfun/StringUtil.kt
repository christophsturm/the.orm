package r2dbcfun

fun String.toSnakeCase(): String {
    return this.mapIndexed { idx, char ->
        when {
            char.isLowerCase() -> char.toString()
            idx > 0 -> "_${char.toLowerCase()}"
            else -> char.toLowerCase()
                .toString()
        }
    }.joinToString("")
}
