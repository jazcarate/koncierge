package ar.com.florius

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class HelloKtTest : ShouldSpec({

    should("main") {
        "sammy".length shouldBe 5
    }
})
