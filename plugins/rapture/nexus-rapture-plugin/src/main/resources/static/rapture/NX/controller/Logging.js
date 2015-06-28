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
  setRemote: function(flag) {
    var me = this;
    this.remote = flag;
    //<if debug>
    me.logInfo('Remote events:', flag ? 'enabled' : 'disabled');
    //</if>
  },

  /**
   * Record a log-event.
   *
   * @public
   * @param event
   */
  recordEvent: function(event) {
    var me = this,
        store = me.getStore('LogEvent');

    // ensure events have a timestamp
    if (!event.timestamp) {
      event.timestamp = new Date();
    }

    store.add(event);

    // HACK: experimental: remote events to server
    if (me.remote) {
      var copy = Ext.clone(event);

      // HACK: kill timestamp... GSON freaks out
      delete copy.timestamp;

      NX.direct.rapture_LogEvent.recordEvent(copy);
    }
  }
});
