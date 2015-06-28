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
 * Global logging helper.
 *
 * @since 3.0
 */
Ext.define('NX.Log', {
  singleton: true,
  requires: [
    'NX.Console'
  ],

  /**
   * @private
   */
  controller: undefined,

  /**
   * Queue of events logged before controller is attached.
   * This is deleted upon attachment after events are passed to the controller.
   *
   * @private
   */
  eventQueue: [],

  /**
   * Attach to the logging controller.
   *
   * @public
   * @param {NX.controller.Logging} controller
   */
  attach: function (controller) {
    var me = this;
    me.controller = controller;

    // reply queued events and clear
    Ext.each(me.eventQueue, function (event) {
      me.controller.recordEvent(event);
    });
    delete me.eventQueue;

    NX.Console.info('Logging controller attached');
  },

  /**
   * Record a log event.
   *
   * @public
   * @param {string} level
   * @param {string} logger
   * @param {string} message
   */
  recordEvent: function (level, logger, message) {
    var me = this,
        event = {
          timestamp: new Date(),
          level: level,
          logger: logger,
          message: message
        };

    // if controller is attached, delegate to record the event
    if (me.controller) {
      me.controller.recordEvent(event);
    }
    else {
      // else buffer the event emit to console
      me.eventQueue.push(event);
      NX.Console.log(level, [logger, message]);
    }
  }
});