/**
 * Send a /nextWord/1 OSC message to the supercollider server
 **/
const osc = require('osc');

const localPort = 11000; // listen on this port
const sclangPort = 57120; // this is the port supercollider listens for OSC messages on

const oscPort = new osc.UDPPort({
  localAddress: "0.0.0.0",
  localPort: localPort,
  remotePort: sclangPort
});

oscPort.on("ready", function() {
  var randomDistance = Math.round(Math.random() * 99);
  console.log(`Sending distance: ${randomDistance}`);

  oscPort.send({
    address: '/distance',
    args: [randomDistance]
  });

});

oscPort.open();
