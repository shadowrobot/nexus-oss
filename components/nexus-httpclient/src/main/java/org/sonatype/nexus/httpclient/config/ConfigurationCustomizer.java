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
package org.sonatype.nexus.httpclient.config;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.httpclient.internal.NexusHttpRoutePlanner;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.http.client.config.AuthSchemes.BASIC;
import static org.apache.http.client.config.AuthSchemes.DIGEST;
import static org.apache.http.client.config.AuthSchemes.NTLM;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTPS;

/**
 * Applies {@link HttpClientConfiguration} to {@link HttpClientPlan}.
 *
 * @since 3.0
 */
@SuppressWarnings("PackageAccessibility") // FIXME: httpclient usage is producing lots of OSGI warnings in IDEA
public class ConfigurationCustomizer
    extends ComponentSupport
    implements HttpClientPlan.Customizer
{
  /**
   * Default non-proxyHosts value, used by Java7 too. Always appended to the user given list, if any.
   */
  private static final List<String> DEFAULT_NO_PROXY_HOSTS_PATTERN_STRINGS = ImmutableList.of(
      "localhost", "127.*", "[::1]", "0.0.0.0", "[::0]"
  );

  /**
   * Simple reusable function that converts "glob-like" expressions to regexp.
   */
  private static final Function<String, String> GLOB_STRING_TO_REGEXP_STRING = new Function<String, String>()
  {
    @Override
    public String apply(final String input) {
      return "(" +
          input.toLowerCase(Locale.US).replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?").replaceAll("\\[", "\\\\[")
              .replaceAll("\\]", "\\\\]") + ")";
    }
  };

  /**
   * Regexp pattern used as default or as fallback when user pattern cannot be converted to regexp. Built from
   * {@link #DEFAULT_NO_PROXY_HOSTS_PATTERN_STRINGS}.
   */
  private static final Pattern DEFAULT_NO_PROXY_HOSTS_PATTERN =
      Pattern.compile(Joiner.on("|").join(
              Iterables.transform(DEFAULT_NO_PROXY_HOSTS_PATTERN_STRINGS, GLOB_STRING_TO_REGEXP_STRING)
          )
      );

  static {
    /**
     * Install custom {@link Authenticator} for proxy.
     */
    Authenticator.setDefault(new Authenticator()
    {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestorType() == RequestorType.PROXY) {
          String prot = getRequestingProtocol().toLowerCase();
          String host = System.getProperty(prot + ".proxyHost", "");
          String port = System.getProperty(prot + ".proxyPort", "80");
          String user = System.getProperty(prot + ".proxyUser", "");
          String password = System.getProperty(prot + ".proxyPassword", "");

          if (getRequestingHost().equalsIgnoreCase(host)) {
            if (Integer.parseInt(port) == getRequestingPort()) {
              // Seems to be OK.
              return new PasswordAuthentication(user, password.toCharArray());
            }
          }
        }
        return null;
      }
    });
  }

  private final HttpClientConfiguration configuration;

  public ConfigurationCustomizer(final HttpClientConfiguration configuration) {
    this.configuration = checkNotNull(configuration);
  }

  @Override
  public void customize(final HttpClientPlan plan) {
    checkNotNull(plan);

    if (configuration.getConnection() != null) {
      apply(configuration.getConnection(), plan);
    }
    if (configuration.getProxy() != null) {
      apply(configuration.getProxy(), plan);
    }
    if (configuration.getAuthentication() != null) {
      apply(configuration.getAuthentication(), plan, null);
    }
  }

  /**
   * Apply connection configuration to plan.
   */
  private void apply(final ConnectionConfiguration connection, final HttpClientPlan plan) {
    if (connection.getTimeout() != null) {
      int timeout = connection.getTimeout().toMillisI();
      plan.getSocket().setSoTimeout(timeout);
      plan.getRequest().setConnectTimeout(timeout);
      plan.getRequest().setSocketTimeout(timeout);
    }

    if (connection.getMaximumRetries() != null) {
      plan.getClient().setRetryHandler(new StandardHttpRequestRetryHandler(connection.getMaximumRetries(), false));
    }

    if (connection.getUserAgentSuffix() != null) {
      checkState(plan.getUserAgent() != null, "Default User-Agent not set");
      plan.getHeaders().put(HttpHeaders.USER_AGENT, plan.getUserAgent() + " " + connection.getUserAgentSuffix());
    }

    if (Boolean.TRUE.equals(connection.getUseTrustStore())) {
      plan.getAttributes().put(SSLContextSelector.USE_TRUST_STORE, Boolean.TRUE);
    }
  }

  /**
   * Apply proxy-server configuration to plan.
   */
  private void apply(final ProxyConfiguration proxy, final HttpClientPlan plan) {
    // HTTP proxy
    ProxyServerConfiguration http = proxy.getHttp();
    if (http != null && http.isEnabled()) {
      HttpHost host = new HttpHost(http.getHost(), http.getPort());
      if (http.getAuthentication() != null) {
        apply(http.getAuthentication(), plan, host);
      }
    }

    // HTTPS proxy
    ProxyServerConfiguration https = proxy.getHttps();
    if (https != null && https.isEnabled()) {
      HttpHost host = new HttpHost(https.getHost(), https.getPort());
      if (https.getAuthentication() != null) {
        apply(https.getAuthentication(), plan, host);
      }
    }
    plan.getClient().setRoutePlanner(createRoutePlanner(proxy));
  }

  /**
   * Creates instance of {@link NexusHttpRoutePlanner} from passed in configuration, never {@code null}.
   */
  @VisibleForTesting
  NexusHttpRoutePlanner createRoutePlanner(final ProxyConfiguration proxy) {
    Map<String, HttpHost> proxies = new HashMap<>(2);

    // HTTP proxy
    ProxyServerConfiguration http = proxy.getHttp();
    if (http != null && http.isEnabled()) {
      HttpHost host = new HttpHost(http.getHost(), http.getPort());
      proxies.put(HTTP, host);
      proxies.put(HTTPS, host);
    }

    // HTTPS proxy
    ProxyServerConfiguration https = proxy.getHttps();
    if (https != null && https.isEnabled()) {
      HttpHost host = new HttpHost(https.getHost(), https.getPort());
      proxies.put(HTTPS, host);
    }

    // Non-proxy hosts (Java http.nonProxyHosts formatted glob-like patterns converted to single Regexp expression)
    LinkedHashSet<String> patterns = new LinkedHashSet<>();
    if (proxy.getNonProxyHosts() != null) {
      patterns.addAll(Arrays.asList(proxy.getNonProxyHosts()));
    }
    patterns.addAll(DEFAULT_NO_PROXY_HOSTS_PATTERN_STRINGS);
    String nonProxyPatternString = Joiner.on("|").join(Iterables.transform(patterns, GLOB_STRING_TO_REGEXP_STRING));
    Pattern nonProxyPattern;
    try {
      nonProxyPattern = Pattern.compile(nonProxyPatternString, Pattern.CASE_INSENSITIVE);
    }
    catch (PatternSyntaxException e) {
      log.warn("Invalid non-proxy host regex: {}, using defaults", nonProxyPatternString, e);
      nonProxyPattern = DEFAULT_NO_PROXY_HOSTS_PATTERN;
    }
    syncHttpSystemProperties(proxy);
    return new NexusHttpRoutePlanner(proxies, nonProxyPattern);
  }

  private void syncHttpSystemProperties(final ProxyConfiguration proxy) {
    // HTTP proxy
    ProxyServerConfiguration http = proxy.getHttp();
    if (http != null && http.isEnabled()) {
      System.setProperty("http.proxyHost", http.getHost());
      System.setProperty("http.proxyPort", Integer.toString(http.getPort()));
      if (http.getAuthentication() != null) {
        if (http.getAuthentication() instanceof UsernameAuthenticationConfiguration) {
          UsernameAuthenticationConfiguration usernamePassword = (UsernameAuthenticationConfiguration) http
              .getAuthentication();
          System.setProperty("http.proxyUser", usernamePassword.getUsername());
          System.setProperty("http.proxyPassword", usernamePassword.getPassword());
        }
        else {
          log.warn("Authentication {} not supported for Java Networking, system properties not set",
              http.getAuthentication().getClass().getSimpleName());
        }
      }
      else {
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
      }
    }
    else {
      System.clearProperty("http.proxyHost");
      System.clearProperty("http.proxyPort");
      System.clearProperty("http.proxyUser");
      System.clearProperty("http.proxyPassword");
    }

    // HTTPS proxy
    ProxyServerConfiguration https = proxy.getHttps();
    if (https != null && https.isEnabled()) {
      System.setProperty("https.proxyHost", https.getHost());
      System.setProperty("https.proxyPort", Integer.toString(https.getPort()));
      if (https.getAuthentication() != null) {
        if (https.getAuthentication() instanceof UsernameAuthenticationConfiguration) {
          UsernameAuthenticationConfiguration usernamePassword = (UsernameAuthenticationConfiguration) https
              .getAuthentication();
          System.setProperty("https.proxyUser", usernamePassword.getUsername());
          System.setProperty("https.proxyPassword", usernamePassword.getPassword());
        }
        else {
          log.warn("Authentication {} not supported for Java Networking, system properties not set",
              https.getAuthentication().getClass().getSimpleName());
        }
      }
      else {
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
      }
    }
    else {
      System.clearProperty("https.proxyHost");
      System.clearProperty("https.proxyPort");
      System.clearProperty("https.proxyUser");
      System.clearProperty("https.proxyPassword");
    }

    // nonProxyHosts
    if (proxy.getNonProxyHosts() != null) {
      System.setProperty("http.nonProxyHosts", Joiner.on("|").join(proxy.getNonProxyHosts()));
    }
    else {
      System.clearProperty("http.nonProxyHosts");
    }
  }

  /**
   * Apply authentication configuration to plan.
   */
  private void apply(final AuthenticationConfiguration authentication,
                     final HttpClientPlan plan,
                     @Nullable final HttpHost proxyHost)
  {
    Credentials credentials;
    List<String> authSchemes;

    if (authentication instanceof UsernameAuthenticationConfiguration) {
      UsernameAuthenticationConfiguration auth = (UsernameAuthenticationConfiguration) authentication;
      authSchemes = ImmutableList.of(DIGEST, BASIC);
      credentials = new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword());
    }
    else if (authentication instanceof NtlmAuthenticationConfiguration) {
      NtlmAuthenticationConfiguration auth = (NtlmAuthenticationConfiguration) authentication;
      authSchemes = ImmutableList.of(NTLM, DIGEST, BASIC);
      credentials = new NTCredentials(auth.getUsername(), auth.getPassword(), auth.getHost(), auth.getDomain());
    }
    else {
      throw new IllegalArgumentException("Unsupported authentication configuration: " + authentication);
    }

    if (proxyHost != null) {
      plan.addCredentials(new AuthScope(proxyHost), credentials);
      plan.getRequest().setProxyPreferredAuthSchemes(authSchemes);
    }
    else {
      plan.addCredentials(AuthScope.ANY, credentials);
      plan.getRequest().setTargetPreferredAuthSchemes(authSchemes);
    }
  }
}
