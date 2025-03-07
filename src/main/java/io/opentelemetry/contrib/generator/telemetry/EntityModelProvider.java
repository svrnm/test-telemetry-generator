/*
 * Copyright 2022 AppDynamics Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.generator.telemetry;

import io.opentelemetry.contrib.generator.core.dto.GeneratorEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityModelProvider {

    private static final ConcurrentMap<String, Map<String, List<GeneratorEntity>>> entityModels = new ConcurrentHashMap<>();

    private EntityModelProvider() {}

    public static void putEntityModel(String requestID, Map<String, List<GeneratorEntity>> entityModel) {
        entityModels.put(requestID, entityModel);
    }

    public static Map<String, List<GeneratorEntity>> getEntityModel(String requestID) {
        return entityModels.get(requestID);
    }
}
