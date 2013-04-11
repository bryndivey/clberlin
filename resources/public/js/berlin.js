(function () {
    $(document).on('click.button.data-id', function(e) {
	var $btn = $(e.target);
	if(["hidden", "seen"].indexOf($btn.attr("value")) === -1) {
	    return true;
	}

	$.post($btn.closest('form').attr('action'), {"action": $btn.attr("value"), "inline": 1});
	$btn.closest('div.span9').hide();
	console.log($btn);
	return false;
	})
})();

	