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

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.che.incubator.workspace.telemetry.model.Event;
import org.eclipse.che.incubator.workspace.telemetry.model.EventProperty;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;

import static io.restassured.RestAssured.*;

@QuarkusTest
public class TelemetryResourceIT {
    @Test
    public void testEvent() {
        ArrayList<EventProperty> properties = new ArrayList<>();
        Event e = new Event("WORKSPACE_STARTED", "1", "127.0.0.1", "curl", "", properties);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\": \"WORKSPACE_STARTED\", \"userId\": \"admin\", \"ip\": \"127.0.0.1\"}")
                .post("/telemetry/event")
                .then()
                .statusCode(200);
    }

    @Test
    public void testActivity() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .post("/telemetry/activity")
                .then()
                .statusCode(200);
    }
}
