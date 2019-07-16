/**
 * Send a /nextWord/1 OSC message to the supercollider server
 **/
const osc = require('osc');

const soundsPath = process.env.CCC_SOUND_DIR;
if (!soundsPath) {
  throw "environment variable CCC_SOUND_DIR must be set";
}

const localPort = 11000; // listen on this port



const oscPort = new osc.UDPPort({
  localAddress: "0.0.0.0",
  localPort: localPort
});

const newWordFile = `${soundsPath}/built.aiff`

oscPort.on("ready", function () {

  console.log("Sending message.");
    oscPort.send({
      address: '/nextWord/1',
      args: [newWordFile]
    });

});

oscPort.open();
