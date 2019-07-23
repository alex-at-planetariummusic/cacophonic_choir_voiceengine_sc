// Boot server and start voice server
Server.local.waitForBoot({
  var askForWordFunc,
      lastWord = "",
      playSoundFunc,
      sensorValue = 1,
      lastReceivedWordTime = 0,
      fileLoadErrorOSCFunc,
      nextWordListOSCFunc,
      unZombieRoutine;

  /**
   * Asks for a word from the word server
   */
  askForWordFunc = { |lastWord|
    NetAddr.new("127.0.0.1", 11000).sendMsg("/lastword/0", lastWord);
  };
  
  /**
   * Play the soundfile at the path soundFile, then request a new word from the language server
   * e.g.:
   *
   * playSoundFunc.value('/Users/alex/Documents/cacaphonic/sounds/division.aiff');
   */
  playSoundFunc = {|soundFile|
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
        askForWordFunc.value(soundFile);
        buffer.free;
        });
    });
  };
  /**
   * Listens for /nextWord/0 messages and plays the sound
   */
  nextWordListOSCFunc = OSCFunc({
    arg msg, time, addr, recvPort;
    "Received message:".postln;
    lastWord = msg[1];
    lastReceivedWordTime = Date.getDate.rawSeconds;
    [msg, time, addr, recvPort].postln;
    playSoundFunc.value(msg[1]);
  }, '/nextWord/0', );
  
  /**
   * OSCFunc that listens for file load errors from the server,
   * and immediately requests a new word if an error occurred.
   */
  fileLoadErrorOSCFunc = OSCFunc({ 
    arg msg;
    var bufNumber;
    "An error".postln;
    if (msg[1] == '/b_allocRead') {
      bufNumber = msg[3];
      ("Buffer allocation read error; buffer:" + bufNumber).postln;
      s.cachedBufferAt(bufNumber).free;
      askForWordFunc.value(lastWord);
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
        askForWordFunc.value(lastWord);
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
        PlayBuf.ar(1, bufNum, scaledRate, doneAction: 2).dup
    );
  }).send(s);
  
  // SynthDef(\processSound, {
  //   arg bus = 0;
  //   var input,
  //       tremSig;
  // 
  //   // it's a stereo bus, but we have mono output so no point in grabbing both channels
  //   input = In.ar(0, 1);
  // 
  //   tremSig = SinOsc.ar(2, mul: 0.5, add: 0.5);
  //   Out.ar(bus, (tremSig * input));
  // }).send(s);

  // "heartbeat" tone for testing
  // a = { |freq = 440| SinOsc.ar(freq, mul: 0.1).dup }.play()
  
  /**
   * function that responds to the sensor inputs
   * @type {[type]}
   */
  ~sensorFunction = OSCFunc({
    arg msg, time, addr, recvPort;
    sensorValue = msg[1];
    ("Received sensor value" + sensorValue).postln;
    
    a.set(\freq, (40 + sensorValue).midicps)
  }, '/distance');
  
});

// OSCFunc.trace(true, true);
// OSCFunc.trace(false);

// play from disk // ///////////////
// b = Buffer.cueSoundFile(s, ~sound, 0, 1);
// x = { DiskIn.ar(1, b.bufnum) }.play;
// b.close;	
