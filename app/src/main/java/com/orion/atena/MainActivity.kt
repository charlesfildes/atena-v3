package com.orion.atena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orion.atena.data.api.OrionApiService
import com.orion.atena.data.local.OrionDatabase
import com.orion.atena.ui.screen.ChatScreen
import com.orion.atena.ui.viewmodel.ChatViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = OrionDatabase.getInstance(applicationContext)
        val chatDao = database.chatDao()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(OrionApiService::class.java)

        setContent {
            val viewModel: ChatViewModel = ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ChatViewModel(apiService, chatDao) as T
                    }
                }
            )[ChatViewModel::class.java]

            ChatScreen(viewModel = viewModel)
        }
    }
}
