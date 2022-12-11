# the.orm

A non-blocking ORM library for kotlin that supports H2SQL and PostgreSQL via Vert.x and R2DBC

# In a nutshell:
* Entities are normal data classes
* No Annotations
* Supports immutable entities
* HasMany and BelongsTo Relations
* Really fast testing support. Its own tests suite runs in 10 seconds against 3 different databases (h2-r2dbc, h2-psql, vertx-psql)

## Is this for me?

the.orm is very opinionated and not for you if you
need to support a legacy database structure or if you really want to be super creative with your sql schema.
its more like rails' active-record and less like sql-alchemy.
A lot is currently missing but a lot is already working so feel free to try it out.

docs are currently also non-existant but take a look at [MultipleReposFunctionalTest](the.orm.itest/src/test/kotlin/io/the/orm/test/functional/MultipleReposFunctionalTest.kt)
and the other tests in the itest module to see how to use it.

Planned but missing:
* schema migration support (you can use flyway in the meantime)
* UUID PK support. (currently only long is supported)
* query support clearly needs a big api change
