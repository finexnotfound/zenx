package com.example.zenx.engine

import android.content.Context
import android.os.SystemClock
import com.example.zenx.ai.FlintAiService
import com.example.zenx.filesystem.ZenFileSystem
import com.example.zenx.git.GitEngine
import com.example.zenx.interpreter.SimpleNodeInterpreter
import com.example.zenx.interpreter.SimplePythonInterpreter
import com.example.zenx.model.LineType
import com.example.zenx.model.TerminalLine
import com.example.zenx.model.TerminalMode
import com.example.zenx.pkg.PackageManager
import com.example.zenx.ui.AsciiArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class CommandResult {
    data class Output(val lines: List<TerminalLine>) : CommandResult()
    data class ModeChange(val newMode: TerminalMode, val initialOutput: List<TerminalLine> = emptyList()) : CommandResult()
    data class OpenNano(val file: File, val content: String) : CommandResult()
    object Clear : CommandResult()
}

class ZenTerminalEngine(
    val context: Context,
    val fileSystem: ZenFileSystem,
    val packageManager: PackageManager,
    val flintAi: FlintAiService
) {
    val gitEngine = GitEngine(fileSystem)
    val pythonInterpreter = SimplePythonInterpreter(fileSystem)
    val nodeInterpreter = SimpleNodeInterpreter(fileSystem)

    private val environment = mutableMapOf(
        "USER" to "zenx",
        "HOME" to fileSystem.homeDir.absolutePath,
        "SHELL" to "/bin/bash",
        "TERM" to "xterm-256color",
        "PATH" to "/data/data/com.zenx/files/usr/bin:/system/bin"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val startTime = SystemClock.elapsedRealtime()

    suspend fun execute(
        rawCommand: String,
        currentMode: TerminalMode,
        onStreamingLine: suspend (TerminalLine) -> Unit = {}
    ): CommandResult {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return CommandResult.Output(emptyList())

        // Handle Flint AI Mode
        if (currentMode == TerminalMode.FLINT_AI) {
            if (trimmed.equals("exit", ignoreCase = true) || trimmed.equals("quit", ignoreCase = true)) {
                return CommandResult.ModeChange(
                    TerminalMode.SHELL,
                    listOf(TerminalLine("Exited Flint AI. Returned to ZEN X shell.", LineType.INFO))
                )
            }
            if (trimmed.equals("clear", ignoreCase = true)) {
                return CommandResult.Clear
            }

            onStreamingLine(TerminalLine("Flint is thinking...", LineType.INFO))
            val response = flintAi.askFlint(trimmed, "Current path: ${fileSystem.getDisplayPath()}")
            return CommandResult.Output(
                listOf(
                    TerminalLine(response, LineType.AI_RESPONSE)
                )
            )
        }

        // Handle Python REPL Mode
        if (currentMode == TerminalMode.PYTHON_REPL) {
            if (trimmed == "exit()" || trimmed == "quit()") {
                return CommandResult.ModeChange(TerminalMode.SHELL, listOf(TerminalLine("Exited Python REPL.", LineType.INFO)))
            }
            val res = pythonInterpreter.evaluateLine(trimmed)
            return CommandResult.Output(if (res.isNotBlank()) listOf(TerminalLine(res, LineType.OUTPUT)) else emptyList())
        }

        // Handle Node REPL Mode
        if (currentMode == TerminalMode.NODE_REPL) {
            if (trimmed == ".exit" || trimmed == "exit") {
                return CommandResult.ModeChange(TerminalMode.SHELL, listOf(TerminalLine("Exited Node REPL.", LineType.INFO)))
            }
            val res = nodeInterpreter.evaluateLine(trimmed)
            return CommandResult.Output(listOf(TerminalLine(res, LineType.OUTPUT)))
        }

        // Shell redirection: command > file or command >> file
        var commandToRun = trimmed
        var redirectFile: String? = null
        var appendRedirect = false

        if (trimmed.contains(">>")) {
            val parts = trimmed.split(">>", limit = 2)
            commandToRun = parts[0].trim()
            redirectFile = parts[1].trim()
            appendRedirect = true
        } else if (trimmed.contains(">")) {
            val parts = trimmed.split(">", limit = 2)
            commandToRun = parts[0].trim()
            redirectFile = parts[1].trim()
            appendRedirect = false
        }

        val tokens = tokenizeCommand(commandToRun)
        if (tokens.isEmpty()) return CommandResult.Output(emptyList())

        val cmd = tokens[0].lowercase(Locale.ROOT)
        val args = tokens.drop(1)

        val result = executeShellCommand(cmd, args, onStreamingLine)

        // Handle file redirection if present
        if (redirectFile != null && result is CommandResult.Output) {
            val combinedText = result.lines.joinToString("\n") { it.text }
            val writeRes = fileSystem.writeFile(redirectFile, combinedText + "\n", append = appendRedirect)
            return if (writeRes.isSuccess) {
                CommandResult.Output(emptyList())
            } else {
                CommandResult.Output(listOf(TerminalLine("bash: ${writeRes.exceptionOrNull()?.message}", LineType.ERROR)))
            }
        }

        return result
    }

    private suspend fun executeShellCommand(
        cmd: String,
        args: List<String>,
        onStreamingLine: suspend (TerminalLine) -> Unit
    ): CommandResult {
        return when (cmd) {
            "clear" -> CommandResult.Clear

            "help" -> CommandResult.Output(listOf(TerminalLine(getHelpText(), LineType.INFO)))

            "pkg", "apt" -> handlePkgCommand(args, onStreamingLine)

            "flint" -> handleFlintCommand(args, onStreamingLine)

            "git" -> handleGitCommand(args, onStreamingLine)

            "ls" -> handleLsCommand(args)

            "cd" -> handleCdCommand(args)

            "pwd" -> CommandResult.Output(listOf(TerminalLine(fileSystem.currentDir.absolutePath, LineType.OUTPUT)))

            "mkdir" -> handleMkdirCommand(args)

            "touch" -> handleTouchCommand(args)

            "cat" -> handleCatCommand(args)

            "rm" -> handleRmCommand(args)

            "cp" -> handleCpCommand(args)

            "mv" -> handleMvCommand(args)

            "tree" -> handleTreeCommand(args)

            "nano", "edit" -> handleNanoCommand(args)

            "echo" -> handleEchoCommand(args)

            "neofetch" -> handleNeofetchCommand()

            "cowsay" -> handleCowsayCommand(args)

            "figlet" -> handleFigletCommand(args)

            "curl" -> handleCurlCommand(args)

            "weather" -> handleWeatherCommand(args)

            "calc" -> handleCalcCommand(args)

            "python", "python3" -> handlePythonCommand(args)

            "node" -> handleNodeCommand(args)

            "whoami" -> CommandResult.Output(listOf(TerminalLine(environment["USER"] ?: "zenx", LineType.OUTPUT)))

            "date" -> CommandResult.Output(listOf(TerminalLine(Date().toString(), LineType.OUTPUT)))

            "uname" -> handleUnameCommand(args)

            "uptime" -> handleUptimeCommand()

            "free" -> handleFreeCommand()

            "df" -> handleDfCommand()

            "grep" -> handleGrepCommand(args)

            "head" -> handleHeadCommand(args)

            "tail" -> handleTailCommand(args)

            "export" -> handleExportCommand(args)

            "env" -> handleEnvCommand()

            else -> {
                // Check if it's an executable file in current dir: ./script.sh or ./script.py
                if (cmd.startsWith("./")) {
                    val fileArg = cmd.removePrefix("./")
                    val file = fileSystem.resolveFile(fileArg)
                    if (file.exists()) {
                        if (file.name.endsWith(".py")) {
                            return CommandResult.Output(listOf(TerminalLine(pythonInterpreter.executeScript(fileArg), LineType.OUTPUT)))
                        } else {
                            val content = file.readLines()
                            val outputs = mutableListOf<TerminalLine>()
                            for (line in content) {
                                if (line.isNotBlank() && !line.startsWith("#")) {
                                    val subRes = execute(line, TerminalMode.SHELL)
                                    if (subRes is CommandResult.Output) {
                                        outputs.addAll(subRes.lines)
                                    }
                                }
                            }
                            return CommandResult.Output(outputs)
                        }
                    }
                }
                CommandResult.Output(
                    listOf(
                        TerminalLine(
                            "bash: $cmd: command not found. Type 'pkg search $cmd' or 'help' to find packages.",
                            LineType.ERROR
                        )
                    )
                )
            }
        }
    }

    private suspend fun handlePkgCommand(
        args: List<String>,
        onStreamingLine: suspend (TerminalLine) -> Unit
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(
                listOf(
                    TerminalLine("Usage: pkg [install|uninstall|list|search|open] [package]", LineType.INFO),
                    TerminalLine("Examples:", LineType.INFO),
                    TerminalLine("  pkg install git", LineType.INFO),
                    TerminalLine("  pkg open flint", LineType.INFO),
                    TerminalLine("  pkg list", LineType.INFO)
                )
            )
        }

        val sub = args[0].lowercase(Locale.ROOT)
        return when (sub) {
            "install", "add" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("pkg: missing package name. Example: pkg install git", LineType.ERROR)))
                }
                val pkgName = args[1].lowercase()
                val lines = mutableListOf<TerminalLine>()
                val success = packageManager.installPackage(pkgName) { line ->
                    val tLine = TerminalLine(line, LineType.INFO)
                    lines.add(tLine)
                    onStreamingLine(tLine)
                }
                CommandResult.Output(lines)
            }

            "open", "run" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("Usage: pkg open [flint|python|node]", LineType.ERROR)))
                }
                val target = args[1].lowercase()
                when (target) {
                    "flint" -> {
                        val welcomeLines = listOf(
                            TerminalLine(AsciiArt.FLINT_BANNER, LineType.AI_RESPONSE),
                            TerminalLine("Flint: Hello! I'm Flint, your terminal AI assistant. How can I help you?", LineType.AI_RESPONSE),
                            TerminalLine("Type your questions or type 'exit' to return to ZEN X bash.", LineType.INFO)
                        )
                        CommandResult.ModeChange(TerminalMode.FLINT_AI, welcomeLines)
                    }
                    "python", "py" -> {
                        CommandResult.ModeChange(
                            TerminalMode.PYTHON_REPL,
                            listOf(TerminalLine("Python 3.12.2 (main) [Clang 17.0.2] on linux\nType \"help\", \"copyright\", or \"exit()\" for more information.", LineType.INFO))
                        )
                    }
                    "node", "js" -> {
                        CommandResult.ModeChange(
                            TerminalMode.NODE_REPL,
                            listOf(TerminalLine("Welcome to Node.js v20.11.1.\nType \".help\" or \".exit\" to quit.", LineType.INFO))
                        )
                    }
                    else -> {
                        CommandResult.Output(listOf(TerminalLine("pkg: cannot open '$target'. Try 'pkg open flint'", LineType.ERROR)))
                    }
                }
            }

            "uninstall", "remove" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("pkg: missing package name to uninstall", LineType.ERROR)))
                }
                val res = packageManager.uninstallPackage(args[1])
                if (res.isSuccess) {
                    CommandResult.Output(listOf(TerminalLine(res.getOrThrow(), LineType.SUCCESS)))
                } else {
                    CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "Error", LineType.ERROR)))
                }
            }

            "list", "installed" -> {
                val installed = packageManager.getInstalledPackages()
                val lines = mutableListOf<TerminalLine>()
                lines.add(TerminalLine("Listing installed packages (${installed.size} total):", LineType.INFO))
                installed.forEach { pkg ->
                    lines.add(TerminalLine("  ${pkg.name} / v${pkg.version} [${pkg.sizeMb} MB] - ${pkg.description}", LineType.OUTPUT))
                }
                CommandResult.Output(lines)
            }

            "search" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("Usage: pkg search <query>", LineType.INFO)))
                }
                val q = args[1]
                val matches = packageManager.searchPackages(q)
                val lines = mutableListOf<TerminalLine>()
                lines.add(TerminalLine("Search results for '$q':", LineType.INFO))
                if (matches.isEmpty()) {
                    lines.add(TerminalLine("  No packages found matching '$q'", LineType.OUTPUT))
                } else {
                    matches.forEach { pkg ->
                        val status = if (packageManager.isInstalled(pkg.name)) "[installed]" else "[available]"
                        lines.add(TerminalLine("  ${pkg.name} ($status) - ${pkg.description}", LineType.OUTPUT))
                    }
                }
                CommandResult.Output(lines)
            }

            else -> {
                CommandResult.Output(listOf(TerminalLine("Unknown pkg action: $sub. Try 'pkg help'", LineType.ERROR)))
            }
        }
    }

    private suspend fun handleFlintCommand(
        args: List<String>,
        onStreamingLine: suspend (TerminalLine) -> Unit
    ): CommandResult {
        if (!packageManager.isInstalled("flint")) {
            return CommandResult.Output(
                listOf(
                    TerminalLine("flint: command not found. Run 'pkg install flint' first!", LineType.ERROR)
                )
            )
        }

        if (args.isEmpty()) {
            val welcomeLines = listOf(
                TerminalLine(AsciiArt.FLINT_BANNER, LineType.AI_RESPONSE),
                TerminalLine("Flint: Hello! I'm Flint, your terminal AI companion.", LineType.AI_RESPONSE),
                TerminalLine("Ask questions, generate bash scripts, or type 'exit' to return to shell.", LineType.INFO)
            )
            return CommandResult.ModeChange(TerminalMode.FLINT_AI, welcomeLines)
        }

        val query = args.joinToString(" ")
        onStreamingLine(TerminalLine("Flint is analyzing...", LineType.INFO))
        val response = flintAi.askFlint(query, "Terminal single-command query.")
        return CommandResult.Output(listOf(TerminalLine(response, LineType.AI_RESPONSE)))
    }

    private suspend fun handleGitCommand(
        args: List<String>,
        onStreamingLine: suspend (TerminalLine) -> Unit
    ): CommandResult {
        if (!packageManager.isInstalled("git")) {
            return CommandResult.Output(
                listOf(
                    TerminalLine("git: command not found.", LineType.ERROR),
                    TerminalLine("Install it by typing: pkg install git", LineType.INFO)
                )
            )
        }

        if (args.isEmpty()) {
            return CommandResult.Output(
                listOf(
                    TerminalLine("usage: git [--version] <command> [<args>]", LineType.INFO),
                    TerminalLine("common Git commands in ZEN X:", LineType.INFO),
                    TerminalLine("   clone      Clone a repository into a new directory", LineType.OUTPUT),
                    TerminalLine("   init       Create an empty Git repository", LineType.OUTPUT),
                    TerminalLine("   status     Show the working tree status", LineType.OUTPUT),
                    TerminalLine("   add        Add file contents to the index", LineType.OUTPUT),
                    TerminalLine("   commit     Record changes to the repository", LineType.OUTPUT),
                    TerminalLine("   log        Show commit logs", LineType.OUTPUT)
                )
            )
        }

        val sub = args[0]
        return when (sub) {
            "clone" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("fatal: You must specify a repository to clone.", LineType.ERROR)))
                }
                val repoUrl = args[1]
                val targetDir = args.getOrNull(2)
                val lines = mutableListOf<TerminalLine>()
                val res = gitEngine.clone(repoUrl, targetDir) { line ->
                    val tLine = TerminalLine(line, LineType.INFO)
                    lines.add(tLine)
                    onStreamingLine(tLine)
                }
                if (res.isSuccess) {
                    lines.add(TerminalLine("Done! Cloned repository ready.", LineType.SUCCESS))
                } else {
                    lines.add(TerminalLine(res.exceptionOrNull()?.message ?: "Clone failed", LineType.ERROR))
                }
                CommandResult.Output(lines)
            }

            "init" -> {
                val res = gitEngine.initRepo(args.getOrNull(1))
                CommandResult.Output(listOf(TerminalLine(res.getOrThrow(), LineType.SUCCESS)))
            }

            "status" -> {
                val res = gitEngine.status()
                if (res.isSuccess) {
                    CommandResult.Output(listOf(TerminalLine(res.getOrThrow(), LineType.OUTPUT)))
                } else {
                    CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "Error", LineType.ERROR)))
                }
            }

            "add" -> {
                if (args.size < 2) {
                    return CommandResult.Output(listOf(TerminalLine("Nothing specified, nothing added.", LineType.ERROR)))
                }
                gitEngine.add(args.drop(1).joinToString(" "))
                CommandResult.Output(emptyList())
            }

            "commit" -> {
                var message = "Update"
                val mIdx = args.indexOf("-m")
                if (mIdx != -1 && mIdx + 1 < args.size) {
                    message = args.subList(mIdx + 1, args.size).joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
                }
                val res = gitEngine.commit(message)
                if (res.isSuccess) {
                    CommandResult.Output(listOf(TerminalLine(res.getOrThrow(), LineType.SUCCESS)))
                } else {
                    CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "Commit failed", LineType.ERROR)))
                }
            }

            "log" -> {
                val res = gitEngine.log()
                if (res.isSuccess) {
                    CommandResult.Output(listOf(TerminalLine(res.getOrThrow(), LineType.OUTPUT)))
                } else {
                    CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "Error", LineType.ERROR)))
                }
            }

            "branch" -> {
                CommandResult.Output(listOf(TerminalLine("* main", LineType.SUCCESS)))
            }

            "--version", "version" -> {
                CommandResult.Output(listOf(TerminalLine("git version 2.43.0", LineType.OUTPUT)))
            }

            else -> {
                CommandResult.Output(listOf(TerminalLine("git: '$sub' is not a git command. See 'git --help'.", LineType.ERROR)))
            }
        }
    }

    private fun handleLsCommand(args: List<String>): CommandResult {
        var showHidden = false
        var detailed = false

        for (arg in args) {
            if (arg.startsWith("-")) {
                if (arg.contains("a")) showHidden = true
                if (arg.contains("l")) detailed = true
            }
        }

        val entries = fileSystem.listFiles(showHidden = showHidden, detailed = detailed)
        if (entries.isEmpty()) return CommandResult.Output(emptyList())

        val lines = mutableListOf<TerminalLine>()
        if (detailed) {
            val totalBlocks = entries.size * 4
            lines.add(TerminalLine("total $totalBlocks", LineType.INFO))
            val sdf = SimpleDateFormat("MMM d HH:mm", Locale.US)
            entries.forEach { e ->
                val dateStr = sdf.format(Date(e.lastModified))
                val sizeStr = String.format("%5d", e.size)
                val type = if (e.isDirectory) LineType.DIRECTORY else LineType.OUTPUT
                lines.add(TerminalLine("${e.permissions} 1 zenx zenx $sizeStr $dateStr ${e.name}", type))
            }
        } else {
            val formatted = entries.joinToString("  ") { e ->
                if (e.isDirectory) e.name + "/" else e.name
            }
            lines.add(TerminalLine(formatted, LineType.OUTPUT))
        }

        return CommandResult.Output(lines)
    }

    private fun handleCdCommand(args: List<String>): CommandResult {
        val target = args.getOrElse(0) { "~" }
        val res = fileSystem.changeDirectory(target)
        return if (res.isSuccess) {
            CommandResult.Output(emptyList())
        } else {
            CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "cd failed", LineType.ERROR)))
        }
    }

    private fun handleMkdirCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(listOf(TerminalLine("mkdir: missing operand", LineType.ERROR)))
        }
        val target = args.last()
        val res = fileSystem.mkdir(target)
        return if (res.isSuccess) {
            CommandResult.Output(emptyList())
        } else {
            CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "mkdir failed", LineType.ERROR)))
        }
    }

    private fun handleTouchCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(listOf(TerminalLine("touch: missing file operand", LineType.ERROR)))
        }
        for (f in args) {
            fileSystem.touch(f)
        }
        return CommandResult.Output(emptyList())
    }

    private fun handleCatCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(listOf(TerminalLine("cat: missing operand", LineType.ERROR)))
        }
        val lines = mutableListOf<TerminalLine>()
        for (f in args) {
            val res = fileSystem.readFile(f)
            if (res.isSuccess) {
                lines.add(TerminalLine(res.getOrThrow(), LineType.OUTPUT))
            } else {
                lines.add(TerminalLine(res.exceptionOrNull()?.message ?: "cat error", LineType.ERROR))
            }
        }
        return CommandResult.Output(lines)
    }

    private fun handleRmCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(listOf(TerminalLine("rm: missing operand", LineType.ERROR)))
        }
        val recursive = args.any { it == "-r" || it == "-rf" || it == "-fr" }
        val targets = args.filter { !it.startsWith("-") }
        val errors = mutableListOf<TerminalLine>()
        for (t in targets) {
            val res = fileSystem.remove(t, recursive)
            if (res.isFailure) {
                errors.add(TerminalLine(res.exceptionOrNull()?.message ?: "rm error", LineType.ERROR))
            }
        }
        return CommandResult.Output(errors)
    }

    private fun handleCpCommand(args: List<String>): CommandResult {
        val targets = args.filter { !it.startsWith("-") }
        val recursive = args.any { it == "-r" || it == "-rf" }
        if (targets.size < 2) {
            return CommandResult.Output(listOf(TerminalLine("cp: missing file operand", LineType.ERROR)))
        }
        val res = fileSystem.copy(targets[0], targets[1], recursive)
        return if (res.isSuccess) CommandResult.Output(emptyList())
        else CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "cp failed", LineType.ERROR)))
    }

    private fun handleMvCommand(args: List<String>): CommandResult {
        if (args.size < 2) {
            return CommandResult.Output(listOf(TerminalLine("mv: missing destination file operand", LineType.ERROR)))
        }
        val res = fileSystem.move(args[0], args[1])
        return if (res.isSuccess) CommandResult.Output(emptyList())
        else CommandResult.Output(listOf(TerminalLine(res.exceptionOrNull()?.message ?: "mv failed", LineType.ERROR)))
    }

    private fun handleTreeCommand(args: List<String>): CommandResult {
        val target = if (args.isNotEmpty()) fileSystem.resolveFile(args[0]) else fileSystem.currentDir
        val treeStr = fileSystem.buildTree(target)
        return CommandResult.Output(listOf(TerminalLine(treeStr.ifBlank { ".\n└── (empty)" }, LineType.OUTPUT)))
    }

    private fun handleNanoCommand(args: List<String>): CommandResult {
        val fileName = args.getOrElse(0) { "untitled.txt" }
        val target = fileSystem.resolveFile(fileName)
        val initialContent = if (target.exists()) target.readText() else ""
        return CommandResult.OpenNano(target, initialContent)
    }

    private fun handleEchoCommand(args: List<String>): CommandResult {
        val text = args.joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
        return CommandResult.Output(listOf(TerminalLine(text, LineType.OUTPUT)))
    }

    private fun handleNeofetchCommand(): CommandResult {
        if (!packageManager.isInstalled("neofetch")) {
            return CommandResult.Output(listOf(TerminalLine("neofetch: not installed. Run 'pkg install neofetch'", LineType.ERROR)))
        }
        val uptimeSec = (SystemClock.elapsedRealtime() - startTime) / 1000
        val uptimeStr = "${uptimeSec / 60}m ${uptimeSec % 60}s"
        val memMax = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
        val memStr = "${memUsed}MiB / ${memMax}MiB"
        val pkgCount = packageManager.getInstalledPackages().size
        val ascii = AsciiArt.neofetch(android.os.Build.MODEL, uptimeStr, memStr, pkgCount)
        return CommandResult.Output(listOf(TerminalLine(ascii, LineType.INFO)))
    }

    private fun handleCowsayCommand(args: List<String>): CommandResult {
        if (!packageManager.isInstalled("cowsay")) {
            return CommandResult.Output(listOf(TerminalLine("cowsay: not installed. Run 'pkg install cowsay'", LineType.ERROR)))
        }
        val msg = if (args.isNotEmpty()) args.joinToString(" ") else "ZEN X is awesome!"
        return CommandResult.Output(listOf(TerminalLine(AsciiArt.cowsay(msg), LineType.OUTPUT)))
    }

    private fun handleFigletCommand(args: List<String>): CommandResult {
        val text = if (args.isNotEmpty()) args.joinToString(" ") else "ZEN X"
        val banner = buildFiglet(text)
        return CommandResult.Output(listOf(TerminalLine(banner, LineType.OUTPUT)))
    }

    private fun buildFiglet(text: String): String {
        return """
  _   _   _   _   _   _   _  
 / \ / \ / \ / \ / \ / \ / \ 
( $text )
 \_/ \_/ \_/ \_/ \_/ \_/ \_/ 
"""
    }

    private suspend fun handleCurlCommand(args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext CommandResult.Output(listOf(TerminalLine("curl: try 'curl --help' for more information", LineType.ERROR)))
        }
        var url = args[0]
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        try {
            val req = Request.Builder().url(url).header("User-Agent", "curl/8.6.0").build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            val preview = body.take(4000)
            CommandResult.Output(listOf(TerminalLine(preview, LineType.OUTPUT)))
        } catch (e: Exception) {
            CommandResult.Output(listOf(TerminalLine("curl: (6) Could not resolve host or error: ${e.message}", LineType.ERROR)))
        }
    }

    private suspend fun handleWeatherCommand(args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        val city = if (args.isNotEmpty()) args[0] else ""
        val url = "https://wttr.in/$city?format=3"
        try {
            val req = Request.Builder().url(url).header("User-Agent", "curl/8.6.0").build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()?.trim() ?: "Weather unavailable"
            CommandResult.Output(listOf(TerminalLine(body, LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult.Output(listOf(TerminalLine("Weather: Paris: +18°C ⛅ Partly cloudy (offline cache)", LineType.INFO)))
        }
    }

    private fun handleCalcCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.Output(listOf(TerminalLine("Usage: calc <expression> (e.g. calc 24 * 7)", LineType.INFO)))
        }
        val expr = args.joinToString(" ")
        val res = pythonInterpreter.evaluateLine(expr)
        return CommandResult.Output(listOf(TerminalLine(res, LineType.OUTPUT)))
    }

    private fun handlePythonCommand(args: List<String>): CommandResult {
        if (!packageManager.isInstalled("python")) {
            return CommandResult.Output(listOf(TerminalLine("python: not installed. Run 'pkg install python'", LineType.ERROR)))
        }
        if (args.isEmpty()) {
            return CommandResult.ModeChange(
                TerminalMode.PYTHON_REPL,
                listOf(TerminalLine("Python 3.12.2 (main) [Clang 17.0.2] on linux\nType \"help\", \"copyright\", or \"exit()\" for more information.", LineType.INFO))
            )
        }
        val fileArg = args[0]
        val output = pythonInterpreter.executeScript(fileArg)
        return CommandResult.Output(listOf(TerminalLine(output, LineType.OUTPUT)))
    }

    private fun handleNodeCommand(args: List<String>): CommandResult {
        if (!packageManager.isInstalled("node")) {
            return CommandResult.Output(listOf(TerminalLine("node: not installed. Run 'pkg install node'", LineType.ERROR)))
        }
        if (args.isEmpty()) {
            return CommandResult.ModeChange(
                TerminalMode.NODE_REPL,
                listOf(TerminalLine("Welcome to Node.js v20.11.1.\nType \".help\" or \".exit\" to quit.", LineType.INFO))
            )
        }
        val fileArg = args[0]
        val output = nodeInterpreter.executeScript(fileArg)
        return CommandResult.Output(listOf(TerminalLine(output, LineType.OUTPUT)))
    }

    private fun handleUnameCommand(args: List<String>): CommandResult {
        return CommandResult.Output(listOf(TerminalLine("Linux localhost 6.1.75-zenx-aarch64 #1 SMP PREEMPT Android 15 aarch64 Android", LineType.OUTPUT)))
    }

    private fun handleUptimeCommand(): CommandResult {
        val uptimeSec = (SystemClock.elapsedRealtime() - startTime) / 1000
        return CommandResult.Output(listOf(TerminalLine(" $uptimeSec seconds,  load average: 0.12, 0.08, 0.04", LineType.OUTPUT)))
    }

    private fun handleFreeCommand(): CommandResult {
        val rt = Runtime.getRuntime()
        val total = rt.totalMemory() / 1024
        val free = rt.freeMemory() / 1024
        val used = total - free
        return CommandResult.Output(
            listOf(
                TerminalLine("               total        used        free      shared  buff/cache   available", LineType.OUTPUT),
                TerminalLine("Mem:        ${total}K     ${used}K     ${free}K          0K     32000K     ${free + 32000}K", LineType.OUTPUT),
                TerminalLine("Swap:             0K          0K          0K", LineType.OUTPUT)
            )
        )
    }

    private fun handleDfCommand(): CommandResult {
        return CommandResult.Output(
            listOf(
                TerminalLine("Filesystem     1K-blocks      Used Available Use% Mounted on", LineType.OUTPUT),
                TerminalLine("/dev/root       61257408  18432128  42825280  31% /", LineType.OUTPUT),
                TerminalLine("/data/user/0   128450560  45120100  83330460  36% /data", LineType.OUTPUT)
            )
        )
    }

    private fun handleGrepCommand(args: List<String>): CommandResult {
        if (args.size < 2) {
            return CommandResult.Output(listOf(TerminalLine("Usage: grep <pattern> <file>", LineType.ERROR)))
        }
        val pattern = args[0]
        val fileArg = args[1]
        val file = fileSystem.resolveFile(fileArg)
        if (!file.exists()) {
            return CommandResult.Output(listOf(TerminalLine("grep: $fileArg: No such file or directory", LineType.ERROR)))
        }
        val matches = file.readLines().filter { it.contains(pattern, ignoreCase = true) }
        return CommandResult.Output(matches.map { TerminalLine(it, LineType.OUTPUT) })
    }

    private fun handleHeadCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) return CommandResult.Output(listOf(TerminalLine("head: missing operand", LineType.ERROR)))
        val file = fileSystem.resolveFile(args.last())
        if (!file.exists()) return CommandResult.Output(listOf(TerminalLine("head: ${args.last()}: No such file", LineType.ERROR)))
        val lines = file.readLines().take(10)
        return CommandResult.Output(lines.map { TerminalLine(it, LineType.OUTPUT) })
    }

    private fun handleTailCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) return CommandResult.Output(listOf(TerminalLine("tail: missing operand", LineType.ERROR)))
        val file = fileSystem.resolveFile(args.last())
        if (!file.exists()) return CommandResult.Output(listOf(TerminalLine("tail: ${args.last()}: No such file", LineType.ERROR)))
        val lines = file.readLines().takeLast(10)
        return CommandResult.Output(lines.map { TerminalLine(it, LineType.OUTPUT) })
    }

    private fun handleExportCommand(args: List<String>): CommandResult {
        if (args.isEmpty()) return handleEnvCommand()
        val arg = args[0]
        if (arg.contains("=")) {
            val parts = arg.split("=", limit = 2)
            environment[parts[0].trim()] = parts[1].trim()
        }
        return CommandResult.Output(emptyList())
    }

    private fun handleEnvCommand(): CommandResult {
        val lines = environment.map { "${it.key}=${it.value}" }
        return CommandResult.Output(lines.map { TerminalLine(it, LineType.OUTPUT) })
    }

    fun completeInput(input: String): List<String> {
        val trimmed = input.trimStart()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.split(" ")
        if (tokens.size == 1) {
            val allCommands = listOf(
                "pkg", "flint", "git", "ls", "cd", "pwd", "mkdir", "touch", "cat",
                "rm", "cp", "mv", "tree", "nano", "echo", "neofetch", "cowsay",
                "figlet", "curl", "weather", "calc", "python", "node", "clear", "help",
                "whoami", "date", "uname", "uptime", "free", "df", "grep", "history"
            )
            return allCommands.filter { it.startsWith(tokens[0], ignoreCase = true) }
        }

        // Complete pkg arguments
        if (tokens[0] == "pkg" && tokens.size == 2) {
            return listOf("install", "open", "list", "search", "uninstall")
                .filter { it.startsWith(tokens[1], ignoreCase = true) }
        }
        if (tokens[0] == "pkg" && tokens[1] == "install" && tokens.size == 3) {
            return PackageManager.AVAILABLE_PACKAGES.map { it.name }
                .filter { it.startsWith(tokens[2], ignoreCase = true) }
        }
        if (tokens[0] == "pkg" && tokens[1] == "open" && tokens.size == 3) {
            return listOf("flint", "python", "node").filter { it.startsWith(tokens[2], ignoreCase = true) }
        }

        // Complete git commands
        if (tokens[0] == "git" && tokens.size == 2) {
            return listOf("clone", "init", "status", "add", "commit", "log", "branch")
                .filter { it.startsWith(tokens[1], ignoreCase = true) }
        }

        // File/Directory auto-complete
        val lastToken = tokens.last()
        val files = fileSystem.currentDir.listFiles() ?: emptyArray()
        return files.map { if (it.isDirectory) it.name + "/" else it.name }
            .filter { it.startsWith(lastToken, ignoreCase = true) }
    }

    private fun tokenizeCommand(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (c in line) {
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
            } else if (c.isWhitespace() && !inQuotes) {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                    current.clear()
                }
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun getHelpText(): String {
        return """
╔═══════════════════════════════════════════════════════════╗
║                   ZEN X TERMINAL HELP                     ║
╚═══════════════════════════════════════════════════════════╝
Package Manager:
  pkg install <pkg>       Install package (git, python, node, tree, cowsay...)
  pkg open flint          Open Flint AI terminal assistant
  pkg list                List all installed packages
  pkg search <query>      Search package repository
  pkg uninstall <pkg>     Remove a package

Flint AI Assistant:
  flint                   Launch interactive AI prompt
  flint "<prompt>"        Ask Flint a quick question
  pkg open flint          Open Flint AI from anywhere

Git Version Control:
  git clone <repo_url>    Clone any GitHub repository
  git init [dir]          Initialize a repository
  git status              Show working tree status
  git add <file>          Stage changes
  git commit -m "<msg>"   Record changes
  git log                 Show commit history

File & Directory Operations:
  ls [-l, -a]             List files & folders
  cd <dir>                Change directory (cd ~, cd .., cd /path)
  pwd                     Print current working directory
  mkdir [-p] <dir>        Create new directory
  touch <file>            Create empty file
  cat <file>              Display file content
  rm [-r] <file/dir>      Remove file or directory
  cp [-r] <src> <dest>    Copy files
  mv <src> <dest>         Move or rename files
  tree                    Visual directory tree
  nano <file>             Interactive terminal text editor

Tools & Interpreters:
  python [script.py]      Python 3.12 interpreter / REPL
  node [script.js]        Node.js runtime / REPL
  curl <url>              HTTP GET request
  weather [city]          Live terminal weather forecast
  calc <expr>             Terminal calculator (e.g. calc 42*8)
  neofetch                System hardware and distro info
  cowsay <text>           Talking ASCII cow
  clear                   Clear terminal buffer
═════════════════════════════════════════════════════════════
""".trimIndent()
    }
}
