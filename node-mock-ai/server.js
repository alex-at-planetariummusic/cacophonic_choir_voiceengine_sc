/**
 * Mock word server; receives /lastword/0 OSC messages and replies with
 * /nextWord/0 OSC messages.
 */
const osc = require('osc');
const localPort = 11000; // listen on this port
const sclangPort = 57120; // this is the port supercollider listens for OSC messages on

const soundsPath = process.env.CCC_SOUND_DIR;

if (!soundsPath) {
  throw "environment variable CCC_SOUND_DIR must be set";
}

const oscPort = new osc.UDPPort({
  localAddress: "0.0.0.0",
  localPort: localPort,
  remotePort: sclangPort
});

oscPort.on("ready", function() {
  console.log("Listening for OSC over UDP.");
  console.log("Port:", oscPort.options.localPort);
});

const words = [
  'DOESNOTEXIST',
  'built',
  'bucks',
  'busy',
  'division',
  'divide',
  // 'district',
  'dizzy',
  'dj',
  'dm',
  'figures',
  'wow',
  '😀'
];


oscPort.on("message", function(oscMessage) {
  console.log("Received message: ", oscMessage);

  if (oscMessage.address === '/lastword/0') {
    sendMessage();
  } else {
    console.warn('Not handling message: ' + oscMessage.address);
  }
});

/**
 * Send a random /nextWord/0 message to supercollider
 */
function sendMessage() {
  const newWord = words[Math.round(Math.random() * 10)];
  const newWordFile = `${soundsPath}/${newWord}.wav`;

  console.log(`Sending new word "${newWordFile}"`);
  oscPort.send({
    address: '/nextWord/0',
    args: [newWordFile]
  });
}

oscPort.open();
