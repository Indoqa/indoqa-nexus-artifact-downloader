/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.nexus.downloader.main;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.indoqa.boot.application.AbstractIndoqaBootApplication;
import com.indoqa.boot.application.AbstractStartupLifecycle;
import com.indoqa.boot.resources.error.RestResourceErrorMapperRegistrationUtils;
import com.indoqa.nexus.downloader.main.config.Config;

public class NexusDownloaderMain extends AbstractIndoqaBootApplication {

    private static final String APPLICATION_NAME = "Nexus-Downloader-Main";

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
        public void didInitializeSpring(AnnotationConfigApplicationContext context) {
            RestResourceErrorMapperRegistrationUtils.registerRestResourceErrorMapper(context);

            super.didInitializeSpring(context);
        }

        @Override
        public void willRefreshSpringContext(AnnotationConfigApplicationContext context) {
            context.register(Config.class);
        }
    }
}
