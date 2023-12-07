package com.test.biddingapp


import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.test.onlinestoreapp.Controller

class PostBiddingItemActivity : ComponentActivity() {
    private lateinit var image_Picker_Launcher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

                    setContent {
                        ListBiddingItemScreen()
                    }


    }

    @Composable
    fun ListBiddingItemScreen() {
        var  context= LocalContext.current;
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var uri_img by remember { mutableStateOf<Uri?>(null) }

        var startingBid by remember { mutableStateOf("") }
        val launcher_image = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri_img = uri
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "List an Item for Auction",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Item Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = startingBid,
                onValueChange = { startingBid = it },
                label = { Text("Starting Bid ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),

                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))


            if (uri_img != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(150.dp).padding(8.dp)
                )
            }

            Button(
                onClick = { launcher_image.launch("image/*")  },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Pick Image", fontSize = 18.sp)
            }


            Spacer(modifier = Modifier.height(16.dp))




            Button(
                onClick = {
                    if (uri_img != null) {
                        Controller.show_loader(context,"Uploading and posting item . . .")
                        upload_image_to_firebase_storage(uri_img!!, context, onSuccess = { downloadUri ->
                            save_bidding_item_to_database(context,title, description, startingBid, downloadUri.toString())
                        }, onFailure = { exception ->
                            Controller.hide_loader()
                            Toast.makeText(context, "Image upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        })
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && startingBid.isNotBlank() && uri_img != null
            ) {
                Text("List Item", fontSize = 18.sp)
            }
        }
    }
}


fun upload_image_to_firebase_storage(uri: Uri, context: Context, onSuccess: (Uri) -> Unit, onFailure: (Exception) -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("images/${uri.lastPathSegment}")
    val uploadTask = imageRef.putFile(uri)

    uploadTask.addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            onSuccess(downloadUri)
        }
    }.addOnFailureListener { exception ->
        onFailure(exception)
    }
}

fun save_bidding_item_to_database(context: Context,title: String, description: String, startingBid: String, imageUrl: String) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("biddingItems")
    val itemId = databaseRef.push().key
    val _timestamp = System.currentTimeMillis()

    val auctionItem = AuctionItemUpload(
        id = itemId ?: "",
        title = title,
        description = description,
        imageUrl = imageUrl,
        startingBid = startingBid.toDouble(),
        timestamp = _timestamp
    )
    itemId?.let {
        databaseRef.child(it).setValue(auctionItem).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Controller.hide_loader()

                Toast.makeText(context,"Saved Successfully!",Toast.LENGTH_LONG).show()

            } else {
                Controller.hide_loader()

            }
        }
    }
}

data class AuctionItemUpload(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    var startingBid: Double,
    val timestamp: Long
)


