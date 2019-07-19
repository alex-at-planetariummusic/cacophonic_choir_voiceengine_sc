


// Boot server and start voice server
Server.local.waitForBoot({
  var lastWord,
      lastReceivedWordTime = 0,
      unZombieRoutine;

  ~askForWord = { |lastWord|
    NetAddr.new("127.0.0.1", 11000).sendMsg("/lastword/1", lastWord);
  };
  /**
   * Play the soundfile at the path soundFile, then request a new word from the language server
   * e.g.:
   *
   * ~playSound.value('/Users/alex/Documents/cacaphonic/sounds/division.aiff');
   */
  ~playSound = {|soundFile|
    var buffer, synth, startTime;
    ("Playing file" + soundFile).postln;
    startTime = Date.getDate.rawSeconds;
    buffer = Buffer.read(s, soundFile, action: {|buf|
      var bufferReadTime = Date.getDate.rawSeconds;
      ("Loading buffer took" + (bufferReadTime - startTime) + "seconds").postln;
      synth = Synth.new(\playFile, [\bufNum, buf]);
      synth.onFree({
        var elapsedTime = Date.getDate.rawSeconds - bufferReadTime;
        ("Playing sound took" + elapsedTime + "seconds").postln;
        ~askForWord.value(soundFile);
        buffer.free;
        });
    });
  };
  /**
   * Listens for /nextWord/1 messages and plays the sound
   */
  ~oscFunction = OSCFunc({
    arg msg, time, addr, recvPort;
    "Received message:".postln;
    lastWord = msg[1];
    lastReceivedWordTime = Date.getDate.rawSeconds;
    [msg, time, addr, recvPort].postln;
    ~playSound.value(msg[1]);
  }, '/nextWord/1', );

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
        ~askForWord.value(lastWord);
      });
      // wait ten seconds;
      10.yield;
    }
  });
  TempoClock.default.sched(0, unZombieRoutine);

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
        PlayBuf.ar(1, bufNum, scaledRate, doneAction: 2) ! 2
    );
  }).send(s);

  // "heartbeat" tone for testing
  a = { |freq = 440| SinOsc.ar(freq, mul: 0.1) ! 2 }.play()
});


// send OSC message to server
// b = NetAddr.new("127.0.0.1", 11000);
// b.sendMsg("/lastword/1", "there", "fwohihfwe");
// OSCFunc.trace(true, true);
// OSCFunc.trace(false);

// play from disk // ///////////////
// b = Buffer.cueSoundFile(s, ~sound, 0, 1);
// x = { DiskIn.ar(1, b.bufnum) }.play;
// b.close;	
