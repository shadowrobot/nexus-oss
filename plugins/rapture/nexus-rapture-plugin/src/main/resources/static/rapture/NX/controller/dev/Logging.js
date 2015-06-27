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
/*global Ext*/

/**
 * Logging dev-panel controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.dev.Logging', {
  extend: 'Ext.app.Controller',

  stores: [
    'LogEvent'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-dev-logging'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'nx-dev-logging button[action=clear]': {
          click: me.clearStore
        },
        'nx-dev-logging checkbox[itemId=remote]': {
          change: me.toggleRemote
        }
      }
    });
  },

  /**
   * Clear the LogEvent store.
   *
   * @private
   */
  clearStore: function () {
    var me = this,
        store = me.getStore('LogEvent');

    store.removeAll();
  },

  /**
   * Toggle event remoting.
   *
   * @private
   * @param {Ext.form.field.Checkbox} checkbox
   */
  toggleRemote: function(checkbox) {
    this.getController('Logging').setRemote(checkbox.getValue());
  }
});