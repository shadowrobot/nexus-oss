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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Named
@Singleton
public class RedirectFilter
    implements Filter
{

  private Map<Integer, String> redirects = new ConcurrentHashMap<>();

  @Override
  public void init(final FilterConfig config) throws ServletException {
    // ignore
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String repositoryName = redirects.get(request.getLocalPort());
      if (repositoryName != null) {
        String uri = httpRequest.getRequestURI();
        if (httpRequest.getQueryString() != null) {
          uri = uri + "?" + httpRequest.getQueryString();
        }
        if (!uri.startsWith("/repository/" + repositoryName)) {
          ((HttpServletResponse) response).sendRedirect(((HttpServletResponse) response).encodeRedirectURL(
              "/repository/" + repositoryName + uri
          ));
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  void addRedirect(Integer port, String path) {
    redirects.put(port, path);
  }

  void removeRedirect(Integer port) {
    redirects.remove(port);
  }

}
