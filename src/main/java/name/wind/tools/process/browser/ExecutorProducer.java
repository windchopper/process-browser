package name.wind.tools.process.browser;

import name.wind.common.cdi.AbstractProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped public class ExecutorProducer extends AbstractProducer {

    @Produces @Named("single") public ExecutorService createSingleThreadExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    public void shutdownExecutorService(@Disposes ExecutorService executorService) {
        executorService.shutdown();
    }

}
