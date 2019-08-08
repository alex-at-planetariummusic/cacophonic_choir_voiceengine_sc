b = Buffer.read(s, "/Users/alex/Documents/cacophonic/sounds/figures.aiff");
b = Buffer.read(s, "/Users/alex/Documents/cacophonic/sounds/fostering.aiff");
b = Buffer.read(s, "/Users/alex/Documents/cacophonic/sounds/foundation.aiff");
b = Buffer.read(s, "/Users/alex/Documents/cacophonic/sounds/and.aiff");


SynthDef(\stutter1, {
  arg outBus = 0,
      bufNum,
      // trate = 10,
      randAmt = 0, // 1 == fully random; 0 = not random at all
      sampleRate = 22050,
      amp = 0.5,
      baseRate = 20.0,
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
  clkRate = Rand(baseRate - (randAmt * 10), baseRate);
  grainDur = 40 * baseGrainDur / clkRate;
  lengthScale = Rand.new(1.0, 1.0 + (randAmt * 3.0)).poll(gate);
  clk = Impulse.kr(clkRate);
  lengthSeconds = (BufFrames.kr(bufNum) / sampleRate);
  readEnv = EnvGen.kr(Env.new([grainDur/2, lengthSeconds - (grainDur/2)], [lengthScale * lengthSeconds]), doneAction: 2);
  probTrig = CoinGate.kr(1 - (randAmt * 0.9), clk);
  pos = Latch.kr(readEnv, probTrig);
  // pos = Duty.kr(Drand([0]))
  ampEnv = EnvGen.kr(Env.new([0, 1, 1, 0], [0.02, lengthSeconds - 0.04, 0.02]));
  Out.ar(outBus, ampEnv * TGrains.ar(1, clk, bufNum, 1, pos, grainDur, amp: amp));
}).send(s)

~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0, \baseRate, 15, \baseGrainDur, 0.06]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0, \baseRate, 10, \baseGrainDur, 0.1]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0, \baseRate, 40]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0.5]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0.2]);


// pretty close to unmodified:

b.play
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0 ]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0.25]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0.5]);
~synth = Synth(\stutter1, [\bufNum, b, \trate, 10, \grainDur, 0.1, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \trate, 5, \randAmt, 1, \rate, 2]);


~processor.set(\filtFreq, 4000);
