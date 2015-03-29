ConductorForGui : Conductor {
	var <>myGui;
	*new { |tempo, midiout|
		^super.new(tempo, midiout).sub_init() }

	sub_init {
		this.myGui = ModalJazzGUI(this);

	}

	updateGui {
	}


}
