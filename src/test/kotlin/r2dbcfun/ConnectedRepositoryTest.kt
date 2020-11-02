package r2dbcfun

import io.kotest.core.spec.style.FunSpec

class ConnectedRepositoryTest  : FunSpec({
    context("ConnectedRepository") {
        test("wraps a Repository and has a Connection") {
            // not yet
//            val subject = ConnectedRepository(Repository.create())
        }
    }
})

class ConnectedRepository<T:Any>(val repo: Repository<T>) {

}


