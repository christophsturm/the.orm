package blueprint

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty
import java.util.*

// this is just the first test from the minutest readme converted to strikt, to check that both work
class SimpleStackExampleTests : JUnit5Minutests {


    // The fixture type is the generic type of the test, here Stack<String>
    fun tests() = rootContext<Stack<String>> {

        // The fixture block tells Minutest how to create an instance of the fixture.
        // Minutest will call it once for every test.
        fixture {
            Stack()
        }

        test("add an item") {
            // In a test, 'this' is the fixture created above
            expectThat(this).isEmpty()

            this.add("item")
            expectThat(this).isNotEmpty()
        }

        // another test will use a new fixture instance
        test("fixture is fresh") {
            // you can also access the fixture as 'fixture' if it reads nicer
            expectThat(fixture).isEmpty()
        }
    }
}
