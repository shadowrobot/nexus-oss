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
 * Logging controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Logging', {
  extend: 'Ext.app.Controller',
  require: [
    'NX.Log'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  stores: [
    'LogEvent'
  ],

  /**
   * Enable event remoting.
   *
   * @property {Boolean}
   */
  remote: false,

  /**
   * Logging threshold.
   *
   * @property {string}
   */
  threshold: 'debug',

  /**
   * Attach to NX.Log helper.
   *
   * @override
   */
  onLaunch: function () {
    NX.Log.attach(this);
  },

  /**
   * Toggle event remoting.
   *
   * @public
   * @param {boolean} flag
   */
  setRemote: function (flag) {
    var me = this;
    this.remote = flag;
    //<if debug>
    me.logInfo('Remote events:', flag ? 'enabled' : 'disabled');
    //</if>
  },

  /**
   * @public
   * @returns {string}
   */
  getThreshold: function () {
    return this.threshold;
  },

  /**
   * @public
   * @param {string} threshold
   */
  setThreshold: function (threshold) {
    this.threshold = threshold;
  },

  /**
   * Mapping of NX.store.LogLevel weights.
   *
   * @private
   */
  levelWeights: {
    all: 1,
    trace: 2,
    debug: 3,
    info: 4,
    warn: 5,
    off: 6
  },

  /**
   * Check if given level exceeds configured threshold.
   *
   * @private
   * @param {string} level
   * @return {boolean}
   */
  exceedsThreshold: function (level) {
    return this.levelWeights[level] >= this.levelWeights[this.threshold];
  },

  /**
   * Record a log-event.
   *
   * @public
   * @param event
   */
  recordEvent: function (event) {
    var me = this;

    // ignore events that do not exceed threshold
    if (!me.exceedsThreshold(event.level)) {
      return;
    }

    // ensure events have a timestamp
    if (!event.timestamp) {
      event.timestamp = new Date();
    }

    me.getStore('LogEvent').add(event);

    // HACK: experimental: remote events to server
    if (me.remote) {
      // HACK: kill timestamp... GSON freaks out
      var copy = Ext.clone(event);
      delete copy.timestamp;

      NX.direct.rapture_LogEvent.recordEvent(copy);
    }
  }
});
