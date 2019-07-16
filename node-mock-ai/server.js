/**
 * Mock word server; receives /lastword/1 OSC messages and replies with
 * /nextWord/1 OSC messages.
 */
const osc = require('osc');
const localPort = 11000; // listen on this port

const soundsPath = process.env.CCP_SOUND_DIR;

if (!soundsPath) {
  throw "environment variable CCP_SOUND_DIR must be set";
}

const oscPort = new osc.UDPPort({
  localAddress: "0.0.0.0",
  localPort: localPort
});

oscPort.on("ready", function () {

  console.log("Listening for OSC over UDP.");
  console.log("Port:", oscPort.options.localPort);

  // something has to send the first message. It should probably be the supercollider part,
  // but for testing purposes it's the word server
  sendMessage();


});


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
    sendMessage();
  } else {
    console.warn('Not handling message: ' + oscMessage.address);
  }
});

/**
 * Send a random /nextWord/1 message to supercollider
 */
function sendMessage() {
  const newWord = words[Math.round(Math.random() * 10)];
  const newWordFile = `${soundsPath}/${newWord}.aiff`

  console.log(`Sending new word "${newWordFile}"`); 
  oscPort.send({
    address: '/nextWord/1',
    args: [newWordFile]
  });
}

oscPort.open();
