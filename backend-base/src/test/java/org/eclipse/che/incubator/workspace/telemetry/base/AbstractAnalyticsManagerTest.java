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
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class AbstractAnalyticsManagerTest {

  @InjectSpy
  AbstractAnalyticsManager analyticsManager;

  @BeforeEach
  public void init() {
    clearLastEvent();
    Mockito.clearInvocations(analyticsManager);
  }

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

  @Test
  public void testLastEventDataIsStored() {
    AnalyticsEvent event = AnalyticsEvent.WORKSPACE_OPENED;
    String ownerId = "/default-theia-plugins/telemetry_plugin";
    String ip = "192.0.0.0";
    String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
    String resolution = "2560x1440";
    Map<String, Object> properties = Map.ofEntries(
      entry("custom property", "foobar")
    );

    analyticsManager.doSendEvent(event, ownerId, ip, userAgent, resolution, properties);

    assertEquals(event, analyticsManager.getLastEvent());
    assertEquals(ip, analyticsManager.getLastIp());
    assertEquals(ownerId, analyticsManager.getLastOwnerId());
    assertEquals(userAgent, analyticsManager.getLastUserAgent());
    assertEquals(resolution, analyticsManager.getLastResolution());
    assertEquals(properties, analyticsManager.getLastEventProperties());
  }

  @Test
  public void testIdenticalEventsMergedSerial() throws InterruptedException {
    AnalyticsEvent event = AnalyticsEvent.WORKSPACE_STARTED;
    String ownerId = "/default-theia-plugins/telemetry_plugin";
    String ip = "192.0.0.0";
    String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
    String resolution = "2560x1440";

    for (int i = 0; i < 10; i++) {
      analyticsManager.doSendEvent(event, ownerId, ip, userAgent, resolution, Collections.emptyMap());
    }
    Mockito.verify(analyticsManager, Mockito.times(1)).onEvent(any(), any(), any(), any(), any(), any());
  }


  @Test
  public void testIdenticalEventsMergedParallel() throws InterruptedException {
    int numberOfThreads = 10;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    AnalyticsEvent event = AnalyticsEvent.WORKSPACE_STARTED;
    String ownerId = "/default-theia-plugins/telemetry_plugin";
    String ip = "192.0.0.0";
    String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
    String resolution = "2560x1440";

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        analyticsManager.doSendEvent(event, ownerId, ip, userAgent, resolution, Collections.emptyMap());
        latch.countDown();
      });
    }
    latch.await();
    Mockito.verify(analyticsManager, Mockito.times(1)).onEvent(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testDifferentEventsNotMerged() throws InterruptedException {
    int numberOfThreads = 6;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    AnalyticsEvent[] events = new AnalyticsEvent[]{
      AnalyticsEvent.WORKSPACE_STARTED,
      AnalyticsEvent.WORKSPACE_INACTIVE,
      AnalyticsEvent.WORKSPACE_STOPPED,
      AnalyticsEvent.EDITOR_USED,
      AnalyticsEvent.PUSH_TO_REMOTE,
      AnalyticsEvent.COMMIT_LOCALLY,
    };

    String ownerId = "/default-theia-plugins/telemetry_plugin";
    String ip = "192.0.0.0";
    String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
    String resolution = "2560x1440";

    for (int i = 0; i < numberOfThreads; i++) {
      AnalyticsEvent event = events[i];
      service.execute(() -> {
        analyticsManager.doSendEvent(event, ownerId, ip, userAgent, resolution, Collections.emptyMap());
        latch.countDown();
      });
    }
    latch.await();
    Mockito.verify(analyticsManager, Mockito.times(6)).onEvent(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testMergeEventProperties() throws InterruptedException {
    Map<String, Object> properties = Map.ofEntries(
      entry("event property", "foobar")
    );

    Map<String, Object> mergedProperties = analyticsManager.getMergedEventProperties(properties);

    assertEquals(6, mergedProperties.size());
    assertTrue(mergedProperties.containsKey(EventProperties.CREATED));
    assertTrue(mergedProperties.containsKey(EventProperties.DEVWORKSPACE_ID));
    assertTrue(mergedProperties.containsKey(EventProperties.DEVWORKSPACE_NAME));
    assertTrue(mergedProperties.containsKey(EventProperties.UPDATED));
    assertTrue(mergedProperties.containsKey(EventProperties.AGE));

    assertEquals("foobar", mergedProperties.get("event property"));
    assertEquals(1644869222000L, mergedProperties.get(EventProperties.CREATED));
    assertEquals("fake-devworkspace", mergedProperties.get(EventProperties.DEVWORKSPACE_ID));
    assertEquals("python-hello-world", mergedProperties.get(EventProperties.DEVWORKSPACE_NAME));
    assertEquals(1644955985435L, mergedProperties.get(EventProperties.UPDATED));
    assertEquals(86763L, mergedProperties.get(EventProperties.AGE));
  }

  private void clearLastEvent() {
    // send a dummy event to overwrite previous event
    analyticsManager.doSendEvent(null, null, null, null, null, Collections.emptyMap());
  }
}
