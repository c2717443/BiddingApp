package com.test.biddingapp


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.test.biddingapp.ui.theme.BiddingAppTheme
import com.test.onlinestoreapp.Controller

class activity_register : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiddingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    RegisterScreen()
                    FirebaseApp.initializeApp(this)

                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen() {

    var _name by remember { mutableStateOf("") }
    var _email by remember { mutableStateOf("") }
    var _password by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) ,verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Register",
            fontSize = 30.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        TextField(value = _name, onValueChange = { _name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))


        TextField(value = _email, onValueChange = { _email = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        TextField(value = _password, onValueChange = { _password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {

                if (_name.isEmpty() || _email.isEmpty() || _password.isEmpty()) {

                    Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()

                }
                else{

                    signUpWithEmailPassword(_name,_email,_password,context)

                }

            }


            )

            {
                Text("Register")
            }
        }


    }
}



fun signUpWithEmailPassword(name: String, email: String, password: String, context: Context) {
    Controller.show_loader(context, "Signing up please wait . . .")
    val auth = FirebaseAuth.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val user: FirebaseUser? = auth.currentUser
                val userId = user?.uid

                if (userId != null) {
                    saveUserInfoToDatabase(context,userId, name)
                }


            } else {
                Controller.hide_loader()
                Toast.makeText(context, "Signup failed. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

private fun saveUserInfoToDatabase(context: Context,userId: String, name: String) {
    val database = FirebaseDatabase.getInstance()
    val usersRef = database.getReference("users")
    val user = User(userId, name)
    usersRef.child(userId).setValue(user)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Controller.hide_loader()

                Toast.makeText(context, "Signup successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, activity_login::class.java)
                context.startActivity(intent)



                if (context is Activity) {
                    context.finish()
                }

            } else {

            }
        }}

data class User(val userId: String, val name: String)





@Preview(showBackground = true)
@Composable
fun RegisterActivityPreview() {
    BiddingAppTheme {
        RegisterScreen()
    }
}
