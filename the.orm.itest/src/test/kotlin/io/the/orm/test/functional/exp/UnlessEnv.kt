package io.the.orm.test.functional.exp

import failgood.Ignored

class UnlessEnv(private val envVar: String) : Ignored {
    override fun isIgnored(): String? {
        return if (System.getenv(envVar) == null)
            "Ignored because env var $envVar is not set"
        else null
    }
}
