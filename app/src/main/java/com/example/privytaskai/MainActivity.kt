package com.example.privytaskai.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.privytaskai.presentation.ui.screens.AddTaskScreen
import com.example.privytaskai.presentation.ui.screens.TaskListScreen
import com.example.privytaskai.presentation.ui.screens.SearchTaskScreen
import com.example.privytaskai.presentation.ui.theme.PrivyTaskTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PrivyTaskTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text(
                                "Privy Task AI",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            NavigationDrawerItem(
                                label = { Text("Add Task") },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate("add_task") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            
                            NavigationDrawerItem(
                                label = { Text("Tasks") },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate("task_list") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            
                            NavigationDrawerItem(
                                label = { Text("Search") },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate("search") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Privy Task AI") },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { 
                                            scope.launch { drawerState.open() } 
                                        }
                                    ) {
                                        Text("☰")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "add_task",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("add_task") {
                                AddTaskScreen()
                            }
                            composable("task_list") {
                                TaskListScreen(
                                    onTaskClick = { taskId ->
                                        // Handle task click navigation if needed
                                    }
                                )
                            }
                            composable("search") {
                                SearchTaskScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
