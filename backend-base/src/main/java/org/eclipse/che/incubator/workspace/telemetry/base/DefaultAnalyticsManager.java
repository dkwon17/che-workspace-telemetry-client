/*
 * Copyright (c) 2016-2021 Red Hat, Inc.
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

import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

public class DefaultAnalyticsManager extends AbstractAnalyticsManager {
  private static final Logger LOG = getLogger(DefaultAnalyticsManager.class);

  public DefaultAnalyticsManager(
    String apiEndpoint,
    String workspaceId,
    String machineToken,
    HttpJsonRequestFactory requestFactory) {
      super(apiEndpoint, workspaceId, machineToken, requestFactory);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void onActivity() {
    LOG.info("Activity from user {}", getUserId());
  }

  @Override
  public void onEvent(AnalyticsEvent event, String ownerId, String ip,
    String userAgent, String resolution, Map<String, Object> properties) {
    LOG.info("Event triggered by user {} in {} from ip {} on agent {} :\n{}\nwith resolution: {}\nwith properties:\n{}", getUserId(), ownerId, ip, userAgent, event, resolution, properties);
  }

  @Override
  public void increaseDuration(AnalyticsEvent event, Map<String, Object> properties) {}

  @Override
  public void destroy() {
    LOG.info("Destroying the Analytics manager");
  }
}
