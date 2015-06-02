package synthesketch;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class AudioPreferencesPanel extends JPanel implements ActionListener {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AudioPreferencesPanel() {
		Dimension padding = new Dimension(10, 10);

		sampleRateComboBox = new JComboBox(SUPPORTED_SAMPLE_RATES.toArray());
		sampleRateComboBox.addActionListener(this);
		bitDepthComboBox = new JComboBox(SUPPORTED_BIT_DEPTHS.toArray());
		bitDepthComboBox.addActionListener(this);
		channelsComboBox = new JComboBox(SUPPORTED_CHANNELS.toArray());
		channelsComboBox.addActionListener(this);
		JPanel comboBoxPanel = new JPanel();
		comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel,
				BoxLayout.LINE_AXIS));
		comboBoxPanel.add(new JLabel("Sample Rate:"));
		comboBoxPanel.add(Box.createRigidArea(padding));
		comboBoxPanel.add(sampleRateComboBox);
		comboBoxPanel.add(Box.createRigidArea(padding));
		comboBoxPanel.add(new JLabel("Bit Depth:"));
		comboBoxPanel.add(Box.createRigidArea(padding));
		comboBoxPanel.add(bitDepthComboBox);
		comboBoxPanel.add(Box.createRigidArea(padding));
		comboBoxPanel.add(new JLabel("Channels:"));
		comboBoxPanel.add(Box.createRigidArea(padding));
		comboBoxPanel.add(channelsComboBox);

		signedCheckBox = new JCheckBox("Signed", true);
		signedCheckBox.addActionListener(this);
		bigEndianCheckBox = new JCheckBox("Big-endian", true);
		bigEndianCheckBox.addActionListener(this);
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel,
				BoxLayout.LINE_AXIS));
		checkBoxPanel.add(signedCheckBox);
		checkBoxPanel.add(Box.createRigidArea(padding));
		checkBoxPanel.add(bigEndianCheckBox);
		
		mixerComboBox = new JComboBox();
		mixerComboBox.addActionListener(this);
		JPanel mixerPanel = new JPanel();
		mixerPanel.setLayout(new BoxLayout(mixerPanel, BoxLayout.LINE_AXIS));
		mixerPanel.add(new JLabel("Mixer:"));
		mixerPanel.add(Box.createRigidArea(padding));
		mixerPanel.add(mixerComboBox);

		setPreferredSize(new Dimension(500, 125));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(comboBoxPanel);
		add(Box.createRigidArea(padding));
		add(comboBoxPanel);
		add(Box.createRigidArea(padding));
		add(checkBoxPanel);
		add(Box.createRigidArea(padding));
		add(mixerPanel);
	}

	static final Collection<Integer> SUPPORTED_SAMPLE_RATES = new LinkedList<Integer>();
	static {
		Integer[] sampleRates = { 11025, 22050, 44100, 48000 };
		SUPPORTED_SAMPLE_RATES.addAll(Arrays.asList(sampleRates));
	}

	static final Collection<Integer> SUPPORTED_BIT_DEPTHS = new LinkedList<Integer>();
	static {
		Integer[] bitDepths = { 8, 16, 32 };
		SUPPORTED_BIT_DEPTHS.addAll(Arrays.asList(bitDepths));
	}

	static final Collection<Integer> SUPPORTED_CHANNELS = new LinkedList<Integer>();
	static {
		Integer[] channels = { 1, 2 };
		SUPPORTED_CHANNELS.addAll(Arrays.asList(channels));
	}

	@SuppressWarnings("rawtypes")
	JComboBox sampleRateComboBox, bitDepthComboBox, channelsComboBox;
	
	JCheckBox signedCheckBox, bigEndianCheckBox;
	
	@SuppressWarnings("rawtypes")
	JComboBox mixerComboBox;

	public AudioFormat getAudioFormat() {
		return new AudioFormat((Integer) sampleRateComboBox.getSelectedItem(),
				(Integer) bitDepthComboBox.getSelectedItem(),
				(Integer) channelsComboBox.getSelectedItem(),
				signedCheckBox.isSelected(), bigEndianCheckBox.isSelected());
	}

	public void setAudioFormat(AudioFormat format)
			throws UnsupportedAudioFormatException {
		if (!SUPPORTED_SAMPLE_RATES.contains((int) format.getSampleRate())
				|| !SUPPORTED_BIT_DEPTHS.contains(format.getSampleSizeInBits())
				|| !SUPPORTED_CHANNELS.contains(format.getChannels())) {
			throw new UnsupportedAudioFormatException();
		}
		sampleRateComboBox.setSelectedItem((int) format.getSampleRate());
		bitDepthComboBox.setSelectedItem(format.getSampleSizeInBits());
		channelsComboBox.setSelectedItem(format.getChannels());
		signedCheckBox
				.setSelected(format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED);
		bigEndianCheckBox.setSelected(format.isBigEndian());
		filterMixers();
	}

	public Mixer getMixer() {
		if (mixerComboBox.getItemCount() < 1) {
			return null;
		} else {
			return AudioSystem.getMixer((Mixer.Info) mixerComboBox
					.getSelectedItem());
		}
	}

	@SuppressWarnings("unchecked")
	void filterMixers() {
		AudioFormat format = getAudioFormat();
		mixerComboBox.removeAllItems();
		for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			try {
				AudioSystem.getSourceDataLine(format, mixerInfo);
				mixerComboBox.addItem(mixerInfo);
			} catch (Exception e) {}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() != mixerComboBox) {
			filterMixers();
		}
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
