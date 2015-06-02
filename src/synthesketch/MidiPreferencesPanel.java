package synthesketch;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class MidiPreferencesPanel extends JPanel implements ActionListener {

	@SuppressWarnings("rawtypes")
	public MidiPreferencesPanel() {
		midiCombo = new JComboBox();
		midiCombo.addActionListener(this);
		refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshMidi();
			}
		});
		setPreferredSize(new Dimension(500, 50));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(new JLabel("Midi Input:"));
		add(Box.createRigidArea(new Dimension(10, 10)));
		add(midiCombo);
		add(Box.createRigidArea(new Dimension(10, 10)));
		add(refreshButton);
		refreshMidi();
	}

	@SuppressWarnings("rawtypes")
	JComboBox midiCombo;

	JButton refreshButton;

	public MidiDevice getMidi() throws MidiUnavailableException {
		if (midiCombo.getItemCount() < 1 || midiCombo.getSelectedIndex() == 0) {
			return null;
		} else {
			return MidiSystem.getMidiDevice((MidiDevice.Info) midiCombo
					.getSelectedItem());
		}
	}

	@SuppressWarnings("unchecked")
	void refreshMidi() {
		midiCombo.removeAllItems();
		midiCombo.addItem("Virtual Keyboard");
		for (MidiDevice.Info midiInfo : MidiSystem.getMidiDeviceInfo()) {
			try {
				MidiDevice midi = MidiSystem.getMidiDevice(midiInfo);
				midi.open();
				midi.getTransmitter();
				midi.close();
				midiCombo.addItem(midiInfo);
			} catch (Exception e) {}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		for (ChangeListener l : listeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}

	List<ChangeListener> listeners = new LinkedList<ChangeListener>();

	public void addChangeListener(ChangeListener listener) {
		listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listeners.remove(listener);
	}

	public ChangeListener[] getChangeListeners() {
		return (ChangeListener[]) listeners.toArray();
	}

}
