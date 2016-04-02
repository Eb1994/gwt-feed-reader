  1. Check out the Google Web Toolkit as described in [Making GWT Better](http://code.google.com/webtoolkit/makinggwtbetter.html)
    * Checking out the GWT 1.4 release branch is recommended instead of trunk:
```
svn co http://google-web-toolkit.googlecode.com/svn/releases/1.4 gwt-1.4
```
  1. `cd` to your GWT checkout directory (either `trunk` or `gwt-1.4`):
    * This command will link the GWT Feed Reader code into your checkout:
```
svn ps svn:externals 'feedreader http://gwt-feed-reader.googlecode.com/svn/trunk/' samples
```
  1. Run `svn update` to download the GwtFeedReader code
  1. Edit `samples/build.xml`:
    1. Add a new target
```
<target name="feedreader" description="Build feedreader">
  <gwt.ant dir="feedreader" />
</target>

```
    1. Add `feedreader` to the `-do` target's dependencies:
```
- <target name="-do" depends="dynatable, hello, i18n, json, kitchensink, mail, simplexml" description="Run all subprojects" />
+ <target name="-do" depends="dynatable, feedreader, hello, i18n, json, kitchensink, mail, simplexml" description="Run all subprojects" />
```
  1. Obtain an [API key](http://code.google.com/apis/ajaxfeeds/signup.html).
  1. Rename `src/com/google/gwt/sample/feedreader/public/GwtFeedReader.html.template` and add your API key to the script tag that sets `window.AjaxFeedApiKey`.
  1. Run `ant samples` to compile.  The output will be in `build/out/samples/GwtFeedReader`
  1. Alternatively, run `ant dist` and unpack the platform-specific file in `build/dist`










