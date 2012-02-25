if (phantom.args.length != 1) {
    console.log('Expected a target URL parameter.');
    phantom.exit(1);
}

var page = new WebPage();
var url = phantom.args[0];

page.onConsoleMessage = function (message) {
    console.log("App console: " + message);
};

console.log("Loading URL: " + url);

page.open(url, function (status) {
    if (status != "success") {
        console.log('Failed to open ' + url);
        phantom.exit(1);
    }

    console.log("Loaded successfully.");
});
