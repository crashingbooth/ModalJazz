DrumPattern {
	var  <>length, <>drumArray, <>name;
	classvar <>drumIndexDict, <>accentDict, <>drumList, <>swingRatio, <>eighths;

	*new { |name, length, drumParts, getDefaultRide =true|
	^super.new.init (name, length, drumParts, getDefaultRide) }

	*initClass {
		// amp of 0.5 will be default volume
		DrumPattern.accentDict = Dictionary.newFrom(List[\s, 0.58, \w, 0.3,  \n, 0.5, \vs, 0.8, \r, 0]);
		DrumPattern.drumList = ["kick","snare","ride","openhh", "closedhh","rim","midtom", "hightom", "lowtom"];
		DrumPattern.drumIndexDict = Dictionary();
		DrumPattern.drumList.do {|name, i| DrumPattern.drumIndexDict[name] = i} ;
		DrumPattern.setSwing(2.7);
	}

	init { |name = "unnamed", length = 4, drumParts, getDefaultRide|
		// amp of 0.5 will be default volume
		this.name = name;
		this.drumArray = Array.fill(DrumPattern.drumList.size,{[]});
		this.length = length;
		if (getDefaultRide, {
			this.getDefaultRideAndHat();});
		this.getMultipleDrums(drumParts);

	}

	at { |i|
		^this.drumArray[i]
	}
	*setSwing {|ratio|
		DrumPattern.swingRatio = ratio;
		DrumPattern.eighths = [DrumPattern.swingRatio/ (DrumPattern.swingRatio + 1), 1 / (DrumPattern.swingRatio + 1)];
	}


	getDefaultRideAndHat {
		this.addOneDrum("ride", [[1],[DrumPattern.eighths[0],\s],[DrumPattern.eighths[1]],[1],[DrumPattern.eighths[0],\s],[DrumPattern.eighths[1]]]);
		this.addOneDrum("closedhh", [[1,\r],[1,\s],[1,\r],[1,\s]])
	}

	addOneDrum { |drumName, drumList|
		var index, totalLength = 0;
		index = DrumPattern.drumIndexDict[drumName];

		this.drumArray[index] = [];
		drumList.do { |event|
			var accent = \n;
			if (event.size == 1, {accent = \n;}, {accent = event[1]});
			this.drumArray[index] = this.drumArray[index].add([event[0], DrumPattern.accentDict[accent]]);
			totalLength = totalLength + event[0];
		};
		if ((totalLength.equalWithPrecision(this.length, 0.01)) != true, {["part length does not match obj length:", totalLength, this.length,"drumlist", drumList].postln });
	}
	getMultipleDrums { |drumParts|
		drumParts.do { |drumLine|
			this.addOneDrum(drumLine[0], drumLine[1]);
		}
	}

	display {
		this.drumArray.do { |line, i|
			var lineStr = "";
			line.do { |vals|
				vals[0] = (vals.[0] + 0.00000001); // to prevent error for for short numbers :(
				lineStr =  lineStr ++ "(" ++ vals[0].asStringPrec(2) ++ ", " ++ vals[1].asString ++ "),";
			};
			lineStr = lineStr[0..(lineStr.size -2)];
			[DrumPattern.drumList[i], lineStr].postln;
		}
	}
}

DrumPlayer {
	var <>midiout, <>tempoclock, <>library, <>currentPattern, <>primaryPat, <>secondaryPat, <>barCount, <>playMode, <>varProb, <>rideLibrary, <>schedule, <>verbose, <>tempoclock, <>basicKSLibrary, <>lastPattern, <>pb, <>mostRecentFill;
	classvar <>midiNumIndex, <>choices;

	*new{ |midiout, tempoclock|
	^super.new.init (midiout, tempoclock) }
	*initClass {
		DrumPlayer.midiNumIndex = [36, 38, 51, 46, 42, 37, 45, 44,41]; //kick, snare, ride, open, closed, rim, lowtom, hightom, lowtom
		DrumPlayer.choices = ["snare", "kick", "closedhh","closedhh", "midtom", "hightom","rest"];
	}
	init { |midiout, tempoclock|
		this.midiout = midiout;
		if (tempoclock == nil, { this.tempoclock = TempoClock.new(132/60)}, { this.tempoclock = tempoclock });

		this.playMode = \playRegularPolymetric; // or \playRandom, \playEntireLibrary, \playNormal, \playSingle, playRegularPolymetric
		this.barCount = 1;
		this.buildKSLibrary(); //kick -snare library

		this.buildRideLibrary();
		this.schedule = Routine({});
		this.setCurrentPattern(DrumPlayer.build1BarPattern(this.basicKSLibrary[0]));
		this.primaryPat = this.basicKSLibrary[0];
		this.secondaryPat = this.basicKSLibrary[0];
		this.verbose = false;
		this.varProb = 0.5;
	}

	playRandomPatterns { |drumNumber|
		this.setCurrentPattern(this.library.choose);
		this.currentPattern.name.postln;
	}

	*build1BarPattern { |ksLibraryIndex, ridePat = nil|
		// ksLibraryIndex should be full list from buildKSLIbrary, ridePat id eventlist only
		var outPattern;

		outPattern = DrumPattern.new(ksLibraryIndex[0],ksLibraryIndex[1],ksLibraryIndex[2]);
		if (ridePat != nil, { outPattern.addOneDrum("ride", ridePat[1])});
		^outPattern;

	}
	setCurrentPattern{ |newPattern|
		if (this.currentPattern == nil, {this.lastPattern = newPattern}, {this.lastPattern = this.currentPattern.copy});
		this.currentPattern = newPattern;
		["in deum parttern", this.currentPattern.name, this.lastPattern.name].postln;
	}
	reportState {
		["playMode", this.playMode, "current", this.currentPattern.name, "last", this.lastPattern.name].postln;
	}

	restoreLastPattern {
		this.setCurrentPattern(this.lastPattern);
	}

	playEntireLibrary { |reps = 4|
		var getVal;
		getVal = (this.barCount / reps).asInteger;
		this.setCurrentPattern(DrumPlayer.build1BarPattern(this.basicKSLibrary.wrapAt(getVal)));
		postf("currently playing % \n", this.currentPattern.name);
	}


	chooseByGamblersFallacy {
		var die = 1.0.rand, oldVarProb = this.varProb, newPattern;
		if (die > this.varProb,
			{ if (3.rand > 0,
				{newPattern = this.primaryPat.copy}, {newPattern = this.secondaryPat.copy});
				this.varProb = this.varProb + 0.1;

			}, {
				this.varProb = 0.5;
				newPattern = this.basicKSLibrary.choose;
				while ( { (newPattern == this.primaryPat) ||(newPattern == this.secondaryPat) },
				{newPattern = this.basicKSLibrary.choose;} )
			}
		);
		^newPattern;
	}
	playRegularPolymetric {
		var die = 2.rand;


		case
		{(this.barCount % 16) == 7} {

			case
			{die == 0 } {this.basicFill(1)}
			{die == 1 } {this.polymetricFill(1, [2,3,3,4].choose)}
			}
		{(this.barCount % 16) == 14} {

			case
			{die == 0 } {this.basicFill(2)}
			{die == 1 } {this.polymetricFill(2, 3)}
			};
		this.playNormal;



	}
	playRegFill {
		var die = 2.rand;
		this.playNormal;
		if ((this.barCount % 2) == 0,
		{

				this.polymetricFill(1, [3,4].choose); }
			);

	}

	playNormal {
		var next;
		next = this.schedule.next;
		if (this.verbose, {["barCount", this.barCount, this.playMode].postln;});
		if ( next == nil,
			{	this.setCurrentPattern(DrumPlayer.build1BarPattern(this.chooseByGamblersFallacy()));
			if (this.verbose,{	this.currentPattern.name.postln });},
			{ this.setCurrentPattern(next.copy); } );


	}

	playSingle {
		var next;
		if (next != nil, {this.setCurrentPattern(next)});
		["playing", this.currentPattern.name].postln;
	}

	playLastTwo {
		var next = this.schedule.next;
		if (next != nil, {["next",next.name].postln});
		this.reportState;
		if (next != nil, {this.setCurrentPattern(next)} ,
		{
			this.setCurrentPattern(this.lastPattern)});
			["playing lastTwo", this.currentPattern.name].postln;
	}

	playRegenerateCustom {
		if ((this.barCount % 2) == 1,
			{ this.setCurrentPattern(this.generatePattern) });
	}

	scheduleRideVar {
		// schedule 2 bars using 2 bar ride variation
		var rideVar = this.rideLibrary.choose, twoPatterns = [nil,nil];

		2.do { |i|
			var ks;
			ks = this.chooseByGamblersFallacy();
			twoPatterns[i] = DrumPlayer.build1BarPattern(ks, rideVar[i]);

		};
		this.schedule = Routine({twoPatterns.do {|pattern| pattern.yield}});

	}


	decideNext {
		/*this.barCount = this.barCount + 1;*/
		case
		{ this.playMode == \playRegularPolymetric} {this.playRegularPolymetric}
		{ this.playMode == \playRandom} {this.playRandomPatterns()}
		{ this.playMode == \playEntireLibrary} {this.playEntireLibrary()}
		{ this.playMode == \playNormal} {this.playNormal()}
		{ this.playMode == \playSingle } {this.playSingle()}
		{ this.playMode == \playLastTwo} {this.playLastTwo()}
		{ this.playMode == \playCustom } {this.playRegenerateCustom()}
		{ this.playMode == \playRegFill } {this.playRegFill};
		// if (((this.barCount % 8) == 0), {["barCount", this.barCount].postln; this.scheduleRideVar()});

		this.barCount = this.barCount + 1;
		if (this.verbose, {this.currentPattern.display();});
	}

	processPattern { |drumNumber|
		// Builds Pseq readable list from currentPattern - always called!
		// [midiNote, dur, amp] or [\rest, dur, 0]
		var drumLine, output = [];

		drumLine = this.currentPattern.[drumNumber];
		if (drumLine.size == 0, {output = [[\rest, this.currentPattern.length, 0]]}, {
			drumLine.do { |event, i| //event is [dur, amp (or rest symbol)]

				if (event[1] == 0, {output = output.add([\rest, event[0], 0])},
				{output = output.add([DrumPlayer.midiNumIndex[drumNumber], event[0], event[1]])})
		};});

		^output;
	}


	play { |mode = nil|
		var pbs = [], beatsched;
		this.pb = [];
		beatsched = BeatSched.new(tempoClock:this.tempoclock);
		beatsched.beat = 0;
		beatsched.qsched(3.98,{ this.decideNext; 4 });
		DrumPlayer.midiNumIndex.do { |drumNum, i|
			this.pb = this.pb.add( Pbind (
				\type, \midi,
				\midiout, this.midiout,
				[\midinote, \dur, \raw_amp], Pn(Plazy{Pseq(this.processPattern(i))}),
				\amp, Pkey(\raw_amp) + Pwhite(-0.02, 0.02),
				\chan, 1,
				\lag, Pwhite(-0.02, 0.02)
				).play(this.tempoclock);
		) };
		^this.pb;
	}

	basicFill { |numBars = 1, repeat = false|
		// create and schedule 1 bar fill
		var fill, patternArr = [], temp = this.generatePattern();
		numBars.do { |i|
			if (repeat == false, { temp = this.generatePattern()});
			patternArr = patternArr.add(temp);
		};

		this.schedule = Routine({patternArr.do {|fill| fill.yield}});

	}

	generatePattern { |phraseLength = 6, reps = 2, minKickDensity = 0, maxKickDensity = 1|
		// ride will play quarters, make phrase of length phraseLength and repeat reps times
		// reps will be mutated (either from most recent rep or 1st rep)
		var initialArray = [], repsArr = [], accent1 = phraseLength.rand, accent2 = phraseLength.rand, outList, kickRate, kicks,minKickFlag = false, maxKickFlag = false;
		var count = 0, outName = "";
		outName = "customFill " ++ phraseLength ++ " x " ++ reps;
		while ({(minKickFlag == false) || (maxKickFlag == false)},

			{   // generate initial pattern w/ accents
				initialArray = [];
				minKickFlag = false;
				maxKickFlag = false;
				phraseLength.do { |i|
					var acc = \n;
					if ( (i == accent1) || (i == accent2), { acc = \s } );
					initialArray = initialArray.add([DrumPlayer.choices.choose, acc]) };

				repsArr = this.createVariations(phraseLength, reps, initialArray);

				// kick counter
				kicks = 0;
				repsArr.do { |initArr|
					initArr.do {
					|pair| if (pair[0] == "kick", {kicks = kicks +1;}) };
				};
				kickRate = (kicks/(reps*phraseLength));

				if (minKickDensity <= kickRate, {minKickFlag = true});
				if (maxKickDensity >= kickRate, {maxKickFlag = true});
				count = count +1;
				if (count > 50, {minKickFlag = true; maxKickFlag = true; "couldn't meet kick ratio".postln});



		} );
		outList = [];
		// flatten repsArr
		this.mostRecentFill = repsArr;
		repsArr.do {  |phrase| outList = outList ++ phrase	};

		^DrumPlayer.monoListToPattern(outList, name: outName, hitsPerBar:(phraseLength * reps));
	}

	createVariations {|phraseLength, reps, source|
		var repsArr = [];
		repsArr = repsArr.add(source);
		(reps -1).do {
			var temp = [repsArr[repsArr.size -1].copy, repsArr[0].copy].choose,  // start with 1st or most recent rep
			change1, change2, pair1, pair2;
			change1 = phraseLength.rand;
			change2 = phraseLength.rand;
			pair1 = temp[change1];
			pair1 = [DrumPlayer.choices.choose, pair1[1]];
			temp[change1] = pair1;
			pair2 = temp[change2];
			pair2 = [DrumPlayer.choices.choose, pair2[1]];
			temp[change1] = pair2;
			repsArr = repsArr.add(temp);}
		^repsArr


	}
	evolveLastFill {
		// takes most recent output of generatePattern, evolves from 1st or last pattern
		var source, reps, repsArr, phraseLength, outList;

		// in case there is no fill already, make one
		if (this.mostRecentFill == nil, {this.setCurrentPattern(this.generatePattern(3,4,0,0.3)) } ,{

		source = [this.mostRecentFill[0], this.mostRecentFill[this.mostRecentFill.size-1]].choose;
		reps = this.mostRecentFill.size;
		phraseLength = source.size;
		repsArr = this.createVariations(phraseLength, reps, source);

		outList = [];
		// flatten repsArr
		this.mostRecentFill = repsArr;
		repsArr.do {  |phrase| outList = outList ++ phrase	};

			this.setCurrentPattern(DrumPlayer.monoListToPattern(outList, name: "evolved", hitsPerBar:(phraseLength * reps))); })

	}

	*monoListToPattern { |monoList, name = "generated pattern", hitsPerBar = 12|
		var drumArray, template, outPattern, oneHitDur = (4/hitsPerBar);
		drumArray = Array.fill (DrumPattern.drumList.size { [] });


		monoList.do { |event, stepNum|
			drumArray.do { |arraySoFar, arraySlotNum |
				if (DrumPattern.drumIndexDict[event[0].asString] == arraySlotNum,
					{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([oneHitDur, event[1]]) },
				{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([oneHitDur, \r]) } );
			}
		};
		template = [];
		DrumPattern.drumList.do { |drumName, i|
			template = template.add([drumName, drumArray[i]]);
		};

		outPattern = DrumPattern.new( name, 4, template);
		outPattern.getDefaultRideAndHat();
		^outPattern
	}

	polymetricFill { |numBars = 2, unit = 3|
		var metersArr, inflatedArr = [], outPatternArr, numHits, hitsPerBar, drumOrder, drumCount = 0, drumArray;

		hitsPerBar = unit*4;
		numHits = hitsPerBar*5;
		drumOrder = ["hightom","midtom","snare","lowtom","snare"].scramble;
		drumOrder = drumOrder[0..rrand(2,3)];
		outPatternArr = [];
		drumArray = Array.fill (DrumPattern.drumList.size { [] });
		metersArr = Array.fill (2, {5.rand  + 2 });
		while ({inflatedArr.size < numHits},
			{
				metersArr.do { |currentMeter|
					inflatedArr = inflatedArr.add("acc");
					(1..(currentMeter-1)).do { inflatedArr = inflatedArr.add(drumOrder.wrapAt(drumCount)); drumCount = drumCount + 1; };
					// drumCount = drumCount + 1;
				}
		});

		// cut off extra
		inflatedArr = inflatedArr[0..(numHits -1)];

		inflatedArr.do { |event, stepNum|
			var kick = 0, ride = 2;
			drumArray.do { |arraySoFar, arraySlotNum |
				if (DrumPattern.drumIndexDict[event.asString] == arraySlotNum,
					{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/unit, \w]) },
				{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/unit, \r]) } );

			};
			if (event == "acc", {
					drumArray[kick][drumArray[ride].size -1] = [1/unit, \n];
					drumArray[ride][drumArray[ride].size -1] = [1/unit, \n];
					});
		};


		numBars.do { |barNum|
			var template = [];
			DrumPattern.drumList.do {  |drumName, i|
				template = template.add([drumName, drumArray[i][(barNum * hitsPerBar)..(barNum * hitsPerBar + (hitsPerBar-1))]]);

			};
			outPatternArr = outPatternArr.add(DrumPattern("polymetric"++numBars++" "++unit, 4, template, false));
		};
		// ^outPatternArr;

		// if single then repeat
		this.schedule = Routine({outPatternArr.do {|pattern| pattern.yield}});

	}

	buildRideLibrary {
		var d, u; // i.e., downbeat, upbeat

		d = DrumPattern.eighths[0]; u = DrumPattern.eighths[1];
		this.rideLibrary = [
			// [0]
			[["ride", [[1],[d,\s],[u],[1],[d,\s],[u]]],
			["ride", [[1],[1,\s], [1],[1,\s]]] ] ,
			// [1]
			[["ride", [[1],[d,\s],[u],[1],[d,\s],[u]]],
			["ride", [[d],[u],[1,\s],[d],[u],[1,\s]]]],
			// [2]
			[["ride", [[1],[d,\s],[u],[1],[1,\s]]],
			["ride", [[d],[u],[1,\s],[1],[d,\s],[u]]]],
			[["ride", [[1],[d,\s],[u],[1],[1,\s]]],
			["ride", [[1],[1],[1],[1]]]]
		];
	}

	reset {
		if (this.pb != nil, {this.pb.stop; this.pb = nil});
		this.barCount = 1;
	}
	buildKSLibrary{
		// basic
		this.basicKSLibrary = [
			["pat 1",4,[
				["kick", [[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3],[1,\r]]],
				["snare", [[1,\r],[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3]]]
			] ],
			["pat 2", 4,[
				["kick", [[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3]]],
				["snare", [[1,\r],[1,\s],[1,\r],[1,\s]]]
			] ],
			/*this.library = this.library.add(DrumPattern.new("pat 3", 4,[
			["snare", [[2 + (1/3),\r],[1/3,\s],[1/3],[1,\r]]]
			] ) );*/
			/*	this.library = this.library.add(DrumPattern.new("pat 4", 4,[
			["snare", [[1/3],[1/3],[(1/3),\r],[1,\r],[1/3],[1/3],[(1/3),\r],[1,\r]]],
			["kick", [[2/3,\r],[1/3],[1,\r],[2/3,\r],[1/3],[1,\r]]]
			] ) );*/
			["pat 5", 4,[
				["snare", [[2/3,\r],[1/3],[1/3,\r],[1/3],[1/3],[1,\r],[1,\s]]],
				["kick", [[2,\r],[2/3,\r],[1/3],[1,\r]]]
			] ],
			// comping
			["pat 6",4,[
				["kick", [[(1+(1/3))], [(1+(1/3))],[(1+(1/3))]]],
				["snare", [[2/3], [1 +(1/3)], [1 +(1/3)], [2/3]]]
			] ],
			["pat 7",4,[
				["kick", [[2],[2]]],
				["snare", [[2 +(1/3),\r], [1/3],[1 +(1/3)]]]
			] ],
			["pat 8, poly1",4,[
				["kick", [[(2/3),\r], [1], [1], [1],[(1/3)]]],
				["snare", [[1/3],[1 +(2/3)],[1/3],[1 +(2/3)] ]]
			] ],
			["pat 9",4,[
				["kick", [[(2/3),\r], [1 +(1/3)], [2/3], [1 +(1/3)]]],
				["snare", [[1,\r],[1 +(1/3),\s],[2/3],[1,\s] ]]
			] ],
			["pat 10",4,[
				["kick", [[1/3,\r],[2/3],[2/3,\s],[2/3],[2/3],[2/3,\s],[1/3]]],
				["snare", [[2/3],[2/3],[2/3],[2/3],[2/3],[2/3]]]
			] ],
			["pat 11a",4,[
				["kick", [[1/3],[1],[1/3],[1],[1/3],[1]]],
				["snare", [[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3]]]
			] ],
			["pat 11b",4,[
				["snare", [[1/3],[1],[1/3],[1],[1/3],[1]]],
				["kick", [[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3]]]
			] ]
			/*	this.library = this.library.add(DrumPattern.new("pat 11",4,[
			["kick", [[(1/3), \r],[2],[1 +(2/3)]]],
			["snare", [[1 +(1/3),\r], [2], [1/3,\s],[1/3]]]
			] ) );*/

		]; // end of library

	}



}
