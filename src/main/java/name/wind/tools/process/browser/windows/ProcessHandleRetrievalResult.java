package name.wind.tools.process.browser.windows;

public class ProcessHandleRetrievalResult {

    private final int processIdentifier;
    ProcessHandle processHandle;
    Exception exception;

    public ProcessHandleRetrievalResult(int processIdentifier) {
        this.processIdentifier = processIdentifier;
    }

    public int processIdentifier() {
        return processIdentifier;
    }

    public ProcessHandle processHandle() {
        return processHandle;
    }

    public Exception exception() {
        return exception;
    }

}
