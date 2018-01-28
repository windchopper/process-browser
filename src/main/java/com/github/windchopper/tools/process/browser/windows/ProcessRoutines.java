package com.github.windchopper.tools.process.browser.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.github.windchopper.tools.process.browser.windows.jna.Kernel32Extended;
import com.github.windchopper.tools.process.browser.windows.jna.PsapiExtended;
import com.github.windchopper.tools.process.browser.windows.jna.Shell32Extended;

import java.nio.file.Paths;
import java.util.List;

import static com.sun.jna.platform.win32.WinBase.STILL_ACTIVE;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ProcessRoutines implements JnaAware {

    private static final int TIMEOUT__LAUNCH_WAIT = 1000;

    private static final int ARRAY_LENGTH__PROCESS_IDENTIFIERS = 1024;
    private static final int ARRAY_LENGTH__MODULE_HANDLES = 1024;

    private static ProcessHandle processHandle(int processIdentifier, char[] characters, WinDef.HMODULE[] moduleHandles) {
        WinNT.HANDLE processHandle = kernel.OpenProcess(WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION, false, processIdentifier);

        if (processHandle != null) {
            try {
                return new ProcessHandle(processIdentifier, processModuleHandles(processIdentifier, processHandle, characters, moduleHandles));
            } finally {
                kernel.CloseHandle(processHandle);
            }
        }

        throw new LastWin32Exception();
    }

    private static List<ProcessModuleHandle> processModuleHandles(int processIdentifier, WinNT.HANDLE processHandle, char[] characters, WinNT.HMODULE[] moduleHandles) {
        IntByReference moduleHandlesCount = new IntByReference();
        if (psapi.EnumProcessModulesEx(processHandle, moduleHandles, moduleHandles.length * Native.POINTER_SIZE, moduleHandlesCount, PsapiExtended.LIST_MODULES_ALL)) {
            return stream(moduleHandles, 0, moduleHandlesCount.getValue() / Native.POINTER_SIZE)
                .map(moduleHandle -> {
                    psapi.GetModuleFileNameExW(processHandle, moduleHandle, characters, characters.length);
                    return new ProcessModuleHandle(processIdentifier, Paths.get(Native.toString(characters)));
                })
                .collect(
                    toList());
        }

        throw new LastWin32Exception();
    }

    /*
     *
     */

    public static void terminateProcess(int processIdentifier, int exitCode) {
        int openFlags = WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_SUSPEND_RESUME | WinNT.PROCESS_TERMINATE | WinNT.SYNCHRONIZE;
        WinNT.HANDLE processHandle = kernel.OpenProcess(openFlags, false, processIdentifier);
        if (processHandle != null) {
            try {
                kernel.TerminateProcess(processHandle, exitCode);
            } finally {
                kernel.CloseHandle(processHandle);
            }
        }

        throw new LastWin32Exception();
    }

    public static ProcessHandleRetrievalResult currentProcess() {
        int processIdentifier = kernel.GetCurrentProcessId();
        ProcessHandleRetrievalResult retrievalResult = new ProcessHandleRetrievalResult(processIdentifier);

        try {
            retrievalResult.processHandle = processHandle(
                processIdentifier,
                new char[WinDef.MAX_PATH],
                new WinDef.HMODULE[ARRAY_LENGTH__MODULE_HANDLES]);
        } catch (Win32Exception thrown) {
            retrievalResult.exception = thrown;
        }

        return retrievalResult;
    }

    public static List<ProcessHandleRetrievalResult> allAvailableProcesses() {
        int[] processIdentifiers = new int[ARRAY_LENGTH__PROCESS_IDENTIFIERS];
        WinDef.HMODULE[] moduleHandles = new WinDef.HMODULE[ARRAY_LENGTH__MODULE_HANDLES];
        char[] characters = new char[WinDef.MAX_PATH];
        IntByReference processIdentifiersCount = new IntByReference();
        if (psapi.EnumProcesses(processIdentifiers, processIdentifiers.length * WinDef.DWORD.SIZE, processIdentifiersCount)) {
            return stream(processIdentifiers, 0, processIdentifiersCount.getValue() / WinDef.DWORD.SIZE)
                .filter(processIdentifier -> processIdentifier != 0) // excluding current process pseudo-handle
                .mapToObj(processIdentifier -> {
                    ProcessHandleRetrievalResult retrievalResult = new ProcessHandleRetrievalResult(processIdentifier);

                    try {
                        retrievalResult.processHandle = processHandle(processIdentifier, characters, moduleHandles);
                    } catch (Win32Exception thrown) {
                        retrievalResult.exception = thrown;
                    }

                    return retrievalResult;
                })
                .collect(
                    toList());
        }

        throw new LastWin32Exception();
    }

    public static String[] currentProcessParameters() {
        IntByReference parametersLength = new IntByReference();
        Pointer parametersPointer = shell.CommandLineToArgvW(kernel.GetCommandLineW(), parametersLength);
        if (parametersPointer != null) {
            try {
                return copyOfRange(parametersPointer.getWideStringArray(0), 1, parametersLength.getValue());
            } finally {
                kernel.LocalFree(parametersPointer);
            }
        }

        throw new LastWin32Exception();
    }

    public static boolean currentProcessHasAdministrativeRights() {
        Advapi32Util.Account[] groups = Advapi32Util.getCurrentUserGroups();

        for (Advapi32Util.Account group : groups) {
            WinNT.PSIDByReference sid = new WinNT.PSIDByReference();
            advapi.ConvertStringSidToSid(group.sidString, sid);

            if (advapi.IsWellKnownSid(sid.getValue(), WinNT.WELL_KNOWN_SID_TYPE.WinBuiltinAdministratorsSid)) {
                return true;
            }
        }

        return false;
    }

    public static boolean currentProcessElevated() {
        WinNT.HANDLEByReference tokenReference = new WinNT.HANDLEByReference();
        if (kernel.OpenProcessToken(kernel.GetCurrentProcess(), WinNT.TOKEN_QUERY, tokenReference)) {
            try {
                WinDef.DWORDByReference tokenElevationType = new WinDef.DWORDByReference();
                IntByReference elevationSize = new IntByReference();
                if (kernel.GetTokenInformation(tokenReference.getValue(), WinNT.TOKEN_INFORMATION_CLASS.TokenElevationType, tokenElevationType.getPointer(), WinDef.DWORD.SIZE, elevationSize)) {
                    return tokenElevationType.getValue().intValue() == Kernel32Extended.TOKEN_ELEVATION_TYPE.TokenElevationTypeFull;
                }
            } finally {
                kernel.CloseHandle(tokenReference.getValue());
            }
        }

        throw new LastWin32Exception();
    }

    public static boolean runProcess(String file, String parameters, boolean elevated) {
        Shell32Extended.SHELLEXECUTEINFO execInfo = new Shell32Extended.SHELLEXECUTEINFO();
        execInfo.hwnd = user.GetForegroundWindow();
        execInfo.lpFile = new WString(file);
        execInfo.fMask = Shell32Extended.SEE_MASK_NOCLOSEPROCESS;
        execInfo.nShow = Shell32Extended.SW_SHOWDEFAULT;

        if (parameters != null) {
            execInfo.lpParameters = new WString(parameters);
        }

        if (elevated) {
            execInfo.lpVerb = new WString("runas");
        }

        if (shell.ShellExecuteEx(execInfo)) {
            kernel.WaitForSingleObject(execInfo.hProcess, TIMEOUT__LAUNCH_WAIT);
            IntByReference exitCode = new IntByReference();
            if (kernel.GetExitCodeProcess(execInfo.hProcess, exitCode)) {
                try {
                    return exitCode.getValue() == STILL_ACTIVE;
                } finally {
                    kernel.CloseHandle(execInfo.hProcess);
                }
            }
        }

        throw new LastWin32Exception();
    }

}
