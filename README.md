# ModalJazz
A SuperCollider tool for providing a 'reasonable MIDI hand-drawn facsimile' of an improvising modal jazz trio (think Miles Davis' 'So What'), based on an input of a sequence of pitch classes and diatonic modes.  

The bass logic, created by Yasuhiro Sekine, is pretty solid. Close your eyes and imagine someone who really wanted to play like Paul Chambers.
The drum logic, provided by myself (not a drummer, let alone a jazz drummer), is heavily inspired by reading analyses of the great Elvin Jones, and isn't remotely convincing.  But it can generate some impressively interesting patterns based on Jones' cross-rhythm fills.  Using electronic drums rather than sampled acoustic drums, and not trying to pretend that the drummer here is human, this will provide you with some very cool percussion loops (and will use current material to help derive future material).

The piano is minimal, intended merely to help guide other improvisers through the chord progression.

## To run:
open the file NewModalJazzGuiClient.sc, and execute the code in the file (highlight the code and hit cmd+enter)
The music is output is MIDI. It maps as follows:
* Channel 1 is bass
* Channel 2 is drums
* Channel 3 is piano

For the Drums:
* C1 is kick
* D1 is snare
* F1 is floor tom
* F#1 is closed hh
* G#1 is the high tom
* D#2 is ride
