$(function() {

    // dont track any elements with data-ga-event="false"
    var exclude = '[data-ga-event="false"]';

    // only take the first part of titles
    var title = (function() {
        var s = $('title').text();
        s = s.substring(0, s.indexOf(' - ')).replace(/:/g, ' -').replace(/\r?\n|\r/g, '');
        return s;
    })();


    //accordion sections (open/close)
	$(':button.govuk-accordion__section-button').each(function(){
		$(this).click(function(e){
			var pageTitle = $('title').text();
			var category = ($(this).attr('aria-expanded') === 'false') ? "accordion - expand" : "accordion - hide";
			var label = $(this).text().trim();
			dataLayer.push(
				{
					'event': 'custom_agents_request',
					'agents_event_category': category,
					'agents_event_action': pageTitle,
					'agents_event_label': label
				});
		});
	});

	//accordion open/close all
	$(':button.govuk-accordion__open-all').each(function(){
		$(this).click(function(e){
			var pageTitle = $('title').text();
			var category = ($(this).attr('aria-expanded') === 'true') ? "accordion - expand" : "accordion - hide";
			var label = $(this).text().trim();
			dataLayer.push(
				{
					'agents_event_category': category,
					'agents_event_action': pageTitle,
					'agents_event_label': label
				});
		});
	});

    // not ga-event, for EACD screen
	$('[name="landing"]').each(function(){
		$(this).click(function(e){
				window.close()

		});
	});



});


