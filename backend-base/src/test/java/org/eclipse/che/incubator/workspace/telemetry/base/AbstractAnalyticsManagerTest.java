/*
 * Copyright (c) 2022 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.incubator.workspace.telemetry.base;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class AbstractAnalyticsManagerTest {

    @Inject
    AbstractAnalyticsManager analyticsManager;

    @Test
    public void testInstantiation() {
        assertNotNull(analyticsManager);
    }

    @Test
    public void testMockResponseProperties() {
        assertEquals("fake-devworkspace", analyticsManager.devworkspaceId);
        assertEquals("python-hello-world", analyticsManager.devworkspaceName);
        assertEquals(1644869222000L, analyticsManager.createdOn);
        assertEquals(1644955985435L, analyticsManager.updatedOn);
        assertEquals(86763L, analyticsManager.age);
    }

}
