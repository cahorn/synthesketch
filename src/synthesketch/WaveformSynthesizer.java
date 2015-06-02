package synthesketch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class WaveformSynthesizer implements Receiver {

	Mixer mixer;
	AudioFormat format;
	int numberOfOscillators;

	public Mixer getMixer() {
		return mixer;
	}

	public AudioFormat getAudioFormat() {
		return format;
	}

	public int getNumberOfOscillators() {
		return numberOfOscillators;
	}

	public void setOutput(Mixer mixer, AudioFormat format)
			throws IllegalArgumentException, LineUnavailableException,
			UnsupportedAudioFormatException {
		setOutput(mixer, format, 8);
	}

	public void setOutput(Mixer mixer, AudioFormat format,
			int numberOfOscillators) throws IllegalArgumentException,
			LineUnavailableException, UnsupportedAudioFormatException {
		clearOutput();
		if (mixer == null || format == null) {
			throw new NullPointerException();
		}
		this.mixer = mixer;
		if (format.getEncoding() != Encoding.PCM_SIGNED
				&& format.getEncoding() != Encoding.PCM_UNSIGNED) {
			throw new UnsupportedAudioFormatException(
					"only PCM encoding is currently supported");
		}
		if (format.getSampleSizeInBits() != 8
				&& format.getSampleSizeInBits() != 16
				&& format.getSampleSizeInBits() != 32) {
			throw new UnsupportedAudioFormatException(
					"only 1, 2, and 4 byte samples are currently supported");
		}
		this.format = format;
		if (numberOfOscillators < 1) {
			throw new IllegalArgumentException(
					"cannot create synthesizer with < 1 oscillator");
		}
		this.numberOfOscillators = numberOfOscillators;
		try {
			createOscillators();
		} catch (IllegalArgumentException e) {
			clearOutput();
			throw new IllegalArgumentException(
					"mixer cannot support the selected audio format");
		} catch (LineUnavailableException e) {
			clearOutput();
			throw new LineUnavailableException(
					"mixer cannot support the required number of data lines");
		}
		if (waveform != null) {
			createSamples();
		}
	}

	public void clearOutput() {
		mixer = null;
		format = null;
		if (oscillators != null) {
			for (Oscillator o : oscillators) {
				o.active = false;
				o.line.close();
			}
			oscillators = null;
			activeOscillators = null;
			idleOscillators = null;
		}
		if (threads != null) {
			threads.shutdown();
			threads = null;
		}
	}

	List<Oscillator> oscillators;
	Map<Integer, Oscillator> activeOscillators;
	Queue<Oscillator> idleOscillators;

	ExecutorService threads;

	void createOscillators() throws IllegalArgumentException,
			LineUnavailableException {
		oscillators = new LinkedList<WaveformSynthesizer.Oscillator>();
		activeOscillators = new TreeMap<Integer, Oscillator>();
		idleOscillators = new LinkedList<Oscillator>();
		threads = Executors.newCachedThreadPool();
		for (int i = 0; i < numberOfOscillators; ++i) {
			SourceDataLine line = AudioSystem.getSourceDataLine(format,
					mixer.getMixerInfo());
			int bufferSize = (int) (MAX_SAMPLE_IN_SECONDS
					* format.getFrameRate() * format.getFrameSize());
			line.open(format, bufferSize);
			Oscillator oscillator = new Oscillator(line);
			oscillators.add(oscillator);
			idleOscillators.add(oscillator);

		}
	}

	class Oscillator implements Runnable {

		public Oscillator(SourceDataLine line) {
			this.line = line;
		}

		SourceDataLine line;
		int midiCode;
		volatile boolean active;

		@Override
		public void run() {
			byte[] sample = samples.get(midiCode);
			line.start();
			active = true;
			while (active) {
				line.write(sample, 0, sample.length);
			}
			line.drain();
			line.stop();
			idleOscillators.add(this);
		}

	}

	public void noteOn(int midiCode) {
		if (activeOscillators != null
				&& !activeOscillators.containsKey(midiCode)
				&& idleOscillators != null && !idleOscillators.isEmpty()) {
			Oscillator oscillator = idleOscillators.remove();
			oscillator.midiCode = midiCode;
			threads.execute(oscillator);
			activeOscillators.put(midiCode, oscillator);
		}
	}

	public void noteOff(int midiCode) {
		if (activeOscillators != null
				&& activeOscillators.containsKey(midiCode)) {
			Oscillator oscillator = activeOscillators.remove(midiCode);
			oscillator.active = false;
		}
	}

	double[] waveform;

	public double[] getWaveform() {
		if (waveform == null) {
			return null;
		} else {
			return Arrays.copyOf(waveform, waveform.length);
		}
	}

	public void setWaveform(double[] waveform) {
		if (waveform == null) {
			this.waveform = null;
			this.samples = null;
		} else {
			this.waveform = Arrays.copyOf(waveform, waveform.length);
			if (format != null) {
				createSamples();
			}
		}
	}

	static final int MIN_MIDI = 36, MAX_MIDI = 96;
	static final double MAX_SAMPLE_IN_SECONDS = 0.05;
	static final int VOLUME_DIVISOR = 4;

	Map<Integer, byte[]> samples;

	void createSamples() {
		samples = new TreeMap<Integer, byte[]>();
		for (int midiCode = MIN_MIDI; midiCode <= MAX_MIDI; ++midiCode) {
			createSample(midiCode);
		}
	}

	void createSample(int midiCode) {
		double frameInSeconds = 1 / format.getFrameRate();
		double periodInSecond = 1 / frequency(midiCode);
		double periodInFrames = periodInSecond / frameInSeconds;
		int sampleInPeriods = (int) Math.floor(MAX_SAMPLE_IN_SECONDS
				/ periodInSecond);
		int sampleInFrames = (int) Math.round(sampleInPeriods * periodInFrames);
		double[] sample = Waveforms.resample(waveform, sampleInFrames,
				periodInFrames);
		int frameInBytes = format.getSampleSizeInBits() / 8
				* format.getChannels();
		ByteBuffer sampleAsBytes = ByteBuffer.allocate(sample.length
				* frameInBytes);
		sampleAsBytes.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN
				: ByteOrder.LITTLE_ENDIAN);
		ShortBuffer sampleAsShorts = sampleAsBytes.asShortBuffer();
		IntBuffer sampleAsInts = sampleAsBytes.asIntBuffer();
		for (int i = 0; i < sample.length; ++i) {
			for (int j = 0; j < format.getChannels(); ++j) {
				if (format.getSampleSizeInBits() == 8) {
					byte sampleAsByte = (byte) (sample[i] * Byte.MAX_VALUE / VOLUME_DIVISOR);
					if (format.getEncoding() == Encoding.PCM_SIGNED) {
						sampleAsBytes.put(sampleAsByte);
					} else {
						sampleAsBytes
								.put((byte) (sampleAsByte + Byte.MAX_VALUE));
					}

				} else if (format.getSampleSizeInBits() == 16) {
					short sampleAsShort = (short) (sample[i] * Short.MAX_VALUE / VOLUME_DIVISOR);
					if (format.getEncoding() == Encoding.PCM_SIGNED) {
						sampleAsShorts.put(sampleAsShort);
					} else {
						sampleAsShorts
								.put((short) (sampleAsShort + Short.MAX_VALUE));
					}
				} else if (format.getSampleSizeInBits() == 32) {
					int sampleAsInt = (int) (sample[i] * Integer.MAX_VALUE / VOLUME_DIVISOR);
					if (format.getEncoding() == Encoding.PCM_SIGNED) {
						sampleAsInts.put(sampleAsInt);
					} else {
						sampleAsInts.put(sampleAsInt + Integer.MAX_VALUE);
					}
				}
			}
		}
		samples.put(midiCode, sampleAsBytes.array());

	}

	private static double frequency(int midiCode) {
		return 440.0 * Math.pow(Math.pow(2, (double) 1 / 12), midiCode - 69);
	}

	@Override
	public void send(MidiMessage rawMessage, long timeStamp) {
		if (rawMessage instanceof ShortMessage) {
			ShortMessage message = (ShortMessage) rawMessage;
			if (message.getCommand() == ShortMessage.NOTE_OFF
					|| (message.getCommand() == ShortMessage.NOTE_ON && message
							.getData2() == 0)) {
				noteOff(message.getData1());
			} else if (message.getCommand() == ShortMessage.NOTE_ON) {
				noteOn(message.getData1());

				// TODO Handle second short message byte (velocity)

			}
		}
	}

	@Override
	public void close() {
		clearOutput();
		setWaveform(null);
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

}
