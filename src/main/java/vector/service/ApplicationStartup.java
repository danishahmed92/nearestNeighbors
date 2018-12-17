package vector.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import vector.glove.GloveModel;


/*
 * @author DANISH AHMED on 10/22/2018
 */

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        GloveModel gloveModel = GloveModel.gloveInstance;
    }
}

