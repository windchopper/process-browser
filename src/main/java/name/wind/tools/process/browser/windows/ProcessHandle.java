package name.wind.tools.process.browser.windows;

import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class ProcessHandle implements ExecutableHandle {

    private int identifier;
    private List<ProcessModuleHandle> modules;

    public ProcessHandle(int identifier, List<ProcessModuleHandle> modules) {
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

    public void terminate(int exitCode) {
        ProcessRoutines.terminateProcess(identifier, exitCode);
    }

}
