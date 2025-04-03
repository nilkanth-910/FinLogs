import android.content.Context
import android.util.Log // Import the Log class
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object FirebaseUtils {

    suspend fun importProductsFromCSV(context: Context, csvFileName: String) {
        withContext(Dispatchers.IO) {
            val database = FirebaseDatabase.getInstance()
            val productsRef = database.getReference("products")
            productsRef.removeValue()
            try {
                val inputStream = context.assets.open(csvFileName)
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String? = reader.readLine() // Skip header

                while (reader.readLine().also { line = it } != null) {
                    val data = line?.split(",")
                    if (data != null && data.size == 10) {
                        val barcode = data[0].toString()
                        val grossPrice = data[1].replace("�", "").toDoubleOrNull() ?: 0.0
                        val hsn = data[2].toString()
                        val mrp = data[3].replace("�", "").toDoubleOrNull() ?: 0.0
                        val productName = data[5].toString()
                        val rate = data[6].replace("�", "").toDoubleOrNull() ?: 0.0
                        val salePrice = data[7].replace("�", "").toDoubleOrNull() ?: 0.0
                        val stock = data[8].toInt()
                        val tax = data[9].replace("�", "").toDoubleOrNull() ?: 0.0

                        val obj = productsRef.push()
                        val key = obj.key
                        val productId = key.toString()

                        val product = mapOf(
                            "barcode" to barcode,
                            "grossPrice" to grossPrice,
                            "hsn" to hsn,
                            "mrp" to mrp,
                            "productId" to productId,
                            "productName" to productName,
                            "rate" to rate,
                            "salePrice" to salePrice,
                            "stock" to stock,
                            "tax" to tax
                        )

                        obj.setValue(product)


                    } else {
                        Log.w("FirebaseUtils", "Invalid CSV row: $line")
                    }
                }
                reader.close()
                inputStream.close()

            } catch (e: Exception) {
                Log.e("FirebaseUtils", "Error importing products: ${e.message}", e) // Log the exception with message and stack trace
            }
        }
    }
}
