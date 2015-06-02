package synthesketch;

import javax.sound.midi.MidiDevice;
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
				final KeyboardPanel keyboard = new KeyboardPanel();
				synth.setTransmitter(keyboard);
				final MidiPreferencesPanel midi = new MidiPreferencesPanel();
				midi.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						try {
							MidiDevice device = midi.getMidi();
							if (device != null) {
								device.open();
								keyboard.setTransmitter(device.getTransmitter());
							} else {
								keyboard.setTransmitter(null);
							}
						} catch (MidiUnavailableException exp) {}
					}
				});
				JFrame window = new JFrame("Waveform");
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.add(editor);
				window.pack();
				window.setResizable(false);
				window.setVisible(true);
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
				JDialog midiDialog = new JDialog(window, "Midi Preferences",
						false);
				midiDialog
						.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				midiDialog.add(midi);
				midiDialog.pack();
				midiDialog.setVisible(true);
			}
		});
	}
}
