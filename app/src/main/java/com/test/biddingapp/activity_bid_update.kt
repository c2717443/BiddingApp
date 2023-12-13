package com.test.biddingapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import com.test.onlinestoreapp.Controller
 
class activity_bid_update : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra("itemId") ?: return
        setContent {
            UpdateBidScreen(itemId)
        }
    }
}

@Composable
fun UpdateBidScreen(itemId: String) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startingBid by remember { mutableStateOf("") }
    var expiryDays by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fetchItemDetails(itemId, context) { item ->
            title = item.title
            description = item.description
            startingBid = item.startingBid.toString()
            expiryDays = item.expiryDays.toString()
            imageUrl = item.imageUrl
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).wrapContentHeight(align = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Update Item", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Item Description") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = startingBid, onValueChange = { startingBid = it }, label = { Text("Starting Bid") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = expiryDays, onValueChange = { expiryDays = it }, label = { Text("Expiry Days") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                updateItemInDatabase(context, itemId, title, description, startingBid.toDoubleOrNull() ?: 0.0, expiryDays.toIntOrNull() ?: 0, imageUrl) {
                    Toast.makeText(context, "Item updated successfully!", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            Text("Update Item", fontSize = 18.sp)
        }
    }
}

fun fetchItemDetails(itemId: String, context: Context, onItemFetched: (AuctionItemUpload) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("biddingItems").child(itemId)
    databaseRef.get().addOnSuccessListener { dataSnapshot ->
        val auctionItem = dataSnapshot.getValue(AuctionItemUpload::class.java)
        auctionItem?.let { onItemFetched(it) }
    }
}

fun updateItemInDatabase(context: Context, itemId: String, title: String, description: String, startingBid: Double, expiryDays: Int, imageUrl: String, onSuccess: () -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("biddingItems")
    val uid= Controller.getUserId(context)
    val currentTimestamp = System.currentTimeMillis()
    val millisInADay = 24 * 60 * 60 * 1000L
    val expiryTimestamp = currentTimestamp + (expiryDays.toDouble() * millisInADay)
    val updatedItem =
        itemId?.let {
            AuctionItemUpload(
            id = it,
            uid=uid?:"",
            title = title,
            description = description,
            startingBid = startingBid,
            expiryDays = expiryDays,
            timestamp = currentTimestamp,
             expiryTimestamp = expiryTimestamp.toLong(),
            imageUrl = imageUrl
        )
        }
    databaseRef.child(itemId).setValue(updatedItem).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onSuccess()
        } else {
            Toast.makeText(context, "Failed to update item: ${task.exception?.message}", Toast.LENGTH_LONG).show()
        }
    }
}

