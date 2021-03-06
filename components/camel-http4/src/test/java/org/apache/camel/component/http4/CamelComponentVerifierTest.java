/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.camel.component.http4;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ComponentVerifier;
import org.apache.camel.component.http4.handler.AuthenticationValidationHandler;
import org.apache.camel.component.http4.handler.BasicValidationHandler;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.localserver.RequestBasicAuth;
import org.apache.http.localserver.ResponseBasicUnauthorized;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseContent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CamelComponentVerifierTest extends BaseHttpTest {
    private static final String AUTH_USERNAME = "camel";
    private static final String AUTH_PASSWORD = "password";

    private HttpServer localServer;
    
    @Before
    @Override
    public void setUp() throws Exception {
        localServer = ServerBootstrap.bootstrap()
            .setHttpProcessor(getHttpProcessor())
            .registerHandler("/basic", new BasicValidationHandler("GET", null, null, getExpectedContent()))
            .registerHandler("/auth", new AuthenticationValidationHandler("GET", null, null, getExpectedContent(), AUTH_USERNAME, AUTH_PASSWORD))
            .registerHandler("/redirect", redirectTo(HttpStatus.SC_MOVED_PERMANENTLY, "/redirected"))
            .registerHandler("/redirected", new BasicValidationHandler("GET", null, null, getExpectedContent()))
            .create();

        localServer.start();

        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if (localServer != null) {
            localServer.stop();
        }
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    private HttpProcessor getHttpProcessor() {
        return new ImmutableHttpProcessor(
            Arrays.asList(
                new RequestBasicAuth()
            ),
            Arrays.asList(
                new ResponseContent(),
                new ResponseBasicUnauthorized())
        );
    }

    // *************************************************
    // Helpers
    // *************************************************

    protected String getLocalServerUri(String contextPath) {
        return new StringBuilder()
            .append("http://")
            .append(localServer.getInetAddress().getHostName())
            .append(":")
            .append(localServer.getLocalPort())
            .append(contextPath != null
                ? contextPath.startsWith("/") ? contextPath : "/" + contextPath
                : "")
            .toString();
    }

    private HttpRequestHandler redirectTo(int code, String path) {
        return new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.setHeader("location", getLocalServerUri(path));
                response.setStatusCode(code);
            }
        };
    }

    // *************************************************
    // Tests
    // *************************************************

    @Test
    public void testConnectivity() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", getLocalServerUri("/basic"));

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.OK, result.getStatus());
    }

    @Test
    public void testConnectivityWithWrongUri() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", "http://www.not-existing-uri.unknown");

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.ERROR, result.getStatus());
        Assert.assertEquals(1, result.getErrors().size());

        ComponentVerifier.Error error = result.getErrors().get(0);

        Assert.assertEquals(ComponentVerifier.CODE_EXCEPTION, error.getCode());
        Assert.assertEquals(ComponentVerifier.ERROR_TYPE_EXCEPTION, error.getAttributes().get(ComponentVerifier.ERROR_TYPE_ATTRIBUTE));
        Assert.assertTrue(error.getParameters().contains("httpUri"));
    }

    @Test
    public void testConnectivityWithAuthentication() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", getLocalServerUri("/auth"));
        parameters.put("authUsername", AUTH_USERNAME);
        parameters.put("authPassword", AUTH_PASSWORD);

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.OK, result.getStatus());
    }

    @Test
    public void testConnectivityWithWrongAuthenticationData() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", getLocalServerUri("/auth"));
        parameters.put("authUsername", "unknown");
        parameters.put("authPassword", AUTH_PASSWORD);

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.ERROR, result.getStatus());
        Assert.assertEquals(1, result.getErrors().size());

        ComponentVerifier.Error error = result.getErrors().get(0);

        Assert.assertEquals("401", error.getCode());
        Assert.assertEquals(ComponentVerifier.ERROR_TYPE_HTTP, error.getAttributes().get(ComponentVerifier.ERROR_TYPE_ATTRIBUTE));
        Assert.assertEquals(401, error.getAttributes().get(ComponentVerifier.HTTP_CODE));
        Assert.assertTrue(error.getParameters().contains("authUsername"));
        Assert.assertTrue(error.getParameters().contains("authPassword"));
    }

    @Test
    public void testConnectivityWithRedirect() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", getLocalServerUri("/redirect"));

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.OK, result.getStatus());
    }

    @Test
    public void testConnectivityWithRedirectDisabled() throws Exception {
        HttpComponent component = context().getComponent("http4", HttpComponent.class);
        HttpComponentVerifier verifier = (HttpComponentVerifier)component.getVerifier();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("httpUri", getLocalServerUri("/redirect"));
        parameters.put("httpClient.redirectsEnabled", "false");

        ComponentVerifier.Result result = verifier.verify(ComponentVerifier.Scope.CONNECTIVITY, parameters);

        Assert.assertEquals(ComponentVerifier.Result.Status.ERROR, result.getStatus());
        Assert.assertEquals(1, result.getErrors().size());

        ComponentVerifier.Error error = result.getErrors().get(0);

        Assert.assertEquals("301", error.getCode());
        Assert.assertEquals(ComponentVerifier.ERROR_TYPE_HTTP, error.getAttributes().get(ComponentVerifier.ERROR_TYPE_ATTRIBUTE));
        Assert.assertEquals(true, error.getAttributes().get("http.redirect"));
        Assert.assertEquals(getLocalServerUri("/redirected"), error.getAttributes().get("http.redirect.location"));
        Assert.assertTrue(error.getParameters().contains("httpUri"));
    }
}