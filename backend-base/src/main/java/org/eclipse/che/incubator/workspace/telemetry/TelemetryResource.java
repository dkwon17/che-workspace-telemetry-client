/*
 * Copyright (c) 2021 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.incubator.workspace.telemetry;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.che.incubator.workspace.telemetry.base.AbstractAnalyticsManager;
import org.eclipse.che.incubator.workspace.telemetry.base.AnalyticsEvent;
import org.eclipse.che.incubator.workspace.telemetry.model.Event;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
@ApplicationScoped
public class TelemetryResource {

  @Inject
  AbstractAnalyticsManager analyticsManager;

  @POST
  @Path("/event")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Posts a telemetry event",
    description = "Submit telemetry events to the workspace telemetry manager.\nThe event Id should be the Id of a built-in event or of an already-registered custom event",
    operationId = "event")
  @APIResponse(responseCode = "200", description = "Event was successfully submitted")
  @APIResponse(responseCode = "400", description = "Error during event submission")
  public String event(
    @RequestBody(
      description = "Event to send",
      required = true)
      Event event) {
    Map<String, Object> params =
      event.getProperties().stream()
        .collect(Collectors.toMap(e -> e.getId(), e -> e.getValue()));

    AnalyticsEvent analyticsEvent = analyticsManager.transformEvent(AnalyticsEvent.valueOf(event.getId()), analyticsManager.getUserId());
    analyticsManager.onActivity();
    analyticsManager.doSendEvent(analyticsEvent, event.getOwnerId(), event.getIp(), event.getAgent(), event.getResolution(), params);
    return "";
  }

  @POST
  @Path("/activity")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Notifies that some activity is still occurring from a given user",
    description = "Notifies that some activity is still occurring for a given user. This will allow maintaining the current session alive for telemetry backends that manage user sessions.",
    operationId = "activity")
  @APIResponse(responseCode = "200", description = "Notification was successfully submitted")
  public String activity() {
    analyticsManager.onActivity();
    return "";
  }

  void onStart(@Observes StartupEvent ev) {
    // Just to trigger the injection of the
    // analytics manager at start, so that initialization
    // errors can be thrown at start.
  }

  void onStop(@Observes ShutdownEvent ev) {
    analyticsManager.destroy();
  }
}
