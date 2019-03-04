package com.indoqa.nexus.downloader.main;

import com.indoqa.boot.application.AbstractIndoqaBootApplication;
import com.indoqa.boot.application.AbstractStartupLifecycle;
import com.indoqa.nexus.downloader.main.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NexusDownloaderMain extends AbstractIndoqaBootApplication {

    private static final String APPLICATION_NAME = "NexusDownloaderMain";

    public static void main(String[] args) {
        NexusDownloaderMain nexusDownloaderMain = new NexusDownloaderMain();
        nexusDownloaderMain.invoke(new NexusDownloaderMainStartupLifecycle());
    }

    @Override
    protected String getApplicationName() {
        return APPLICATION_NAME;
    }

    private static class NexusDownloaderMainStartupLifecycle extends AbstractStartupLifecycle {

        @Override
        public void willRefreshSpringContext(AnnotationConfigApplicationContext context) {
            context.register(Config.class);
        }
    }
}
