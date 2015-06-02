package synthesketch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class KeyboardPanel extends JPanel implements KeyListener,
		MouseListener, Receiver, Transmitter {

	public KeyboardPanel() {
		setPreferredSize(SIZE);
		addKeyListener(this);
		addMouseListener(this);
	}

	static final int MIN_MIDI = 45, MAX_MIDI = 72;

	static final Dimension SIZE = new Dimension(600, 200);

	Map<Integer, Key> keysByMidi = new TreeMap<Integer, Key>();
	{
		int whiteKeys = 0;
		for (int midiCode = MIN_MIDI; midiCode <= MAX_MIDI; ++midiCode) {
			if (isWhiteKeyCode(midiCode)) {
				++whiteKeys;
			}
		}
		double keyWidth = (double) SIZE.width / whiteKeys;
		for (int midiCode = MIN_MIDI, whiteKey = 0; midiCode <= MAX_MIDI; ++midiCode) {
			Rectangle location;
			if (isWhiteKeyCode(midiCode)) {
				location = new Rectangle((int) (keyWidth * whiteKey),
						(int) (SIZE.height * 0.6), (int) (Math.ceil(keyWidth)),
						SIZE.height);
				++whiteKey;
			} else {
				location = new Rectangle(
						(int) (keyWidth * whiteKey - (keyWidth / 4)), 0,
						(int) (keyWidth / 2), (int) (SIZE.height * 0.6));
			}
			keysByMidi.put(midiCode, new Key(midiCode, location));
		}
	}

	static boolean isWhiteKeyCode(int midiCode) {
		return !(midiCode % 12 == 1 || midiCode % 12 == 3 || midiCode % 12 == 6
				|| midiCode % 12 == 8 || midiCode % 12 == 10);
	}

	Map<Integer, Key> keysByKeyboard = new TreeMap<Integer, Key>();
	{
		keysByKeyboard.put(KeyEvent.VK_Z, keysByMidi.get(45));
		keysByKeyboard.put(KeyEvent.VK_S, keysByMidi.get(46));
		keysByKeyboard.put(KeyEvent.VK_X, keysByMidi.get(47));
		keysByKeyboard.put(KeyEvent.VK_C, keysByMidi.get(48));
		keysByKeyboard.put(KeyEvent.VK_F, keysByMidi.get(49));
		keysByKeyboard.put(KeyEvent.VK_V, keysByMidi.get(50));
		keysByKeyboard.put(KeyEvent.VK_G, keysByMidi.get(51));
		keysByKeyboard.put(KeyEvent.VK_B, keysByMidi.get(52));
		keysByKeyboard.put(KeyEvent.VK_N, keysByMidi.get(53));
		keysByKeyboard.put(KeyEvent.VK_J, keysByMidi.get(54));
		keysByKeyboard.put(KeyEvent.VK_M, keysByMidi.get(55));

		keysByKeyboard.put(KeyEvent.VK_1, keysByMidi.get(56));
		keysByKeyboard.put(KeyEvent.VK_Q, keysByMidi.get(57));
		keysByKeyboard.put(KeyEvent.VK_2, keysByMidi.get(58));
		keysByKeyboard.put(KeyEvent.VK_W, keysByMidi.get(59));
		keysByKeyboard.put(KeyEvent.VK_E, keysByMidi.get(60));
		keysByKeyboard.put(KeyEvent.VK_4, keysByMidi.get(61));
		keysByKeyboard.put(KeyEvent.VK_R, keysByMidi.get(62));
		keysByKeyboard.put(KeyEvent.VK_5, keysByMidi.get(63));
		keysByKeyboard.put(KeyEvent.VK_T, keysByMidi.get(64));
		keysByKeyboard.put(KeyEvent.VK_Y, keysByMidi.get(65));
		keysByKeyboard.put(KeyEvent.VK_7, keysByMidi.get(66));
		keysByKeyboard.put(KeyEvent.VK_U, keysByMidi.get(67));
		keysByKeyboard.put(KeyEvent.VK_8, keysByMidi.get(68));
		keysByKeyboard.put(KeyEvent.VK_I, keysByMidi.get(69));
		keysByKeyboard.put(KeyEvent.VK_9, keysByMidi.get(70));
		keysByKeyboard.put(KeyEvent.VK_O, keysByMidi.get(71));
		keysByKeyboard.put(KeyEvent.VK_P, keysByMidi.get(72));
	}

	class Key {

		public Key(int midiCode, Rectangle location) {
			this.midiCode = midiCode;
			this.location = location;
			this.playing = false;
		}

		int midiCode;
		Rectangle location;
		volatile boolean playing;

		public void noteOn() {
			if (!playing) {
				playing = true;
				if (receiver != null) {
					try {
						receiver.send(new ShortMessage(ShortMessage.NOTE_ON,
								midiCode, 75), System.currentTimeMillis());
					} catch (InvalidMidiDataException e) {}
				}
			}
		}

		public void noteOff() {
			if (playing) {
				playing = false;
				if (receiver != null) {
					try {
						receiver.send(new ShortMessage(ShortMessage.NOTE_OFF,
								midiCode, 0), System.currentTimeMillis());
					} catch (InvalidMidiDataException e) {}
				}
			}
		}

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (keysByKeyboard.containsKey(e.getKeyCode())) {
			keysByKeyboard.get(e.getKeyCode()).noteOn();
			repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (keysByKeyboard.containsKey(e.getKeyCode())) {
			keysByKeyboard.get(e.getKeyCode()).noteOff();
			repaint();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	private Key mouseKey = null;

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		for (Key key : keysByMidi.values()) {
			if (key.location.contains(e.getPoint())) {
				key.noteOn();
				mouseKey = key;
				repaint();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseKey != null && mouseKey.playing) {
			mouseKey.noteOff();
			mouseKey = null;
			repaint();
		}
	}

	@Override
	public void paint(Graphics g) {
		final Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, SIZE.width, SIZE.height);
		for (Entry<Integer, Key> pair : keysByKeyboard.entrySet()) {
			final Key key = pair.getValue();
			if (isWhiteKeyCode(key.midiCode)) {
				if (key.playing) {
					g2.setColor(Color.BLUE);
					g2.fillRect(key.location.x, 0, key.location.width,
							SIZE.height);
				}
				g2.setColor(Color.BLACK);
				g2.drawLine(key.location.x, 0, key.location.x, SIZE.height);
			}
		}
		for (Entry<Integer, Key> pair : keysByKeyboard.entrySet()) {
			final Key key = pair.getValue();
			if (!isWhiteKeyCode(key.midiCode)) {
				g2.setColor(key.playing ? Color.BLUE : Color.BLACK);
				g2.fill(key.location);
				g2.setColor(Color.BLACK);
				g2.draw(key.location);
			}
		}
	}

	@Override
	public void send(MidiMessage rawMessage, long timeStamp) {
		if (rawMessage instanceof ShortMessage) {
			ShortMessage message = (ShortMessage) rawMessage;
			if (message.getCommand() == ShortMessage.NOTE_OFF
					|| (message.getCommand() == ShortMessage.NOTE_ON && message
							.getData2() == 0)) {
				keysByMidi.get(message.getData1()).playing = false;
				repaint();
			} else if (message.getCommand() == ShortMessage.NOTE_ON) {
				keysByMidi.get(message.getData1()).playing = true;
				repaint();
			}
		}
		if (receiver != null) {
			receiver.send(rawMessage, timeStamp);
		}
	}

	@Override
	public void close() {
		setReceiver(null);
	}

	Transmitter transmitter;

	public Transmitter getTransmitter() {
		return transmitter;
	}
	
	public void setTransmitter(Transmitter transmitter) {
		if (transmitter != null) {
			transmitter.setReceiver(this);
		}
		if (this.transmitter != null) {
			this.transmitter.setReceiver(null);
		}
		this.transmitter = transmitter;
	}
	
	Receiver receiver;

	@Override
	public Receiver getReceiver() {
		return receiver;
	}

	@Override
	public void setReceiver(Receiver receiver) {
		this.receiver = receiver;
	}

}
