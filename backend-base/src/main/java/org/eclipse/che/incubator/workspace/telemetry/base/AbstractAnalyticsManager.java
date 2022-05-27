/*
 * Copyright (c) 2016-2022 Red Hat, Inc.
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

import static org.eclipse.che.incubator.workspace.telemetry.base.AnalyticsEvent.WORKSPACE_OPENED;
import static org.eclipse.che.incubator.workspace.telemetry.base.AnalyticsEvent.WORKSPACE_STARTED;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;

import com.google.common.annotations.VisibleForTesting;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.eclipse.che.incubator.workspace.telemetry.anonymizer.SHA1HashGenerator;
import org.eclipse.che.incubator.workspace.telemetry.finder.DevWorkspaceFinder;
import org.eclipse.che.incubator.workspace.telemetry.finder.UsernameFinder;
import org.slf4j.Logger;

import javax.inject.Inject;

public abstract class AbstractAnalyticsManager {
  private static final String LAST_UPDATED_ANNOTATION = "che.eclipse.org/last-updated-timestamp";

  protected String userId;

  @VisibleForTesting
  final protected String devworkspaceId;

  @VisibleForTesting
  final protected String devworkspaceName;

  @VisibleForTesting
  protected Long createdOn;
  @VisibleForTesting
  protected Long updatedOn;
  @VisibleForTesting
  final protected Long stoppedOn;
  @VisibleForTesting
  final protected Long age;
  @VisibleForTesting
  final protected Long returnDelay;
  @VisibleForTesting
  final protected Boolean firstStart;

  @VisibleForTesting
  protected String workspaceStartingUserId = null;

  @VisibleForTesting
  protected Map<String, Object> commonProperties;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private AnalyticsEvent lastEvent = null;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private Map<String, Object> lastEventProperties = null;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private long lastEventTime;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private String lastOwnerId = null;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private String lastIp = null;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private String lastUserAgent = null;

  /**
   * Reads and writes to this variable is synchronized to this object
   */
  private String lastResolution = null;

  /**
   *  Merge identical events when events happen within a timeframe of debounceTimeMillis milliseconds
   */
  protected long debounceTimeMillis = 1500;

  public abstract boolean isEnabled();

  public abstract void onActivity();

  public abstract void onEvent(AnalyticsEvent event, String ownerId, String ip, String userAgent, String resolution,
                               Map<String, Object> properties);

  public abstract void increaseDuration(AnalyticsEvent event, Map<String, Object> properties);

  public abstract void destroy();

  /**
   * No args constructor created for the following error during build:
   *
   * <pre>
   * No args javax.enterprise.inject.spi.DeploymentException: It's not possible to automatically add a synthetic no-args
   * constructor to an unproxyable bean class. You need to manually add a non-private no-args constructor
   * </pre>
   */
  public AbstractAnalyticsManager() {
    this.devworkspaceId = null;
    this.devworkspaceName = null;
    this.createdOn = null;
    this.updatedOn = null;
    this.stoppedOn = null;
    this.age = null;
    this.returnDelay = null;
    this.firstStart = null;
  }

  @Inject
  public AbstractAnalyticsManager(BaseConfiguration baseConfiguration, DevWorkspaceFinder devworkspaceFinder, UsernameFinder usernameFinder) {
    this.devworkspaceId = baseConfiguration.devworkspaceId;

    String username = usernameFinder.findUsername();
    this.userId = username != null ? SHA1HashGenerator.generateHash(username) : null;

    GenericKubernetesResource devworkspace = devworkspaceFinder.findDevWorkspace(this.devworkspaceId);
    if (devworkspace == null) {
      this.devworkspaceName = null;
      this.createdOn = null;
      this.updatedOn = null;
      this.stoppedOn = null;
      this.age = null;
      this.returnDelay = null;
      this.firstStart = null;
      commonProperties = makeCommonProperties();
      return;
    }

    ObjectMeta metadata = devworkspace.getMetadata();
    this.devworkspaceName = metadata.getName();
    this.createdOn = getEpochMilliseconds(metadata.getCreationTimestamp());
    this.updatedOn = getEpochMilliseconds(metadata.getAnnotations().get(LAST_UPDATED_ANNOTATION));
    this.stoppedOn = null;
    this.age = getSecondsBetween(updatedOn, createdOn);
    this.returnDelay = getSecondsBetween(updatedOn, stoppedOn);

    if (updatedOn != null) {
      firstStart = stoppedOn == null;
    } else {
      firstStart = null;
    }
    commonProperties = makeCommonProperties();
  }

  public void doSendEvent(AnalyticsEvent event, String ownerId, String ip, String userAgent, String resolution,
                          Map<String, Object> properties) {
    long currentEventTime = System.currentTimeMillis();
    boolean sendEvent = false;

    synchronized(this) {
      sendEvent = shouldSendEvent(event, properties, currentEventTime);
      if (sendEvent) {
        lastEvent = event;
        lastEventTime = currentEventTime;
        lastOwnerId = ownerId;
        lastIp = ip;
        lastUserAgent = userAgent;
        lastResolution = resolution;
        lastEventProperties = Collections.unmodifiableMap(properties);;
      }
    }

    if (sendEvent) {
      onEvent(event, ownerId, ip, userAgent, resolution, getMergedEventProperties(properties));
    } else {
      increaseDuration(event, properties);
    }
  }

  public final String getDevworkspaceId() {
    return devworkspaceId;
  }

  public final String getUserId() {
    return userId;
  }

  /**
   * transformEvent performs preliminary modification to the event passed to
   * onEvent. If the event is an instance of WORKSPACE_OPEN, and the starting
   * user ID is null, it sets the starting user ID and returns a WORKSPACE_STARTED
   * event.
   *
   * @param event  the incoming event
   * @param userId the incoming user ID
   * @return the correct AnalyticsEvent, WORKSPACE_STARTED if the conditions are
   * met, the same event otherwise.
   */
  public AnalyticsEvent transformEvent(AnalyticsEvent event, String userId) {
    if (event == WORKSPACE_OPENED && workspaceStartingUserId == null) {
      event = AnalyticsEvent.WORKSPACE_STARTED;
    }
    if (event == WORKSPACE_STARTED) {
      workspaceStartingUserId = userId;
    }
    return event;
  }

  public void setCommonProperties(Map<String, Object> commonProperties) {
    this.commonProperties = commonProperties;
  }

  public Map<String, Object> getCommonProperties() {
    return commonProperties;
  }

  /**
   * @param timestamp a rfc3339 formatted timestamp
   * @return epoch milliseconds from provided timestamp
   */
  private long getEpochMilliseconds(String timestamp) {
    return Instant.parse(timestamp).toEpochMilli();
  }

  private Long getSecondsBetween(Long end, Long start) {
    Long timeBetween = null;
    if (end != null && start != null) {
      timeBetween = (end - start) / 1000;
    }
    return timeBetween;
  }

  /**
   * create a map of the common and current event properties merged together
   *
   * @return a map of the current event and common workspace properties
   */
  public Map<String, Object> getMergedEventProperties(Map<String, Object> eventProperties) {
    ImmutableMap.Builder<String, Object> currentEventPropertiesBuilder = ImmutableMap.builder();
    commonProperties.forEach((k, v) -> {
      currentEventPropertiesBuilder.put(k, v);
    });
    eventProperties.forEach((k, v) -> {
      currentEventPropertiesBuilder.put(k, v);
    });
    return currentEventPropertiesBuilder.build();
  }

  /**
   * @return the last occurred event
   */
  public synchronized AnalyticsEvent getLastEvent() {
    return lastEvent;
  }

  /**
   * @return the properties of the last event
   */
  public synchronized Map<String, Object> getLastEventProperties() {
    return lastEventProperties;
  }

  /**
   * @return the unix timestamp of the last event
   */
  public synchronized long getLastEventTime() {
    return lastEventTime;
  }

  /**
   * @return the owner id of the last event
   */
  public synchronized String getLastOwnerId() {
    return lastOwnerId;
  }

  /**
   * @return the ip of the last event
   */
  public synchronized String getLastIp() {
    return lastIp;
  }

  /**
   * @return the user agent of the last event
   */
  public synchronized String getLastUserAgent() {
    return lastUserAgent;
  }

  /**
   * @return the screen resolution of the last event
   */
  public synchronized String getLastResolution() {
    return lastResolution;
  }

  private Map<String, Object> makeCommonProperties() {
    ImmutableMap.Builder<String, Object> commonPropertiesBuilder = ImmutableMap.builder();

    Arrays.asList(new SimpleImmutableEntry<>(EventProperties.CREATED, createdOn),
      new SimpleImmutableEntry<>(EventProperties.DEVWORKSPACE_ID, devworkspaceId),
      new SimpleImmutableEntry<>(EventProperties.DEVWORKSPACE_NAME, devworkspaceName),
      new SimpleImmutableEntry<>(EventProperties.UPDATED, updatedOn),
      new SimpleImmutableEntry<>(EventProperties.AGE, age)
    ).forEach((entry) -> {
      if (entry.getValue() != null) {
        commonPropertiesBuilder.put(entry.getKey(), entry.getValue());
      }
    });
    return commonPropertiesBuilder.build();
  }

  private boolean shouldSendEvent(AnalyticsEvent event, Map<String, Object> properties, long eventTime) {
    if (lastEventTime != 0
      && (sameAsLastEvent(event, properties) && insideDebounceTime(eventTime, lastEventTime, debounceTimeMillis))) {
      return false;
    } else {
      return true;
    }
  }

  private boolean sameAsLastEvent(AnalyticsEvent event, Map<String, Object> properties) {
    if (lastEvent == null || lastEvent != event) {
      return false;
    }

    if (lastEventProperties == null) {
      return false;
    }

    for (String property : event.getPropertiesToCheck()) {
      Object lastValue = lastEventProperties.get(property);
      Object newValue = properties.get(property);
      if (lastValue != null && newValue != null && lastValue.equals(newValue)) {
        continue;
      }
      if (lastValue == null && newValue == null) {
        continue;
      }
      return false;
    }
    return true;
  }

  private boolean insideDebounceTime(long eventTime, long lastEventTime, long debounceTimeMillis) {
    return (eventTime - lastEventTime) < debounceTimeMillis;
  }

}
