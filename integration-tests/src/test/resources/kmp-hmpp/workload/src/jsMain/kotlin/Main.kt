import com.example.ClientMainAnnotated
import com.example.CommonMainAnnotated
import com.example.JsMainAnnotated

fun main() {
    println("commonMain: " + CommonMainAnnotated().allFiles)
    println("clientMain: " + ClientMainAnnotated().allFiles)
    println("jsMain: " + JsMainAnnotated().allFiles)
}
