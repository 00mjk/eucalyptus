(function($, eucalyptus) {
  eucalyptus.explorer = function(){ 
    $.widget("eucalyptus.explorer", {
      options : {
        menus : [ {key:'dashboard', text:menu_dashboard}, 
                {key:'images', text:menu_images},
                {key:'instances', text:menu_instances},
                {key:'storage', text:menu_storage},
                {key:'netsec', text:menu_netsec}],
        submenus : { storage: [{key:'volume', text:menu_storage_volumes}, {key:'snapshot', text:menu_storage_snapshots},{key:'bucket',text:menu_storage_buckets}],
                  netsec: [{key:'sgroup',text:menu_netsec_sgroup},{key:'keypair',text:menu_netsec_keypair},{key:'eip',text:menu_netsec_eip}]},
      },

      _init : function() { },

      _create : function() {
        // resources-explorer
        var $ul = $('<ul>').addClass('resources-nav');
        for(i=0; i<this.options.menus.length; i++){
          $ul.append(this.createResourceMenu(i, this.options.menus[i]));
        }
        this.element.append($ul);
 
      },

      createUserMenu : function () {
        user_menu = { preference : menu_user_preferences,
                      logout : menu_user_logout,
        }
        var arr = [];
        var header = this;
        $.each(user_menu, function (key, val) {
          $.extend(arr, $('<li>').append(
             $('<a>').attr('href','#').text(val).click(function(evt) {
               header._trigger("select", evt, {selected:key});
             })));
        });
        return arr;
      },

      createResourceMenu : function (idx, menu) {
        var thisObj = this;
        var $submenu = $('<ul>');
        var header = this;
        if(menu.key in this.options.submenus){
          $.each(this.options.submenus[menu.key], function (idx, submenu){
            $submenu.append($('<li>').append(
                            $('<a>').attr('href','#').text(submenu.text).click(
                              function (evt){
                                header._trigger("select", evt, {selected:submenu.key}); 
                              })));
          });
        }
        var clsName = 'lnk-'+menu.key.toLowerCase();
        var $menu = $('<li>').append(
                         $('<a>').addClass(clsName).attr('href','#').text(menu.text));
        if(menu.key in this.options.submenus){
          $menu.append($submenu);
          $menu.find('a').click(function(evt) {
            $submenu.slideToggle('fast');
            $menu.toggleClass('toggle-on'); 
            $('html body').trigger('click','navigator:'+menu.key);
            if ($menu.hasClass('toggle-on'))
              $('html body').eucaevent('add_click', 'navigator:'+menu.key, evt);
            else
              $('html body').eucaevent('del_click', 'navigator:'+menu.key);
            return false;
          });
        }
        else {
          $menu.find('a').click( 
            function (evt) {
              header._trigger("select", evt, {selected:menu.key}); 
            }
          );
        }
        return $menu;
      },
      _destroy : function() { },

      // called when the explorer is slide down
      onSlide : function() { 
        // make sure no menus has 'toggle-on' class set
        this.element.find('.toggle-on').each( function() {
          $(this).removeClass('toggle-on');   
        }); 
 
        this.element.find('.resources-nav').each(function() {
          $(this).find('li ul').hide();
        });
      },
     }); // end of widget()
   } // end of eucalyptus.explorer()
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
