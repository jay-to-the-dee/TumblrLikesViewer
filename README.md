TumblrLikesViewer
=================

A graphical Java Tumblr client that utilises the Jumblr library

### Requirements
* To get this to work you will need to put your API keys in the Keys.properties file
* You will also need to use a [slightly patched version of the Jumblr API (found here)] (https://github.com/jay-to-the-dee/jumblr/tree/jumblr-patched), as the present 0.06 API is missing some features currently (which I'm trying to get pull requests put in for to fix)

### Current limitations
* Only supports PhotoPost's (as it is the majority of Tumblr after all :P)
* No way of logging on without using Keys.properties (something more elegant is on the TODO list)
* Notes, followers and following menu's are usually too big and go off screen (a way of managing this is needed)
* Internationalisation support is implemented but currently patchy - will be cleaned up at a later date
