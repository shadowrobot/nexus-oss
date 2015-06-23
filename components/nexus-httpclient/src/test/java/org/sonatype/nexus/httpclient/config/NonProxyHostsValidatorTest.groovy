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

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.mock

/**
 * Tests for {@link NonProxyHostsValidator}.
 */
class NonProxyHostsValidatorTest
    extends TestSupport
{
  NonProxyHostsValidator validator = new NonProxyHostsValidator()

  @Test
  void 'validatiom test'() {
    assertThat(validator.isValid(['sonatype.org'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['*.sonatype.org'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['sonatype.*'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['*.sonatype.*'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['1.2.3.4'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['*.2.3.4'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['1.2.3.*'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['*.2.3.*'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))
    assertThat(validator.isValid(['csétamás.hu'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(true))

    assertThat(validator.isValid([''].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
    assertThat(validator.isValid(['  '].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
    assertThat(validator.isValid(['foo|sonatype.org'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
    assertThat(validator.isValid(['sonatype..org'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
    assertThat(validator.isValid(['*..sonatype.*'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
    assertThat(validator.isValid(['1..2.3.4'].toArray(new String[0]), mock(ConstraintValidatorContext)), equalTo(false))
  }
}
