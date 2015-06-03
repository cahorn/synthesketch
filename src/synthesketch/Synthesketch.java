package synthesketch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class Synthesketch extends JFrame {

	public Synthesketch() {
		waveformPanel = new WaveformPanel();
		waveformPanel.setWaveform(Waveforms.SINE_WAVE);
		waveformPanel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				synth.setWaveform(waveformPanel.getWaveform());
			}
		});

		keyboardPanel = new KeyboardPanel();

		audioPrefsPanel = new AudioPreferencesPanel();
		try {
			audioPrefsPanel.setAudioFormat(FORMAT);
		} catch (UnsupportedAudioFormatException e) {}
		audioPrefsPanel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				try {
					try {
						synth.setOutput(audioPrefsPanel.getMixer(),
								audioPrefsPanel.getAudioFormat());
					} catch (NullPointerException e) {}
				} catch (Exception e) {
					System.err.println("error: " + e.getMessage());
				}
			}
		});

		midiPrefsPanel = new MidiPreferencesPanel();
		midiPrefsPanel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				try {
					MidiDevice device = midiPrefsPanel.getMidi();
					if (device != null) {
						device.open();
						keyboardPanel.setTransmitter(device.getTransmitter());
					} else {
						keyboardPanel.setTransmitter(null);
					}
				} catch (MidiUnavailableException e) {
					System.err.println("error: midi unavailable");
				}
			}
		});

		synth = new WaveformSynthesizer();
		try {
			synth.setOutput(audioPrefsPanel.getMixer(),
					audioPrefsPanel.getAudioFormat());
		} catch (NullPointerException e) {
			System.err.println("error: default audio mixer unavailable");
		} catch (Exception e) {
			System.err.println("error: " + e.getMessage());
		}
		synth.setWaveform(waveformPanel.getWaveform());
		synth.setTransmitter(keyboardPanel);

		keyboardDialog = new JDialog(this, "Keyboard", false);
		keyboardDialog.add(keyboardPanel);
		keyboardDialog.pack();
		keyboardDialog.addKeyListener(keyboardPanel);
		keyboardDialog.setResizable(false);
		keyboardDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				keyboardMenuItem.setSelected(false);
			}
		});

		audioPrefsDialog = new JDialog(this, "Audio Preferences", false);
		audioPrefsDialog.add(audioPrefsPanel);
		audioPrefsDialog.pack();
		audioPrefsDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				audioPrefsMenuItem.setSelected(false);
			}
		});

		midiPrefsDialog = new JDialog(this, "Midi Preferences", false);
		midiPrefsDialog.add(midiPrefsPanel);
		midiPrefsDialog.pack();
		midiPrefsDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				midiPrefsMenuItem.setSelected(false);
			}
		});

		keyboardMenuItem = new JCheckBoxMenuItem("Keyboard");
		keyboardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keyboardDialog.setVisible(keyboardMenuItem.isSelected());
			}
		});

		audioPrefsMenuItem = new JCheckBoxMenuItem("Audio Preferences");
		audioPrefsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				audioPrefsDialog.setVisible(audioPrefsMenuItem.isSelected());
			}
		});

		midiPrefsMenuItem = new JCheckBoxMenuItem("Midi Preferences");
		midiPrefsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				midiPrefsDialog.setVisible(midiPrefsMenuItem.isSelected());
			}
		});

		JMenuItem quitMenuItem = new JMenuItem("Quit");
		quitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		JMenu windowMenu = new JMenu("Window");
		windowMenu.add(keyboardMenuItem);
		windowMenu.add(audioPrefsMenuItem);
		windowMenu.add(midiPrefsMenuItem);
		windowMenu.add(new JSeparator());
		windowMenu.add(quitMenuItem);

		JMenuItem sineWaveMenuItem = new JMenuItem("Load Sine Wave");
		sineWaveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				waveformPanel.setWaveform(Waveforms.SINE_WAVE);
				synth.setWaveform(waveformPanel.getWaveform());
			}
		});

		JMenuItem squareWaveMenuItem = new JMenuItem("Load Square Wave");
		squareWaveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				waveformPanel.setWaveform(Waveforms.SQUARE_WAVE);
				synth.setWaveform(waveformPanel.getWaveform());
			}
		});

		JMenuItem sawWaveMenuItem = new JMenuItem("Load Saw Wave");
		sawWaveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				waveformPanel.setWaveform(Waveforms.SAW_WAVE);
				synth.setWaveform(waveformPanel.getWaveform());
			}
		});

		JMenu waveformMenu = new JMenu("Waveform");
		waveformMenu.add(sineWaveMenuItem);
		waveformMenu.add(squareWaveMenuItem);
		waveformMenu.add(sawWaveMenuItem);

		JOptionPane aboutDialogTemplate = new JOptionPane(
				"An arbitrary waveform synthesizer with a simple sketch-based interface\n\n"
						+ "Author: Colby Horn <chorn@middlebury.edu>\n"
						+ "Source Code: https://github.com/purplewolf139/synthesketch",
				JOptionPane.INFORMATION_MESSAGE);
		aboutDialog = aboutDialogTemplate.createDialog(this,
				"About Synthesketch");

		JMenuItem aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aboutDialog.setVisible(true);
			}
		});

		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(aboutMenuItem);

		JMenuBar menubar = new JMenuBar();
		menubar.add(windowMenu);
		menubar.add(waveformMenu);
		menubar.add(helpMenu);

		setJMenuBar(menubar);
		add(waveformPanel);
		pack();
		setTitle("Synthesketch");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
	}

	static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);

	WaveformSynthesizer synth;

	JDialog keyboardDialog, audioPrefsDialog, midiPrefsDialog;
	WaveformPanel waveformPanel;
	KeyboardPanel keyboardPanel;
	AudioPreferencesPanel audioPrefsPanel;
	MidiPreferencesPanel midiPrefsPanel;

	JCheckBoxMenuItem keyboardMenuItem, audioPrefsMenuItem, midiPrefsMenuItem;

	JDialog aboutDialog;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Synthesketch synthesketch = new Synthesketch();
				synthesketch.setVisible(true);
			}
		});
	}
}
