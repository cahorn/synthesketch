package synthesketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Synthesketch {

	static final boolean DEBUG = false;

	static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);

	public static void main(String[] args) throws LineUnavailableException,
			UnsupportedAudioFormatException, InterruptedException {
		final WaveformSynthesizer synth = new WaveformSynthesizer();
		Scanner input = new Scanner(System.in);
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
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final AudioPreferencesPanel audio = new AudioPreferencesPanel();
				try {
					audio.setAudioFormat(FORMAT);
				} catch (UnsupportedAudioFormatException e) {}
				try {
					synth.setOutput(audio.getMixer(), audio.getAudioFormat());
				} catch (Exception e) {}
				audio.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						try {
							try {
								synth.setOutput(audio.getMixer(),
										audio.getAudioFormat());
							} catch (NullPointerException exp) {}
						} catch (Exception exp) {
							System.err.println("error: " + exp.getMessage());
						}
					}
				});
				final WaveformPanel editor = new WaveformPanel();
				editor.setWaveform(Waveforms.SINE_WAVE);
				synth.setWaveform(editor.getWaveform());
				editor.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						synth.setWaveform(editor.getWaveform());
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
				JDialog keyboardDialog = new JDialog(window, "Keyboard", false);
				keyboardDialog
						.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				keyboardDialog.add(keyboard);
				keyboardDialog.pack();
				keyboardDialog.addKeyListener(keyboard);
				keyboardDialog.setResizable(false);
				keyboardDialog.setVisible(true);
				JDialog audioDialog = new JDialog(window, "Audio Preferences",
						false);
				audioDialog
						.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				audioDialog.add(audio);
				audioDialog.pack();
				audioDialog.setVisible(true);
			}
		});
	}
}
