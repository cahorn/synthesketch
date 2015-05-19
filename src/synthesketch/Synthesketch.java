package synthesketch;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

public class Synthesketch {

	static final boolean DEBUG = false;

	static final AudioFormat FORMAT = new AudioFormat(44100, 32, 1, true, false);

	public static void main(String[] args) throws LineUnavailableException,
			InterruptedException, UnsupportedAudioFormatException {
		WaveformSynthesizer synth = new WaveformSynthesizer();
		List<Mixer> mixers = new LinkedList<Mixer>();
		for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			try {
				synth.setOutput(mixer, FORMAT);
				mixers.add(mixer);
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println(mixer.getMixerInfo() + ": "
							+ e.getMessage());
				}
			}
		}
		for (int i = 0; i < mixers.size(); ++i) {
			System.out.format("%2d: %s", i, mixers.get(i).getMixerInfo());
			System.out.println();
		}
		System.out.print("Select a mixer: ");
		Scanner input = new Scanner(System.in);
		int selection = input.nextInt();
		input.close();
		synth.setOutput(mixers.get(selection), FORMAT);
		synth.setWaveform(WaveformSynthesizer.SINE_WAVE);
		synth.noteOn(60);
		synth.noteOn(64);
		synth.noteOn(67);
		Thread.sleep(3000);
		synth.noteOff(60);
		synth.noteOff(64);
		synth.noteOff(67);
		synth.close();
	}
}
