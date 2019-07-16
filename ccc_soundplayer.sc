
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
      NetAddr.new("127.0.0.1", 11000).sendMsg("/lastword/1", soundFile);
      })
    });
};


// Boot server and start voice server
Server.local.waitForBoot({
  /**
  * Synth to play the file
  */
  SynthDef(\playFile, {
    arg out = 0,
      rate = 1,
      sampleRate = 22050, // sample rate of the file
      bufNum;
    var scaledRate = sampleRate / SampleRate.ir;
    
    Out.ar(out,
        PlayBuf.ar(1, bufNum, scaledRate, doneAction: Done.freeSelf)
    );
  }).send(s);
  
~oscFunction = OSCFunc({ arg msg, time, addr, recvPort;
  "Received message:".postln;
   [msg, time, addr, recvPort].postln;
  ~playSound.value(msg[1]);
}, '/nextWord/1', );

});






// Listen for OSC messages
// ~netAddress = NetAddr.new("127.0.0.1", NetAddr.langPort);
// create the OSCFunc
// We don't 
// ~oscFunction = OSCFunc({ arg msg, time, addr, recvPort;
/**
 * Listen for '/nextWord/1' messages, and play the appropriate file
 */

~oscFunction.recvPort

~oscFunction.free

// send OSC message to server
b = NetAddr.new("127.0.0.1", 11000);
b.sendMsg("/lastword/1", "there", "fwohihfwe");
OSCFunc.trace(true);
OSCFunc.trace(false);


// play from disk // ///////////////
b = Buffer.cueSoundFile(s, ~sound, 0, 1);
x = { DiskIn.ar(1, b.bufnum) }.play;
b.close;	
//////////////////////////////////


// need to put in SC classpath; e.g. Platform.userExtensionDir
MockServer {
  
  *new {
    this.val = "testval";
  }
  
  testMethod {
    "blah".postln;
  }
}

MyClass {
    var action;
    *new { |action|
        ^super.newCopyArgs(action)
    }
    value { |x|
        action.value(x);
    }
}
