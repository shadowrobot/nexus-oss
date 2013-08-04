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

package org.sonatype.nexus.client.core.subsystem.repository.maven;

import org.sonatype.nexus.client.core.subsystem.repository.HostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;

/**
 * A Nexus Maven hosted {@link Repository}.
 *
 * @since 2.3
 */
public interface MavenHostedRepository
    extends HostedRepository<MavenHostedRepository>
{

  /**
   * Artifact of this repository will be included in search results.
   *
   * @return itself, for fluent api usage
   */
  MavenHostedRepository includeInSearchResults();

  /**
   * Artifact of this repository will be excluded from search results.
   *
   * @return itself, for fluent api usage
   */
  MavenHostedRepository excludeFromSearchResults();

}
