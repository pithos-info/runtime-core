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

package info.pithos.runtime.core.metrics;

import info.pithos.runtime.core.context.SystemContext;
import info.pithos.runtime.model.metrics.Metrics.InfraMetricKey;
import info.pithos.runtime.model.metrics.Metrics.InfraMetricRaw;
import info.pithos.runtime.model.metrics.Metrics.JourneyMetricKey;
import info.pithos.runtime.model.metrics.Metrics.JourneyMetricRaw;
import info.pithos.runtime.model.metrics.Metrics.MetricEvent;
import info.pithos.runtime.model.metrics.Metrics.ServiceMetricKey;
import info.pithos.runtime.model.metrics.Metrics.ServiceMetricRaw;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class MetricEventBuilder {

    private static final DateTimeFormatter DATE_BUCKET_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

    private MetricEventBuilder() {}

    // ─── Tier routing ──────────────────────────────────────────────────────────
    //
    // componentId set              → Tier 3  InfraMetricRaw / InfraCounter
    // step set + rc.journey set    → Tier 1  JourneyMetricRaw / JourneyCounter
    // neither                      → Tier 2  ServiceMetricRaw / ServiceCounter

    public static boolean isInfraTier(MetricEvent event) {
        return !event.getComponentId().isEmpty();
    }

    public static boolean isJourneyTier(RequestContext rc, MetricEvent event) {
        return !event.getStep().isEmpty() && !rc.getJourney().isEmpty();
    }

    // ─── Raw row builders (unit == MS only) ────────────────────────────────────

    public static JourneyMetricRaw toJourneyMetricRaw(RequestContext rc, SystemContext sc,
                                                       MetricEvent event) {
        return JourneyMetricRaw.newBuilder()
                .setKey(JourneyMetricKey.newBuilder()
                        .setDateBucket(dateBucket())
                        .setEnterpriseId(rc.getEnterpriseId())
                        .setJourney(rc.getJourney())
                        .setRequestId(rc.getRequestId())
                        .setStep(event.getStep())
                        .build())
                .setMetric(event.getMetric())
                .setUnit(event.getUnit())
                .setValue(event.getValue())
                .setTimestampMs(System.currentTimeMillis())
                .setTraceId(rc.getTraceId())
                .build();
    }

    public static ServiceMetricRaw toServiceMetricRaw(RequestContext rc, SystemContext sc,
                                                       MetricEvent event) {
        return ServiceMetricRaw.newBuilder()
                .setKey(ServiceMetricKey.newBuilder()
                        .setDateBucket(dateBucket())
                        .setEnterpriseId(rc.getEnterpriseId())
                        .setServiceName(sc.getServiceName())
                        .setRequestId(rc.getRequestId())
                        .build())
                .setInstanceId(instanceId(sc))
                .setJourney(rc.getJourney())
                .setProtocol(event.getProtocol())
                .setMethod(event.getMethod())
                .setMetric(event.getMetric())
                .setUnit(event.getUnit())
                .setValue(event.getValue())
                .setTimestampMs(System.currentTimeMillis())
                .setTraceId(rc.getTraceId())
                .build();
    }

    public static InfraMetricRaw toInfraMetricRaw(RequestContext rc, SystemContext sc,
                                                   MetricEvent event) {
        return InfraMetricRaw.newBuilder()
                .setKey(InfraMetricKey.newBuilder()
                        .setDateBucket(dateBucket())
                        .setComponentType(event.getComponentType())
                        .setComponentId(event.getComponentId())
                        .setRequestId(rc.getRequestId())
                        .build())
                .setInstanceId(instanceId(sc))
                .setComponentProvider(event.getComponentProvider())
                .setMetric(event.getMetric())
                .setUnit(event.getUnit())
                .setValue(event.getValue())
                .setTimestampMs(System.currentTimeMillis())
                .setTraceId(rc.getTraceId())
                .build();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    public static String dateBucket() {
        return DATE_BUCKET_FMT.format(LocalDateTime.now(ZoneOffset.UTC));
    }

    // Pod/node identity — HOSTNAME in k8s; falls back to serviceName until OTel wiring
    public static String instanceId(SystemContext sc) {
        String hostname = System.getenv("HOSTNAME");
        return (hostname != null && !hostname.isEmpty()) ? hostname : sc.getServiceName();
    }
}
