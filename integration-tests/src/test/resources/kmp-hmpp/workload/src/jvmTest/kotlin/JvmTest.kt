import com.example.ClientMainAnnotated
import com.example.CommonMainAnnotated
import com.example.JvmMainAnnotated
import com.example.JvmTestAnnotated
import org.junit.jupiter.api.Test

class JvmTest {
    @Test
    fun main() {
        println("commonMain: " + CommonMainAnnotated().allFiles)
        println("clientMain: " + ClientMainAnnotated().allFiles)
        println("jvmMain: " + JvmMainAnnotated().allFiles)
        println("jvmTest: " + JvmTestAnnotated().allFiles)
    }
}
