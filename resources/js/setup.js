var global=global||this,self=self||this,nashorn=!0,console=global.console||{};['error','log','info','warn'].forEach(function(o){o in console||(console[o]=function(){})}),['setTimeout','setInterval','setImmediate','clearTimeout','clearInterval','clearImmediate'].forEach(function(o){o in global||(global[o]=function(){})});