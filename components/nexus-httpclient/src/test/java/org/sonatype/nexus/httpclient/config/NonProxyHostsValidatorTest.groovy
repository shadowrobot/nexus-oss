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

package org.sonatype.nexus.httpclient.config

import javax.validation.ConstraintValidatorContext

import org.sonatype.sisu.litmus.testsupport.TestSupport

import org.junit.Test
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.mock

/**
 * Tests for {@link NonProxyHostsValidator}.
 */
class NonProxyHostsValidatorTest
    extends TestSupport
{
  @Mock
  ConstraintValidatorContext context

  NonProxyHostsValidator validator = new NonProxyHostsValidator()

  private test(String expression, boolean expected) {
    assertThat(validator.isValid([expression].toArray(new String[0]), context), equalTo(expected))
  }

  @Test
  void 'validation positive test'() {
    test('sonatype.org', true)
    test('*.sonatype.org', true)
    test('*.sonatype.*', true)
    test('1.2.3.4', true)
    test('*.2.3.4', true)
    test('1.2.3.*', true)
    test('*.2.3.*', true)
    test('csétamás.hu', true)
    test('2001:db8:85a3:8d3:1319:8a2e:370:7348', true)
    test('[2001:db8:85a3:8d3:1319:8a2e:370:7348]', true)
    test('[::1]', true)
    test('localhost', true)
  }

  @Test
  void 'validation negative test' () {
    test('', false)
    test('  ', false)
    test('foo|sonatype.org', false)
    test('sonatype..org', false)
    test('*..sonatype.*', false)
    test('1..2.3.4', false)
    test('[[2001:db8:85a3:8d3:1319:8a2e:370:7348]', false)
    test('[2001:db8:85a3:8d3:1319:8a2e:370:7348', false)
    test('2001:db8:85a3:8d3:1319:8a2e:370:7348]', false)
  }
}
