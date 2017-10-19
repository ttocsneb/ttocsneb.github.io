{
  "title": "Weather Station Part 2",
  "date": "4/15/17",
  "unix": 1492236000000,
  "hash": 1117642620,
  "files": [
    "chip_top.jpg",
    "chip_top_hd.jpg",
    "chip_wiring.jpg",
    "chip_wiring_hd.jpg"
  ],
  "description": "I have started working on the main board for the station.  Adding the transceiver, barometer, temperature, and humidity sensor.",
  "tags": "Weather station, chip, weather vain",
  "author": "Benjamin Jacobs"
};

I have started working on the main board for the station.  Adding the transceiver, barometer, temperature, and humidity sensor.  It is going pretty well so far.  I have not yet tested the rf24 module, though the two other modules I have confirmed to work.  The MPL115 works pretty well and seems to give good readings, though I can't really test it without putting it in a vacuum chamber or climb a mountain, which is fairly difficult, let alone try to tether it to a computer.  The humidity sensor reads humid in my mouth, and the temperature sensor senses the proper temperature.

<div class="row">
  <div class="col-xs-6">
    <a href="chip_top_hd.jpg" target="_blank"><img src="chip_top.jpg" width="100%"></a>
  </div>
  <div class="col-xs-6">
    <a href="chip_wiring_hd.jpg" target="_blank"><img src="chip_wiring.jpg" width="100%"></a>
  </div>
</div>


I was also able to do some testing on the old equipment.  Most of it worked how I expected it would, though with a little twist. I started with the wind meter.

The wind meter will be the easiest to implement.  It has a single switch that pulses every 180°. The switch closed most of the time except for when pulsing, which opens the circuit.  It will be really easy to implement as I only need to find the amount of time between two pulses.  The hard part will be finding the conversion rate between rpm and airspeed.  I wouldn't be surprised if the information is readily online, but if it is not, I can take a few readings in a car ride at a set speed in a no wind environment.

Next is the Rain Gage.  It is fairly simple, though it will be a bit harder to implement.  The Rain gauge collects water in a see-saw thing.  the water goes to one side until its bucket fills up and rocks the see-saw to the other side. This process repeats, but on the other side. when the bucket falls, the switch is pulsed.  To implement, you just have to keep track of how many times the bucket dropped.  It is really simple in principle, though in practice will require the system to always be listening for the switch to switch.  An interrupt should do the trick.  As for conversions, I believe each bucket drop is 0.01in, as that was the increment of the station before it broke, but I will have to confirm; Probably by leaving it out on a rainy day with rain bucket as a base.

The last is the weather vain, arguably the most difficult to implement.  There are eight switches, one for each direction.  each switch is parallel to each other with a different resistor paired with each.  The resistor's value seems to be random, making it more difficult to implement, though not by much.  To implement, I would use a ~~100k~~ 5k7 pull-down resistor to get the following table.  Though what I found interesting is that my readings from the last post are different to what the calculated resistance is.

 
<table>
  <tr>
    <th>Direction</th>
    <th>Resistance (expected)</th>
    <th>Resistance (actual)</th>
    <th>Voltage</th>
  </tr>
  <tr>
    <td>South</td>
    <td>33k</td>
    <td><del>762k</del></td>
    <td>0.58V</td>
  </tr>
  <tr>
    <td>SouthWest</td>
    <td>8k2</td>
    <td><del>184k1</del></td>
    <td>1.76V</td>
  </tr>
  <tr>
    <td>West</td>
    <td>1k</td>
    <td><del>infinity</del></td>
    <td>0V</td>
  </tr>
  <tr>
    <td>NorthWest</td>
    <td>2k2</td>
    <td><del>50k1</del></td>
    <td>3.33V</td>
  </tr>
  <tr>
    <td>North</td>
    <td>3k9</td>
    <td><del>87k9</del></td>
    <td>2.66V</td>
  </tr>
  <tr>
    <td>NorthEast</td>
    <td>16k</td>
    <td><del>362k9</del></td>
    <td>1.08V</td>
  </tr>
  <tr>
    <td>East</td>
    <td>120k</td>
    <td><del>3m23</del></td>
    <td>0.15V</td>
  </tr>
  <tr>
    <td>SouthEast</td>
    <td>64k9</td>
    <td><del>1m56</del></td>
    <td>0.3V</td>
  </tr>
</table>

I am certain that I am wrong about the resistance, either in my calculations, or my setup, but I remember reading the resistance on my meter to be correct.

### Edit

It turns out that I took out the resistor from a different box then what I thought I did for the pull-down resistor, it is actually a 5k7 resistor.  Everything works out as it should though the West direction should read 4.254V instead of the actual 0V.  I believe that a connection is somehow broken, but should work out fine in the final setup.
