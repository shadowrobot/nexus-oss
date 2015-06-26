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
package org.sonatype.nexus.transaction;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Wraps an intercepted method with transactional behaviour.
 *
 * @since 3.0
 */
class TransactionalWrapper
{
  private final Transactional spec;

  private final MethodInvocation mi;

  private final Class<?>[] methodThrows;

  public TransactionalWrapper(final Transactional spec, final MethodInvocation mi) {
    this.spec = spec;
    this.mi = mi;

    methodThrows = mi.getMethod().getExceptionTypes();
  }

  /**
   * Applies transactional behaviour around the method call, supports automatic retries.
   */
  public Object proceedWithTransaction(final Transaction tx) throws Throwable {
    while (true) {
      boolean committed = false;
      try {
        tx.begin();
        Throwable throwing = null;
        try {
          return mi.proceed();
        }
        catch (final Throwable e) { // make sure we capture VM errors here (will be rethrown later)
          throwing = e;
        }
        finally {
          if (throwing == null || requestCommit(throwing)) {
            tx.commit();
            committed = true;
          }
          if (throwing != null) {
            throw throwing;
          }
        }
      }
      catch (final Exception e) { // ignore VM errors as here as we don't rollback/retry on them
        if (!committed) {
          tx.rollback();
          if (requestRetry(e) && tx.allowRetry()) {
            continue;
          }
        }
        throw e;
      }
    }
  }

  /**
   * @return {@code true} if we should commit the transaction on this exception.
   */
  private boolean requestCommit(final Throwable e) {
    if (instanceOf(e, spec.ignore())) {
      return true; // explicit ignore overrides everything else
    }
    if (instanceOf(e, spec.retryOn())) {
      return false; // explicit retry implies explicit rollback
    }
    if (spec.rollbackOn().length > 0) {
      return !instanceOf(e, spec.rollbackOn());
    }
    return instanceOf(e, methodThrows);
  }

  /**
   * @return {@code true} if we should retry the transaction on this exception.
   */
  private boolean requestRetry(final Throwable e) {
    if (spec.retryOn().length > 0) {
      return instanceOf(e, spec.retryOn());
    }
    return e.getClass().getName().contains("Retry");
  }

  /**
   * @return {@code true} if the given object is an instance of one of the types.
   */
  private static boolean instanceOf(final Object object, final Class<?>... types) {
    for (final Class<?> t : types) {
      if (t.isInstance(object)) {
        return true;
      }
    }
    return false;
  }
}
