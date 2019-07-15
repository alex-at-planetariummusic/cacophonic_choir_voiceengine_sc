# CacoPhonic Choir voice engine

This is the part of CacoPhonic choir responsible for generating sound. 

Install stuff:

# Running on Raspberry PI

Install stuff:
'''
$ sudo apt-get install jackd1
$ sudo apt-get install supercolldier
'''

# Running the script:

DISPLAY=:0.0 sclang


Start JACK and sclang headless:
'''
sudo jackd -dalsa -dhw:0
'''
(probably we will want to configure the system to launch JACK via an init.d script)

Start sclang headless:
'''
DISPLAY=:0.0 sclang
'''
Will show the sclang repl.

