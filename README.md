# r2dbcfun
playing around with r2dbc and kotlin coroutines

`./gradlew check` 

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
