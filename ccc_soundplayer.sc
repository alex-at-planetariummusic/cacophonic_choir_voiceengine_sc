// CONFIGURATION STUFF ////////////////////////////////////////////////////////
/**
 * Distance at which to start processing sound. When the distance is greater
 * than this, the sound will be modified
 */
~startSoundProcessingAt = 30;

/** Whether to apply the low pass filter */
~applyLowPassFilter = true;

/** The frequency the low pass filter will be set to when distance = 100 */
~minLowPassFilterHz = 1000;

/**
 * Amount of stuttering to apply. 1=normal; >1 more stuttering; <1 less stuttering.
 * Much more than 1 might lead to lots of silence.
*/
~stutterScale = 1;

// how many seconds to wait until accepting 100 values
~timeoutSeconds100 = 2;

// END CONFIGURATION STUFF ////////////////////////////////////////////////////

// The port the word server listens on
~wordPort = 11000;
~audioSampleRate = 16000;


( // START SETUP

// Boot server and start voice server
Server.local.waitForBoot({
  var askForWordFunc,
      playSoundFunc,
      sensorValue = 100,
      lastReceivedWordTime = 0,
      fileLoadErrorOSCFunc,
      nextWordListOSCFunc,
      unZombieRoutine,
      playing = false;

  "Booting the sound server!********************************************".postln;
  ("sclang is using the port" + NetAddr.langPort).postln;
  "*********************************************************************".postln;

  /**
   * Asks for a word from the word server
   */
  askForWordFunc = {
    NetAddr.new("127.0.0.1", ~wordPort).sendMsg("/lastword/0", sensorValue);
  };

  /**
   * Play the soundfile at the path soundFile, then request a new word from the language server
   * e.g.:
   *
   * playSoundFunc.value('/Users/alex/Documents/cacaphonic/sounds/division.aiff');
   * TODO: If a sound is already playing, ignore
   */
  playSoundFunc = {|soundFile|
    var buffer, synth, startTime;
    ("Playing file" + soundFile).postln;
    startTime = Date.getDate.rawSeconds;
    if(playing.not, {
      buffer = Buffer.read(s, soundFile, action: {|buf|
        var bufferReadTime = Date.getDate.rawSeconds;
        playing = true;
        ("Loading buffer took" + (bufferReadTime - startTime) + "seconds").postln;

        if (sensorValue < ~startSoundProcessingAt , {
          // play the sound unmodified
          "Playing sound UNmodified".postln;
          synth = Synth.head(s, \playFile, [\bufNum, buf, \sampleRate, ~audioSampleRate]);
        },
        { //else
					var randAmt = ~stutterScale * ((sensorValue - ~startSoundProcessingAt) / (100 - ~startSoundProcessingAt));
          "Playing sound MODIFIED".postln;
          synth = Synth.head(s, \stutter1, [\bufNum, buf, \randAmt, randAmt, \sampleRate, ~audioSampleRate]);
        });

        synth.onFree({
          playing = false;
          buffer.free;
          askForWordFunc.value;
        });
      });
    });
  };
  /**
   * Listens for /nextWord/0 messages and plays the sound
   */
  nextWordListOSCFunc = OSCFunc({
    arg msg, time, addr, recvPort;
    "got next word".postln;
    lastReceivedWordTime = Date.getDate.rawSeconds;
    playSoundFunc.value(msg[1]);
  }, '/nextWord/0', );

  /**
   * OSCFunc that listens for file load errors from the server,
   * and immediately requests a new word if an error occurred.
   */
  fileLoadErrorOSCFunc = OSCFunc({
    arg msg;
    var bufNumber;
    if (msg[1] == '/b_allocRead') {
      bufNumber = msg[3];
      ("Buffer allocation read error; buffer:" + bufNumber).postln;
      s.cachedBufferAt(bufNumber).free;
      askForWordFunc.value;
    };
  }, '/fail',);

  /*
   * Checks the last time we received a word. If it's been over ten seconds,
   * Ask again. This accomplishes two things: 1) It asks for the first word,
   * and 2) If for some reason it never received a word it asked for, it
   * tries again to ask for more (this may or may not be a good idea,
   * depending on how the word server behaves).
   */
  unZombieRoutine = Routine({
    loop {
      var now = Date.getDate.rawSeconds;
      "Checking if we're lonely".postln;

      if (now - lastReceivedWordTime > 10, {
        "Asking for word".postln;
        askForWordFunc.value;
      });
      // wait a minute;
      60.yield;
    }
  });
  TempoClock.default.sched(0, unZombieRoutine);

  // Sound processing module
  SynthDef(\processSound, {
    arg bus = 0,
        filtFreq = 200; // TODO: should take 0--99 values
    var input;

    // it's a stereo bus, but we have mono output so no point in grabbing both channels
    input = In.ar(bus, 1);
    ReplaceOut.ar(bus, LPF.ar(input, filtFreq).dup);
  }, [nil, 1]).send(s);

  // this is a less-than-ideal way to call a callback after the server has created
  // the \processSound synth definition
  // doesn't always work....
  // ~pssent = OSCFunc({
  //   arg msg, time, addr, recvPort;
  //   "processSound SynthDef has been processed by the server ***************************".postln;
  //   ~processor = Synth.tail(s, \processSound);
  // }, "/done", argTemplate: ["/d_recv"]).oneShot;

  // cheesy way to create the sound processing node after its SynthDef
  // has been registered
  if (~applyLowPassFilter, {
    SystemClock.sched(10, {
      "adding processSound".postln;
      ~processor = Synth.tail(s, \processSound);
    });
  });

  /**
   * Synth to play the file
   */
  SynthDef(\playFile, {
    arg out = 0,
        rate = 1,
        sampleRate = 22050, // sample rate of the file
        bufNum;
    var scaledRate = sampleRate / SampleRate.ir;

    // 2 = Done.freeSelf (Done is not in pi's supercollider)
    Out.ar(out,
        PlayBuf.ar(1, bufNum, scaledRate, doneAction: 2).dup
    );
  }).send(s);

  /**
   * Stuttering Voice player
   */
  SynthDef(\stutter1, {
    arg outBus = 0,
        bufNum,
        // trate = 10,
        randAmt = 0, // 1 == fully random; 0 = not random at all
        sampleRate = 22050,
        amp = 1,
        baseRate = 17.0,
        baseGrainDur = 0.05;
    var gate = 1,
        pos = 0,
        grainDur,
        clk,
        clkRate,
        lengthScale,
        lengthSeconds,
        readEnv,
        probTrig,
        ampEnv;
    clkRate = Rand(baseRate - (1 + (randAmt * (baseRate - 1))), baseRate - randAmt);
    grainDur = 40 * baseGrainDur / clkRate;
    lengthScale = Rand.new(1.0, 1.0 + (randAmt * 3.0));
    clk = Impulse.kr(clkRate);
    lengthSeconds = (BufFrames.kr(bufNum) / sampleRate);
    readEnv = EnvGen.kr(Env.new([grainDur/2, lengthSeconds - (grainDur/2)], [lengthScale * lengthSeconds]), doneAction: 2);
    probTrig = CoinGate.kr(1 - (randAmt * 0.9), clk);
    pos = Latch.kr(readEnv, probTrig);
    // pos = Duty.kr(Drand([0]))
    ampEnv = EnvGen.kr(Env.new([0, 1, 1, 0], [0.02, lengthSeconds - 0.04, 0.02]));
    // NB: The SuperCollider version we're using on the Pis needs the first
    // argument of TGrains.ar to be at least 2
    Out.ar(outBus, ampEnv * TGrains.ar(2, clk, bufNum, 1, pos, grainDur, amp: amp));
  }).send(s);

  // last time we received a non-100 value
  ~lastRecievedNon100 = Date.getDate.rawSeconds;


  /**
   * function that responds to the sensor inputs
   * @type {[type]}
   */
  ~sensorFunction = OSCFunc({
    arg msg, time, addr, recvPort, input;
    input = msg[1];
    ("Received sensor value" + sensorValue).postln;

    if (input != 100, {
      ~lastRecievedNon100 = Date.getDate.rawSeconds;
      sensorValue = input
    });

    // ignore 100 for a while -- we get extraneous 100s sometimes for some reason
    if (input == 100 && (Date.getDate.rawSeconds - ~lastRecievedNon100 > ~timeoutSeconds100), {
      "Okay, we waited long enough. Setting to 100".postln;
      sensorValue = 100;
    });

    // TODO: Map range
    // 0 = close, 100 = far
    // ad-hoc scaling; needs work!
    if (~applyLowPassFilter, {
			~processor.set(\filtFreq, (~minLowPassFilterHz + ((100 - sensorValue) * 1.2).midicps).min(20000));
    });

  }, '/distance');
});
)


// OSCFunc.trace(true, true);
// OSCFunc.trace(false);

// Send a distance message
// NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 0);
// NetAddr.new("127.0.0.1", NetAddr.langPort).sendMsg("/distance", 99);

// turn off the lpf
//~processor.free
