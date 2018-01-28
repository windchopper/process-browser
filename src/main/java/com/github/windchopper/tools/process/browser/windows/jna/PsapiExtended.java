package com.github.windchopper.tools.process.browser.windows.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public interface PsapiExtended extends Psapi {

    PsapiExtended INSTANCE = (PsapiExtended) Native.loadLibrary("Psapi", PsapiExtended.class, W32APIOptions.UNICODE_OPTIONS);

    /**
     * Flag for {@code EnumProcessModulesEx} routine.
     * List the 32-bit modules.
     */
    int LIST_MODULES_32BIT = 0x01;

    /**
     * Flag for {@code EnumProcessModulesEx} routine.
     * List the 64-bit modules.
     */
    int LIST_MODULES_64BIT = 0x02;

    /**
     * Flag for {@code EnumProcessModulesEx} routine.
     * List all modules.
     */
    int LIST_MODULES_ALL = 0x03;

    /**
     * Flag for {@code EnumProcessModulesEx} routine.
     * Use the default behavior.
     */
    int LIST_MODULES_DEFAULT = 0x00;

    boolean EnumProcesses(int[] pProcessIds, int cb, IntByReference pBytesReturned);
    boolean EnumProcessModulesEx(WinNT.HANDLE hProcess, WinNT.HMODULE[] lphModules, int cb, IntByReference pBytesReturned, int dwFilterFlag);

}
