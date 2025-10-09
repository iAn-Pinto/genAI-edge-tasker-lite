# Phase 1.1 Implementation Guide
**Goal:** Complete Testing & Hardening (2-4 Days)  
**Target:** >80% Test Coverage + CI Ready  
**Current Status:** Foundation 85% → Target 100%

---

## Quick Start

This guide provides concrete steps to complete Phase 1.1 and prepare for Phase 2.

### Timeline Overview
- **Day 1:** Complete repository tests (4 hours)
- **Day 2:** Complete ViewModel tests + add DAO tests (6 hours)
- **Day 3:** Code coverage verification + documentation (4 hours)
- **Day 4:** Buffer for fixes + Phase 2 preparation (4 hours)

---

## Task 1: Complete Repository Tests (Day 1)

### File: `app/src/test/java/com/example/privytaskai/data/repository/TaskRepositoryImplTest.kt`

**Current State:** Framework exists, tests marked as TODO

**Implementation Steps:**

#### Test 1: Insert Task with Privacy Audit
```kotlin
@Test
fun `insertTask should audit data flow and return task id`() = runTest {
    // Given: A new task
    val task = Task(
        id = 0,
        title = "Test Task",
        description = "Test Description",
        priority = TaskPriority.MEDIUM,
        category = "Test",
        embedding = List(768) { 0.1f }  // Mock embedding
    )
    
    val taskEntity = TaskEntity(
        id = 1L,
        title = task.title,
        description = task.description,
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.MEDIUM.name,
        category = "Test"
    )
    
    // Mock DAO behavior
    whenever(taskDao.insertTask(any())).thenReturn(1L)
    
    // When: Inserting task
    val taskId = repository.insertTask(task)
    
    // Then: Task inserted and privacy audited
    assertEquals(1L, taskId)
    verify(taskDao).insertTask(any())
    verify(privacyAuditor).auditDataFlow(
        "INSERT_TASK",
        "TASK_DATA",
        "LOCAL_STORAGE"
    )
    
    // Verify embedding storage
    verify(embeddingDao).upsert(any())
}
```

#### Test 2: Search Similar Tasks
```kotlin
@Test
fun `searchSimilarTasks should validate local processing and return sorted results`() = runTest {
    // Given: Multiple tasks with embeddings
    val task1 = TaskEntity(
        id = 1L,
        title = "Machine Learning Tutorial",
        description = "Learn ML basics",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.HIGH.name,
        category = "Education"
    )
    
    val task2 = TaskEntity(
        id = 2L,
        title = "Grocery Shopping",
        description = "Buy milk and eggs",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.LOW.name,
        category = "Errands"
    )
    
    val embedding1 = EmbeddingEntity(
        taskId = 1L,
        vectorCsv = (0..767).map { 0.8f }.joinToString(",")
    )
    
    val embedding2 = EmbeddingEntity(
        taskId = 2L,
        vectorCsv = (0..767).map { 0.2f }.joinToString(",")
    )
    
    // Mock DAO responses
    whenever(taskDao.getAll()).thenReturn(listOf(task1, task2))
    whenever(embeddingDao.getAll()).thenReturn(listOf(embedding1, embedding2))
    
    // Mock embedding service
    val queryEmbedding = List(768) { 0.7f }
    whenever(embeddingService.generateEmbedding(any())).thenReturn(queryEmbedding)
    whenever(embeddingService.csvToVector(embedding1.vectorCsv))
        .thenReturn(List(768) { 0.8f })
    whenever(embeddingService.csvToVector(embedding2.vectorCsv))
        .thenReturn(List(768) { 0.2f })
    whenever(embeddingService.cosineSimilarity(any(), any()))
        .thenAnswer { invocation ->
            val vec1 = invocation.getArgument<List<Float>>(0)
            val vec2 = invocation.getArgument<List<Float>>(1)
            // Simple mock: higher similarity for similar values
            if (vec2[0] == 0.8f) 0.95f else 0.3f
        }
    
    // When: Searching for ML-related query
    val results = repository.searchSimilarTasks("artificial intelligence", limit = 10)
    
    // Then: Results sorted by similarity
    assertEquals(2, results.size)
    assertTrue(results[0].second > results[1].second) // First result more similar
    assertEquals("Machine Learning Tutorial", results[0].first.title)
    
    // Verify privacy audit
    verify(privacyAuditor).auditDataFlow(
        "SEARCH_TASKS",
        "QUERY_EMBEDDING",
        "LOCAL_PROCESSING"
    )
    verify(privacyAuditor).validateLocalProcessing("EMBEDDING_GENERATION")
}
```

#### Test 3: Get All Tasks
```kotlin
@Test
fun `getAllTasks should return flow of tasks and audit access`() = runTest {
    // Given: Tasks in database
    val taskEntities = listOf(
        TaskEntity(1L, "Task 1", "Desc 1", false, System.currentTimeMillis(), "HIGH", "Work"),
        TaskEntity(2L, "Task 2", "Desc 2", false, System.currentTimeMillis(), "LOW", "Personal")
    )
    
    val flow = flowOf(taskEntities)
    whenever(taskDao.observeAll()).thenReturn(flow)
    
    // When: Getting all tasks
    val tasks = repository.getAllTasks().first()
    
    // Then: Tasks returned
    assertEquals(2, tasks.size)
    assertEquals("Task 1", tasks[0].title)
    
    // Verify privacy audit
    verify(privacyAuditor).auditDataFlow(
        "GET_ALL_TASKS",
        "TASK_DATA",
        "LOCAL_STORAGE"
    )
}
```

---

## Task 2: Complete ViewModel Tests (Day 2 - Morning)

### File: `app/src/test/java/com/example/privytaskai/presentation/viewmodel/TaskViewModelTest.kt`

**Current State:** Framework exists, tests marked as TODO

**Implementation Steps:**

#### Test 1: Add Task Success
```kotlin
@Test
fun `addTask should update UI state when successful`() = runTest {
    // Given: Use case returns success
    val task = Task(
        title = "New Task",
        description = "Description",
        priority = TaskPriority.HIGH,
        category = "Work"
    )
    
    whenever(addTaskUseCase(task)).thenReturn(Result.success(1L))
    
    // Create ViewModel
    viewModel = TaskViewModel(addTaskUseCase, getAllTasksUseCase, searchTasksUseCase)
    
    // When: Adding task
    viewModel.addTask(task)
    advanceUntilIdle() // Process coroutine
    
    // Then: Loading state managed correctly
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.error)
    
    // Verify use case called
    verify(addTaskUseCase).invoke(task)
}
```

#### Test 2: Add Task Failure
```kotlin
@Test
fun `addTask should set error message when failed`() = runTest {
    // Given: Use case returns failure
    val task = Task(title = "Task", description = "Desc")
    val exception = Exception("Database error")
    whenever(addTaskUseCase(task)).thenReturn(Result.failure(exception))
    
    // Create ViewModel
    viewModel = TaskViewModel(addTaskUseCase, getAllTasksUseCase, searchTasksUseCase)
    
    // When: Adding task fails
    viewModel.addTask(task)
    advanceUntilIdle()
    
    // Then: Error state set
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.error)
    assertTrue(state.error?.contains("Database error") == true)
}
```

#### Test 3: Search Tasks
```kotlin
@Test
fun `searchTasks should update search results when successful`() = runTest {
    // Given: Search use case returns results
    val results = listOf(
        Pair(Task(id = 1, title = "ML Task", description = "Learn ML"), 0.95f),
        Pair(Task(id = 2, title = "AI Task", description = "Study AI"), 0.85f)
    )
    whenever(searchTasksUseCase("machine learning", 10))
        .thenReturn(Result.success(results))
    
    // Create ViewModel
    viewModel = TaskViewModel(addTaskUseCase, getAllTasksUseCase, searchTasksUseCase)
    
    // When: Searching
    viewModel.searchTasks("machine learning")
    advanceUntilIdle()
    
    // Then: Results updated
    val state = viewModel.searchState.value
    assertEquals(2, state.results.size)
    assertEquals("ML Task", state.results[0].first.title)
    assertFalse(state.isLoading)
}
```

---

## Task 3: Add DAO Tests (Day 2 - Afternoon)

### Create: `app/src/androidTest/java/com/example/privytaskai/data/database/dao/TaskDaoTest.kt`

```kotlin
package com.example.privytaskai.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.privytaskai.data.database.database.TaskDatabase
import com.example.privytaskai.data.database.entities.TaskEntity
import com.example.privytaskai.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    
    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao
    
    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }
    
    @After
    fun closeDatabase() {
        database.close()
    }
    
    @Test
    fun insertTaskAndRetrieve() = runTest {
        // Given: A task entity
        val task = TaskEntity(
            id = 0,
            title = "Test Task",
            description = "Test Description",
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            priority = TaskPriority.HIGH.name,
            category = "Test"
        )
        
        // When: Inserting task
        val taskId = taskDao.insertTask(task)
        
        // Then: Task can be retrieved
        val tasks = taskDao.getAll()
        assertEquals(1, tasks.size)
        assertEquals("Test Task", tasks[0].title)
        assertTrue(taskId > 0)
    }
    
    @Test
    fun observeTasksReturnsFlow() = runTest {
        // Given: Multiple tasks
        val task1 = TaskEntity(0, "Task 1", "Desc 1", false, 
            System.currentTimeMillis(), "HIGH", "Work")
        val task2 = TaskEntity(0, "Task 2", "Desc 2", false,
            System.currentTimeMillis(), "LOW", "Personal")
        
        taskDao.insertTask(task1)
        taskDao.insertTask(task2)
        
        // When: Observing tasks
        val tasks = taskDao.observeAll().first()
        
        // Then: All tasks returned
        assertEquals(2, tasks.size)
    }
    
    @Test
    fun updateTaskCompletionStatus() = runTest {
        // Given: An incomplete task
        val task = TaskEntity(0, "Task", "Desc", false,
            System.currentTimeMillis(), "MEDIUM", "Test")
        val taskId = taskDao.insertTask(task)
        
        // When: Updating completion status
        taskDao.updateTask(task.copy(id = taskId, isCompleted = true))
        
        // Then: Task marked as complete
        val updatedTasks = taskDao.getAll()
        assertTrue(updatedTasks[0].isCompleted)
    }
    
    @Test
    fun deleteTask() = runTest {
        // Given: A task
        val task = TaskEntity(0, "Task", "Desc", false,
            System.currentTimeMillis(), "MEDIUM", "Test")
        val taskId = taskDao.insertTask(task)
        
        // When: Deleting task
        taskDao.deleteTask(task.copy(id = taskId))
        
        // Then: Task removed
        val tasks = taskDao.getAll()
        assertEquals(0, tasks.size)
    }
}
```

### Create: `app/src/androidTest/java/com/example/privytaskai/data/database/dao/EmbeddingDaoTest.kt`

```kotlin
package com.example.privytaskai.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.privytaskai.data.database.database.TaskDatabase
import com.example.privytaskai.data.database.entities.EmbeddingEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class EmbeddingDaoTest {
    
    private lateinit var database: TaskDatabase
    private lateinit var embeddingDao: EmbeddingDao
    
    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        embeddingDao = database.embeddingDao()
    }
    
    @After
    fun closeDatabase() {
        database.close()
    }
    
    @Test
    fun insertEmbeddingAndRetrieve() = runTest {
        // Given: An embedding
        val embedding = EmbeddingEntity(
            taskId = 1L,
            vectorCsv = (0..767).map { it * 0.01f }.joinToString(",")
        )
        
        // When: Upserting embedding
        embeddingDao.upsert(embedding)
        
        // Then: Embedding can be retrieved
        val embeddings = embeddingDao.getAll()
        assertEquals(1, embeddings.size)
        assertEquals(1L, embeddings[0].taskId)
    }
    
    @Test
    fun getEmbeddingByTaskId() = runTest {
        // Given: Multiple embeddings
        embeddingDao.upsert(EmbeddingEntity(1L, "0.1,0.2,0.3"))
        embeddingDao.upsert(EmbeddingEntity(2L, "0.4,0.5,0.6"))
        
        // When: Getting specific embedding
        val embedding = embeddingDao.getByTaskId(1L)
        
        // Then: Correct embedding returned
        assertNotNull(embedding)
        assertEquals(1L, embedding?.taskId)
        assertEquals("0.1,0.2,0.3", embedding?.vectorCsv)
    }
    
    @Test
    fun upsertReplacesExistingEmbedding() = runTest {
        // Given: An existing embedding
        val original = EmbeddingEntity(1L, "0.1,0.2")
        embeddingDao.upsert(original)
        
        // When: Upserting with same taskId
        val updated = EmbeddingEntity(1L, "0.3,0.4")
        embeddingDao.upsert(updated)
        
        // Then: Embedding replaced
        val embeddings = embeddingDao.getAll()
        assertEquals(1, embeddings.size)
        assertEquals("0.3,0.4", embeddings[0].vectorCsv)
    }
}
```

---

## Task 4: Verify Code Coverage (Day 3)

### Step 1: Configure JaCoCo (if not already configured)

Add to `app/build.gradle.kts`:
```kotlin
plugins {
    id("jacoco")
}

android {
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/databinding/*",
        "**/generated/callback/*",
        "**/Hilt_*",
        "**/*_Factory*",
        "**/*_MembersInjector*"
    )
    
    val javaTree = fileTree("${project.buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val kotlinTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(listOf(javaTree, kotlinTree)))
    sourceDirectories.setFrom(files(listOf(
        "${project.projectDir}/src/main/java",
        "${project.projectDir}/src/main/kotlin"
    )))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
```

### Step 2: Generate Coverage Report

```bash
# Run tests
./gradlew testDebugUnitTest

# Generate coverage report
./gradlew jacocoTestReport

# View report
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Step 3: Verify Coverage Targets

**Target Coverage by Package:**
- `domain/` - Target: >90% (pure logic, easy to test)
- `data/repository/` - Target: >85%
- `data/database/dao/` - Target: >80%
- `presentation/viewmodel/` - Target: >80%
- `privacy/` - Target: >90% (critical for compliance)

**Overall Target:** >80% for domain and data layers combined

---

## Task 5: Documentation Updates (Day 3)

### Update README.md

Add test coverage section:
```markdown
## Test Coverage

Current coverage (as of [DATE]):
- Domain Layer: 92%
- Data Layer: 87%
- Presentation Layer: 81%
- Overall: 85%

Run tests:
```bash
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
```

Generate coverage report:
```bash
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```
```

### Update ARCHITECTURE.md

Add testing section:
```markdown
## Testing Strategy

### Unit Tests (JVM)
- **Repository Tests:** Mock DAOs and embedding service
- **ViewModel Tests:** Mock use cases
- **Use Case Tests:** Mock repository
- **Coverage Target:** >80% for domain/data layers

### Instrumentation Tests (Android)
- **DAO Tests:** In-memory Room database
- **Integration Tests:** End-to-end search functionality
- **Coverage Target:** >70% for database layer

### Running Tests
```bash
# Unit tests (fast)
./gradlew testDebugUnitTest

# Instrumentation tests (slow, requires emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew test connectedAndroidTest
```
```

---

## Task 6: Prepare for Phase 2 (Day 4)

### 6.1 Create CI Workflow File

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/android.yml`:
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
          flags: unittests
          name: codecov-umbrella
      
      - name: Run lint
        run: ./gradlew lintDebug
      
      - name: Build debug APK
        run: ./gradlew assembleDebug

  build:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      
      - name: Build release APK
        run: ./gradlew assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: app/build/outputs/apk/release/app-release-unsigned.apk
```

### 6.2 Add ProGuard Rules

Create `app/proguard-rules.pro`:
```proguard
# Add project specific ProGuard rules here.

# Uncomment this to preserve line number information for debugging
-keepattributes SourceFile,LineNumberTable

# Keep all model classes
-keep class com.example.privytaskai.domain.model.** { *; }
-keep class com.example.privytaskai.data.database.entities.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** Companion;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# SentencePiece tokenizer (if using native library)
-keep class * implements com.google.android.odml.image.BitmapExtractor { *; }
```

### 6.3 Enable R8 in build.gradle.kts

Update `app/build.gradle.kts`:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for now (replace with proper keystore)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
```

---

## Success Criteria Checklist

### Phase 1.1 Complete When:

- [ ] All repository tests implemented (3+ tests)
- [ ] All ViewModel tests implemented (3+ tests)
- [ ] DAO tests added (TaskDao + EmbeddingDao)
- [ ] Code coverage >80% for domain/data layers
- [ ] Coverage report generated and documented
- [ ] README updated with test instructions
- [ ] ARCHITECTURE.md updated with testing section
- [ ] CI workflow file created (ready for Phase 2)
- [ ] ProGuard rules added (ready for Phase 2)
- [ ] Build configuration updated for release (ready for Phase 2)

### Validation Commands

```bash
# Run all tests
./gradlew test

# Check test results
find app/build -name "TEST-*.xml" -type f

# Generate and view coverage
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Verify build works
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## Common Issues & Solutions

### Issue 1: Hilt Compilation Errors
**Solution:** Run `./gradlew clean` and rebuild. Ensure kapt is properly configured.

### Issue 2: Test Dispatcher Issues
**Solution:** Use `UnconfinedTestDispatcher()` in BaseUnitTest and call `advanceUntilIdle()` after async operations.

### Issue 3: Room Database Test Failures
**Solution:** Use in-memory database with `allowMainThreadQueries()` for faster tests.

### Issue 4: Mock Verification Failures
**Solution:** Ensure `verify()` calls match exact method signatures. Use `any()` matchers for complex objects.

### Issue 5: Flow Collection Timeouts
**Solution:** Use `flow.first()` or `flow.take(1).toList()` instead of `collect {}` in tests.

---

## Next Steps After Phase 1.1

Once Phase 1.1 is complete (all checkboxes above checked):

1. **Commit and push all changes**
2. **Verify CI workflow runs successfully** (will run automatically on push)
3. **Begin Phase 2:** Add Detekt, Ktlint, and verify release builds
4. **Update roadmap status** in README.md

---

## Resources

- [Testing Coroutines](https://developer.android.com/kotlin/coroutines/test)
- [Room Testing](https://developer.android.com/training/data-storage/room/testing-db)
- [Hilt Testing](https://developer.android.com/training/dependency-injection/hilt-testing)
- [JaCoCo Coverage](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [GitHub Actions for Android](https://github.com/actions/setup-java)

---

**Last Updated:** December 2024  
**Status:** Ready for Implementation  
**Estimated Completion:** 2-4 days
