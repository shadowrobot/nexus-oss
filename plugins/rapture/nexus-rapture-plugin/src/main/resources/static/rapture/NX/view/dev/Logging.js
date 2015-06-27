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
 * Logging dev-panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Logging', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-dev-logging',

  title: 'Logging',
  store: 'LogEvent',
  emptyText: 'No events',
  viewConfig: {
    deferEmptyText: false
  },

  columns: [
    { text: 'level', dataIndex: 'level' },
    { text: 'logger', dataIndex: 'logger', flex: 1 },
    { text: 'message', dataIndex: 'message', flex: 3 },
    { text: 'timestamp', dataIndex: 'timestamp', width: 300 }
  ],

  tbar: [
    {
      xtype: 'button',
      text: 'Clear events',
      action: 'clear'
    },
    '-',
    {
      xtype: 'checkbox',
      boxLabel: 'Remote events',
      itemId: 'remote'
    }
  ],

  plugins: [
    'gridfilterbox'
  ]
});