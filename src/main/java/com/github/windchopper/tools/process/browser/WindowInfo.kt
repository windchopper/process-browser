package com.github.windchopper.tools.process.browser

abstract class WindowInfo<T>(protected val nativeHandle: T, private val title: String?) {

    abstract fun makeFullScreen()

    override fun toString(): String {
        return "${title?:Application.messages["stage.selection.emptyTitle"]}: ${nativeHandle}"
    }

    companion object {

        @Throws(IllegalStateException::class) fun allWindowsOf(pid: Long): List<WindowInfo<*>> {
            return when(OperatingSystem.detect()) {
                OperatingSystem.WIN32 -> Win32WindowInfo.allProcessWindows(pid)
                else -> throw IllegalStateException(Application.messages["stage.processList.error.operatingSystemNotSupported"])
            }
        }

    }

}