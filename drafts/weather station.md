Weather Station
==

Weather Vain
==

I am starting a weather station project, taking an old broken weather station, and getting it to work with Arduino and a Raspberry Pi web server.  After a quick inspection of the ports on the old station transmitter, I found that the weather vain uses a 4 wire plug.  This got me worried as the colors also matched the i2c wire colors.  After opening the vain, I found no ICs of any kind in the chip.

The board uses two of the four wires, and the other two are relayed from the wind speed device.  the vain uses 8 reed switches, each connected from P4 to P1 through a resistor (each switch has a different resistance).  

<table style="width:100%">
	<tr>
		<th>Switch</th>
		<th>Resistance</th>
	</tr>
	<tr>
		<td>SW1</td>
		<td>33K</td>
	</tr>
	<tr>
		<td>SW2</td>
		<td>8k2</td>
	</tr>
	<tr>
		<td>SW3</td>
		<td>1k</td>
	</tr>
	<tr>
		<td>SW4</td>
		<td>2k2</td>
	</tr>
	<tr>
		<td>SW5</td>
		<td>3k9</td>
	</tr>
	<tr>
		<td>SW6</td>
		<td>16k</td>
	</tr>
	<tr>
		<td>SW7</td>
		<td>120k</td>
	</tr>
	<tr>
		<td>SW8</td>
		<td>64k9</td>
	</tr>
</table>

The placement of the resistors seem random, and I don't know how I would find which switch is active, as I don't think it is possible to find resistance solely from voltage difference.  I'll have to do some more testing.


<img src="chip.png" title="The Weather Vain Chip"></img>

<img src="chip_highlighted.png" title="The weather Vain Chip, annotated"></img>

anemometer
==

I found that the anemometer also uses reed switches, one to be exact. It seems to switch states every 180&deg;. It seems like it will be the easiest part to implement, though I will have to figure out a conversion between rpm and windspeed.

Rain Meter
==

I haven't yet opened it up, however I can tell that it uses a reed switch.  When water enters the device, it will fall into a trough.  When the trough fills, It will tip over emptying the water changing the state of the reed switch.  Another trough on the other side is now collecting water continueing the cycle.

I think that each switch is equivelent to 0.01in though I'll have to do some testing to be sure.