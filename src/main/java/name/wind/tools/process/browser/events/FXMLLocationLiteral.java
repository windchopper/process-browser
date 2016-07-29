package name.wind.tools.process.browser.events;

import javax.enterprise.util.AnnotationLiteral;

public class FXMLLocationLiteral extends AnnotationLiteral<FXMLLocation> implements FXMLLocation {

    private final String value;

    public FXMLLocationLiteral(String value) {
        this.value = value;
    }

    @Override public String value() {
        return value;
    }

}
