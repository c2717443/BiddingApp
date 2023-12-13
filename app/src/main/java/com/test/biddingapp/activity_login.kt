package com.test.biddingapp



import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.test.biddingapp.ui.theme.BiddingAppTheme
import com.test.onlinestoreapp.Controller

class activity_login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiddingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    FirebaseApp.initializeApp(this)
                    if (Controller.getLoginState(this)) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    }
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen() {

    var _email by remember { mutableStateOf("") }
    var _password by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally)
    {

        Text(
            text = "Login",
            fontSize = 30.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        TextField(value = _email, onValueChange = { _email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        TextField(value = _password, onValueChange = { _password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {

                if (_email.isEmpty() || _password.isEmpty()) {

                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()

                }
                else{

                    sign_in_using_auth_firebase(_email,_password,context)

                }

            }


            )

            {
                Text("Login")
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(text = "New user?", color = Color.Black, modifier = Modifier.clickable {

                val intent = Intent(context, activity_register::class.java)
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }

            }.padding(8.dp))

        }

    }
}




fun sign_in_using_auth_firebase(email: String, password: String, context: Context) {
    Controller.show_loader(context, "Logging in please wait . . .")
    val auth = FirebaseAuth.getInstance()

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser

                if (user != null) {
                    fetchUserInfoFromDatabase(user.uid, context)
                }

            } else {
                Controller.hide_loader()
                Toast.makeText(context, "Login failed. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

private fun fetchUserInfoFromDatabase(userId: String, context: Context) {
    val database = FirebaseDatabase.getInstance()
    val usersRef = database.getReference("users").child(userId)

    usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val userSnapshot = snapshot.child("userId")
                val nameSnapshot = snapshot.child("name")

                val userId = userSnapshot.value as? String
                val name = nameSnapshot.value as? String

                if (userId != null && name != null ) {
                    Controller.saveUserId(context, userId)
                    Controller.saveUserName(context, name)
                    Controller.saveLoginState(context, true)
                    Controller.hide_loader()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)

                }

                }

        }

        override fun onCancelled(error: DatabaseError) {

            Controller.hide_loader()
            Toast.makeText(context, "Failed to fetch user information.", Toast.LENGTH_SHORT).show()
        }
    })
}



