package io.the.orm

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.hasMany
import io.the.orm.query.isNotNull
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider

@Test
object HasManyTest {
    data class Entity(val name: String, val id: PK? = null)
    data class HolderOfEntity(val name: String, val nestedEntities: HasMany<Entity>, val id: PK? = null)

    private const val SCHEMA = """
    create sequence holder_of_entity_seq no maxvalue;
create table holder_of_entitys
(
    id             bigint       not null default nextval('holder_of_entity_seq') primary key,
    name           varchar(100) not null
);

    create sequence entity_seq no maxvalue;
create table entitys
(
    id             bigint       not null default nextval('entity_seq') primary key,
    holder_of_entity_id      bigint       not null,
    foreign key (holder_of_entity_id) references holder_of_entitys (id),
    name           varchar(100) not null
);

"""

    val repo = MultiRepo(listOf(Entity::class, HolderOfEntity::class))
    val context = describeOnAllDbs<HasMany<Entity>>(schema = SCHEMA) {
        it("can create an entity with nested entities") {
            val holder = HolderOfEntity(
                "name",
                hasMany(setOf(Entity("nested entity 1"), Entity("nested entity 2")))
            )
            RepoTransactionProvider(repo, it()).transaction(HolderOfEntity::class, Entity::class) { holderRepo, entityRepo->
                val createdHolder = holderRepo.create(holder)
                // this is a hack to load all entities. query api really needs a rethought
                val entities = entityRepo.queryFactory.createQuery(Entity::name.isNotNull()).with(entityRepo.connectionProvider, Unit).find()
                assert(entities.map { it.name }.containsExactlyInAnyOrder("nested entity 1", "nested entity 2"))
            }
        }
    }
}
