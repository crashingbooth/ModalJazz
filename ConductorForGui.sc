ConductorForGui : Conductor {
	var <>myGui, <>appClock;
	*new { |tempo, midiout|
		^super.new(tempo, midiout).sub_init(tempo) }

	sub_init {
		this.myGui = ModalJazzGUI(this);


	}

	getRootModeInfo {
		this.currentScale = this.bass.scale;
		this.currentRoot = this.bass.root;
	}


	updateGui {
		var drumMode, patName;
		SystemClock.sched(0.1, {
			this.getRootModeInfo();
			{
				this.myGui.resetModeButtons(this.currentScale, this.myGui.externalColor);
				this.myGui.resetRootButtons(this.currentRoot, this.myGui.externalColor);

				if (this.playingProgression,
					{ this.myGui.exChartButton.setBackgroundColor(0,this.myGui.externalColor) },
					{ this.myGui.exChartButton.setBackgroundColor(0, Color.gray(0.9)) }
				);
				drumMode = this.drums.playMode;
				patName = this.drums.currentPattern.name;

				this.myGui.drumModeButtonsReset();
				//<>drum_regularButton, <>drum_playSingleButton, <>drum_last2Button,
				case
				{drumMode.asSymbol == \playSingle } { this.myGui.drum_playSingleButton.setBackgroundColor(0, this.myGui.externalColor)}
				{drumMode.asSymbol == \playLastTwo } { this.myGui.drum_last2Button.setBackgroundColor(0, this.myGui.externalColor)}
				{drumMode.asSymbol ==  \playRegularPolymetric } { this.myGui.drum_regularButton.setBackgroundColor(0, this.myGui.externalColor)};



				// this.drumLastPatternLabel = StaticText(this.drumView, this.m@this.m, this.w@this.h);
				/*this.myGui.drumLastPatternLabel.string_("current: " ++ this.drums.lastPattern.name);
				this.myGui.drumCurrentPatternLabel.string_("previous: " ++ this.drums.currentPattern.name);*/

				this.myGui.drumCurrentPatternLabel.string_(this.drums.currentPattern.name);
				this.myGui.drumLastPatternLabel.string_(this.drums.lastPattern.name);
				// ["last", this.drums.lastPattern.name].postln;



			}.defer
		})

	}

	prepareNextMode { |scale, root|
		super.prepareNextMode(scale,root);
		this.updateGui;

	}
	play {
		super.play;
		this.tempoclock.sched(0, {this.updateGui; 4});
	}


}
/*
UPDATES:
>currentMode
>currentRoot
nextRoot
nextMode
>isPlayingProgression

currentDrumPattern
lastDrumPattern
playMode
*/
