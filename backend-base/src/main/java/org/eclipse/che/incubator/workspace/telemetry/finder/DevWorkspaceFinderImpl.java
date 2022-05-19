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
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class DevWorkspaceFinderImpl implements DevWorkspaceFinder {
  private static final Logger LOG = getLogger(DevWorkspaceFinderImpl.class);
  private static final String PLURAL = "devworkspaces";
  private static final String GROUP = "workspace.devfile.io";
  private static final String VERSION = "v1alpha2";
  private static final String SCOPE = "Namespaced";
  private static final String DEVWORKSPACE_NAMESPACE = "DEVWORKSPACE_NAMESPACE";
  private static final String KUBERNETES_NAMESPACE_PATH = Config.KUBERNETES_NAMESPACE_PATH;

  public GenericKubernetesResource findDevWorkspace(String devworkspaceId) {
    CustomResourceDefinitionContext devworkspaceContext = new CustomResourceDefinitionContext.Builder()
      .withPlural(PLURAL)
      .withGroup(GROUP)
      .withVersion(VERSION)
      .withScope(SCOPE)
      .build();

    final AtomicReference<GenericKubernetesResource> devworkspace = new AtomicReference<>();
    String namespace = null;
    try (KubernetesClient client = new DefaultKubernetesClient()) {

      namespace = System.getenv(DEVWORKSPACE_NAMESPACE);

      if (namespace == null) {
        namespace = getNamespaceFromServiceAccount();
      }

      client.genericKubernetesResources(devworkspaceContext).inNamespace(namespace).list().getItems().forEach(
        dw -> {
          String dwId = dw.get("status", "devworkspaceId");
          if (devworkspaceId.equals(dwId)) {
            devworkspace.set(dw);
          }
        }
      );
    } catch (IOException e) {
      LOG.warn("Failed to read namespace in {}", KUBERNETES_NAMESPACE_PATH, e);
    } catch (Exception e) {
      LOG.warn("Failed to find devworkspace with id: {} in namespace: {}", devworkspaceId, namespace, e);
    }

    return devworkspace.get();
  }

  private String getNamespaceFromServiceAccount() throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(KUBERNETES_NAMESPACE_PATH));
      String namespace = br.readLine();
      return namespace.strip();
    } catch (IOException e) {
      throw e;
    } finally {
      if (br != null) {
        br.close();
      }
    }
  }
}
