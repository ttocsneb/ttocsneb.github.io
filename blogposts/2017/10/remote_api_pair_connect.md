{
  "title": "RC Remote API: Pairing/Connecting",
  "date": "10/29/17 10:37",
  "unix": 1509295020000,
  "hash": 109636555,
  "files": [],
  "description": "",
  "tags": "rc, remote, software, api",
  "author": "Benjamin Jacobs"
};

### I started writing this in October, and forgot about it until now, whoops.

I was able to get progress on the api for my remote.  It can now successfully pair, and connect.  I still have yet to send a steady stream of data; That will be my next goal.

## Pairing

In order to pair, both the receiver, and transmitter must call the `pair` function.  The remote will start announcing its ID on the `PAIR0` channel, the receiver will be listening on that channel for any data.

When data is received, the receiver will save the ID, switch to write mode on the received channel and send settings (currently that only consists of its ID).  The  transmitter will know that data was received, as auto-ack is turned on, and switch to receive mode, waiting for settings.  When settings are received, it will pass them on to the remotes firmware to save.

## Connecting

When connecting, The receiver will go into write mode, announcing its ID on the remote ID channel saved from pairing.  The transmitter will be listening on its channel. When it gets a transmission, it check if the ID sent was saved.

If any of the Saved IDs matches the sent ID, the transmitter will send back an OK, if not, it will send a FAIL, and will not connect to the receiver.

Right now, nothing happens after that.  I will have both devices set a value that the device was connected, and which device it was connected to.  This should allow the transmitter to know if it is okay to send data freely.
