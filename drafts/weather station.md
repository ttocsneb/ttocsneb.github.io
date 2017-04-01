Weather Station
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