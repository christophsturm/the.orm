[![Download](https://api.bintray.com/packages/christophsturm/maven/r2dbcfun/images/download.svg)](https://bintray.com/christophsturm/maven/r2dbcfun/_latestVersion)
[![Github CI](https://github.com/christophsturm/r2dbcfun/workflows/CI/badge.svg)](https://github.com/christophsturm/r2dbcfun/actions)

# r2dbcfun
playing around with r2dbc and kotlin coroutines

`./gradlew check` 


# Usage
Database Structure:
```
    create sequence users_id_seq no maxvalue;
    create table users
    (
        id      bigint       not null default nextval('users_id_seq'),
        name    varchar(100) not null,
        email   varchar(100),
        is_cool boolean,
        bio     text
    );
    
    alter table users
        add primary key (id);
```
Kotlin Code:
```
    data class UserPK(override val id: Long) : PK
    
    data class User(
        val id: UserPK? = null,
        val name: String,
        val email: String?,
        val isCool: Boolean = false,
        val bio: String? = null
    )

    val repo = R2dbcRepo.create<User, UserPK>(connection)

    val firstUser = repo.create(User(name = "chris", email = "my email"))
    val secondUser = repo.create(User(name = "chris", email = "different email"))
    val users = repo.findBy(User::name, "chris").toCollection(mutableListOf())
    expectThat(users).containsExactlyInAnyOrder(firstUser, secondUser)

```

for more examples look at the [unit tests](src/test/kotlin/r2dbcfun/R2dbcRepoTest.kt)

Supported databases: H2 and PostgreSQL
