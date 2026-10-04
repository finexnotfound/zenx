package com.example.zenx.pkg

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay

data class ZenPackage(
    val name: String,
    val version: String,
    val description: String,
    val sizeMb: Double,
    val dependencies: List<String> = emptyList(),
    val isCore: Boolean = false
)

class PackageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zenx_packages", Context.MODE_PRIVATE)

    companion object {
        val AVAILABLE_PACKAGES = listOf(
            ZenPackage("git", "2.43.0", "Fast, scalable, distributed revision control system", 14.8, listOf("coreutils", "curl")),
            ZenPackage("flint", "1.0.0", "Intelligent terminal AI assistant powered by Gemini", 3.2, listOf("curl")),
            ZenPackage("python", "3.12.2", "Interpreted, interactive object-oriented programming language", 28.4, listOf("coreutils")),
            ZenPackage("node", "20.11.1", "JavaScript runtime built on Chrome's V8 engine", 34.6, listOf("coreutils")),
            ZenPackage("curl", "8.6.0", "Command line tool for transferring data with URLs", 4.1, isCore = true),
            ZenPackage("neofetch", "7.1.0", "Fast, highly customizable system info script", 0.8, listOf("bash")),
            ZenPackage("cowsay", "3.7.0", "Configurable talking ASCII cow CLI program", 0.4),
            ZenPackage("figlet", "2.2.5", "Program for making large letters out of ordinary text", 0.9),
            ZenPackage("tree", "2.1.1", "Recursive directory listing program", 0.6),
            ZenPackage("nano", "7.2", "Small, friendly text editor inspired by Pico", 2.4, isCore = true),
            ZenPackage("weather", "1.2.0", "Terminal weather query utility using wttr.in", 0.5, listOf("curl")),
            ZenPackage("calc", "2.14.3", "Arbitrary precision numeric calculator", 1.1),
            ZenPackage("grep", "3.11", "Pattern matching and text filtering utility", 1.5, isCore = true),
            ZenPackage("bash", "5.2.21", "GNU Bourne-Again SHell", 6.8, isCore = true),
            ZenPackage("coreutils", "9.4", "Core GNU utilities (ls, cat, mkdir, rm, cp, mv)", 8.2, isCore = true)
        )
    }

    init {
        // Initialize default core packages if not already set
        if (!prefs.contains("initialized")) {
            val initial = setOf("coreutils", "bash", "curl", "nano", "grep", "flint")
            prefs.edit().putStringSet("installed", initial).putBoolean("initialized", true).apply()
        }
    }

    fun isInstalled(pkgName: String): Boolean {
        val installed = prefs.getStringSet("installed", emptySet()) ?: emptySet()
        return installed.contains(pkgName.lowercase())
    }

    fun getInstalledPackages(): List<ZenPackage> {
        val installed = prefs.getStringSet("installed", emptySet()) ?: emptySet()
        return AVAILABLE_PACKAGES.filter { installed.contains(it.name) }
    }

    fun searchPackages(query: String): List<ZenPackage> {
        val q = query.lowercase()
        return AVAILABLE_PACKAGES.filter {
            it.name.contains(q) || it.description.lowercase().contains(q)
        }
    }

    suspend fun installPackage(
        pkgName: String,
        onOutput: suspend (String) -> Unit
    ): Boolean {
        val target = pkgName.lowercase()
        val pkg = AVAILABLE_PACKAGES.find { it.name == target }

        if (pkg == null) {
            onOutput("E: Unable to locate package '$pkgName'")
            onOutput("N: Use 'pkg search <keyword>' to search for packages.")
            return false
        }

        if (isInstalled(target)) {
            onOutput("$target is already installed (version ${pkg.version}).")
            return true
        }

        // Check dependencies
        for (dep in pkg.dependencies) {
            if (!isInstalled(dep)) {
                onOutput("Note: selecting '$dep' as dependency for '$target'")
                installPackage(dep, onOutput)
            }
        }

        onOutput("Reading package lists... Done")
        delay(120)
        onOutput("Building dependency tree... Done")
        delay(100)
        onOutput("The following NEW packages will be installed:")
        onOutput("  $target (version ${pkg.version}) [${pkg.sizeMb} MB]")
        delay(150)
        onOutput("Get:1 https://packages.zenx.org/apt/main aarch64 $target (${pkg.version}) [${pkg.sizeMb} MB]")

        // Simulate animated download
        val steps = 5
        for (i in 1..steps) {
            delay(120)
            val percent = (i * 20)
            val progressChars = "#".repeat(i * 4) + "-".repeat((steps - i) * 4)
            val loadedMb = String.format("%.1f", (pkg.sizeMb * percent) / 100.0)
            onOutput("Downloading: [$progressChars] $percent% ($loadedMb MB / ${pkg.sizeMb} MB)")
        }

        onOutput("Unpacking $target (${pkg.version})...")
        delay(150)
        onOutput("Setting up $target (${pkg.version})...")
        delay(120)
        onOutput("Creating binaries: /data/data/com.zenx/files/usr/bin/$target")

        val current = prefs.getStringSet("installed", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(target)
        prefs.edit().putStringSet("installed", current).apply()

        onOutput("Processing triggers for man-db (2.12.0)...")
        delay(100)
        onOutput("Package '$target' successfully installed! ✨")
        return true
    }

    fun uninstallPackage(pkgName: String): Result<String> {
        val target = pkgName.lowercase()
        val pkg = AVAILABLE_PACKAGES.find { it.name == target }
            ?: return Result.failure(Exception("E: Package '$pkgName' not found"))

        if (pkg.isCore) {
            return Result.failure(Exception("E: Package '$target' is an essential core package and cannot be removed"))
        }

        if (!isInstalled(target)) {
            return Result.failure(Exception("Package '$target' is not installed, so not removed"))
        }

        val current = prefs.getStringSet("installed", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(target)
        prefs.edit().putStringSet("installed", current).apply()

        return Result.success("Package '$target' (${pkg.version}) has been successfully uninstalled.")
    }
}
