package com.kartiks.habittracker

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.kartiks.habittracker.ui.MainViewModel
import com.kartiks.habittracker.ui.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
class MainViewModelStartupTest {

    @Test
    fun testMainViewModelConstructorReflection() {
        // Assert constructor taking only Application exists
        val constructor = MainViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull("MainViewModel must expose public constructor taking android.app.Application", constructor)
        assertTrue("Constructor must be public", Modifier.isPublic(constructor.modifiers))

        // Assert exactly 1 public constructor exists
        val constructors = MainViewModel::class.java.constructors
        assertEquals("MainViewModel should have exactly one public constructor for reflection", 1, constructors.size)
        assertEquals("Constructor parameter count must be 1", 1, constructors[0].parameterCount)
        assertEquals("Constructor parameter must be Application", Application::class.java, constructors[0].parameterTypes[0])
    }

    @Test
    fun testAndroidViewModelFactoryInstantiation() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // Simulate ViewModelProvider factory instantiation as performed by 'by viewModels()' in ComponentActivity
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(MainViewModel::class.java)

        assertNotNull("ViewModelProvider.AndroidViewModelFactory must successfully instantiate MainViewModel", viewModel)
        assertNotNull("uiState must be initialized", viewModel.uiState.value)
        assertEquals("Initial tab must be TODAY", NavigationTab.TODAY, viewModel.uiState.value.selectedTab)
    }
}
