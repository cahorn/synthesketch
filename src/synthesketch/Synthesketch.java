package synthesketch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

public class Synthesketch {

	static final boolean DEBUG = false;

	static final AudioFormat FORMAT = new AudioFormat(44100, 32, 1, true, false);

	public static void main(String[] args) throws LineUnavailableException,
			UnsupportedAudioFormatException, MidiUnavailableException,
			InterruptedException {
		WaveformSynthesizer synth = new WaveformSynthesizer();
		Scanner input = new Scanner(System.in);
		List<Mixer> mixers = new LinkedList<Mixer>();
		for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			try {
				synth.setOutput(mixer, FORMAT);
				mixers.add(mixer);
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println(mixerInfo + ": " + e.getMessage());
				}
			}
		}
		for (int i = 0; i < mixers.size(); ++i) {
			System.out.format("%2d: %s", i, mixers.get(i).getMixerInfo());
			System.out.println();
		}
		System.out.print("Select a mixer: ");
		Mixer selectedMixer = mixers.get(input.nextInt());
		List<MidiDevice> midis = new ArrayList<MidiDevice>();
		for (MidiDevice.Info midiInfo : MidiSystem.getMidiDeviceInfo()) {
			try {
				MidiDevice midi = MidiSystem.getMidiDevice(midiInfo);
				midi.open();
				midi.getTransmitter();
				midi.close();
				midis.add(midi);
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println(midiInfo + ": " + e.getMessage());
				}
			}
		}
		for (int i = 0; i < midis.size(); ++i) {
			System.out.format("%2d: %s", i, midis.get(i).getDeviceInfo());
			System.out.println();
		}
		System.out.print("Select a midi input: ");
		MidiDevice selectedMidi = midis.get(input.nextInt());
		input.close();
		synth.setOutput(selectedMixer, FORMAT);
		selectedMidi.open();
		selectedMidi.getTransmitter().setReceiver(synth);
		synth.setWaveform(Waveforms.SINE_WAVE);
		synchronized (synth) {
			synth.wait();
		}
	}
}
