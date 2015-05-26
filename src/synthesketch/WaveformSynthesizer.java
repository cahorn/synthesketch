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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class WaveformSynthesizer implements Receiver {

	Mixer mixer;
	AudioFormat format;

	public void setOutput(Mixer mixer, AudioFormat format)
			throws LineUnavailableException, UnsupportedAudioFormatException {
		this.mixer = mixer;
		this.format = format;
		createOscillators();
		createSamples();
	}

	int numberOfOscillators = 8;

	public void setNumberOfOscillators(int numberOfOscillators)
			throws LineUnavailableException {
		this.numberOfOscillators = numberOfOscillators;
		createOscillators();
	}

	public int getNumberOfOscillators() {
		return numberOfOscillators;
	}

	List<Oscillator> oscillators;
	Map<Integer, Oscillator> activeOscillators;
	Queue<Oscillator> idleOscillators;

	ExecutorService threads;

	void createOscillators() throws LineUnavailableException {
		oscillators = new LinkedList<WaveformSynthesizer.Oscillator>();
		activeOscillators = new TreeMap<Integer, Oscillator>();
		idleOscillators = new LinkedList<Oscillator>();
		threads = Executors.newCachedThreadPool();
		for (int i = 0; i < numberOfOscillators; ++i) {
			try {
				SourceDataLine line = AudioSystem.getSourceDataLine(format,
						mixer.getMixerInfo());
				int bufferSize = (int) (MAX_SAMPLE_IN_SECONDS
						* format.getFrameRate() * format.getFrameSize());
				line.open(format, bufferSize);
				Oscillator oscillator = new Oscillator(line);
				oscillators.add(oscillator);
				idleOscillators.add(oscillator);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						"mixer cannot support the selected audio format");
			} catch (LineUnavailableException e) {
				throw new LineUnavailableException(
						"mixer cannot support the required number of data lines");
			}
		}
	}

	public void noteOn(int midiCode) {
		if (!activeOscillators.containsKey(midiCode)
				&& !idleOscillators.isEmpty()) {
			Oscillator oscillator = idleOscillators.remove();
			oscillator.setMidiCode(midiCode);
			threads.execute(oscillator);
			activeOscillators.put(midiCode, oscillator);
		}
	}

	public void noteOff(int midiCode) {
		if (activeOscillators.containsKey(midiCode)) {
			Oscillator oscillator = activeOscillators.remove(midiCode);
			oscillator.setActive(false);
		}
	}

	class Oscillator implements Runnable {

		public Oscillator(SourceDataLine line) {
			this.line = line;
		}

		SourceDataLine line;

		int midiCode;

		public void setMidiCode(int midiCode) {
			this.midiCode = midiCode;
		}

		volatile boolean active;

		public void setActive(boolean active) {
			this.active = active;
		}

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

	public static final double[] SINE_WAVE = new double[44100];
	{
		for (int i = 0; i < SINE_WAVE.length; ++i) {
			SINE_WAVE[i] = Math.sin(interpolate(0, 2 * Math.PI, (double) i
					/ SINE_WAVE.length));
		}
	}

	public static final double[] SQUARE_WAVE = new double[44100];
	{
		for (int i = 0; i < SQUARE_WAVE.length; ++i) {
			SQUARE_WAVE[i] = Math.signum(i - SQUARE_WAVE.length / 2);
		}
	}

	public static final double[] SAW_WAVE = new double[44100];
	{
		for (int i = 0; i < SAW_WAVE.length; ++i) {
			SAW_WAVE[i] = (double) i / SAW_WAVE.length;
		}
	}

	double[] waveform;

	public void setWaveform(double[] waveform)
			throws UnsupportedAudioFormatException {
		this.waveform = waveform;
		createSamples();
	}

	public double[] getWaveform() {
		return Arrays.copyOf(waveform, waveform.length);
	}

	static final int MIN_MIDI = 36, MAX_MIDI = 96;
	static final double MAX_SAMPLE_IN_SECONDS = 0.05;
	static final int VOLUME_DIVISOR = 4;

	Map<Integer, byte[]> samples;

	void createSamples() throws UnsupportedAudioFormatException {
		if (waveform != null) {
			samples = new TreeMap<Integer, byte[]>();
			for (int midiCode = MIN_MIDI; midiCode <= MAX_MIDI; ++midiCode) {
				createSample(midiCode);
			}
		}
	}

	void createSample(int midiCode) throws UnsupportedAudioFormatException {
		double frameInSeconds = 1 / format.getFrameRate();
		double periodInSecond = 1 / frequency(midiCode);
		double periodInFrames = periodInSecond / frameInSeconds;
		int sampleInPeriods = (int) Math.floor(MAX_SAMPLE_IN_SECONDS
				/ periodInSecond);
		int sampleInFrames = (int) Math.round(sampleInPeriods * periodInFrames);
		double[] sample = new double[sampleInFrames];
		for (int i = 0; i < sample.length; ++i) {
			double waveformPosition = i % periodInFrames / periodInFrames;
			int lowerSample = (int) Math.floor(waveformPosition
					* waveform.length)
					% waveform.length;
			int upperSample = (int) Math.ceil(waveformPosition
					* waveform.length)
					% waveform.length;
			sample[i] = interpolate(waveform[lowerSample],
					waveform[upperSample],
					waveformPosition - Math.floor(waveformPosition));
		}
		int frameInBytes = format.getSampleSizeInBits() / 8
				* format.getChannels();
		ByteBuffer sampleAsBytes = ByteBuffer.allocate(sample.length
				* frameInBytes);
		sampleAsBytes.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN
				: ByteOrder.LITTLE_ENDIAN);
		ShortBuffer sampleAsShorts = sampleAsBytes.asShortBuffer();
		IntBuffer sampleAsInts = sampleAsBytes.asIntBuffer();
		if (format.getEncoding() != Encoding.PCM_SIGNED
				&& format.getEncoding() != Encoding.PCM_UNSIGNED) {
			throw new UnsupportedAudioFormatException(
					"only PCM encoding is currently supported");
		}
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
				} else {
					throw new UnsupportedAudioFormatException(
							"only 1, 2, and 4 byte samples are currently supported");
				}
			}
		}
		samples.put(midiCode, sampleAsBytes.array());

	}

	double interpolate(double x, double y, double d) {
		return x + d * (y - x);
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
		for (Oscillator o : oscillators) {
			o.setActive(false);
		}
		threads.shutdown();
	}

}
