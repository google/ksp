import com.example.ClientMainAnnotated
import com.example.CommonMainAnnotated
import com.example.JvmMainAnnotated

fun main() {
    println("commonMain: " + CommonMainAnnotated().allFiles)
    println("clientMain: " + ClientMainAnnotated().allFiles)
    println("jvmMain: " + JvmMainAnnotated().allFiles)
}
