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
/*global Ext, NX*/

/**
 * Adds logging support helpers to objects.
 *
 * @since 3.0
 */
Ext.define('NX.LogAware', {
  requires: [
    'NX.Log'
  ],

  /**
   * @param {String} level
   * @param {Array} args
   */
  log: function (level, args) {
    //<if debug>
    NX.Log.recordEvent(level, Ext.getClassName(this), args.join(' '));
    //</if>
  },

  /**
   * @public
   */
  logTrace: function () {
    //<if debug>
    this.log('trace', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * @public
   */
  logDebug: function () {
    //<if debug>
    this.log('debug', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * @public
   */
  logInfo: function () {
    //<if debug>
    this.log('info', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * @public
   */
  logWarn: function () {
    //<if debug>
    this.log('warn', Array.prototype.slice.call(arguments));
    //</if>
  },

  /**
   * @public
   */
  logError: function () {
    //<if debug>
    this.log('error', Array.prototype.slice.call(arguments));
    //</if>
  }
});