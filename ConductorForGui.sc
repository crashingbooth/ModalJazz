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
		SystemClock.sched(0.1, {
		this.getRootModeInfo();
		{

		this.myGui.resetModeButtons(this.currentScale, this.myGui.externalColor);
		this.myGui.resetRootButtons(this.currentRoot, this.myGui.externalColor);

		if (this.playingProgression,
					{
						this.myGui.exChartButton.setBackgroundColor(0,this.myGui.externalColor)
					},
					{

						this.myGui.exChartButton.setBackgroundColor(0, Color.gray(0.9))
					}
				);

		}.defer
		})
	}

	prepareNextMode { |scale, root|
		super.prepareNextMode(scale,root);
		this.updateGui;

	}
	play {
		super.play;
		this.tempoclock.sched(0, {this.updateGui; 8});
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
