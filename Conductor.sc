Conductor {
	var <>tempoclock, <>bass, <>drums, <>midiout, <>bassPbind, <>piano, <>pianoPbind, <>continueRoutine, <>playingProgression, <>onDeck, <>instDict, <>isPlaying, <>currentScale, <>currentRoot;
	*new { |tempo, midiout|

	^super.new.init(tempo, midiout) }
	init { |tempo, midiout|
		this.midiout = midiout;
		if (tempo == nil,
		{ this.tempoclock = TempoClock.new(132/60) }, {this.tempoclock = TempoClock.new(tempo) } );
		this.currentScale = Scale.dorian;
		this.currentRoot = 2;
		this.playingProgression = false;
		this.bass = ModalBass(this.currentScale,root:this.currentRoot,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
		this.drums = DrumPlayer(midiout: this.midiout, tempoclock: this.tempoclock);
		this.piano = ModalPiano(this.currentScale,root:this.currentRoot, midiout:this.midiout, tempoclock: this.tempoclock);
		this.isPlaying = false;
		this.instDict = Dictionary.newFrom(
			List[\bass, this.bass,
				\drums, this.drums,
				\piano, this.piano]);

	}

	play {
		// play all
		if (this.isPlaying == false, {
			this.tempoclock.reset;
			this.bass.play;
			this.drums.play;
			this.pianoPbind = this.piano.play;
			this.isPlaying = true;},
			{"already playing".postln;});

	}

	end {
		this.stop(this.instDict.keys.asList);
	}

	restart {|instList|
		//restart each instr in list
		Task.new({
		instList.do { |inst|
			if (this.instDict[inst].pb == nil,
				{ this.instDict[inst].play; })}
		}).play(this.tempoclock, quant:8);
	}

	stop { |instList|
		Task.new({
		instList.do { |inst|
			this.instDict[inst].reset}
		}).play(this.tempoclock, quant:[7,0.5]);
	}




	chooseScaleRoot {
		var scale, root;
		scale = [Scale.ionian, Scale.dorian, Scale.phrygian, Scale.lydian, Scale.mixolydian, Scale.aeolian, Scale.locrian].wchoose([0.1,0.4,0.2,0.3,0.2,0.2,0.1]);
		root = [-8,-7,-5,-3,-1,0,2].choose;
		["NEXT SCALE WILL BE", scale, ModalBass.getNoteName(root)].postln;
		^[scale, root];
	}

	doOstinato {
		var bassScore;
		bassScore = [Conductor.generateOstinato(), Scale.dorian, 2, 0 ];
		this.bass.scoreBag = bassScore;
		this.bass.changeToScore = true;
		this.drums.playMode = \playRegFill;
		this.tempoclock.sched(31, { "called".postln; this.drums.playMode = \playRegularPolymetric });
	}

	manualModeSelect { |scale, root|
		// override schedule
		// manually control mode after executeChart
		this.continueRoutine = false;
		this.onDeck = [scale, root];
		/* NB for prepareNextMode will fail if not called in the last cycle*/

	}

	executeChart { |chordChart|
		// entries in the form [scale, root, numPatterns]
		var expandedChart = [], mainSched, onDeckChart, onDeckSched, schedule, counter = 0;
		chordChart.do { |event, i|
			var counter = 1;
			expandedChart = expandedChart.add([event[0], event[1]]);
			while ({counter < event[2]},
				{ expandedChart = expandedChart.add([]);
			counter = counter + 1} );
		};
		onDeckChart = expandedChart.rotate(-1);
		onDeckChart[onDeckChart.size -1] = onDeckChart[onDeckChart.size -1].add("end");
		this.prepareNextMode(expandedChart[0][0],expandedChart[0][1]);


		// reset and re-initialize everything
		schedule = Task.new({
		if (this.tempoclock.beats < 1, {
		if (this.bass.pb != nil , {this.bass.pb.stop});
		if (this.drums.pb != nil , {this.drums.pb.stop});
		if (this.pianoPbind != nil , {this.pianoPbind.stop});
		this.bass = ModalBass(expandedChart[0][0],root:expandedChart[0][1],phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
				this.piano = ModalPiano(expandedChart[0][0],root:expandedChart[0][1], midiout:this.midiout, tempoclock: this.tempoclock);
				this.play;});




			this.drums.barCount = 2;  //this might need to be 0? (hacky as all heck)



		onDeckChart.postln;
		expandedChart.postln;
		mainSched = Routine({expandedChart.do {|oneBar|
		oneBar.yield}});
		onDeckSched = Routine({onDeckChart.do {|oneBar|
		oneBar.yield}});

		this.continueRoutine = true; // this value is changed by calling this.manualModeSelect

		this.tempoclock.sched(0,{ var nextOnDeck = onDeckSched.next, nextMain = mainSched.next;
				this.playingProgression = true;
			if ((nextOnDeck == nil),
				{ if (this.continueRoutine,
					{
					onDeckSched.reset; nextOnDeck = onDeckSched.next;
					mainSched.reset; nextMain = mainSched.next;},
					{ this.drums.barCount = 2; "ENDED".postln; this.playingProgression = false; this.setScale(this.onDeck[0], this.onDeck[1]); this.prepareNextMode(onDeck[0], onDeck[1]);nil}
			)});
			if ((nextOnDeck != nil),{  [counter, "CONTINUED", nextMain, nextOnDeck, this.tempoclock.beats].postln; counter = counter +1;
				this.handleChartRoutine( nextOnDeck, nextMain); 8 /*repeat in 8*/});
			 });


		nil}).play(this.tempoclock,quant:8);
	}

	handleChartRoutine { |onDeckEvent, mainEvent|
		if (onDeckEvent != [], { ["C - onDeck to", onDeckEvent[0], onDeckEvent[1]].postln;
			this.prepareNextMode(onDeckEvent[0], onDeckEvent[1])} );
		if ((onDeckEvent.size == 3) && (this.continueRoutine == false), {this.prepareNextMode(onDeck[0], onDeck[1])});
	/*	if (mainEvent != [], { ["C - main set to", mainEvent[0], mainEvent[1]].postln;
			this.setScale(mainEvent[0], mainEvent[1])} );*/
	}

	prepareNextMode{|scale, root|
		this.bass.prepareNextMode(scale, root);
		this.piano.prepareNextMode(scale, root);
	}
	setScale{|scale, root|
		this.bass.setScale(scale, root);
		this.piano.setScale(scale, root);
	}

	changeRandomly { |duration = 4, startScale, startRoot = 2|
		var chart = [], onDeck, onDeckSched;
		if (startScale == nil, {startScale = Scale.dorian});
		if ((startScale != this.bass.scale) || (startRoot != this.bass.root), {
			if (this.bass.pb != nil , {this.bass.pb.stop});
			this.bass = ModalBass(startScale,root:startRoot,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
			this.bass.play;
		});
		if ((startScale != this.piano.scale) || (startRoot != this.piano.root), {
			if (this.pianoPbind != nil , {this.pianoPbind.stop});
			this.piano = ModalPiano(startScale,root:startRoot, midiout:this.midiout, tempoclock: this.tempoclock);
			this.basspiano = this.piano.play;
		});

		// onDeckSched = changeRandomSelect
		this.tempoclock.schedAbs(0,{ var nextOnDeck = onDeckSched.next;
			if (nextOnDeck == nil,
				{ onDeckSched = this.changeRandomSelect(duration); nextOnDeck = onDeckSched.next;});

		this.handleChartRoutine(nextOnDeck); 8 });

	}
	changeRandomSelect { |duration|
		var onDeck, chart = [];
		onDeck = this.chooseScaleRoot();
		chart = Array.fill(duration -1, {[]});
		chart = chart.add(onDeck);
		^Routine({chart.do {|oneBar| oneBar.yield}});
	}

	// methods for controlling drums and fills
	drum_behaviour { |behaviour|
		// \playSingle, \playLastTwo, \playRegularPolymetric
		this.drums.playMode = behaviour
	}

	drum_restoreLastPattern {
		this.drums.restoreLastPattern()
	}

	drum_evolve {
		this.drums.evolveLastFill();
	}

	drum_basicFill { |unitLength, numReps, minDens = 0, maxDens = 0.5|
		this.drums.setCurrentPattern(this.drums.generatePattern(unitLength,numReps,minKickDensity: minDens, maxKickDensity: maxDens));
	}

	drum_polymetricFill { |numBars, unit|
		this.drums.polymetricFill(numBars,unit);
		//e.g. this.drums.polymetricFill(1,3) -> one bar of triplets
	}


	*generateOstinato  {
		var downBeat = ["firstHalf", "secondHalf"].choose,
		positions = [[0,2].choose, [4,6].choose],
		startDur, details, variation,
		outScore = [];
		positions.dopostln;
		if (downBeat == "secondHalf",
			{positions[0] = positions[0] + 1 },
			{positions[1] = positions[1] + 1 });
		positions.dopostln;
		positions.do {|pos, i|
			positions[i] = Conductor.eigthsPosToTripletsPos(pos);
		};


		positions.dopostln;
		outScore = Conductor.scoreFromPositions(positions);

		variation = Conductor.varyOstinato(positions);
		if (2.rand == 0,
			{^(outScore ++ variation ++outScore ++ variation)},
			{^(outScore ++ outScore ++ variation ++ outScore)});
		// ^(outScore ++ outScore ++ outScore ++ outScore);


	}
	*scoreFromPositions { |positions|
		var outScore = [], durations = [];

		positions.do {|position, i|
			// ["TILLHERE",positions.size].postln;
			if (i != (positions.size - 1), {
				durations = durations.add( positions[i+1] - positions[i]);
			});
		};

		durations = durations.add((12 + (12 -  positions[positions.size-1])));
		outScore = outScore.add([\rest, positions[0]*(1/3)]);
		durations.do { |duration|
			outScore = outScore.add([[0,7].choose, duration*(1/3)]);
		};
		^outScore;

	}
	*varyOstinato { |original|
		var gaps = [original[0], original[1] -original[0], 12 - original[1]], maxV, maxI, variation, newNote, outVar, dur;


		maxI = gaps.maxIndex;
		if (maxI.size > 1, {maxI = maxI.choose});
		maxV = gaps[maxI];

		variation = original.copy;
		dur = div(maxV,2);

		case
		{maxI == 0} {newNote = (original[0] - dur); variation.insert(0,newNote)}
		{maxI == 1} {newNote = (original[1] - dur); variation.insert(1,newNote)}
		{maxI == 2} {newNote = (original[1] + dur); variation = variation.add(newNote)};
		variation.postln;


		outVar = Conductor.scoreFromPositions(variation);
		// outVar[2] = [[-1,2,-3,4, 6].choose, outVar[1][1]];
		^outVar;


	}


	*eigthsPosToTripletsPos {|eighth|
		var outVal;
		if (eighth % 2 == 0,
			{outVal = eighth + (eighth * 0.5)},
			{outVal = eighth + ((eighth + 1) *0.5)});
		^outVal;
	}

}
