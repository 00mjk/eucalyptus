/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.volume', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    delDialog : null,
    detachDialog : null,
    forceDetachDialog : null,
    addDialog : null,
    waitDialog : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #volumeTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $volTable = $wrapper.children().first();
      var $volHelp = $wrapper.children().last();
      this.baseTable = $volTable;
      this.element.add($volTable);
      this.tableWrapper = $volTable.eucatable({
        id : 'volumes', // user of this widget should customize these options,
        dt_arg : {
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeVolumes",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "sDom": '<"table_volumes_header"><"table-volume-filter">f<"clear"><"table_volumes_top">rt<"table-volumes-legend">p<"clear">',
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "id" },
            {
              "fnRender": function(oObj) { s = (oObj.aData.status == 'in-use') ? oObj.aData.attach_data.status : oObj.aData.status; return '<div title="'+ $.i18n.map['volume_state_' + s.replace('-','_')] +'" class="volume-status-' + oObj.aData.status + '">&nbsp;</div>'; },
              "sWidth": "20px",
              "bSearchable": false,
              "iDataSort": 8, // sort on hiden status column
            },
            { "mDataProp": "size" },
            { "mDataProp": "attach_data.instance_id" },
            { "mDataProp": "snapshot_id" },
            { "mDataProp": "zone" },
            // output creation time in browser format and timezone
            { "fnRender": function(oObj) { d = new Date(oObj.aData.create_time); return d.toLocaleString(); } },
            {
              "bVisible": false,
              "mDataProp": "status"
            }
          ],
        },
        text : {
          header_title : volume_h_title,
          create_resource : volume_create,
          resource_found : volume_found,
        },
        menu_actions : function(){ return thisObj._buildActionsMenu()},
        context_menu : {build_callback : function(state) { return thisObj.buildContextMenu(state);}},
        menu_click_create : function (args) { thisObj.waitDialog.eucadialog('open')},
        row_click : function (args) { thisObj._handleRowClick(args); },
      //  td_hover_actions : { instance: [4, function (args) { thisObj.handleInstanceHover(args); }], snapshot: [5, function (args) { thisObj.handleSnapshotHover(args); }] }
        help_click : function(evt) {
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_volume['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
          thisObj._flipToHelp(evt,$helpHeader, $volHelp);
        },
      });
      this.tableWrapper.appendTo(this.element);

      //add filter to the table
      // TODO: make templates
      $tableFilter = $('div.table-volume-filter');
      $tableFilter.addClass('euca-table-filter');
      $tableFilter.append(
        $('<span>').addClass("filter-label").html(table_filter_label),
        $('<select>').attr('id', 'volumes-selector'));

      filterOptions = ['all', 'attached', 'detached'];
      $sel = $tableFilter.find("#volumes-selector");
      for (o in filterOptions)
        $sel.append($('<option>').val(filterOptions[o]).text($.i18n.map['volume_selecter_' + filterOptions[o]]));

      $.fn.dataTableExt.afnFiltering.push(
	function( oSettings, aData, iDataIndex ) {
          // first check if this is called on a volumes table
          if (oSettings.sInstance != 'volumes')
            return true;
          selectorValue = $("#volumes-selector").val();
          switch (selectorValue) {
            case 'attached':
              return 'in-use' == aData[8];
              break;
            case 'detached':
              return 'available' == aData[8];
              break;
          }
          return true;
        }
      );

      // attach action
      $("#volumes-selector").change( function() { thisObj.reDrawTable() } );

      // TODO: let's move legend to html as a template
      //add leged to the volumes table
      $tableLegend = $("div.table-volumes-legend");
      $tableLegend.append($('<span>').addClass('volume-legend').html(volume_legend));

      statuses = ['available', 'in-use', 'creating', 'deleting', 'deleted', 'error'];
      for (s in statuses)
        $tableLegend.append($('<span>').addClass('volume-status-legend').addClass('volume-status-' + statuses[s]).html($.i18n.map['volume_state_' + statuses[s].replace('-','_')]));

      $tmpl = $('html body').find('.templates #volumeDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.delDialog = $del_dialog.eucadialog({
         id: 'volumes-delete',
         title: volume_dialog_del_title,
         buttons: {
           'delete': {text: volume_dialog_del_btn, click: function() { thisObj._deleteListedVolumes(); $del_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_delete_title'], content: $del_help},
       });

      $tmpl = $('html body').find('.templates #volumeDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $detach_dialog = $rendered.children().first();
      var $detach_help = $rendered.children().last();
      this.detachDialog = $detach_dialog.eucadialog({
         id: 'volumes-detach',
         title: volume_dialog_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, click: function() { thisObj._detachListedVolumes(false); $detach_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $detach_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_detach_title'], content: $detach_help},
       });

      $tmpl = $('html body').find('.templates #volumeForceDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $force_detach_dialog = $rendered.children().first();
      var $force_detach_help = $rendered.children().last();
      this.forceDetachDialog = $force_detach_dialog.eucadialog({
         id: 'volumes-force-detach',
         title: volume_dialog_force_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, click: function() { thisObj._detachListedVolumes(true); $force_detach_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $force_detach_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_force_detach_title'], content: $force_detach_help},
       });

      $tmpl = $('html body').find('.templates #volumeWaitDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $wait_dialog = $rendered.children().first();
      var $wait_dialog_help = $rendered.children().last();
      this.waitDialog = $wait_dialog.eucadialog({
         id: 'volumes-wait',
         title: volume_dialog_wait,
         buttons: {
           'cancel': { text: volume_dialog_cancel_btn, focus:true, click: function() { $wait_dialog.dialog("close"); } } 
         },
         help: {title: help_volume['dialog_volume_wait_title'], content: $wait_dialog_help},
         onOpen: function(dialog) { thisObj._initAddDialog(); }
       });

      var createButtonId = 'volumes-add-btn';
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();
      this.addDialog = $add_dialog.eucadialog({
         id: 'volumes-add',
         title: volume_dialog_add_title,
         buttons: {
           'create': { domid: createButtonId, text: volume_dialog_create_btn, disabled: true, click: function() { 
              var size = $.trim($add_dialog.find('#volume-size').val());
              var az = $add_dialog.find('#volume-add-az-selector').val();
              var $snapshot = $add_dialog.find('#volume-add-snapshot-selector :selected');
              var isValid = true;
              $notification = $add_dialog.find('div.dialog-notifications');
              if ( size == parseInt(size) ) {
                if ( $snapshot.val() != '' && parseInt($snapshot.attr('title')) > parseInt(size) ) {
                  isValid = false;
                  $notification.html(volume_dialog_snapshot_error_msg);
                }
              } else {
                isValid = false; 
                $notification.html(volume_dialog_size_error_msg);
              }
              if ( az === '' ) {
                isValid = false;
                $notification.html($notification.html + "<br/>" + volume_dialog_size_error_msg);
              }
              if ( isValid ) {
                thisObj._createVolume(size, az, $snapshot.val());
                $add_dialog.dialog("close");
              } 
            }},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_add_title'], content: $add_help},
       });
       this.addDialog.eucadialog('onKeypress', 'volume-size', createButtonId, function () {
         var az = thisObj.addDialog.find('#volume-add-az-selector').val();
         return az != '';
       });
       this.addDialog.find('#volume-add-az-selector').change( function () {
         size = $.trim(thisObj.addDialog.find('#volume-size').val());
         az = thisObj.addDialog.find('#volume-add-az-selector').val();
         $button = thisObj.addDialog.parent().find('#' + createButtonId);
         if ( size.length > 0 && az !== '')     
           $button.prop("disabled", false).removeClass("ui-state-disabled");
         else
           $button.prop("disabled", false).addClass("ui-state-disabled");
       });
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _initAddDialog : function() {
      this.addDialog.find('div.dialog-notifications').html('');
      $.ajax({
          type:"GET",
          url:"/ec2?Action=DescribeAvailabilityZones",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"false",
          success:
           function(data, textStatus, jqXHR){
              $azSelector = $('#volume-add-az-selector').html('');
              $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
              if ( data.results ) {
                for( res in data.results) {
                  azName = data.results[res].name;
                  $azSelector.append($('<option>').attr('value', azName).text(azName));
                } 
              } else {
                notifyError('tbd', tbd);
              }
           },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError('tbd', tbd);
            }
      });
      $.ajax({
          type:"GET",
          url:"/ec2?Action=DescribeSnapshots",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"false",
          success:
           function(data, textStatus, jqXHR){
              $snapSelector = $('#volume-add-snapshot-selector').html('');
              $snapSelector.append($('<option>').attr('value', '').text($.i18n.map['selection_none']));
              if ( data.results ) {
                for( res in data.results) {
                  snapshot = data.results[res];
                  if ( snapshot.status === 'completed' ) {
                    $snapSelector.append($('<option>').attr('value', snapshot.id).attr('title', snapshot.volume_size).text(
                      snapshot.id + ' (' + snapshot.volume_size + ' ' + $.i18n.map['size_gb'] +')'));
                  }
                } 
              } else {
                notifyError('tbd', tbd);
              }
           },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError('tbd', tbd);
            }
        });
      this.waitDialog.dialog("close");
      this.addDialog.dialog("open");
    },

    _buildActionsMenu : function() {
      thisObj = this;
      selectedVolumes = thisObj.tableWrapper.eucatable('getValueForSelectedRows', 8); // 8th column=status (this is volume's knowledge)
      itemsList = {};
      // add attach action
      if ( selectedVolumes.length == 1 && selectedVolumes.indexOf('available') == 0 ){
         itemsList['attach'] = { "name": volume_action_attach, callback: function(key, opt) { thisObj.attachAction(); } }
      }
      // detach actions
      if ( selectedVolumes.length > 0 ) {
        addDetach = true;
        for (s in selectedVolumes) {
          if ( selectedVolumes[s] != 'in-use' ) {
            addDetach = false;
            break;
          }
        }
        if ( addDetach ) {
          itemsList['detach'] = { "name": volume_action_detach, callback: function(key, opt) { thisObj.detachAction(false); } }
          itemsList['force_detach'] = { "name": volume_action_force_detach, callback: function(key, opt) { thisObj.detachAction(true); } }
        }
      }
      if ( selectedVolumes.length  == 1 ) {
         if ( selectedVolumes[0] == 'in-use' || selectedVolumes[0] == 'available' ) {
            itemsList['create_snapshot'] = { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj.createSnapshotAction(); } }
        }
      }
      // add delete action
      if ( selectedVolumes.length > 0 ){
         itemsList['delete'] = { "name": volume_action_delete, callback: function(key, opt) { thisObj.deleteAction(); } }
      }
      return itemsList;
    },

    _getVolumeId : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    buildContextMenu : function(row) {
     // var thisObj = this; ==> this causes the problem..why?
      var thisObj = $('html body').find(DOM_BINDING['main']).data("volume");

      var state = row['status'];
      switch (state) {
        case 'available':
          return {
            "attach": { "name": volume_action_attach, callback: function(key, opt) { thisObj.attachAction(thisObj._getVolumeId(opt.selector)); } },
            "create_snapshot": { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj.createSnapshotAction(thisObj._getVolumeId(opt.selector)); } },
            "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj.deleteAction(thisObj._getVolumeId(opt.selector)); } }
          }
        case 'in-use':
          return {
            "detach": { "name": volume_action_detach, callback: function(key, opt) { thisObj.detachAction($(opt.selector), false); } },
            "force_detach": { "name": volume_action_force_detach, callback: function(key, opt) { thisObj.detachAction($(opt.selector), true); } },
            "create_snapshot": { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj.createSnapshotAction(thisObj._getVolumeId(opt.selector)); } },
            "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj.deleteAction(thisObj._getVolumeId(opt.selector)); } }
          }
        default:
          return {
            "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj.deleteAction(thisObj._getVolumeId(opt.selector)); } }
          }
      }
    },
/*
    handleInstanceHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },

    handleSnapshotHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          $(e.currentTarget).off('click');
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },
*/
    reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _handleRowClick : function(args) {
      if ( this.tableWrapper.eucatable('countSelectedRows') == 0 )
        this.tableWrapper.eucatable('deactivateMenu');
      else
        this.tableWrapper.eucatable('activateMenu');
    },

    _deleteListedVolumes : function () {
      thisObj = this;
      $volumesToDelete = this.delDialog.find("#volumes-to-delete");
      var rowsToDelete = $volumesToDelete.text().split(ID_SEPARATOR);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var volumeId = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DeleteVolume&VolumeId=" + volumeId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess('delete volume', volume_delete_success + ': ' + volumeId);
                thisObj.baseTable.eucatable('refreshTable');
              } else {
                notifyError('delete volume', volume_delete_error + ': ' + volumeId); // TODO: error code
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError('delete volume', volume_delete_error + ': ' + volumeId); // TODO: error code?
            }
          })(volumeId)
        });
      }
    },

    _createVolume : function (size, az, snapshotId) {
      thisObj = this;
      sid = snapshotId != '' ? "&SnapshotId=" + snapshotId : '';
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateVolume&Size=" + size + "&AvailabilityZone=" + az + sid,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"true",
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess('add-volume', volume_create_success + ' ' + data.results.id);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError('add-volume', volume_create_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError('add-volume', volume_create_error);
          }
      });
    },

    _detachListedVolumes : function (force) {
      thisObj = this;
      dialogToUse = force ? this.forceDetachDialog : this.detachDialog; 
      $volumesToDelete = dialogToUse.find("#volumes-to-detach");
      var volumes = $volumesToDelete.text().split(ID_SEPARATOR);
      for ( i = 0; i<volumes.length; i++ ) {
        var volumeId = volumes[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DetachVolume&VolumeId=" + volumeId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                if (force)
                  notifySuccess('force-detach-volume', volume_force_detach_success + ' ' + volumeId);
                else
                  notifySuccess('detach-volume', volume_detach_success + ' ' + volumeId);
                thisObj.baseTable.eucatable('refreshTable');
              } else {
                if (force)
                  notifyError('force-detach-volume', volume_force_detach_error + ' ' + volumeId);
                else
                  notifyError('detach-volume', volume_detach_error + ' ' + volumeId);
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              if (force)
                notifyError('force-detach-volume', volume_force_detach_error + ' ' + volumeId);
              else
                notifyError('detach-volume', volume_detach_error + ' ' + volumeId);
            }
          })(volumeId)
        });
      }
    },

    close: function() {
      this._super('close');
    },

    deleteAction : function(volumeId) {
      volumesToDelete = [];
      if ( !volumeId ) {
        $tableWrapper = thisObj.tableWrapper;
        volumesToDelete = $tableWrapper.eucatable('getAllSelectedRows');
      } else {
        volumesToDelete[0] = volumeId;
      }

      if ( volumesToDelete.length > 0 ) {
        // show delete dialog box
        $deleteNames = this.delDialog.find("span.resource-ids")
        $deleteNames.html('');
        $volumesToDelete = this.delDialog.find("#volumes-to-delete");
        $volumesToDelete.html(volumesToDelete.join(ID_SEPARATOR));
        for ( i = 0; i<volumesToDelete.length; i++ ) {
          t = escapeHTML(volumesToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.delDialog.dialog('open');
      }
    },

    attachAction : function() {
      selectedRows = this.tableWrapper.eucatable('getAllSelectedRows');
    },

    detachAction : function(row, force) {
      volumes = [];
      if ( !row ) {
        $tableWrapper = thisObj.tableWrapper;
        rows = $tableWrapper.eucatable('getContentForSelectedRows');
        for(r in rows){
          $row = $(rows[r]);
          volumes.push([$row.find('td:eq(1)').text(), $row.find('td:eq(4)').text()]);
        }
      } else {
        volumes.push([row.find('td:eq(1)').text(), row.find('td:eq(4)').text()]);
      }

      dialogToUse = force ? this.forceDetachDialog : this.detachDialog;
      if ( volumes.length > 0 ) {
        $detachIds = dialogToUse.find("tbody.resource-ids");
        $detachIds.html('');
        $volumesToDetach = dialogToUse.find("#volumes-to-detach");
        ids = [];
        for ( i = 0; i<volumes.length; i++ ) {
          vol = escapeHTML(volumes[i][0]);
          inst = escapeHTML(volumes[i][1]);
          ids.push(volumes[i][0]);
          $tr = $('<tr>').append($('<td>').text(vol),$('<td>').text(inst));
          $detachIds.append($tr);
        }
        $volumesToDetach.html(ids.join(ID_SEPARATOR));
        dialogToUse.dialog('open');
      }
    },

    createSnapshotAction : function() {
      selectedRows = this.tableWrapper.eucatable('getAllSelectedRows');
    },

  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
