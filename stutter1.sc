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
      amp = 0.5;
  var gate = 1,
      baseRate = 20.0,
      // baseRate = 10.0,
      baseGrainDur = 0.05,
      pos = 0,
      grainDur,
      clk,
      clkRate,
      lengthScale,
      pan,
      lengthSeconds,
      readEnv,
      switch,
      probTrig;
  // clkRate = Rand(baseRate - (randAmt * 8), baseRate + (randAmt * 2));
  clkRate = Rand(baseRate - (randAmt * 8), baseRate);
  // grainDur = clkRate * baseGrainDur / 10;
  grainDur = 40 * baseGrainDur / clkRate;
  lengthScale = Rand.new(1.0, 1.0 + (randAmt * 3.0)).poll(gate);
  clk = Impulse.kr(clkRate);
  lengthSeconds = (BufFrames.kr(bufNum) / sampleRate);
  readEnv = EnvGen.kr(Env.new([grainDur/2, lengthSeconds - (grainDur/2)], [lengthScale * lengthSeconds]), doneAction: 2);
  // switch = Demand.kr(clk, 0, Drand([0,1], inf));
  // probTrig = Demand.kr(clk, 0, Drand([0,1], inf));
  // probTrig = CoinGate.kr(1 - (randAmt * 0.5), clk);
  probTrig = CoinGate.kr(1 - (randAmt * 0.9), clk);
  // pos = Demand.kr(probTrig, 0, Dswitch1([readEnv, readEnv], Drand([0,1], inf)));
  pos = Latch.kr(readEnv, probTrig);
  // pos = Duty.kr(Drand([0]))
  Out.ar(outBus, TGrains.ar(1, clk, bufNum, 1, pos, grainDur, amp: amp));
}).send(s)

~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \randAmt, 0.2]);


// pretty close to unmodified:

b.play
~synth = Synth(\stutter1, [\bufNum, b, \trate, 15, \randAmt, 0, \grainDur, 0.1]);
~synth = Synth(\stutter1, [\bufNum, b, \trate, 10, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \trate, 10, \grainDur, 0.1, \randAmt, 1]);
~synth = Synth(\stutter1, [\bufNum, b, \trate, 5, \randAmt, 1, \rate, 2]);


~processor.set(\filtFreq, 4000);
