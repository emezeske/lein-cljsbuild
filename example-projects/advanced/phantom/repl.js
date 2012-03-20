if (phantom.args.length != 1) {
    console.log('Expected a target URL parameter.');
    phantom.exit(1);
}

var page = require('webpage').create();
var url = phantom.args[0];
var page_opened = false;

page.onConsoleMessage = function (message) {
    console.log("App console: " + message);
};

console.log("Loading URL: " + url);

page.open(url, function (status) {
    // FIXME: This is a horrible, ridiculous hack.  For some reason, PhantomJS calls
    //        page.open() twice, and if doesn't return immediately the second time,
    //        things break.  Reference:
    //            http://code.google.com/p/phantomjs/issues/detail?id=353&q=wait&sort=-type
    if (page_opened) {
        return;
    }
    page_opened = true;

    if (status != "success") {
        console.log('Failed to open ' + url);
        phantom.exit(1);
    }

    console.log("Loaded successfully.");
});
