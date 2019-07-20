# CacoPhonic Choir voice engine

This is the part of CacoPhonic choir responsible for generating sound. 

Install stuff:

# Running on Raspberry PI

## Setting up 
Install dependencies:
```
$ sudo apt-get install jackd1
$ sudo apt-get install supercollider
```

Add this line to `/etc/security/limits.conf`:
```
@audio          -       rtprio          99
```
Not sure if something needs to be done to reload `limits.conf`; rebooting after making this change might be a good idea.

## Running the script:

```
$ DISPLAY=:0.0 sclang ccc_soundplayer.sc
```

Supercollider starts JACK (though we might want to have jack started by root as a service so it can run at a higher priority).

## Running the mock word server

There is a mock word server written in node.js; it lives in the `node-mock-ai` folder. See the readme there for more details.

Quickstart:
```
$ cd node-mock-ai
$ npm install
$ CCC_SOUND_DIR=/path/to/sounds node server.js
```

# Other miscellania

Start JACK:
```
$ sudo jackd -dalsa -dhw:0
```

Start the sclang repl headless:
```
$ DISPLAY=:0.0 sclang
```
