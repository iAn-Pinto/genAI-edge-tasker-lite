package com.example.privytaskai.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.privytaskai.presentation.ui.screens.AddTaskScreen
import com.example.privytaskai.presentation.ui.screens.TaskListScreen
import com.example.privytaskai.presentation.ui.screens.SearchTaskScreen
import com.example.privytaskai.presentation.ui.theme.PrivyTaskTheme
import com.example.privytaskai.util.EmbeddingModelTester
import com.example.privytaskai.util.EmbeddingMigrationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var embeddingMigrationHelper: EmbeddingMigrationHelper

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🧪 Phase 1 Validation: Test EmbeddingGemma on app startup
        runPhase1ValidationTests()

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
                                    onTaskClick = { _ ->
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

    /**
     * Phase 1 Validation Tests
     *
     * Runs automatically on app startup to validate:
     * - EmbeddingGemma model loading
     * - Inference performance (<100ms target)
     * - Cosine similarity accuracy
     * - Task type embeddings
     *
     * Check LogCat for results:
     * adb logcat | grep "Phase1Validation\|EmbeddingGemma"
     */
    private fun runPhase1ValidationTests() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "")
                Log.d(TAG, "╔═══════════════════════════════════════════════════════════╗")
                Log.d(TAG, "║         🧪 Phase 1 Validation Tests Starting...         ║")
                Log.d(TAG, "╚═══════════════════════════════════════════════════════════╝")
                Log.d(TAG, "")

                val tester = EmbeddingModelTester(this@MainActivity)
                val allPassed = tester.runAllTests()

                Log.d(TAG, "")
                if (allPassed) {
                    Log.d(TAG, "╔═══════════════════════════════════════════════════════════╗")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "║         ✅ PHASE 1 COMPLETE - All Tests Passed! 🎉       ║")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "║  EmbeddingGemma is working correctly!                    ║")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "╚═══════════════════════════════════════════════════════════╝")

                    // Check and migrate embeddings if needed
                    checkAndMigrateEmbeddings()
                } else {
                    Log.e(TAG, "╔═══════════════════════════════════════════════════════════╗")
                    Log.e(TAG, "║                                                           ║")
                    Log.e(TAG, "║      ❌ PHASE 1 INCOMPLETE - Some Tests Failed           ║")
                    Log.e(TAG, "║                                                           ║")
                    Log.e(TAG, "║  Check the error logs above for details                  ║")
                    Log.e(TAG, "║                                                           ║")
                    Log.e(TAG, "╚═══════════════════════════════════════════════════════════╝")
                }
                Log.d(TAG, "")

            } catch (e: Exception) {
                Log.e(TAG, "")
                Log.e(TAG, "╔═══════════════════════════════════════════════════════════╗")
                Log.e(TAG, "║                                                           ║")
                Log.e(TAG, "║         💥 PHASE 1 FAILED - Exception Occurred            ║")
                Log.e(TAG, "║                                                           ║")
                Log.e(TAG, "╚═══════════════════════════════════════════════════════════╝")
                Log.e(TAG, "Error: ${e.message}", e)
                Log.e(TAG, "")
            }
        }
    }

    /**
     * Check if embeddings need migration and regenerate if needed
     * This fixes the issue where old tasks have 64-dim embeddings
     */
    private fun checkAndMigrateEmbeddings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "")
                Log.d(TAG, "🔍 Checking if embedding migration is needed...")

                val needsMigration = embeddingMigrationHelper.needsMigration()

                if (needsMigration) {
                    Log.d(TAG, "")
                    Log.d(TAG, "╔═══════════════════════════════════════════════════════════╗")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "║      🔄 Migrating Old Embeddings to EmbeddingGemma...    ║")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "║  This will regenerate embeddings for all existing tasks  ║")
                    Log.d(TAG, "║  Please wait... (may take a few minutes)                 ║")
                    Log.d(TAG, "║                                                           ║")
                    Log.d(TAG, "╚═══════════════════════════════════════════════════════════╝")
                    Log.d(TAG, "")

                    val result = embeddingMigrationHelper.regenerateAllEmbeddings()

                    result.onSuccess { count ->
                        Log.d(TAG, "")
                        Log.d(TAG, "╔═══════════════════════════════════════════════════════════╗")
                        Log.d(TAG, "║                                                           ║")
                        Log.d(TAG, "║     ✅ Embedding Migration Complete!                     ║")
                        Log.d(TAG, "║                                                           ║")
                        Log.d(TAG, "║  Regenerated $count task embeddings with EmbeddingGemma  ║")
                        Log.d(TAG, "║  Search should now work correctly!                       ║")
                        Log.d(TAG, "║                                                           ║")
                        Log.d(TAG, "╚═══════════════════════════════════════════════════════════╝")
                        Log.d(TAG, "")
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Embedding migration failed: ${error.message}", error)
                    }
                } else {
                    Log.d(TAG, "✅ All embeddings are up to date - no migration needed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during embedding migration check", e)
            }
        }
    }
}
