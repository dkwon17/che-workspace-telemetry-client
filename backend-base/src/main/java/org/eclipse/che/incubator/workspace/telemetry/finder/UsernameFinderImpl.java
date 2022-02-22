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
package org.eclipse.che.incubator.workspace.telemetry.finder;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.che.incubator.workspace.telemetry.base.AbstractAnalyticsManager;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class UsernameFinderImpl implements UsernameFinder {
  private static final Logger LOG = getLogger(AbstractAnalyticsManager.class);

  private static final String USERNAME_FILE = "/config/user/profile/name";
  private static final String SECRET_NAME = "user-profile";
  private static final String USERNAME_KEY = "name";

  @Override
  public String findUsername() {
    String username = findUsernameFromFile();
    if (username == null) {
      username = findUsernameFromSecret();
    }
    return username;
  }

  public String findUsernameFromFile() {
    String userName = null;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(USERNAME_FILE));
      userName = br.readLine();
    } catch (IOException e) {
      LOG.warn("Cannot retrieve username from " + USERNAME_FILE, e);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          LOG.warn("Cannot close BufferedReader", e);
        }
      }
    }
    return userName;
  }

  private String findUsernameFromSecret() {
    String username = null;
    try (KubernetesClient client = new DefaultKubernetesClient()) {
      Secret userProfile = client.secrets().inNamespace(client.getNamespace()).withName(SECRET_NAME).get();
      username = userProfile.getData().get(USERNAME_KEY);
    } catch (Exception e) {
      LOG.warn("Cannot retrieve username from secret", e);
    }
    return username;
  }

}
