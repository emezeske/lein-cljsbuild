if (phantom.args.length != 1) {
    console.log('Expected a target URL parameter.');
    phantom.exit(1);
}

var page = new WebPage();
var url = phantom.args[0];

page.open(url, function (status) {
    if (status != "success") {
        console.log('Failed to open ' + url);
        phantom.exit(1);
    }

    // TODO: Should anything else happen here?
});
