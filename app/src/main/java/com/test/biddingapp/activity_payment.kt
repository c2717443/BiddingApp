package com.test.biddingapp


import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.test.onlinestoreapp.Controller

class activity_payment : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val imageUrl = intent.getStringExtra("imageUrl")
        val startingBid = intent.getDoubleExtra("startingBid", 0.0)
        setContent {
            PaymentScreen(title, description, imageUrl, startingBid)
        }
    }
}

@Composable
fun PaymentScreen(title: String?, description: String?, imageUrl: String?, startingBid: Double) {
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var context= LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Payment Details", style = MaterialTheme.typography.h5, modifier = Modifier.padding(16.dp))
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            label = { Text("Card Number") },
            placeholder = { Text("Card Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = cvv,
            onValueChange = { cvv = it },
            label = { Text("CVV") },
            placeholder = { Text("CVV") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            label = { Text("Expiry Date (MM/YY)") },
            placeholder = { Text("MM/YY") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val boughtBid = BoughtBid(
                    userId = Controller.getUserId(context),
                    title = title ?: "",
                    description = description ?: "",
                    imageUrl = imageUrl ?: "",
                    amount = startingBid
                )
                addBoughtBidToFirebase(Controller.getUserId(context),boughtBid, context)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Pay")
        }
    }
}

fun addBoughtBidToFirebase(uid:String,boughtBid: BoughtBid, context: Context) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("boughtBids")
    val bidId = databaseRef.push().key ?: return
    databaseRef.child(uid).child(bidId).setValue(boughtBid)
        .addOnSuccessListener {
            Toast.makeText(context, "Bid bought successfully!", Toast.LENGTH_LONG).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to buy bid: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

data class BoughtBid(
    val userId: String="",
    val title: String="",
    val description: String="",
    val imageUrl: String="",
    val amount: Double=0.0,
    val paymentTimestamp: Long = System.currentTimeMillis()
)
