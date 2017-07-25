/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import hudson.Plugin;
import jenkins.model.Jenkins;

import java.util.HashMap;
import java.util.Map;

public class AppInsightsClientFactory {
    private static Map<Class<? extends Plugin>, AppInsightsClient> appInsightsClientMap = new HashMap<>();
    private static Object lock = new Object();

    public static AppInsightsClient getInstance(final Class<? extends Plugin> clazz) {
        if (!appInsightsClientMap.containsKey(clazz)) {
            synchronized (lock) {
                if (!appInsightsClientMap.containsKey(clazz)) {
                    final Plugin plugin = Jenkins.getActiveInstance().getPlugin(clazz);
                    appInsightsClientMap.put(clazz, new AppInsightsClient(plugin));
                }
            }
        }

        return appInsightsClientMap.get(clazz);
    }
}
