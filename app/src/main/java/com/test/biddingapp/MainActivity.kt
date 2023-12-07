package com.test.biddingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.test.onlinestoreapp.Controller
import kotlinx.android.parcel.Parcelize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            BiddingListScreen()
            FirebaseApp.initializeApp(this)
        }
    }
}

@Composable
fun BiddingListScreen() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bidding Items", color = Color.White) },
                actions = {
                    // Overflow menu state
                    var showMenu by remember { mutableStateOf(false) }


                    TextButton(onClick = {
                        val intent = Intent(context, PostBiddingItemActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Text("Want to List?", color = MaterialTheme.colors.onPrimary)
                    }

                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            showMenu = false

                            val intent = Intent(context, activity_login::class.java)
                            context.startActivity(intent)
                            Controller.saveLoginState(context,false)
                            if (context is activity_login) {
                                context.finish()
                            }

                        }) {
                            Text("Logout")
                        }

                    }



                },
            )

        }
    ) { innerPadding ->
        BiddingItemsListScreen(Modifier.padding(innerPadding))
    }
}

@Composable
fun BiddingItemsListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var biddingItems by remember { mutableStateOf(listOf<BiddingItem>()) }

    LaunchedEffect(key1 = "load_bidding_items") {
        val database = Firebase.database.reference.child("biddingItems")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                biddingItems = snapshot.children.mapNotNull { it.getValue(BiddingItem::class.java) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load bidding items.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LazyColumn(contentPadding = PaddingValues(all = 16.dp)) {
        items(biddingItems) { biddingItem ->
            BiddingItemRow(biddingItem, context)
        }
    }
}

@Composable
fun BiddingItemRow(biddingItem: BiddingItem, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, BiddingDetailActivity::class.java)
                intent.putExtra("BiddingItem", biddingItem)
                context.startActivity(intent)
            }
            .padding(vertical = 8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = biddingItem.title, style = MaterialTheme.typography.h6)
            Text(text = biddingItem.description)
            Text(text = "Starting Bid: ${biddingItem.startingBid}")
        }
    }
}

@Parcelize
data class BiddingItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    var startingBid: Double = 0.0,
    var timestamp: Long = 0

) : Parcelable
