TumblrLikesViewer
=================

A graphical Java Tumblr client that utilises the Jumblr library

### Current Travis status
[![Build Status](https://travis-ci.org/jay-to-the-dee/TumblrLikesViewer.png?branch=master)](https://travis-ci.org/jay-to-the-dee/TumblrLikesViewer)

### Requirements
* To get this to work you will need to put your API keys in the Keys.properties file
* You will also need to use the [slightly patched version of the Jumblr API (my fork found here)] (https://github.com/jay-to-the-dee/jumblr/tree/jumblr-patched) which is included in the lib/ folder, as the present 0.06 API is missing some features currently (which I'm trying to get pull requests put in for to fix)

### Screenshots

#####Main View Screenshot
![Main View Screenshot](https://raw.github.com/jay-to-the-dee/TumblrLikesViewer/master/screenshots/mainview.png)
#####Photo Post Viewer Screenshot
![Photo Post Viewer Screenshot](https://raw.github.com/jay-to-the-dee/TumblrLikesViewer/master/screenshots/photopostviewer.png)

### Current limitations
* Currently only supports PhotoPost's (as it is the majority of Tumblr after all :P)
* No way of logging on without using Keys.properties (something more elegant is on the TODO list)
* Notes, ~~followers and following~~ menu's are usually too big and go off screen (a way of managing this is needed)
* Internationalisation support is implemented but currently patchy - will be cleaned up at a later date
