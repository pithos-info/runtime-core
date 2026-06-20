/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.runtime.core.context;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import info.pithos.runtime.model.config.Config.BootstrapConfigs;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.runtime.model.config.Config.Configs;

class ServiceConfigsTest {

    private static final String SVC = "pithos-test";

    private ConfigMap mapWith(Configs configs) {
        return ConfigMap.newBuilder()
            .setBootstrapConfigs(BootstrapConfigs.newBuilder().setServiceName(SVC).build())
            .putConfigs(SVC, configs)
            .build();
    }

    @Test
    void constructor_nullConfigMap_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceConfigs(null));
    }

    // --- getConfig ---

    @Test
    void getConfig_keyPresent_returnsValue() {
        // NOTE: currently fails — condition checks intConfigsMap instead of configsMap
        ConfigMap cm = mapWith(Configs.newBuilder().putConfigs("host", "localhost").build());
        assertEquals("localhost", new ServiceConfigs(cm).getConfig("host", "default"));
    }

    @Test
    void getConfig_keyAbsent_returnsDefault() {
        assertEquals("fallback", new ServiceConfigs(mapWith(Configs.newBuilder().build()))
            .getConfig("missing", "fallback"));
    }

    // --- getIntConfig ---

    @Test
    void getIntConfig_keyPresent_returnsValue() {
        ConfigMap cm = mapWith(Configs.newBuilder().putIntConfigs("port", 8080).build());
        assertEquals(8080, new ServiceConfigs(cm).getIntConfig("port", 0));
    }

    @Test
    void getIntConfig_keyAbsent_returnsDefault() {
        assertEquals(9090, new ServiceConfigs(mapWith(Configs.newBuilder().build()))
            .getIntConfig("port", 9090));
    }

    // --- getBoolConfig ---

    @Test
    void getBoolConfig_keyPresent_returnsValue() {
        // NOTE: currently fails — condition checks intConfigsMap instead of boolConfigsMap
        ConfigMap cm = mapWith(Configs.newBuilder().putBoolConfigs("ssl", true).build());
        assertTrue(new ServiceConfigs(cm).getBoolConfig("ssl", false));
    }

    @Test
    void getBoolConfig_keyAbsent_returnsDefault() {
        assertTrue(new ServiceConfigs(mapWith(Configs.newBuilder().build()))
            .getBoolConfig("ssl", true));
    }

    // --- getLongConfig ---

    @Test
    void getLongConfig_keyPresent_returnsValue() {
        // NOTE: currently fails — condition checks intConfigsMap instead of longConfigsMap
        ConfigMap cm = mapWith(Configs.newBuilder().putLongConfigs("timeout", 30_000L).build());
        assertEquals(30_000L, new ServiceConfigs(cm).getLongConfig("timeout", 0L));
    }

    @Test
    void getLongConfig_keyAbsent_returnsDefault() {
        assertEquals(5_000L, new ServiceConfigs(mapWith(Configs.newBuilder().build()))
            .getLongConfig("timeout", 5_000L));
    }
}
