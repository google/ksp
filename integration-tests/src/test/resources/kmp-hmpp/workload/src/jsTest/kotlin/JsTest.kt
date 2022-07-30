import com.example.ClientMainAnnotated
import com.example.CommonMainAnnotated
import com.example.JsMainAnnotated
import com.example.JsTestAnnotated
import kotlin.test.Test

class JsTest {
    @Test
    fun main() {
        println("commonMain: " + CommonMainAnnotated().allFiles)
        println("clientMain: " + ClientMainAnnotated().allFiles)
        println("jsMain: " + JsMainAnnotated().allFiles)
        println("jsTest: " + JsTestAnnotated().allFiles)
    }
}
