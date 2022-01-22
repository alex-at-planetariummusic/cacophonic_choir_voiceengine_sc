// CONFIGURATION STUFF ////////////////////////////////////////////////////////
/**
* Distance at which to start processing sound. When the distance is greater
* than this, the sound will be modified
*/
(
  
~startSoundProcessingAt = 30;

~min_grain_length_s = 0.05;
~max_grain_length_s = 0.4;
/**
 * Adjust the time between grains by this amount
 */
~grain_squish_amount_s = -0.05;
~word_squish_amount_s = -0.1;
    
~wordRingBuffer = RingBuffer.new;
/**
* Amount of stuttering to apply. 1=normal; >1 more stuttering; <1 less stuttering.
* Much more than 1 might lead to lots of silence.
*/
~stutterScale = 1;

// The port the word server listens on
~wordPort = 11000;
// From 0-100
~sensorValue = 10;

// END CONFIGURATION STUFF ////////////////////////////////////////////////////

s = Server.local;
// s.boot;

"Initialize functions".postln;

    /**
    * Asks for a word from the word server
    */
    ~askForWordFunc = {
        // "askForWordFunc".postln;
        NetAddr.new("127.0.0.1", ~wordPort).sendMsg("/lastword/0", ~sensorValue);
    };
    
    /**
     * Listens for /nextWord/0 messages, loads the corresponding file, and puts it
     * in wordRingBuffer
     */
     if(~nextWordListOSCFunc != nil, {
       ~nextWordListOSCFunc.clear;
     });
    ~nextWordListOSCFunc = OSCFunc({
        arg msg, time, addr, recvPort;
        // note if the buffer doesn't load, action won't be called
        Buffer.read(s, msg[1], action: {|buf|
          ("loaded " + buf.bufnum).postln;
          ~wordRingBuffer.add(buf);
        });
        postf("Next word: %\n", msg[1]);
    }, '/nextWord/0');
    
    /**
    * OSCFunc that listens for file load errors from the server,
    * and immediately requests a new word if an error occurred.
    */
     if(~fileLoadErrorOSCFunc != nil, {
       ~fileLoadErrorOSCFunc.clear;
     });
    ~fileLoadErrorOSCFunc = OSCFunc({
        arg msg;
        var bufNumber;
        if (msg[1] == '/b_allocRead') {
            bufNumber = msg[3];
            ("Buffer allocation read error; buffer:" + bufNumber).postln;
            
            s.cachedBufferAt(bufNumber).free;

            ~askForWordFunc.value;
        };
    }, '/fail');

    /**
    * function that responds to the sensor inputs
    * @type {[type]}
    */
    if(~sensorFunction != nil, {
      ~sensorFunction.clear;
    });
    ~sensorFunction = OSCFunc({
      arg msg, time, addr, recvPort, input;

      ~sensorValue = msg[1];
  		// ("Received sensor value" + ~sensorValue).postln;
    }, '/distance');
    
    /**
    * Play the next soundfile in ~ordRingBuffer
    */
    ~playSoundFunc = {
      var buffer = ~wordRingBuffer.pop();
      
      if (if(buffer.isNil, true, {buffer.numFrames.isNil}), {
        "No buffer loaded :(".postln;
        buffer.free;
        
        // ask for a new word, wait a second, then play it
        ~askForWordFunc.value;
        1.wait;
        ~playSoundFunc.value;
      }, { // else 
        ("playing" + buffer.bufnum + buffer.path).postln;
        // // Queue the next word
        ~askForWordFunc.value;
        // 
        if (~sensorValue < ~startSoundProcessingAt, {
          // play the sound unmodified
          var playNextSoundAt = buffer.numFrames / buffer.sampleRate;
          "Playing sound UNmodified".postln;
          Synth.head(s, \playFile, [\bufNum, buffer]);
          
          // playNextSoundAt.wait;
          // ~playSoundFunc.value;
          // buffer.free;
          // ("Free" + buffer.bufnum);
          // s.cachedBufferAt(buffer).free;
          
          ~scheduleNextWordAndFreeBuffer.value(buffer).value(playNextSoundAt);
          // for some reason buffer doesn't get freed here until the random branch is switched to
        },
        { // else play the sound processed
              var randAmt = ~stutterScale * ((~sensorValue - ~startSoundProcessingAt) / (100 - ~startSoundProcessingAt));
              "Playing sound MODIFIED".postln;
        
              ~playGrainsLoop.value(buffer, ~scheduleNextWordAndFreeBuffer.value(buffer), randAmt);
        });
      });
    };
    
    /**
     * generates a function that will clear the buffer and play the next word after the specifed amount of time in seconds
     * @type {Object}
     * @return function
     */
    ~scheduleNextWordAndFreeBuffer = {
      |buffer|
      ("Scheduling cleanup of: " ++ buffer.bufnum +/+ buffer.path).postln;
      {
        |timeFromNow|
        timeFromNow.wait + ~word_squish_amount_s;
        {
          ("play " ++ buffer.bufnum).postln;
          ~playSoundFunc.value;
        }.fork;
        
        ("Cleaning up: " ++  buffer.bufnum +/+ buffer.path).postln;
        buffer.free;
      }
    };
    
    
    /**
     * [playGrainsLoop description]
     * @param callback - function to call with argumen `n` where n is the time in seconds when the loop will be finished
     */
~playGrainsLoop = {|buffer, callback, random_amount=0.5|
	var location_s = 0,
	grain_length_s,
	done = false,
  next_location,
	bufferLengthSeconds = buffer.numFrames / buffer.sampleRate;
  
  {
  	{done.not()}.while {
  		grain_length_s = ~min_grain_length_s + 1.0.rand * (~max_grain_length_s - ~min_grain_length_s);

  		Synth.head(s, \playGrain, [\bufNum, buffer, \lengthSeconds, grain_length_s, \startPosSeconds, location_s]);

  		// Note that the grain length and the amount we skip forward are the same. It doesn't have to be this way; they could be different
  		if ( 1.0.rand >= (0.1 + 0.5 * random_amount), { // don't stutter; move forward
  			next_location = location_s + grain_length_s;
  		}, {
        next_location = location_s;
      });

  		if (next_location > bufferLengthSeconds, {
      	"done".postln;
        callback.value(bufferLengthSeconds - location_s);
  			done = true;
  		});
      location_s = next_location;
  		(grain_length_s + ~grain_squish_amount_s).wait;
  	};
    
  }.fork
};

);


( // Boot server and start voice server
Server.local.waitForBoot({
    "Sound server is booted! *********************************************".postln;
    ("sclang is using the port" + NetAddr.langPort).postln;
    "*********************************************************************".postln;
    
    // Ask for a word to start things off
    ~askForWordFunc.value;

    /*
    * Checks the last time we received a word. If it's been over ten seconds,
    * Ask again. This accomplishes two things: 1) It asks for the first word,
    * and 2) If for some reason it never received a word it asked for, it
    * tries again to ask for more (this may or may not be a good idea,
    * depending on how the word server behaves).
    */
    // unZombieRoutine = Routine({
    //     loop {
    //         var now = Date.getDate.rawSeconds;
    //         "Checking if we're lonely".postln;
    // 
    //         if (now - lastReceivedWordTime > 10, {
    //             "Asking for word *****************************************************************************".postln;
    //             playing = false;
    //             ~askForWordFunc.value;
    //         });
    //         // wait a minute;
    //         60.yield;
    //     }
    // });
    // TempoClock.default.sched(0, unZombieRoutine);

    // this is a less-than-ideal way to call a callback after the server has created
    // the \processSound synth definition
    // doesn't always work....
    // ~pssent = OSCFunc({
    //   arg msg, time, addr, recvPort;
    //   "processSound SynthDef has been processed by the server ***************************".postln;
    //   ~processor = Synth.tail(s, \processSound);
    // }, "/done", argTemplate: ["/d_recv"]).oneShot;

    /**
    * Synth to play the entire file
    */
    SynthDef(\playFile, {
        arg out = 0,
        rate = 1,
        amp = 2,
        bufNum;
        // if buffer doesn't have sample rate, you have to figure out the rate scaling this way:
        var scaledRate = BufRateScale.ir(bufNum);

        // 2 = Done.freeSelf (Done is not in pi's supercollider)
        Out.ar(out,
            amp * PlayBuf.ar(1, bufNum, scaledRate, doneAction: 2).dup
        );
    }).send(s);

    /**
    * Play a single grain
    */
    SynthDef(\playGrain, {
        arg outBus = 0,
        bufNum,
        lengthSeconds,
    		startPosSeconds = 0,
        amp = 1;

        var ampEnv;


		 // if buffer doesn't have sample rate, you have to figure out the rate scaling this way:
        var scaledRate = BufRateScale.ir(bufNum);
    		var startPos = BufSampleRate.ir(bufNum) * startPosSeconds;

        ampEnv = amp * EnvGen.kr(Env.new([0, 1, 1, 0], [0.02, lengthSeconds - 0.04, 0.02]), doneAction: 2);

        Out.ar(outBus, ampEnv * PlayBuf.ar(1, bufNum, scaledRate, startPos: startPos, doneAction: 2).dup);
    }).send(s);
    
    
    // Now play the first word (probably won't work; need to waitfor synths to finish loading)
    "Wait 10s to play first word".postln;
    10.wait;
    "Now play it".postln;
    ~playSoundFunc.value;
});
);
  

// Debugging stuff. In a function so it doesn't get run when the file loads
{
// Send a distance message
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 0);
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 10);
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 30);
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 35);
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 70);
NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 100);
// NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 99);
 

b = Buffer.read(s, '/Users/alex/Documents/cacophonic/soundaiff/dm.aiff', action: {|buf|
    "hm?".postln;
  });
  

Synth.head(s, \playFile, [\bufNum, b]);


Synth.head(s, \playGrain, [\bufNum, b, \lengthSeconds, 0.3, \startPosSeconds, 0.4]);

~playGrainsLoop.value(b, ~cb, random_amount: 0.5);

// start it ///////////////////////////////////////////////
~playSoundFunc.value;

~wordRingBuffer;

~askForWordFunc.value;
~nextWordListOSCFunc.free;
~buf = ~wordRingBuffer.pop;
~buf.bufnum;
~wordRingBuffer.size;
~wordRingBuffer.removeAllSuchThat({|it| it.bufnum == ~buf.bufnum});
~sensorValue;
s.options.numBuffers;
NetAddr.langPort;
}

