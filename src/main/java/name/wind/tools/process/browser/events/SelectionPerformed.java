package name.wind.tools.process.browser.events;

public class SelectionPerformed<T> {

    private final T selectedObject;

    public SelectionPerformed(T selectedObject) {
        this.selectedObject = selectedObject;
    }

    public T selectedObject() {
        return selectedObject;
    }

}
