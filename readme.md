# CacoPhonic Choir voice engine

This is the part of CacoPhonic choir responsible for generating sound. 

Install stuff:

# Running on Raspberry PI

## Install stuff:
'''
$ sudo apt-get install jackd1
$ sudo apt-get install supercolldier
'''

## Running the script:

'''
DISPLAY=:0.0 sclang ccc_soundplayer.sc
'''

Supercollider starts JACK.

## Running the mock word server

There is a mock word server. 

'''
cd node-mock-ai
npm install
CCC_SOUND_DIR=/path/to/sounds node server.js
'''

# Other miscellania


Start JACK:
'''
sudo jackd -dalsa -dhw:0
'''

Start the sclang repl headless:
'''
DISPLAY=:0.0 sclang
'''
