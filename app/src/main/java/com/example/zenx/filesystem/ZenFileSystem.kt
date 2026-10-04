package com.example.zenx.filesystem

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZenFileSystem(context: Context) {
    val homeDir: File = File(context.filesDir, "home").apply {
        if (!exists()) {
            mkdirs()
            initializeDefaultFiles(this)
        }
    }

    var currentDir: File = homeDir
        private set

    private fun initializeDefaultFiles(home: File) {
        val readme = File(home, "README.txt")
        if (!readme.exists()) {
            readme.writeText(
                """
============================================================
              WELCOME TO ZEN X TERMINAL
============================================================
Created by: finex (finexcreates@gmail.com)
ZEN X is a full-featured Linux-like terminal for Android.

Quick Tips:
  1. Package Manager:
     - pkg install git        Install Git version control
     - pkg install python     Install Python 3 interpreter
     - pkg install node       Install Node.js runtime
     - pkg list               Show installed packages
  2. Flint AI:
     - pkg open flint         Launch interactive Flint AI
     - flint "query"          Single query to Flint
  3. Git Clone:
     - git clone https://github.com/octocat/Hello-World
  4. File Operations:
     - ls, cd, pwd, mkdir, cat, touch, rm, nano, cp, mv
  5. Tools:
     - neofetch, cowsay "hi", curl wttr.in, calc 42*8
============================================================
""".trimIndent()
            )
        }

        val demoPy = File(home, "hello.py")
        if (!demoPy.exists()) {
            demoPy.writeText(
                """
# Welcome to Python in ZEN X!
import sys

print("Hello from ZEN X Python!")
name = "Android Developer"
print(f"Welcome, {name}!")

def fib(n):
    a, b = 0, 1
    for _ in range(n):
        print(a, end=" ")
        a, b = b, a + b
    print()

print("Fibonacci sequence:")
fib(10)
""".trimIndent()
            )
        }

        val scriptSh = File(home, "welcome.sh")
        if (!scriptSh.exists()) {
            scriptSh.writeText(
                """
echo "Running ZEN X Startup Check..."
date
echo "System ready!"
""".trimIndent()
            )
        }

        val projectsDir = File(home, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
    }

    fun getDisplayPath(): String {
        val homePath = homeDir.absolutePath
        val curPath = currentDir.absolutePath
        return when {
            curPath == homePath -> "~"
            curPath.startsWith(homePath) -> "~" + curPath.substring(homePath.length)
            else -> curPath
        }
    }

    fun resolveFile(pathStr: String): File {
        val trimmed = pathStr.trim()
        return when {
            trimmed == "~" -> homeDir
            trimmed.startsWith("~/") -> File(homeDir, trimmed.substring(2))
            trimmed.startsWith("/") -> File(trimmed)
            else -> File(currentDir, trimmed).canonicalFile
        }
    }

    fun changeDirectory(pathStr: String): Result<String> {
        val target = if (pathStr.isBlank() || pathStr == "~") {
            homeDir
        } else {
            resolveFile(pathStr)
        }

        return if (target.exists() && target.isDirectory) {
            currentDir = target
            Result.success(getDisplayPath())
        } else if (!target.exists()) {
            Result.failure(Exception("cd: no such file or directory: $pathStr"))
        } else {
            Result.failure(Exception("cd: not a directory: $pathStr"))
        }
    }

    data class FileEntry(
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val permissions: String
    )

    fun listFiles(showHidden: Boolean = false, detailed: Boolean = false): List<FileEntry> {
        val files = currentDir.listFiles() ?: emptyArray()
        val sorted = files.filter { showHidden || !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))

        return sorted.map { file ->
            val perms = buildString {
                append(if (file.isDirectory) "d" else "-")
                append(if (file.canRead()) "r" else "-")
                append(if (file.canWrite()) "w" else "-")
                append(if (file.canExecute()) "x" else "-")
            }
            FileEntry(
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 4096 else file.length(),
                lastModified = file.lastModified(),
                permissions = perms
            )
        }
    }

    fun readFile(pathStr: String): Result<String> {
        val file = resolveFile(pathStr)
        return if (!file.exists()) {
            Result.failure(Exception("cat: $pathStr: No such file or directory"))
        } else if (file.isDirectory) {
            Result.failure(Exception("cat: $pathStr: Is a directory"))
        } else {
            try {
                Result.success(file.readText())
            } catch (e: Exception) {
                Result.failure(Exception("cat: cannot read $pathStr: ${e.message}"))
            }
        }
    }

    fun writeFile(pathStr: String, content: String, append: Boolean = false): Result<Unit> {
        return try {
            val file = resolveFile(pathStr)
            file.parentFile?.mkdirs()
            if (append) {
                file.appendText(content)
            } else {
                file.writeText(content)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("write failed: ${e.message}"))
        }
    }

    fun touch(pathStr: String): Result<Unit> {
        return try {
            val file = resolveFile(pathStr)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            } else {
                file.setLastModified(System.currentTimeMillis())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("touch: ${e.message}"))
        }
    }

    fun mkdir(pathStr: String, makeParents: Boolean = true): Result<Unit> {
        val file = resolveFile(pathStr)
        return if (file.exists()) {
            Result.failure(Exception("mkdir: cannot create directory '$pathStr': File exists"))
        } else {
            val created = if (makeParents) file.mkdirs() else file.mkdir()
            if (created) Result.success(Unit) else Result.failure(Exception("mkdir: failed to create '$pathStr'"))
        }
    }

    fun remove(pathStr: String, recursive: Boolean = false): Result<Unit> {
        val file = resolveFile(pathStr)
        if (!file.exists()) {
            return Result.failure(Exception("rm: cannot remove '$pathStr': No such file or directory"))
        }
        return if (file.isDirectory) {
            if (recursive) {
                if (file.deleteRecursively()) Result.success(Unit)
                else Result.failure(Exception("rm: failed to remove '$pathStr'"))
            } else {
                Result.failure(Exception("rm: cannot remove '$pathStr': Is a directory (use -r)"))
            }
        } else {
            if (file.delete()) Result.success(Unit)
            else Result.failure(Exception("rm: cannot remove '$pathStr'"))
        }
    }

    fun copy(srcStr: String, destStr: String, recursive: Boolean = false): Result<Unit> {
        val src = resolveFile(srcStr)
        val dest = resolveFile(destStr)
        if (!src.exists()) {
            return Result.failure(Exception("cp: cannot stat '$srcStr': No such file or directory"))
        }
        return try {
            val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
            if (src.isDirectory) {
                if (!recursive) {
                    Result.failure(Exception("cp: -r not specified; omitting directory '$srcStr'"))
                } else {
                    src.copyRecursively(finalDest, overwrite = true)
                    Result.success(Unit)
                }
            } else {
                src.copyTo(finalDest, overwrite = true)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("cp: ${e.message}"))
        }
    }

    fun move(srcStr: String, destStr: String): Result<Unit> {
        val src = resolveFile(srcStr)
        val dest = resolveFile(destStr)
        if (!src.exists()) {
            return Result.failure(Exception("mv: cannot stat '$srcStr': No such file or directory"))
        }
        val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
        return if (src.renameTo(finalDest)) {
            Result.success(Unit)
        } else {
            try {
                if (src.isDirectory) {
                    src.copyRecursively(finalDest, overwrite = true)
                    src.deleteRecursively()
                } else {
                    src.copyTo(finalDest, overwrite = true)
                    src.delete()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("mv: cannot move '$srcStr' to '$destStr': ${e.message}"))
            }
        }
    }

    fun buildTree(dir: File = currentDir, prefix: String = "", depth: Int = 0, maxDepth: Int = 3): String {
        if (depth > maxDepth) return "$prefix└── ...\n"
        val sb = StringBuilder()
        val files = dir.listFiles()?.filter { !it.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()
        files.forEachIndexed { index, file ->
            val isLast = index == files.lastIndex
            val connector = if (isLast) "└── " else "├── "
            val childPrefix = prefix + if (isLast) "    " else "│   "
            val display = if (file.isDirectory) file.name + "/" else file.name
            sb.append(prefix).append(connector).append(display).append("\n")
            if (file.isDirectory) {
                sb.append(buildTree(file, childPrefix, depth + 1, maxDepth))
            }
        }
        return sb.toString()
    }
}
