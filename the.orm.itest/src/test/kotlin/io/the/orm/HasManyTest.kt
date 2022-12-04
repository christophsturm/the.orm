package io.the.orm

import failgood.Test
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.hasMany
import io.the.orm.test.describeOnAllDbs

@Test
object HasManyTest {
    data class Entity(val name: String, val id: PK? = null)
    data class HolderOfEntity(val name: String, val nestedEntities: HasMany<Entity>, val id: PK? = null)

    private const val SCHEMA = """
    create sequence entity_seq no maxvalue;
create table entitys
(
    id             bigint       not null default nextval('entity_seq') primary key,
    name           varchar(100) not null
);
    create sequence holder_of_entity_seq no maxvalue;
create table holder_of_entitys
(
    id             bigint       not null default nextval('holder_of_entity_seq') primary key,
    entity_id      bigint       not null,
    name           varchar(100) not null
);

"""

    val context = describeOnAllDbs<HasMany<Entity>>(schema = SCHEMA) {
        it("can create an entity with nested entities") {
            val holder = HolderOfEntity(
                "name",
                hasMany(setOf(Entity("nested entity 1"), Entity("nested entity 2")))
            )
            val repo = Repo.create<HolderOfEntity>()
            repo.create(it(), holder)
        }
    }
}
