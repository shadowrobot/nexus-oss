/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.raw.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.bootstrap.jetty.JettyServer;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.StorageFacet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * A {@link ConnectorFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
@Named
public class ConnectorFacetImpl
    extends FacetSupport
    implements ConnectorFacet
{
  private final RedirectFilter redirectFilter;

  private ServerConnector jettyConnector;

  @Inject
  public ConnectorFacetImpl(final RedirectFilter redirectFilter) {
    this.redirectFilter = redirectFilter;
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    // empty
  }

  @Override
  protected void doStart() throws Exception {
    Server server = JettyServer.getServer();
    if (server != null) {
      jettyConnector = new ServerConnector(server);
      server.addConnector(jettyConnector);
      jettyConnector.start();
      redirectFilter.addRedirect(getPort(), getRepository().getName());
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (jettyConnector != null) {

      redirectFilter.removeRedirect(getPort());
      jettyConnector.stop();
      Server server = JettyServer.getServer();
      server.removeConnector(jettyConnector);
    }
  }

  @Override
  public int getPort() {
    return jettyConnector.getLocalPort();
  }
}
