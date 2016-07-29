package name.wind.tools.process.browser;

import javafx.fxml.FXMLLoader;
import name.wind.common.cdi.AbstractProducer;
import name.wind.common.cdi.temporary.TemporaryScoped;
import name.wind.common.util.Builder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped public class FXMLLoaderProducer extends AbstractProducer implements ResourceBundleAware {

    @Produces @TemporaryScoped protected FXMLLoader createFXMLLoader() {
        return Builder.direct(FXMLLoader::new)
            .set(target -> target::setResources, bundle)
            .get();
    }

}
