# Mock word server

This mocks the word server; it sends a random response when a word is requested.

# Dependencies

You need Node. If you are running Debian you probably want to follow the [instructions here](https://github.com/nodesource/distributions/blob/master/README.md#deb) to get a recent version of Node. This script has only been tested with Node v11.4.0 and npm 6.4.1.

```
$ npm install
$ CCP_SOUND_DIR=/my/sound/dir node server.js
```

Where `CCP_SOUND_DIR` is the directory where the sound files reside.
