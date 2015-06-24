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
 * Asset info panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetInfo', {
  extend: 'NX.view.info.Panel',
  alias: 'widget.nx-coreui-component-assetinfo',
  requires: [
    'NX.I18n'
  ],

  /**
   * model to display 
   */
  assetModel: null,
  
  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.callParent(arguments);
    
    me.setTitle(NX.I18n.get('Component_AssetInfo_Info_Title'));
  },

  /**
   * @public
   * @param {object} assetModel the asset to display 
   * @param {String} format the format for the asset
   */
  setAssetModel: function(assetModel, format) {
    var me = this,
        info = {};
    me.assetModel = assetModel;
    
    // display common data
    var contentType = assetModel.get('contentType');
    var size = assetModel.get('size');
    info[NX.I18n.get('Assets_Info_Path')] = NX.util.Url.asRepositoryLink(assetModel, format);
    info[NX.I18n.get('Assets_Info_ContentType')] = contentType;
    info[NX.I18n.get('Assets_Info_FileSize')] = Ext.util.Format.fileSize(size);
    info[NX.I18n.get('Assets_Info_Last_Updated')] = new Date(assetModel.get('lastUpdated')) ;
    info[NX.I18n.get('Assets_Info_Locally_Cached')] = contentType !== 'unknown' && size > 0 ;
    info[NX.I18n.get('Assets_Info_BlobRef')] = assetModel.get('blobRef');
    me.showInfo(info);
  }

});
