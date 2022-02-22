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

public interface DevWorkspaceFinder {
  /**
   * @param devworkspaceId
   * @return  a DevWorkspace corresponding to the specified devworkspaceId. If not found, returns null.
   */
    public GenericKubernetesResource findDevWorkspace(String devworkspaceId);
}
