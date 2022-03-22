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

import io.quarkus.test.Mock;
import org.eclipse.che.incubator.workspace.telemetry.finder.DevWorkspaceFinder;
import org.eclipse.che.incubator.workspace.telemetry.finder.UsernameFinder;
import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@Mock
public class MockAnalyticsManager extends AbstractAnalyticsManager {
  public MockAnalyticsManager(BaseConfiguration baseConfiguration, DevWorkspaceFinder devworkspaceFinder, UsernameFinder usernameFinder) {
    super(baseConfiguration, devworkspaceFinder, usernameFinder);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void onActivity() {

  }

  @Override
  public void onEvent(AnalyticsEvent event, String ownerId, String ip, String userAgent, String resolution, Map<String, Object> properties) {

  }

  @Override
  public void increaseDuration(AnalyticsEvent event, Map<String, Object> properties) {

  }

  @Override
  public void destroy() {

  }

}
