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

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.Mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Mock
public class MockDevworkspaceFinder implements DevWorkspaceFinder {
  @Override
  public GenericKubernetesResource findDevWorkspace(String devworkspaceId) {
    String devworkspaceYaml = readYAML();
    return Serialization.unmarshal(devworkspaceYaml, GenericKubernetesResource.class);
  }

  private String readYAML() {
    StringBuilder yaml = new StringBuilder();
    try {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream("mock-devworkspace.yaml");
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String l;
      while ((l = reader.readLine()) != null) {
        yaml.append(l).append("\n");
      }
    } catch(IOException e) {
      e.printStackTrace();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    return yaml.toString();
  }
}
