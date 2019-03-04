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
package com.indoqa.nexus.downloader.client.configuration;

import static com.indoqa.nexus.downloader.client.configuration.HttpDownloaderConfiguration.*;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.apache.http.impl.DefaultHttpResponseFactory.INSTANCE;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class HttpDownloaderConfigurationTest {

    private ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
    private HttpClient mock = Mockito.mock(HttpClient.class);
    private Executor executor = Executor.newInstance(mock);

    @Before
    public void setup() throws IOException {
        Mockito.doReturn(getHttpResponse()).when(mock).execute(captor.capture(), ArgumentMatchers.any(HttpContext.class));
    }

    @Test
    public void emptyArgs() {
        String args[] = new String[] {""};
        ConfigurationHolder configurationHolder = HttpDownloaderConfiguration.create(args);
        assertNotNull(configurationHolder);
        assertTrue("Empty args should lead to an error.", configurationHolder.isErroneous());
    }

    @Test
    public void missingRequiredParameter() {
        String args[] = new String[] {" "};
        ConfigurationHolder configurationHolder = HttpDownloaderConfiguration.create(args);
        assertNotNull(configurationHolder);
        assertTrue("Empty args should lead to an error.", configurationHolder.isErroneous());
        assertEquals("Could not download configuration.", configurationHolder.getErrorMessage());
    }

    @Test
    public void testDifferentUrl() {
        HttpDownloaderConfiguration.create(new String[] {"12 "}, executor, Optional.of("downloader.example.com"), Optional.empty());

        HttpUriRequest value = captor.getValue();
        assertNotNull(value);
        assertEquals("https://downloader.example.com", value.getURI().toString());
        assertEquals("12", getHeaderValue(value, HEADER_IDQ_NEXUS_DL_PROJECT));
        assertEquals(DEFAULT_JSON_FILE, getHeaderValue(value, HEADER_IDQ_NEXUS_DL_VARIANT));

        checkHostname(value);
    }

    @Test
    public void testProjectAndVariant() throws IOException, URISyntaxException {
        HttpDownloaderConfiguration.create(new String[] {"12", "single-only"}, executor, Optional.empty(), Optional.empty());

        HttpUriRequest value = captor.getValue();
        assertNotNull(value);
        assertEquals(new URL("https://downloader-config.indoqa.com").toURI(), value.getURI());
        assertEquals("12", getHeaderValue(value, HEADER_IDQ_NEXUS_DL_PROJECT));
        assertEquals("single-only.json", getHeaderValue(value, HEADER_IDQ_NEXUS_DL_VARIANT));

        checkHostname(value);
    }

    @Test
    public void testProject() throws IOException, URISyntaxException {
        HttpDownloaderConfiguration.create(new String[] {"12 "}, executor, Optional.empty(), Optional.empty());

        HttpUriRequest value = captor.getValue();
        assertNotNull(value);
        assertEquals(new URL("https://downloader-config.indoqa.com").toURI(), value.getURI());
        assertEquals("12", getHeaderValue(value, HEADER_IDQ_NEXUS_DL_PROJECT));
        assertEquals(DEFAULT_JSON_FILE, getHeaderValue(value, HEADER_IDQ_NEXUS_DL_VARIANT));

        checkHostname(value);
    }

    private void checkHostname(HttpUriRequest value) {
        Optional<String> hostname = Hostname.getHostname();
        if (hostname.isPresent()) {
            assertEquals(hostname.get(), getHeaderValue(value, HEADER_IDQ_NEXUS_DL_HOST));
        } else {
            assertNull(getHeaderValue(value, HEADER_IDQ_NEXUS_DL_HOST));
        }
    }

    private HttpResponse getHttpResponse() {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8)));

        HttpResponse httpResponse = INSTANCE.newHttpResponse(HTTP_1_1, HttpStatus.SC_OK, new HttpCoreContext());
        httpResponse.setEntity(entity);
        return httpResponse;
    }

    private String getHeaderValue(HttpUriRequest request, String header) {
        Header firstHeader = request.getFirstHeader(header);

        if (firstHeader == null) {
            return null;
        }

        HeaderElement[] elements = firstHeader.getElements();
        if (elements == null || elements.length == 0) {
            return null;
        }

        return elements[0].getName();
    }
}