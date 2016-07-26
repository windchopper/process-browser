package name.wind.tools.process.browser.windows.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

public interface Kernel32Extended extends Kernel32 {

    Kernel32Extended INSTANCE = (Kernel32Extended) Native.loadLibrary(Kernel32Extended.class, W32APIOptions.UNICODE_OPTIONS);

    boolean Module32First(WinNT.HANDLE hSnapshot, MODULEENTRY32.ByReference lpme);
    boolean Module32Next(WinNT.HANDLE hSnapshot, MODULEENTRY32.ByReference lpme);

    Pointer GetCommandLineW();

    boolean OpenProcessToken(WinNT.HANDLE ProcessHandle, int DesiredAccess, HANDLEByReference TokenHandle);

    boolean GetTokenInformation(
        WinNT.HANDLE TokenHandle,
        int /* WinNT.TOKEN_INFORMATION_CLASS */ TokenInformationClass,
        Structure.ByReference TokenInformation,
        int TokenInformationLength,
        IntByReference ReturnLength
    );

    class TOKEN_ELEVATION extends Structure {

        public static class ByReference extends TOKEN_ELEVATION implements Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }

        public DWORD TokenIsElevated;

        public TOKEN_ELEVATION() {
            TokenIsElevated = new WinDef.DWORD(0);
        }

        public TOKEN_ELEVATION(Pointer memory) {
            super(memory);
            read();
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("TokenIsElevated");
        }

    }

    class MODULEENTRY32 extends Structure {
        public static class ByReference extends MODULEENTRY32 implements Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }
        public MODULEENTRY32() {
            dwSize = new WinDef.DWORD(size());
        }

        public MODULEENTRY32(Pointer memory) {
            super(memory);
            read();
        }


        public WinDef.DWORD dwSize;
        public WinDef.DWORD th32ModuleID;
        public WinDef.DWORD th32ProcessID;
        public WinDef.DWORD GlblcntUsage;
        public WinDef.DWORD ProccntUsage;
        public Pointer modBaseAddr;
        public WinDef.DWORD modBaseSize;
        public WinDef.HMODULE hModule;
        public char[] szModule = new char[255+1]; // MAX_MODULE_NAME32
        public char[] szExePath = new char[WinDef.MAX_PATH];
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwSize", "th32ModuleID", "th32ProcessID", "GlblcntUsage", "ProccntUsage", "modBaseAddr", "modBaseSize", "hModule", "szModule", "szExePath");
        }
    }

}