[![Download](https://api.bintray.com/packages/christophsturm/maven/r2dbcfun/images/download.svg)](https://bintray.com/christophsturm/maven/r2dbcfun/_latestVersion)
[![Github CI](https://github.com/christophsturm/r2dbcfun/workflows/CI/badge.svg)](https://github.com/christophsturm/r2dbcfun/actions)

# r2dbcfun

a simple non-blocking ORM for kotlin and r2dbc. Supports R2DBC and Vertx SQL Client.

features:

* automatically map data classes to database tables
* coroutines based api
* no annotations

status:
It can map a data class to a database table, and it has a type safe query language.

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

    val repo = Repository.create<User>()

    val firstUser = repo.create(connection, User(name = "christoph", email = "my email"))
    val secondUser = repo.create(connection, User(name = "christian", email = "different email"))
    val findByName = repo.queryFactory.createQuery(User::name.like())
    val users = findByName(connection, "chris%").toCollection(mutableListOf())
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

#### typesafe query api
```
// create a query that queries 2 fields and takes 2 parameters, one of type string and one of type Pair<LocalDate,LocalDate>
                            val findByUserNameLikeAndBirthdayBetween =
                                repo.queryFactory.createQuery(User::name.like(), User::birthday.between())

// run the query
                                findByUserNameLikeAndBirthdayBetween(connection, "fred%", Pair(date1, date2))
```

for more examples look at the [unit tests](src/test/kotlin/r2dbcfun/test/functional)
