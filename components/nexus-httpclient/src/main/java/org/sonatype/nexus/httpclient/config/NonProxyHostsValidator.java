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

import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

/**
 * {@link NonProxyHosts} validator.
 *
 * @since 3.0
 */
public class NonProxyHostsValidator
    extends ConstraintValidatorSupport<NonProxyHosts, String[]>
{
  /**
   * Wildcard in a nonProxyHost element may be only on it's beginning or end, nowhere else.
   */
  private static final Pattern WILDCARD_PATTERN = Pattern.compile("\\*?[^*]+\\*?");

  @Override
  public boolean isValid(final String[] values, final ConstraintValidatorContext context) {
    for (String value : values) {
      if (!isValid(value)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if value is considered as valid nonProxyHosts expression. This is NOT validating the
   * single-string used to set system property (where expressions are delimited with "|")!
   */
  private boolean isValid(final String value) {
    // A value should be a non-empty string optionally prefixed or suffixed with an asterisk
    // must be non-empty, non-blank
    if (Strings2.isBlank(value)) {
      return false;
    }
    // must not contain | separator (used to separate multiple values in system properties)
    if (value.indexOf('|') > -1) {
      return false;
    }
    // asterisk '*' can be only on beginning or end
    // FIXME: IPv6 with wildcard: seems to not work, as sun.misc.RegexpPool does only prefix/suffix matching, does not accounts for "[" or "]"!?
    if (value.indexOf('*') > -1) {
      if (!WILDCARD_PATTERN.matcher(value).matches()) {
        return false;
      }
    }
    // is it IP4/IP6 maybe? IP6 surrounded with "[]" must have brackets removed
    String val = value;
    if (val.startsWith("[") && val.endsWith("]")) {
      val = val.substring(1, val.length() - 1);
    }
    if (InetAddresses.isInetAddress(val.replaceAll("\\*", "1"))) {
      return true;
    }
    // is it internet domain name
    if (InternetDomainName.isValid(value.replaceAll("\\*", "foo"))) {
      return true;
    }
    return false;
  }
}
