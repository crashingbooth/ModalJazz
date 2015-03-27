ModalJazzGUI {
	var <>cond, <>master, <>startButton,
	<>progression, <>alt_progression,// for executeChart
	<>localMode, <>localRoot,
	<>playControlsView, <>modeSelectView,<>metaManualSelectView, <>manualSelectView, <>setModeButtonHolderView,<>drumView, <>modalInstrumentView,
	<>exChartButton,
	<>h, <>w, <>m,
	<>d_DorButton, <>d_LydButton, <>setModeButton, <>modeButtons, <>rootButtons,
	<>playingProg; // part of global display;
	classvar <>alreadyExists, <>modes, <>modeNames, <>roots;
	*new { |conductor|
		^super.new.init(conductor) }
	*initClass {
		//guarentee singleton
		if (ModalJazzGUI.alreadyExists == true, {"ABORT".postln; ^nil},
			{ ModalJazzGUI.alreadyExists = true; });
		ModalJazzGUI.modeNames = ["Ionian", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Aeolian", "Locrian"];
		ModalJazzGUI.modes = [Scale.ionian, Scale.dorian, Scale.phrygian, Scale.lydian, Scale.mixolydian, Scale.aeolian, Scale.locrian];
		ModalJazzGUI.roots = ["F#", "G", "Ab", "A", "Bb", "B","C", "C#", "D", "Eb", "E", "F"];
	}
	init { |conductor|
		this.cond = conductor;
		this.progression =  [[Scale.dorian, 2, 4],[Scale.lydian, 2, 4],[Scale.phrygian, 4, 4],[Scale.dorian, 3, 4]];
		this.alt_progression = [[Scale.dorian, 2, 4],[Scale.lydian, 2, 4],[Scale.phrygian, 4, 4],[Scale.dorian, 3, 4]];
		this.playingProg = false;
		this.localMode = Scale.dorian;
		this.localRoot = 2;
		this.buildGUI();
	}

	buildGUI {
		var sec, h, w, m,  // width, height, margin, section
		playingProg;
		this.h = 30; this.m = 5; this.w = 60;
		h = this.h; w = this.w; m = this.m; sec = (h + (2 * m));
		this.master = Window("ModalJazz", Rect(1300,0, 1020, 750)).front
		.alwaysOnTop_(true);
		this.master.view.decorator_(FlowLayout(this.master.bounds, m@m, m@m));

		// play controls
		this.playControlsView = CompositeView(this.master, 1000@(sec));
		this.playControlsView.decorator_(FlowLayout(this.playControlsView.bounds, m@m, m@m));
		this.playControlsView.background_(Color.magenta);
		this.startButton = Button(this.playControlsView, w@h)
		.states_([["start", Color.green]])
		.action_({
			this.cond.manualModeSelect(Scale.dorian, 2);
			this.cond.play; "GUI:Started".postln;
			this.startButton.states_([["playing", Color.blue]]);
		});

		// mode select
		this.modeSelectView = CompositeView(this.master, 1000@(h+h+m+m+m));
		this.modeSelectView.decorator_(FlowLayout(this.modeSelectView.bounds, m@m, m@m));
		this.modeSelectView.background_(Color.blue);
		this.exChartButton = Button(this.modeSelectView,(w*2)@h)
		.states_([["play Progression"]])
		.action_({ this.cond.executeChart(this.progression);
		this.playingProg = true;
		"GUI: progression".postln
		});

		this.d_DorButton = Button(this.modeSelectView,w@h)
		.states_([["D dorian"]])
		.action_({if (this.cond.continueRoutine == false, {this.cond.prepareNextMode(Scale.dorian, 2)});
			this.cond.manualModeSelect(Scale.dorian, 2);
			"GUI: D dorian set".postln
		});
		this.d_LydButton = Button(this.modeSelectView,w@h)
		.states_([["D lydian"]])
		.action_({if (this.cond.continueRoutine == false, {this.cond.prepareNextMode(Scale.lydian, 2)});
		this.cond.manualModeSelect(Scale.lydian, 2);
		"GUI: D lydian set".postln
		});

		// manual select
		this.metaManualSelectView = CompositeView(this.modeSelectView, 720@(h+h+m+2));
		this.metaManualSelectView.decorator_(FlowLayout(this.metaManualSelectView.bounds, 0@0, 0@0));
		this.metaManualSelectView.background_(Color.rand);
		this.setModeButton = Button(this.metaManualSelectView, 0@0, w@(h+h+h+m))
		.states_([["set"]])
		.action_({ this.applySetButton(this.localMode, this.localRoot)});
		this.manualSelectView = CompositeView(this.metaManualSelectView, 650@(h+h+m+1));
		this.manualSelectView.decorator_(FlowLayout(this.manualSelectView.bounds, 0@0, (m+10)@m));
		this.manualSelectView.background_(Color.green);
		this.setModeButtons();
		this.manualSelectView.decorator.nextLine;
		this.setRootButtons();

		//
		this.master.view.decorator.nextLine;
		this.modalInstrumentView = CompositeView(this.master,400@550);
		this.modalInstrumentView.background_(Color.rand);
		this.drumView = CompositeView(this.master, 600@550);
		this.drumView.background_(Color.rand);


	}
	// mode select helper methods
	setModeButtons{
		this.modeButtons = Array.fill (7,
			{ |i| Button(this.manualSelectView, (this.w*1.3)@this.h)
				.states_([[ModalJazzGUI.modeNames[i], Color.black,  Color.gray(0.9)]])
				.action_({this.localMode = ModalJazzGUI.modes[i];
					this.resetModeButtons(i);
					ModalJazzGUI.modeNames[i].postln});
			});
	}
	resetModeButtons{ |select|
		this.modeButtons.do {|self| self.setBackgroundColor(0, Color.gray(0.9))};
		if (select != nil, {this.modeButtons[select].setBackgroundColor(0, Color.yellow)}) ;
	}
	setRootButtons {
	this.rootButtons = Array.fill (12,
			{ |i| Button(this.manualSelectView, ((h*1.3))@h)
				.states_([[ModalJazzGUI.roots[i], Color.black,  Color.gray(0.9)]])
				.action_({this.localRoot = (i-6);
					this.resetRootButtons(i);
					ModalJazzGUI.roots[i].postln});
			});
	}
	resetRootButtons{ |select|
		this.rootButtons.do {|self| self.setBackgroundColor(0, Color.gray(0.9))};
		select.postln;
		if (select != nil, {this.rootButtons[select].setBackgroundColor(0, Color.yellow)}) ;
	}
	applySetButton {
		this.cond.prepareNextMode(this.localMode, this.localRoot);
		this.resetModeButtons();
		this.resetRootButtons();
		this.modeButtons[ModalJazzGUI.modes.indexOf(this.localMode)].setBackgroundColor(0, Color.blue(0.8));
		this.rootButtons[this.localRoot + 6].setBackgroundColor(0, Color.blue(0.8));


	}


}
/*
TODO:
use 3 colours for manual select: blue (machine selected), purple (called by set), yellow (human temp)
*/
