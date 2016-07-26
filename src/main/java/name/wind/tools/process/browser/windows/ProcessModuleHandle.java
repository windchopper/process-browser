package name.wind.tools.process.browser.windows;

import java.nio.file.Path;

public class ProcessModuleHandle implements ExecutableHandle {

    private final Path path;

    ProcessModuleHandle(Path path) {
        this.path = path;
    }

    @Override public Path path() {
        return path;
    }

}
