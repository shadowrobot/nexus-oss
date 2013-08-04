/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.plugins.capabilities.internal.storage;

import java.io.IOException;
import java.util.Collection;

import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;

public interface CapabilityStorage
{

  /**
   * Adds a capability
   *
   * @param item to be added
   * @throws IOException IOException If any problem encountered while read/store of capabilities storage
   */
  public void add(CapabilityStorageItem item)
      throws IOException;

  /**
   * Updates stored capability if exists.
   *
   * @param item to be updated
   * @return false if capability to be updated does not exist in storage, true otherwise
   * @throws IOException If any problem encountered while read/store of capabilities storage
   */
  public boolean update(CapabilityStorageItem item)
      throws IOException;

  /**
   * Deletes stored capability if exists.
   *
   * @param id of capability to be deleted
   * @return false if capability to be deleted does not exist in storage, true otherwise
   * @throws IOException If any problem encountered while read/store of capabilities storage
   */
  public boolean remove(CapabilityIdentity id)
      throws IOException;

  /**
   * Retrieves stored capabilities.
   *
   * @return capabilities (never null)
   * @throws IOException If any problem encountered while read/store of capabilities storage
   */
  public Collection<CapabilityStorageItem> getAll()
      throws IOException;

}
