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
  $.widget('eucalyptus.eucadialog', {
    options : { 
       id : null, // e.g., keys-add, keys-delete, etc
       title : null,
       buttons :  {},
       help : null,  // help title and content 
       // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
    },
    help_flipped : false,
    
    _init : function() {
      var thisObj = this;
      this.element.dialog({
         autoOpen: false,  // assume the three params are fixed for all dialogs
         modal: true,
         width: 600,
         title: thisObj.options.title,
         open: function(event, ui) {
             $titleBar = thisObj.element.parent().find('.ui-dialog-titlebar');
             if ( $titleBar.find("div.help-link").length == 0 ) {
               // first time opened
               $titleBar.append($('<div>').addClass('help-link').append($('<a>').attr('href','#').text('?')));
             }

             $('.ui-widget-overlay').live("click", function() {
               thisObj.close();
             });

             $.each(thisObj.options.buttons, function (btn_id, btn_prop){ 
               // random ID will cause trouble with selenium
               var $btn = $(":button:contains('"+ btn_id +"')");
               if(btn_prop.domid !== undefined)
                 $btn.attr('id',btn_prop.domid);
               else
                 $btn.attr('id', 'btn-'+thisObj.options.id);
               $btn.find('span').text(btn_prop.text);
               if(btn_prop.focus !== undefined && btn_prop.focus)
                 $btn.focus();    
               if(btn_prop.disabled !== undefined && btn_prop.disabled)
                 $btn.prop("disabled", true).addClass("ui-state-disabled");
             });

             /* page-level help -- prototype */
             thisObj._setHelp(thisObj.element.parent());
         },
         buttons: thisObj._makeButtons(),
      });
    },
    
    open : function() {
      this.element.dialog('open');      
    },
    
    close : function() {
      // this method should clean-up things
      this.element.dialog('close');
      this.element.find('input').each(function () { 
        $(this).val(''); // clear all input fields TODO: what if some fields have initialized data?
      });
      if(this.help_flipped){
        this.element.find('.dialog-inner-content').revertFlip();
        $buttonPane.show();
        $titleBar.find('span').text(thisObj.options.title);
      }  
    },
    
    _create : function() {  
    },

    _destroy : function() {
    },

    _setHelp : function($dialog) {
      var thisObj = this;
      $buttonPane = $dialog.find('.ui-dialog-buttonpane');
      $titleBar = $dialog.find('.ui-dialog-titlebar');
      $contentPane =  this.element.find('.dialog-inner-content');

      $helpLink = $titleBar.find('.help-link a');
      $helpLink.click(function(evt) {
        if(!thisObj.help_flipped){ // TODO: is this right comparison(text comparison)?
          $contentPane.flip({
            direction : 'lr',
            speed : 300,
            color : 'white',
            bgColor : 'white',
            easingIn: 'easeInQuad',
            easingOut: 'easeOutQuad',
            content : thisObj.options.help.content,
            onEnd : function(){
              thisObj.element.find('.help-revert-button a').click( function(evt) {
                  $contentPane.revertFlip();
              });
              // at the end of flip/revertFlip, change the ?/x button
              if(!thisObj.help_flipped){
                $helpLink.html('&larr;');
                if(thisObj.options.help.title)
                  $titleBar.find('span').text(thisObj.options.help.title);
                $buttonPane.hide(); 
                thisObj.help_flipped =true;
              } else{
                $helpLink.text('?');
                $buttonPane.show();
                $titleBar.find('span').text(thisObj.options.title);
                thisObj.help_flipped=false;
              }
            }
          });
        }else{ // when flipped to help page
          $contentPane.revertFlip();
        }
      }); 
    },

    _makeButtons : function() {
      var btnArr = [];
       // e.g., add : { text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
      $.each(this.options.buttons, function (btn_id, btn_prop){
        var btn = {}
        btn.text = btn_id; // this will be replaced in open()
        $.each(btn_prop, function(key, value){
          if (typeof(value) == "function")
            btn[key] = value;
        });
        btnArr.push(btn);  
      });
      return btnArr; 
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
