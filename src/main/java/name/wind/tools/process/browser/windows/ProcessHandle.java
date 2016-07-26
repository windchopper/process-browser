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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class ProcessHandle implements ExecutableHandle {

    private static final int ARRAY_LENGTH__PROCESS_IDENTIFIERS = 1024;
    private static final int ARRAY_LENGTH__MODULE_HANDLES = 1024;

    private static final Kernel32Extended kernel = Kernel32Extended.INSTANCE;
    private static final PsapiExtended psapi = PsapiExtended.INSTANCE;
    private static final Advapi32 advapi = Advapi32.INSTANCE;
    private static final Shell32Extended shell = Shell32Extended.INSTANCE;

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
        return unmodifiableList(modules);
    }

    /*
     *
     */

    public boolean hasAdministrativeRights() {
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

    public boolean elevated() {
        WinNT.HANDLEByReference tokenReference = new WinNT.HANDLEByReference();
        if (kernel.OpenProcessToken(kernel.GetCurrentProcess(), WinNT.TOKEN_QUERY, tokenReference)) {
            try {
                Kernel32Extended.TOKEN_ELEVATION elevation = new Kernel32Extended.TOKEN_ELEVATION();
                IntByReference elevationSize = new IntByReference();
                if (kernel.GetTokenInformation(tokenReference.getValue(), WinNT.TOKEN_INFORMATION_CLASS.TokenElevation, new Kernel32Extended.TOKEN_ELEVATION.ByReference(elevation.getPointer()), elevation.size(), elevationSize)) {
                    return elevation.TokenIsElevated.intValue() != 0;
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

    public static void relaunchCurrentProcessAsAdministrator() {
        char[] chars = new char[WinDef.MAX_PATH];

//        int ownPID = kernel.GetCurrentProcessId();

//        int openFlags = WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_SUSPEND_RESUME | WinNT.PROCESS_TERMINATE | WinNT.SYNCHRONIZE;
//        WinNT.HANDLE processHandle = kernel.OpenProcess(openFlags, false, ownPID);
        WinNT.HANDLE processHandle = kernel.GetCurrentProcess();

        try {
            psapi.GetModuleFileNameExW(processHandle, null, chars, chars.length);

            String command = Native.toString(chars), args = null;

            IntByReference r = new IntByReference();
            Pointer argv_ptr = Shell32Extended.INSTANCE.CommandLineToArgvW(kernel.GetCommandLineW(), r);
            String[] argv = argv_ptr.getWideStringArray(0);
            Kernel32Extended.INSTANCE.LocalFree(argv_ptr);

            argv = Arrays.copyOfRange(argv, 1, argv.length);

            args = String.join(" ", (CharSequence[]) argv);

            System.out.println(command);
            System.out.println(args);

            Shell32Extended.SHELLEXECUTEINFO execInfo = new Shell32Extended.SHELLEXECUTEINFO();
            execInfo.lpFile = new WString(command);
            if (args != null)
                execInfo.lpParameters = new WString(args);
            execInfo.nShow = Shell32Extended.SW_SHOWDEFAULT;
            execInfo.fMask = Shell32Extended.SEE_MASK_NOCLOSEPROCESS;
            execInfo.lpVerb = new WString("runas");
            boolean result = shell.ShellExecuteEx(execInfo);

            if (!result)
            {
                throw win32Exception();
            } else {
                WinNT.HANDLE childProcessHandle = execInfo.hProcess;


            }
        } finally {
            kernel.CloseHandle(processHandle);
        }
    }

}
