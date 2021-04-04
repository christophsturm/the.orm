package io.the.orm

object TestConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
    val CI = System.getenv("CI") != null
    val PITEST = System.getenv("PITEST") != null
}
