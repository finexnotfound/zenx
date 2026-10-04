package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zenx.engine.CommandResult
import com.example.zenx.engine.ZenTerminalEngine
import com.example.zenx.filesystem.ZenFileSystem
import com.example.zenx.model.TerminalMode
import com.example.zenx.pkg.PackageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZenTerminalEngineTest {

    private lateinit var context: Context
    private lateinit var fs: ZenFileSystem
    private lateinit var pkgManager: PackageManager
    private lateinit var engine: ZenTerminalEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fs = ZenFileSystem(context)
        pkgManager = PackageManager(context)
        val flint = com.example.zenx.ai.FlintAiService()
        engine = ZenTerminalEngine(context, fs, pkgManager, flint)
    }

    @Test
    fun testFileSystemOperations() {
        val touchRes = fs.touch("test_file.txt")
        assertTrue(touchRes.isSuccess)

        val writeRes = fs.writeFile("test_file.txt", "Hello ZEN X!")
        assertTrue(writeRes.isSuccess)

        val readRes = fs.readFile("test_file.txt")
        assertTrue(readRes.isSuccess)
        assertEquals("Hello ZEN X!", readRes.getOrNull())

        val files = fs.listFiles()
        assertTrue(files.any { it.name == "test_file.txt" })
    }

    @Test
    fun testPackageManager() = runBlocking {
        assertTrue(pkgManager.isInstalled("coreutils"))
        assertTrue(pkgManager.isInstalled("bash"))

        val search = pkgManager.searchPackages("git")
        assertTrue(search.isNotEmpty())

        val installSuccess = pkgManager.installPackage("cowsay") {}
        assertTrue(installSuccess)
        assertTrue(pkgManager.isInstalled("cowsay"))
    }

    @Test
    fun testTerminalExecution() = runBlocking {
        val echoRes = engine.execute("echo 'ZEN X Rocks'", TerminalMode.SHELL)
        assertTrue(echoRes is CommandResult.Output)
        val lines = (echoRes as CommandResult.Output).lines
        assertEquals("ZEN X Rocks", lines.first().text)

        val calcRes = engine.execute("calc 25 * 4", TerminalMode.SHELL)
        assertTrue(calcRes is CommandResult.Output)
        val calcLines = (calcRes as CommandResult.Output).lines
        assertEquals("100", calcLines.first().text)

        val helpRes = engine.execute("help", TerminalMode.SHELL)
        assertTrue(helpRes is CommandResult.Output)
        assertTrue((helpRes as CommandResult.Output).lines.first().text.contains("ZEN X TERMINAL HELP"))
    }
}
