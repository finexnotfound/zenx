package com.example.zenx.ui

object AsciiArt {
    const val ZENX_BANNER = """
 ███████╗███████╗███╗   ██╗    ██╗  ██╗
 ╚══███╔╝██╔════╝████╗  ██║    ╚██╗██╔╝
   ███╔╝ █████╗  ██╔██╗ ██║     ╚███╔╝ 
  ███╔╝  ██╔══╝  ██║╚██╗██║     ██╔██╗ 
 ███████╗███████╗██║ ╚████║    ██╔╝ ██╗
 ╚══════╝╚══════╝╚═╝  ╚════╝    ╚═╝  ╚═╝
"""

    const val ZENX_WELCOME = """
* Advanced Linux Environment for Android
* Created by: finex (finexcreates@gmail.com)
* Package Manager: pkg | Shell: bash-zenx v2.4.0
* Flint AI Terminal Assistant is READY!
* Type 'pkg install git' to install git & clone repositories.
* Type 'pkg open flint' or 'flint' to launch Flint AI.
* Type 'help' for available terminal commands.
"""

    const val FLINT_BANNER = """
   ______ _      _____ _   _ _______ 
  |  ____| |    |_   _| \ | |__   __|
  | |__  | |      | | |  \| |  | |   
  |  __| | |      | | | . ` |  | |   
  | |    | |____ _| |_| |\  |  | |   
  |_|    |______|_____|_| \_|  |_|   
 ⚡ FLINT AI TERMINAL ASSISTANT v1.0.0
 Connected to Gemini Intelligence
"""

    fun neofetch(osInfo: String, uptime: String, memory: String, pkgCount: Int): String {
        return """
        .---.            zenx@android-zenx
       /     \           -----------------
      | () () |          OS: ZEN X Linux / Android 15
       \  _  /           Kernel: 6.1.75-zenx-aarch64
        /   \            Uptime: $uptime
       /|   |\           Packages: $pkgCount (pkg)
      / |   | \          Shell: zsh/bash 5.2.21
     (  |   |  )         Terminal: ZEN X Console
      \ '---' /          CPU: ARM Cortex-A78 (8) @ 2.84GHz
       '-----'           Memory: $memory
                         Arch: aarch64-linux-android
"""
    }

    fun cowsay(message: String): String {
        val bubbleLength = (message.length + 2).coerceAtLeast(6)
        val borderTop = " " + "_".repeat(bubbleLength)
        val borderBottom = " " + "-".repeat(bubbleLength)
        val textLine = "< $message >"
        return """
$borderTop
$textLine
$borderBottom
        \   ^__^
         \  (oo)\_______
            (__)\       )\/\
                ||----w |
                ||     ||
"""
    }
}
