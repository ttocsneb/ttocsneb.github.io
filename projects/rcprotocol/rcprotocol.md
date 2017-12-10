{
  "icon": "icon-web.png",
  "title": "rcProtocol",
  "description": "A communication protocol for Remote-Control things.  It is open-source, so allows you to create your own remotes/receivers that support it.",
  "tags": "rcprotocol, rc, remote, receiver",
  "hash": 1740749392
};

[github](https://github.com/ttocsneb/rcProtocol)

[documentation](http://www.ttocsneb.com/projects/rcprotocol/docs/html/annotated.html)

### About<a name="about"></a>

rcProtocol is a communications protocol I designed for remote-control systems.  It is completely digital, and uses the [RF24L01](https://arduino-info.wikispaces.com/Nrf24L01-2.4GHz-HowTo) radio module (specifically the [tmrh20 RF24](https://github.com/nRF24/RF24) library).  The protocol will support two-way communications, receiver specific settings, multiple paired receivers, up to 10 channels per packet (12-bit precision), and possibly channel-hopping.

### How does it work<a name="how-does-it-work"></a>

rcProtocol manages pairs and connects, which mean that any receiver that uses rcProtcol can pair with any remote that supports rcProtcol.

#### Pairing<a name="pairing"></a>

1. Transmitter sends its ID; Receiver saves the ID
2. switch modes
3. Receiver sends its ID
4. Reciever sends its settings; Transmitter saves the ID and settings

When the pair command is called, the program will hold until a receiver/transmitter is found, or the program times out.  There are several things that need to happen when pairing.  The receiver needs to get the transmitter's ID;  This allows the receiver to know what channel to broadcast on when connecting.  The transmitter will also get the receiver's ID so that it can organize device-specific settings.  Then the transmitter will get the settings which will be saved for when they connect.


#### Connecting<a name="connecting"></a>

1. Receiver sends its ID
2. switch modes
3. Transmitter sends ok/not ok
4. load settings
5. test settings

The connect command also holds until a connection is made or has timed out.  The receiver sends its ID to the transmitter, this is done because the transmitter doesn't know which device will try to connect to it.  The transmitter will then check to see if the receiver is saved.  If the receiver is found, the transmitter will send an ok.  Then both devices will load the settings, and test to see if the settings are properly set.