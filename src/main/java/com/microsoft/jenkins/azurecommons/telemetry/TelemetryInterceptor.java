/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TelemetryInterceptor implements Interceptor {
    static final String API_VERSION = "api-version";
    static final String SUBSCRIPTIONS = "subscriptions";
    static final String MS_REQUEST_ID = "x-ms-request-id";
    static final String PROVIDERS = "providers";
    static final String RESOURCE_PATH = "resource-path";
    static final String RESPONSE_CODE = "httpCode";
    static final String RESPONSE_MESSAGE = "httpMessage";

    final AppInsightsClient appInsightsClient;

    public TelemetryInterceptor(final AppInsightsClient client) {
        this.appInsightsClient = client;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        if (appInsightsClient != null) {
            sendTelemetry(response);
        }
        return response;
    }

    private void sendTelemetry(final Response response) {
        final Map<String, String> properties = new HashMap<>();
        final HttpUrl httpUrl = response.request().url();
        final String objectName = parseProvider(httpUrl, properties);
        if (objectName == null) {
            // Might not bea standard API path. Ignore
            return;
        }
        properties.put(API_VERSION, httpUrl.queryParameter(API_VERSION));
        properties.put(RESPONSE_CODE, String.valueOf(response.code()));
        properties.put(RESPONSE_MESSAGE, response.message());
        parseRequestId(response, properties);
        parseSubscriptionId(httpUrl, properties);
        appInsightsClient.sendEvent(AppInsightsConstants.AZURE_REST, response.request().method(), properties, false);
    }

    private String parseProvider(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(PROVIDERS)) {
            int index = httpUrl.pathSegments().indexOf(PROVIDERS);
            if (index + 1 < httpUrl.pathSegments().size()) {
                String path = httpUrl.encodedPath();
                properties.put(RESOURCE_PATH, path.substring(path.indexOf(PROVIDERS)));
                return httpUrl.pathSegments().get(index + 1);
            }
        }
        return null;
    }

    private void parseRequestId(final Response response, final Map<String, String> properties) {
        final String requestId = response.header(MS_REQUEST_ID);
        if (requestId != null)
            properties.put(MS_REQUEST_ID, requestId);
    }

    private void parseSubscriptionId(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(SUBSCRIPTIONS)) {
            int index = httpUrl.pathSegments().indexOf(SUBSCRIPTIONS);
            if (index + 1 < httpUrl.pathSegments().size())
                properties.put(AppInsightsConstants.AZURE_SUBSCRIPTION_ID, httpUrl.pathSegments().get(index + 1));
        }
    }

}
