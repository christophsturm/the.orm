[![Download](https://api.bintray.com/packages/christophsturm/maven/r2dbcfun/images/download.svg)](https://bintray.com/christophsturm/maven/r2dbcfun/_latestVersion)
[![Github CI](https://github.com/christophsturm/r2dbcfun/workflows/CI/badge.svg)](https://github.com/christophsturm/r2dbcfun/actions)

# r2dbcfun (name tbd)
a simple ORM for kotlin and r2dbc

`./gradlew check` 

features:
* automatically map data classes to database tables
* coroutines based api
* no annotations

status:
It can map a data class to a database table 

planned: 
* relations (1:n, n:m)

# Usage
Kotlin Code:
```
    // optional pk class
    data class UserPK(override val id: Long) : PK
    
    
    data class User(
        val id: UserPK? = null, // long is also supported if you don't want to use pk classes
        val name: String,
        val email: String?,
        val isCool: Boolean = false,
        val bio: String? = null
    )

    val repo = R2dbcRepo.create<User>(connection)

    val firstUser = repo.create(User(name = "chris", email = "my email"))
    val secondUser = repo.create(User(name = "chris", email = "different email"))
    val users = repo.findBy(User::name, "chris").toCollection(mutableListOf())
    expectThat(users).containsExactlyInAnyOrder(firstUser, secondUser)
```

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

for more examples look at the [unit tests](src/test/kotlin/r2dbcfun/R2dbcRepoTest.kt)

Supported databases: H2 and PostgreSQL
