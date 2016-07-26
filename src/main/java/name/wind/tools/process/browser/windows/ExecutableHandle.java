package name.wind.tools.process.browser.windows;

import java.nio.file.Path;

public interface ExecutableHandle {

    Path path();

    default String name() {
        return path().getFileName().toString();
    }

}
