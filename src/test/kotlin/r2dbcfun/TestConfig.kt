package r2dbcfun

object TestConfig {
    val H2_ONLY = System.getenv("H2_ONLY") != null
    val CI = System.getenv("CI") != null
    val PITEST = System.getenv("PITEST") != null
}