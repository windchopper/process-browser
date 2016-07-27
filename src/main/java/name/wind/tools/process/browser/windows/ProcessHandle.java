package name.wind.tools.process.browser.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.windows.jna.Kernel32Extended;
import name.wind.tools.process.browser.windows.jna.PsapiExtended;
import name.wind.tools.process.browser.windows.jna.Shell32Extended;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static com.sun.jna.platform.win32.WinBase.STILL_ACTIVE;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class ProcessHandle implements ExecutableHandle {

    private static final int TIMEOUT__RELAUNCH_WAIT = 3000;

    private static final int ARRAY_LENGTH__PROCESS_IDENTIFIERS = 1024;
    private static final int ARRAY_LENGTH__MODULE_HANDLES = 1024;

    private static final Kernel32Extended kernel = Kernel32Extended.INSTANCE;
    private static final PsapiExtended psapi = PsapiExtended.INSTANCE;
    private static final Advapi32 advapi = Advapi32.INSTANCE;
    private static final Shell32Extended shell = Shell32Extended.INSTANCE;
    private static final User32 user = User32.INSTANCE;

    private final int identifier;
    private final List<ProcessModuleHandle> modules;

    ProcessHandle(int identifier, List<ProcessModuleHandle> modules) {
        this.identifier = identifier;
        this.modules = modules;
    }

    public int identifier() {
        return identifier;
    }

    @Override public Path path() {
        return modules.stream().findFirst().map(ProcessModuleHandle::path).orElse(null);
    }

    public List<ProcessModuleHandle> modules() {
        return unmodifiableList(modules.subList(1, modules.size()));
    }

    /*
     *
     */

    public void destroy(int exitCode) {
        int openFlags = WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_SUSPEND_RESUME | WinNT.PROCESS_TERMINATE | WinNT.SYNCHRONIZE;
        WinNT.HANDLE processHandle = kernel.OpenProcess(openFlags, false, identifier);
        if (processHandle != null) {
            try {
                kernel.TerminateProcess(processHandle, exitCode);
            } finally {
                kernel.CloseHandle(processHandle);
            }
        } else {
            throw win32Exception();
        }
    }

    /*
     *
     */

    private static Win32Exception win32Exception() {
        return new Win32Exception(kernel.GetLastError());
    }

    private static ProcessHandle processHandle(int processIdentifier, char[] characters, WinDef.HMODULE[] moduleHandles) {
        return Value.of(kernel.OpenProcess(WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION, false, processIdentifier))
            .map(processHandle -> {
                try {
                    return new ProcessHandle(processIdentifier, moduleHandles(processHandle, characters, moduleHandles));
                } finally {
                    kernel.CloseHandle(processHandle);
                }
            })
            .orElse(null);
    }

    private static List<ProcessModuleHandle> moduleHandles(WinNT.HANDLE processHandle, char[] characters, WinNT.HMODULE[] moduleHandles) {
        IntByReference moduleHandlesCount = new IntByReference();
        if (psapi.EnumProcessModulesEx(processHandle, moduleHandles, moduleHandles.length * Native.POINTER_SIZE, moduleHandlesCount, PsapiExtended.LIST_MODULES_ALL)) {
            return stream(moduleHandles, 0, moduleHandlesCount.getValue() / Native.POINTER_SIZE)
                .map(moduleHandle -> {
                    psapi.GetModuleFileNameExW(processHandle, moduleHandle, characters, characters.length);
                    return new ProcessModuleHandle(Paths.get(Native.toString(characters)));
                })
                .collect(
                    toList());
        } else {
            throw win32Exception();
        }
    }

    /*
     *
     */

    public static ProcessHandle current() {
        return processHandle(kernel.GetCurrentProcessId(), new char[WinDef.MAX_PATH], new WinDef.HMODULE[ARRAY_LENGTH__MODULE_HANDLES]);
    }

    public static List<ProcessHandle> allAvailable() {
        int[] processIdentifiers = new int[ARRAY_LENGTH__PROCESS_IDENTIFIERS];
        WinDef.HMODULE[] moduleHandles = new WinDef.HMODULE[ARRAY_LENGTH__MODULE_HANDLES];
        char[] characters = new char[WinDef.MAX_PATH];
        IntByReference processIdentifiersCount = new IntByReference();
        if (psapi.EnumProcesses(processIdentifiers, processIdentifiers.length * WinDef.DWORD.SIZE, processIdentifiersCount)) {
            return stream(processIdentifiers, 0, processIdentifiersCount.getValue() / WinDef.DWORD.SIZE)
                .mapToObj(processIdentifier -> processHandle(processIdentifier, characters, moduleHandles))
                .filter(Objects::nonNull)
                .collect(
                    toList());
        } else {
            throw win32Exception();
        }
    }

    public static String[] parameters() {
        IntByReference parametersLength = new IntByReference();
        Pointer parametersPointer = shell.CommandLineToArgvW(kernel.GetCommandLineW(), parametersLength);
        if (parametersPointer != null) {
            try {
                return copyOfRange(parametersPointer.getWideStringArray(0), 1, parametersLength.getValue());
            } finally {
                kernel.LocalFree(parametersPointer);
            }
        } else {
            throw win32Exception();
        }
    }

    public static boolean hasAdministrativeRights() {
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

    public static boolean elevated() {
        WinNT.HANDLEByReference tokenReference = new WinNT.HANDLEByReference();
        if (kernel.OpenProcessToken(kernel.GetCurrentProcess(), WinNT.TOKEN_QUERY, tokenReference)) {
            try {
                WinDef.DWORDByReference tokenElevationType = new WinDef.DWORDByReference();
                IntByReference elevationSize = new IntByReference();
                if (kernel.GetTokenInformation(tokenReference.getValue(), WinNT.TOKEN_INFORMATION_CLASS.TokenElevationType, tokenElevationType.getPointer(), WinDef.DWORD.SIZE, elevationSize)) {
                    return tokenElevationType.getValue().intValue() == Kernel32Extended.TOKEN_ELEVATION_TYPE.TokenElevationTypeFull;
                } else {
                    throw win32Exception();
                }
            } finally {
                kernel.CloseHandle(tokenReference.getValue());
            }
        } else {
            throw win32Exception();
        }
    }

    public static void launchElevated() throws InterruptedException {
        WinNT.HANDLE currentProcessHandle = kernel.GetCurrentProcess();

        char[] characters = new char[WinDef.MAX_PATH];
        psapi.GetModuleFileNameExW(currentProcessHandle, null, characters, characters.length);

        String file = Native.toString(characters), arguments = String.join(" ", (CharSequence[]) parameters());

        Shell32Extended.SHELLEXECUTEINFO execInfo = new Shell32Extended.SHELLEXECUTEINFO();
        execInfo.lpFile = new WString(file);
        execInfo.lpParameters = new WString(arguments);
        execInfo.lpVerb = new WString("runas");
        execInfo.fMask = Shell32Extended.SEE_MASK_NOCLOSEPROCESS;
        execInfo.nShow = Shell32Extended.SW_SHOWDEFAULT;

        if (shell.ShellExecuteEx(execInfo)) {
            Thread.sleep(TIMEOUT__RELAUNCH_WAIT);
            IntByReference exitCode = new IntByReference();
            if (kernel.GetExitCodeProcess(execInfo.hProcess, exitCode)) {
                if (exitCode.getValue() == STILL_ACTIVE) {
                    kernel.TerminateProcess(currentProcessHandle, 0);
                } else {
                    kernel.CloseHandle(execInfo.hProcess);
                    throw new RuntimeException("Elevated process terminates");
                }
            } else {
                throw win32Exception();
            }
        } else {
            throw win32Exception();
        }
    }

}
