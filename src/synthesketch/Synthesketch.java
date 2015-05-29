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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Synthesketch {

	static final boolean DEBUG = false;

	static final AudioFormat FORMAT = new AudioFormat(44100, 32, 1, true, false);

	public static void main(String[] args) throws LineUnavailableException,
			UnsupportedAudioFormatException, InterruptedException {
		final WaveformSynthesizer synth = new WaveformSynthesizer();
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
		final MidiDevice selectedMidi = midis.get(input.nextInt());
		input.close();
		synth.setOutput(selectedMixer, FORMAT);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final WaveformPanel editor = new WaveformPanel();
				editor.setWaveform(Waveforms.SINE_WAVE);
				try {
					synth.setWaveform(editor.getWaveform());
				} catch (UnsupportedAudioFormatException e) {}
				editor.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						try {
							synth.setWaveform(editor.getWaveform());
						} catch (UnsupportedAudioFormatException except) {}
					}
				});
				JFrame window = new JFrame("Waveform");
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.add(editor);
				window.pack();
				window.setResizable(false);
				window.setVisible(true);
				KeyboardPanel keyboard = new KeyboardPanel();
				try {
					selectedMidi.open();
					selectedMidi.getTransmitter().setReceiver(keyboard);
				} catch (MidiUnavailableException e) {}
				keyboard.setReceiver(synth);
				JDialog dialog = new JDialog(window, "Keyboard", false);
				dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				dialog.add(keyboard);
				dialog.pack();
				dialog.addKeyListener(keyboard);
				dialog.setResizable(false);
				dialog.setVisible(true);
			}
		});
	}
}
