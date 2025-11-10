package com.example.lab_week_09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab_week_09.ui.theme.LAB_WEEK_09Theme
import com.example.lab_week_09.ui.theme.OnBackgroundTitleText
import com.example.lab_week_09.ui.theme.OnBackgroundItemText
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    // Moshi adapter for List<String>
    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val listStringType by lazy { Types.newParameterizedType(List::class.java, String::class.java) }
    private val listStringAdapter by lazy { moshi.adapter<List<String>>(listStringType) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LAB_WEEK_09Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    App(navController = navController)
                }
            }
        }
    }

    @Composable
    fun App(navController: androidx.navigation.NavHostController) {
        NavHost(navController = navController, startDestination = "home") {
            // Home route
            composable("home") {
                Home { listOfNames ->
                    // serialize to JSON, then URL-encode so safe to pass via nav args
                    val json = listStringAdapter.toJson(listOfNames)
                    val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
                    navController.navigate("resultContent/?listData=$encoded")
                }
            }

            // Result route accepting encoded JSON string
            composable(
                route = "resultContent/?listData={listData}",
                arguments = listOf(navArgument("listData") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("listData").orEmpty()
                val parsedList: List<String> = if (encoded.isNotEmpty()) {
                    val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    try {
                        listStringAdapter.fromJson(decoded) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                ResultContent(parsedList)
            }
        }
    }
}

/* ---------- Data model ---------- */
data class Student(var name: String)

/* ---------- Home (state + navigation lambda) ---------- */
@Composable
fun Home(navigateFromHomeToResult: (List<String>) -> Unit) {
    val listData = remember {
        mutableStateListOf(
            Student("Tanu"),
            Student("Tina"),
            Student("Tono")
        )
    }

    // store input as plain String (simpler and immutable)
    var inputText by remember { mutableStateOf("") }

    HomeContent(
        listData = listData,
        inputText = inputText,
        onInputValueChange = { inputText = it },
        onAddClick = {
            if (inputText.isNotBlank()) {
                listData.add(Student(inputText.trim()))
                inputText = "" // reset input
            }
        },
        onFinishClick = {
            // pass list of names (List<String>) for serialization
            navigateFromHomeToResult(listData.map { it.name })
        }
    )
}

/* ---------- HomeContent (UI) ---------- */
@Composable
fun HomeContent(
    listData: SnapshotStateList<Student>,
    inputText: String,
    onInputValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    // validation: input must not be blank
    val isInputValid = inputText.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBackgroundTitleText(text = stringResource(id = R.string.enter_item))
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = inputText,
                    onValueChange = onInputValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    placeholder = { Text(text = "e.g. Budi") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Submit button (disabled when input is blank)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = onAddClick,
                        enabled = isInputValid,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.button_click))
                    }

                    // Finish button: navigates to ResultContent
                    Button(
                        onClick = onFinishClick
                    ) {
                        Text(text = stringResource(id = R.string.button_navigate))
                    }
                }

                // optional helper text when input invalid
                if (!isInputValid) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nama tidak boleh kosong",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // list items
        items(listData) { item ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                OnBackgroundItemText(text = item.name)
            }
        }
    }
}

/* ---------- ResultContent (receives parsed list of names) ---------- */
@Composable
fun ResultContent(listData: List<String>) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxSize()
    ) {
        OnBackgroundTitleText(text = "Result Content")
        Spacer(modifier = Modifier.height(12.dp))

        if (listData.isEmpty()) {
            OnBackgroundItemText(text = "No data received")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(listData) { name ->
                    OnBackgroundItemText(text = name)
                }
            }
        }
    }
}

/* ---------- Preview ---------- */
@Preview(showBackground = true)
@Composable
fun PreviewResult() {
    LAB_WEEK_09Theme {
        ResultContent(listOf("Tanu", "Tina", "Tono"))
    }
}
