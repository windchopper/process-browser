package name.wind.tools.process.browser.windows;

public class DerivedProcessHasNotSurvivedException extends Exception {

    private final int exitCode;

    public DerivedProcessHasNotSurvivedException(int exitCode) {
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }

}
