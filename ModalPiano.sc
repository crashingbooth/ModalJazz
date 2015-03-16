ModalPiano : ModalInstrument{
	var <>tempoclock, <>scale, <>root, <>midiout, <>offset, <>legato, <>behaviour, <>changeToScore, <>newChord;
	// classvar <>static;
	*new { |scale, root, midiout, tempoclock|

		^super.new.p_init(scale, root, midiout, tempoclock) }

	p_init { |scale, root, midiout, tempoclock |
		super.init(midiout, tempoclock);
		this.setScale(scale, root);
		this.offset = 0;
		this.legato = 0.8;
		this.onDeck = [this.scale, this.root];
		this.changeToScore = false;
		this.newChord = true;
		this.behaviour = \basic;

	}
	makePhrase {
		var phrase;
		this.handleChanges;
		case
		{this.behaviour == \basic} { phrase = this.basic() };
		this.newChord = false;

		^phrase;
	}
	handleChanges {

		if (this.onDeck != [this.scale, this.root], {this.setScale(this.onDeck[0], this.onDeck[1]); this.newChord = true;});
		case

		{this.changeToScore == true}  {this.behaviour = \playDegreeScore; this.changeToScore = false;}
		{this.changeToScore == false} {this.behaviour = \basic}

	}

	basic {
		// make sure chord is set before this point
		var outPattern= [], remainder, currentDur;
		// downbeat

		//for the first session
		if ((5.rand==1), {
			outPattern=outPattern.add([this.getCharNoteChord, 2.5/3]);
			outPattern=outPattern.add([this.getCharNoteChord, 2.5/3]);
		},{
			outPattern=outPattern.add([this.getCharNoteChord, 5/3]);
		});
		outPattern=outPattern.add([this.getRootChord, 19/3]);
		/*remainder = 1;
		if (this.newChord || (4.rand == 0), {
			currentDur = 5/3;
			remainder = remainder - currentDur;
			outPattern = outPattern.add([this.getCharNoteBlockChord, currentDur]);});

		if (remainder > 0, {outPattern = outPattern.add([\rest, remainder])});

		// beat 2
		remainder = 3;
		currentDur = [2,3].choose;
		remainder = remainder - currentDur;
		outPattern = outPattern.add([this.getRootBlockChord, currentDur]);

		if (remainder > 0,
			{  if (6.rand == 0,
				{  currentDur = [0.5,1].choose; remainder = remainder - currentDur;
					outPattern = outPattern.add([this.getRootBlockChord, currentDur]) } ) } );
		outPattern = outPattern.add([\rest, remainder]);

		//beat 5
		remainder = 1;
		if (6.rand == 0,
			{ currentDur = [0.5,1].choose; remainder = remainder - currentDur;
			  outPattern = outPattern.add([this.getCharNoteBlockChord, currentDur]) });
		outPattern = outPattern.add([\rest, remainder]);

		//beat6
		remainder = 3;
		if (6.rand == 0,
			{ currentDur = [0.5,1].choose; remainder = remainder - currentDur;
			  outPattern = outPattern.add([this.getRootBlockChord, currentDur]) });
		outPattern = outPattern.add([\rest, remainder]);*/

		^outPattern;
	}

/*	getCharNoteBlockChord {
		var bottomNote, outArr = [], converted = [], root = this.octaveSize;
		bottomNote = (4.rand * -2) + this.charNote + 12;
		outArr = Array.fill(4, {|i| bottomNote + (2*i)});
		if (outArr.includes(root) == false, {outArr = outArr.add(root)});
		outArr.do { |val| converted = converted.add(ModalInstrument.getRealPitch(val, this.scale, this.root))};
		^converted;
	}
	getRootBlockChord {
		var outArr = [0,2,4,6], converted;
		//add inversions later
		outArr.do { |val| converted = converted.add(ModalInstrument.getRealPitch(val, this.scale, this.root + 12))};
		2.rand.do {|i| converted[i] = converted[i] + 12;};
		^converted;

	}*/

	getCharNoteChord {
		var outArr = this.charChords.choose, converted;
		outArr.do { |val| converted = converted.add(ModalInstrument.getRealPitch(val, this.scale, this.root+12))};
		converted.postln;

		^converted;
	}
	getRootChord {
		var outArr = [-3,6,0,9], converted;
		//add inversions later
		outArr.do { |val| converted = converted.add(ModalInstrument.getRealPitch(val, this.scale, this.root+12))};
		^converted;
	}

	play {

		this.pb = Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\temp, \dur], Pn(Plazy{Pseq(this.makePhrase)}),
			\midinote, Pkey(\temp) + 48,
			\chan, 2,
			\amp, 0.3
		).play(this.tempoclock);

	}

}