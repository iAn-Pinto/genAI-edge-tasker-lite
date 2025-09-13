package com.example.privytaskai.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUnitTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    
    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }
}