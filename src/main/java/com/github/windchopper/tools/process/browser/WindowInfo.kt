package com.github.windchopper.tools.process.browser

abstract class WindowInfo<T>(protected val nativeHandle: T, private val title: String?) {

    abstract fun makeFullScreen()

    override fun toString(): String {
        return "${title?:Application.messages.getString("stage.selection.emptyTitle")}: ${nativeHandle}"
    }

    companion object {

        fun available(): Boolean {
            return OperatingSystem.detect() == OperatingSystem.WIN32
        }

        fun allWindowsOf(pid: Long): List<WindowInfo<*>> {
            return when(OperatingSystem.detect()) {
                OperatingSystem.WIN32 -> Win32WindowInfo.allProcessWindows(pid)
                else -> emptyList()
            }
        }

    }

}