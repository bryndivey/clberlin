(function () {
    $(document).on('click.button.data-id', function(e) {
	var $btn = $(e.target);
	if(["hidden", "seen"].indexOf($btn.attr("value")) === -1) {
	    return true;
	}

	$.post($btn.closest('form').attr('action'), {"action": $btn.attr("value"), "inline": 1});
	$btn.closest('div.span9').detach();
	console.log($btn);
	return false;
	})
})();

	
(function () {
    $(document).keypress('.input.comment-input', function(e) {
	if (e.keyCode != 13) {
	    return true;
	}

	var $input = $(e.target);
	console.log($input.val());

	$.post($input.closest('form').attr('action'), {"action": "comment", "value": $input.val(), "inline": 1});
	return false;
	})
});

(function () {
    $(".tags-input").each(function(i, e) {
	$(e).typeahead({source: tagsSource});});})();


(function () {
    var handler = function(e) {
	if (e.keyCode != 13) {
	    return true;
	}

	var $input = $(e.target);
	var val = $input.val();

	$.post($input.closest('form').attr('action'), {"action": "tag", "value": val, "inline": 1});
	$input.val("");

	var $newtag = $('<span class="label remove-tag">' + val + '</span>');
	$($input.closest('.listing').find('.tags')).append($newtag);
	if(tagsSource.indexOf(val) == -1) {
	    tagsSource.push(val);
	}
	return false;
    };

    $(document).keypress('input.tags-input', handler);
})();

(function () {
    $(document).on('click', '.remove-tag', function(e) {
	var $a = $(e.target);
	var data = {"action": "remove-tag", "value": $a.contents().text(), "inline": 1};
	$.post($a.closest('form').attr('action'), data);
	$a.detach();
	return false;
	})
})();

	

	