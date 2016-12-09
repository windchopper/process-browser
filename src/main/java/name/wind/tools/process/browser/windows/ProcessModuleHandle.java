package name.wind.tools.process.browser.windows;

import java.nio.file.Path;

public class ProcessModuleHandle implements ExecutableHandle {

    private final int parentIdentifier;
    private final Path path;

    ProcessModuleHandle(int parentIdentifier, Path path) {
        this.parentIdentifier = parentIdentifier;
        this.path = path;
    }

    public int parentIdentifier() {
        return parentIdentifier;
    }

    @Override public Path path() {
        return path;
    }

}
