package com.test.biddingapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.database.*
import com.test.biddingapp.ui.theme.BiddingAppTheme
import com.test.onlinestoreapp.Controller
import com.test.onlinestoreapp.Controller.calculateRemainingTime
import kotlinx.coroutines.launch

class BiddingDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val biddingItem = intent.getParcelableExtra<BiddingItem>("BiddingItem")

        setContent {
            BiddingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    biddingItem?.let {
                        BiddingDetailScreen(it)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BiddingDetailScreen(biddingItem: BiddingItem) {
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var bidAmount by remember { mutableStateOf("") }
    val bids = remember { mutableStateListOf<Bid>() }
    var remainingTimeText by remember { mutableStateOf("") }
    var context = LocalContext.current
    var lastUpdatedTimestamp by remember { mutableStateOf(biddingItem.timestamp) }
    var wonBidderName by remember { mutableStateOf("") }
    val bidsLoaded = remember { mutableStateOf(false) }

    LaunchedEffect(bids) {
        if (bids.isNotEmpty()) {
            bidsLoaded.value = true
        }
    }



    DisposableEffect(Unit) {
        val onTimestampChanged: (Long) -> Unit = { newTimestamp ->
            lastUpdatedTimestamp = newTimestamp
        }
        listenForTimestampChanges(biddingItem.id, onTimestampChanged)

        onDispose {

        }
    }



    LaunchedEffect(key1 = lastUpdatedTimestamp, key2 = bidsLoaded.value) {
        while (true) {
            remainingTimeText = calculateRemainingTime(lastUpdatedTimestamp)

            if (remainingTimeText == "Bidding time is over" && bidsLoaded.value) {
                val highestBid = bids.maxByOrNull { it.bidAmount }
                wonBidderName = highestBid?.bidderName ?: "No bids"
                break
            }

            kotlinx.coroutines.delay(1000L)
        }
    }


    LaunchedEffect(Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bids/${biddingItem.id}")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bids.clear()
                snapshot.children.mapNotNullTo(bids) {
                    it.getValue(Bid::class.java)
                }
              //  bids.sortByDescending { it.bidAmount }
                bidsLoaded.value = true
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            BidInputSheet(bidAmount, onBidAmountChange = { bidAmount = it }) {
                coroutineScope.launch {
                    sheetState.hide()
                    val newBid = Bid(Controller.getUserName(context), bidAmount.toDouble(), System.currentTimeMillis())
                    val bidsReference = FirebaseDatabase.getInstance().getReference("bids/${biddingItem.id}")
                    bidsReference.push().setValue(newBid)
                        .addOnSuccessListener {

                            val updatedTimestamp = System.currentTimeMillis()
                            val updatedBiddingItem = biddingItem.copy(timestamp = updatedTimestamp)

                            val biddingItemsReference = FirebaseDatabase.getInstance().getReference("biddingItems/${biddingItem.id}")
                            biddingItemsReference.setValue(updatedBiddingItem)
                                .addOnSuccessListener {

                                    Toast.makeText(context, "Bid Placed Successfully!", Toast.LENGTH_LONG).show()

                                }
                                .addOnFailureListener { e ->

                                    Toast.makeText(context, "Failed!", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->

                        }
                }
            }
        }
    )
 {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = biddingItem.imageUrl,
                contentDescription = "Bidding Item Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Title: ${biddingItem.title}" ,  modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.h6)
            Text(text = "Description: ${biddingItem.description}" ,  modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(text = "Starting Bid: ${biddingItem.startingBid}",  modifier = Modifier.align(Alignment.CenterHorizontally))
            Button(
                onClick = {
                    coroutineScope.launch { sheetState.show() }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Place Bid")
            }
            if (remainingTimeText == "Bidding time is over") {

                Text(text = "Bidding time is over", modifier = Modifier.padding(5.dp).align(Alignment.CenterHorizontally))

                Text(text = "Won bid: $wonBidderName", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            } else {
                Text(text = "Remaining Time: $remainingTimeText", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            }


            LazyColumn {
             //   bids.reverse()
                val sortedBids = bids.sortedByDescending { it.bidAmount }
                items(sortedBids) { bid ->

                    BidItem(bid)
                }
            }
        }
    }
}

@Composable
fun BidItem(bid: Bid) {
    val bidTimeFormatted = Controller.convertLongToTime(bid.bidTime)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = bid.bidderName, style = MaterialTheme.typography.subtitle1)
                Text(text = bidTimeFormatted, style = MaterialTheme.typography.caption)
            }
            Text(text = "${bid.bidAmount}", style = MaterialTheme.typography.subtitle1)
        }
    }
}



@Composable
fun BidInputSheet(
    bidAmount: String,
    onBidAmountChange: (String) -> Unit,
    onPlaceBidClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = bidAmount,
            onValueChange = onBidAmountChange,
            label = { Text("Your Bid") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onPlaceBidClicked
        ) {
            Text("Place")
        }
    }
}


data class Bid(
    val bidderName: String = "",
    val bidAmount: Double = 0.0,
    val bidTime: Long = 0
)

fun listenForTimestampChanges(biddingItemId: String, onTimestampChanged: (Long) -> Unit) {
    val biddingItemReference = FirebaseDatabase.getInstance().getReference("biddingItems/$biddingItemId")

    biddingItemReference.child("timestamp").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val timestamp = snapshot.getValue(Long::class.java)
            timestamp?.let { onTimestampChanged(it) }
        }

        override fun onCancelled(error: DatabaseError) {

        }
    })
}




