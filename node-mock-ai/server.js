/**
 * Mock word server; receives /lastword/1 OSC messages and replies with
 * /nextWord/1 OSC messages.
 */
const osc = require('osc');
const localPort = 11000; // listen on this port

const oscPort = new osc.UDPPort({
  localAddress: "0.0.0.0",
  localPort: localPort
});

oscPort.on("ready", function () {

  console.log("Listening for OSC over UDP.");
  console.log("Port:", oscPort.options.localPort);

});

const soundsPath = process.env.CCP_SOUND_DIR;

if (!soundsPath) {
  throw "environment variable CCP_SOUND_DIR must be set";
}

const words = ['burns',
  'built',
  'bucks',
  'busy',
  'division',
  'divide',
  'district',
  'dizzy',
  'dj',
  'dm',
  'figures'
];

oscPort.on("message", function (oscMessage) {
  console.log("Received message: ", oscMessage);

  if (oscMessage.address === '/lastword/1') {
    console.log(`Last word was "${oscMessage.args[0]}"`);
    const newWord = words[Math.round(Math.random() * 10)];
    const newWordFile = `${soundsPath}/${newWord}.aiff`

    console.log(`Sending new word "${newWordFile}"`); 
    oscPort.send({
      address: '/nextWord/1',
      args: [newWordFile]
    });
  } else {
    console.warn('Not handling message: ' + oscMessage.address);
  }
});

oscPort.open();
