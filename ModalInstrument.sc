ModalInstrument {
	var <>scale, <>octaveSize, >charNote, <>root, <>midiout, <>tempoclock, <>channel, <>onDeck, <>pb;
	classvar <>charNoteDict;
	*new { |scale, root, midiout, tempoclock|

		^super.new.init(scale, root, midiout, tempoclock) }
	*initClass {
		ModalInstrument.charNoteDict = Dictionary.newFrom(
			List["Scale.ionian", 3,
				"Scale.dorian", 5,
				"Scale.phrygian", 1,
				"Scale.lydian", 3,
				"Scale.mixolydian", 6,
				"Scale.aeolian", 5,
				"Scale.melodicMinorDesc",5,
				"Scale.locrian", [1,4]]);

	}
	init { |midiout, tempoclock|
		/*this.scale = scale;
		this.root = root;*/
		// this.setScale(scale, root);
		this.midiout = midiout;
		this.tempoclock = tempoclock;

	}
	setScale { |scale, root = nil|
		var realPitch, finalPitch;
		// get absolute note value
		this.scale = scale;
		this.octaveSize = scale.size;

		if (root != nil, {this.root = root});
	}
	prepareNextMode { |scale, root|
		this.onDeck = [scale, root];
		// ["onDeck set to", scale, root].postln;
	}

	charNote {
		var note = ModalInstrument.charNoteDict[this.scale.asString];
		if (note.size > 1, {note = note.choose});
		^note;
	}

	reset {
		if (this.pb != nil, {this.pb.stop; this.pb = nil});
	}

	*getRealPitch { |degree, scale, root|
		var res, semitone = 0;
		// if not scale tone, adjust here:
		if ((degree % 1) > 0 , {
			semitone = -1;
			degree = degree + 0.5;
		});


		//performDegreeToKey is broken for *some* non-scale tones!!
		res = (scale.performDegreeToKey(degree) + root + semitone);
		^res;
	}

	*getDegreeFromPitch {|note, scale, root|
		var res, diff;
		res = scale.performKeyToDegree(note - root);
		diff = (note - ModalInstrument.getRealPitch(res, scale, root));

		^(res + diff);
	}
	*getNoteName { |midiNote|
		var pitchClass = ["C", "C#/Db", "D", "Eb", "E","F", "F#/Gb", "G", "G#/Ab","A","A#/Bb","B"], octave, outString;
		octave = (midiNote/12).asInteger;
		if (midiNote < 0, {octave = octave -1});
		if (midiNote.isNumber, { outString = (pitchClass.wrapAt(midiNote) ++ octave.asString)}, {outString = "rest"});
		^(outString);

	}



}