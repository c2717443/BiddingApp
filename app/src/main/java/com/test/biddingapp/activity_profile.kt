package com.test.biddingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.test.biddingapp.ui.theme.BiddingAppTheme
import com.test.onlinestoreapp.Controller

class activity_profile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen()
        }
    }
}
enum class ProfileTab {
    POSTED_BIDS, BOUGHT_BIDS
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val userName = Controller.getUserName(context)
    val userItems = remember { mutableStateListOf<AuctionItemUpload>() }
    val uid = Controller.getUserId(context)
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTED_BIDS) }
    val tabTitles = listOf("Posted Bids", "Bought Bids")
    LaunchedEffect(uid) {
        fetchUserItems(context, uid) { items ->
            userItems.clear()
            userItems.addAll(items)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = userName,
            style = MaterialTheme.typography.h6,
            fontSize = 35.sp,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0f),
            contentColor = Color.Black,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = Color.Black
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title, color = Color.Black) },
                    selected = index == selectedTab.ordinal,
                    onClick = { selectedTab = ProfileTab.values()[index] },
                    selectedContentColor = Color.Black,
                    unselectedContentColor = Color.Black.copy(alpha = ContentAlpha.medium)
                )
            }
        }



        when (selectedTab) {
            ProfileTab.POSTED_BIDS -> PostedBidsTab(userItems)
            ProfileTab.BOUGHT_BIDS -> BoughtBidsTab()
        }
    }
}

@Composable
fun PostedBidsTab(userItems: MutableList<AuctionItemUpload>) {
    val context = LocalContext.current
    LazyColumn {
        items(userItems) { item ->
            UserItemRow(
                item = item,
                onDeleteClicked = {
                    userItems.remove(item)
                    deleteItem(item.id, context,
                        onSuccess = {
                            Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()

                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Failed to delete item: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onRowClicked = {

                    val intent = Intent(context, activity_bid_update::class.java).apply {
                        putExtra("itemId", item.id)
                    }
                    context.startActivity(intent)

                }
            )
        }
    }
}

@Composable
fun BoughtBidsTab() {
    val context = LocalContext.current
    val uid = Controller.getUserId(context)
    val boughtBids = remember { mutableStateListOf<BoughtBid>() }

    LaunchedEffect(uid) {
        fetchBoughtBids(context, uid) { bids ->
            boughtBids.clear()
            boughtBids.addAll(bids)
        }
    }

    LazyColumn {
        items(boughtBids) { bid ->
            BoughtBidItem(bid)
        }
    }
}

@Composable
fun BoughtBidItem(bid: BoughtBid) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp
    ) {

        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = bid.imageUrl,
                contentDescription = "Item Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Text("Title: ${bid.title}", style = MaterialTheme.typography.h6)
            Text("Amount: ${bid.amount}")
        }
    }
}

@Composable
fun UserItemRow(item: AuctionItemUpload, onDeleteClicked: () -> Unit, onRowClicked: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onRowClicked)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item image and details
            Column(modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "Item Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.title, style = MaterialTheme.typography.h6)
                Text(text = "Bid: ${item.startingBid}", style = MaterialTheme.typography.subtitle1)
            }

            // Delete icon
            IconButton(onClick = onDeleteClicked) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

fun fetchUserItems(context: Context, uid: String, onItemsFetched: (List<AuctionItemUpload>) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("biddingItems")

    databaseRef.orderByChild("uid").equalTo(uid).addValueEventListener(object :
        ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val items = snapshot.children.mapNotNull { it.getValue(AuctionItemUpload::class.java) }
            onItemsFetched(items)
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Failed to load items: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

fun deleteItem(itemId: String, context: Context, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("biddingItems")

    databaseRef.child(itemId).removeValue()
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun fetchBoughtBids(context: Context, uid: String, onBidsFetched: (List<BoughtBid>) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("boughtBids")

    databaseRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val bids = snapshot.children.mapNotNull { it.getValue(BoughtBid::class.java) }
            onBidsFetched(bids)
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Failed to load bids: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}
