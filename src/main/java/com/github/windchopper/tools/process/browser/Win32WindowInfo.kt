package com.github.windchopper.tools.process.browser

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser.MONITORINFOEX
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import com.sun.jna.ptr.IntByReference
import java.util.*
import java.util.logging.Logger

class Win32WindowInfo(nativeHandle: HWND, title: String?): WindowInfo<HWND>(nativeHandle, title) {

    companion object {

        private val logger = Logger.getLogger(WindowInfo::class.java.name)

        private val user = User32.INSTANCE
        private val kernel = Kernel32.INSTANCE

        private fun Int.toNaturalInt(): Int? = if (this > 0) this else null

        private fun <T> T.checkLastError(checker: (errorCode: Int) -> Unit): T {
            if (this == null || this == 0 || this == false || this is WinNT.HANDLE && (this.pointer == null || this.pointer == Pointer.NULL)) {
                kernel.GetLastError().toNaturalInt()
                    ?.let { checker.invoke(it) }
            }

            return this
        }

        private fun <T> T.throwLastError(): T {
            return checkLastError {
                throw Win32Exception(it)
            }
        }

        private fun windowTitle(nativeHandle: HWND): String? {
            return user.GetWindowTextLength(nativeHandle).throwLastError().toNaturalInt()
                ?.let {
                    with(CharArray(it + 1)) {
                        return user.GetWindowText(nativeHandle, this, this.size).throwLastError().toNaturalInt()
                            ?.let { String(this) }
                    }
                }
        }

        @Suppress("UNUSED_ANONYMOUS_PARAMETER") fun allProcessWindows(pid: Long): List<WindowInfo<*>> {
            val windowInfoList = ArrayList<WindowInfo<*>>()

            val windowEnumerator = WNDENUMPROC { handle, pointer ->
                if (user.IsWindowVisible(handle)) {
                    with (IntByReference()) {
                        user.GetWindowThreadProcessId(handle, this).checkLastError { logger.severe(Kernel32Util.formatMessage(it)) }
                        if (value == pid.toInt()) {
                            windowInfoList.add(Win32WindowInfo(handle, windowTitle(handle)))
                        }
                    }
                }

                true
            }

            user.EnumWindows(windowEnumerator, null).throwLastError()

            return windowInfoList
        }

    }

    override fun makeFullScreen() {
        val monitorHandle = user.MonitorFromWindow(nativeHandle, User32.MONITOR_DEFAULTTONEAREST).throwLastError()

        with (MONITORINFOEX()) {
            if (user.GetMonitorInfo(monitorHandle, this).throwLastError().booleanValue()) {
                with (rcMonitor) {
                    user.SetWindowPos(nativeHandle, null, left, top, right, bottom, User32.SWP_FRAMECHANGED).throwLastError()
                }
            }
        }

        val originalWindowStyle = user.GetWindowLong(nativeHandle, User32.GWL_STYLE).throwLastError()

        val modifiedWindowStyle = originalWindowStyle and (
            User32.WS_CAPTION or User32.WS_THICKFRAME or User32.WS_MINIMIZE or User32.WS_MAXIMIZE or User32.WS_SYSMENU).inv()

        if (modifiedWindowStyle != originalWindowStyle) {
            user.SetWindowLong(nativeHandle, User32.GWL_STYLE, modifiedWindowStyle).throwLastError()
        }
    }

}
