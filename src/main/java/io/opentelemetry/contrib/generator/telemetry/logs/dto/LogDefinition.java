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

package io.opentelemetry.contrib.generator.telemetry.logs.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import io.opentelemetry.contrib.generator.telemetry.misc.GeneratorUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Data
public class LogDefinition {

    private String severityOrderFunction;
    private Map<String, Integer> reportingEntitiesCounts;
    private Integer payloadFrequencySeconds;
    private Integer payloadCount;
    private Integer copyCount;
    private Map<String, Object> attributes;
    @JsonIgnore
    private String id;

    public long validate(String requestID, Set<String> allEntityTypes, Integer globalPayloadFrequencySeconds, int logIndex) {
        id = "log_by_ttg_" + logIndex;
        if (copyCount == null || copyCount < 1) {
            copyCount = 1;
        }
        validateMandatoryFields();
        validateEntityTypesCount(allEntityTypes);
        addRequestIDAndLogNameToValueFunction(requestID);
        attributes = GeneratorUtils.addArgsToAttributeExpressions(requestID, "log", id, attributes);
        return validatePayloadFrequency(globalPayloadFrequencySeconds);
    }

    private void validateMandatoryFields() {
        if (payloadCount == null || payloadCount < 1) {
            throw new GeneratorException("Payload count cannot be less than 1. Update the value in log " + this);
        }
        if (MapUtils.emptyIfNull(reportingEntitiesCounts).isEmpty()) {
            throw new GeneratorException("Mandatory field 'reportingResourcesCount' not provided in log definition YAML for log " + this);
        }
        if (StringUtils.defaultString(severityOrderFunction).isBlank()) {
            throw new GeneratorException("Mandatory field 'severityFrequency' not provided in log definition YAML for log " + this);
        }
        validateAttributes();
    }

    private long validatePayloadFrequency(Integer globalPostFrequencySeconds) {
        if (payloadFrequencySeconds == null || payloadFrequencySeconds < 10) {
            if(globalPostFrequencySeconds == null) {
                List<Integer> freqList = Arrays.asList(15, 30, 45, 60, 75, 90);
                payloadFrequencySeconds = freqList.get(ThreadLocalRandom.current().nextInt(freqList.size()));
                log.warn("Invalid/No value of 'payloadFrequencySeconds' found for log " + this +
                        ". Setting a value randomly = " + payloadFrequencySeconds + " as 'globalPayloadFrequencySeconds' value is missing.");
            } else {
                payloadFrequencySeconds = globalPostFrequencySeconds;
                log.warn("Invalid/No value of 'payloadFrequencySeconds' found for log " + this +
                        ". Setting the value as specified in 'globalPayloadFrequencySeconds' = " + globalPostFrequencySeconds);
            }
        }
        revisePayloadFrequencySeconds();
        return (long) payloadFrequencySeconds * payloadCount;
    }

    private void revisePayloadFrequencySeconds(){
        int totalPacketCount = reportingEntitiesCounts.size() * copyCount;
        String warning = "Revised value of payloadFrequencySeconds to rationalize log Generation to: ";
        if(inBetween(totalPacketCount, 5000, 25000) && payloadFrequencySeconds < 30){
            payloadFrequencySeconds = 30;
            log.warn(warning + payloadFrequencySeconds);
        }
        else if(inBetween(totalPacketCount, 25000, 100000) && payloadFrequencySeconds < 60){
            payloadFrequencySeconds = 60;
            log.warn(warning + payloadFrequencySeconds);
        }
        else if(inBetween(totalPacketCount, 100000, Integer.MAX_VALUE) && payloadFrequencySeconds < 90){
            payloadFrequencySeconds = 90;
            log.warn(warning + payloadFrequencySeconds);
        }
    }

    private void validateAttributes() {
        attributes = GeneratorUtils.validateAttributes(attributes);
    }

    private void validateEntityTypesCount (Set<String> allEntityTypes) {
        Map<String, Integer> entityCount = new HashMap<>();
        for (Map.Entry<String, Integer> eachEntity : MapUtils.emptyIfNull(reportingEntitiesCounts).entrySet()) {
            if (eachEntity.getKey().trim().length() == 0) {
                throw new GeneratorException("Blank key or value found in 'reportingEntityCounts'");
            }
            if (!allEntityTypes.contains(eachEntity.getKey().trim())){
                throw new GeneratorException("Invalid entity type (" + eachEntity.getKey() + ") found in log definition YAML " +
                        "for log " + this);
            }
            if (eachEntity.getValue() == null || eachEntity.getValue() < 1){
                log.warn("Unexpected value of reporting entity count found for " + eachEntity.getKey() + ". Updating value to 1 for log " + this);
                entityCount.put(eachEntity.getKey().trim(), 1);
            }
            else entityCount.put(eachEntity.getKey().trim(), eachEntity.getValue());
        }
        reportingEntitiesCounts = entityCount;
    }

    private void addRequestIDAndLogNameToValueFunction(String requestID) {
        List<String> valueFunctions = Arrays.asList("severityDistributionCount", "severityDistributionPercentage", "severityDistributionCountIndex");
        for (String eachValueFx: valueFunctions) {
            severityOrderFunction = severityOrderFunction.replace(eachValueFx + "(",
                    eachValueFx + "(\"" + requestID + "\", \"" + id + "\", ");
        }
    }

    public static boolean inBetween(int i, int minValueInclusive, int maxValueExclusive) {
        return (i >= minValueInclusive && i < maxValueExclusive);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object logDefinition) {
        if (this == logDefinition) {
            return true;
        }
        if (logDefinition instanceof LogDefinition) {
            return this.id.equals(((LogDefinition) logDefinition).getId());
        }
        return false;
    }
}
