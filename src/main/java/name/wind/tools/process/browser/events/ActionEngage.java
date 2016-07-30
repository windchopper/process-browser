package name.wind.tools.process.browser.events;

public class ActionEngage<TargetType> {

    private final TargetType target;

    public ActionEngage(TargetType target) {
        this.target = target;
    }

    public TargetType target() {
        return target;
    }

}
