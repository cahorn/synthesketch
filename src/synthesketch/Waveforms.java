package synthesketch;

public class Waveforms {

	public static final double[] SINE_WAVE = new double[44100];
	static {
		for (int i = 0; i < SINE_WAVE.length; ++i) {
			SINE_WAVE[i] = Math.sin(interpolate(0, 2 * Math.PI, (double) i
					/ SINE_WAVE.length));
		}
	}

	public static final double[] SQUARE_WAVE = new double[44100];
	static {
		for (int i = 0; i < SQUARE_WAVE.length; ++i) {
			SQUARE_WAVE[i] = Math.signum(i - SQUARE_WAVE.length / 2);
		}
	}

	public static final double[] SAW_WAVE = new double[44100];
	static {
		for (int i = 0; i < SAW_WAVE.length; ++i) {
			SAW_WAVE[i] = (double) i / SAW_WAVE.length;
		}
	}

	public static double[] resample(double[] waveform, int resampledLength) {
		return resample(waveform, resampledLength, resampledLength);
	}

	public static double[] resample(double[] waveform, int resampledLength,
			double resampledWaveformLength) {
		double[] resampled = new double[resampledLength];
		for (int i = 0; i < resampled.length; ++i) {
			double waveformPosition = i % resampledWaveformLength
					/ resampledWaveformLength;
			int lowerSample = (int) Math.floor(waveformPosition
					* waveform.length)
					% waveform.length;
			int upperSample = (int) Math.ceil(waveformPosition
					* waveform.length)
					% waveform.length;
			resampled[i] = interpolate(waveform[lowerSample],
					waveform[upperSample],
					waveformPosition - Math.floor(waveformPosition));
		}
		return resampled;
	}

	public static double interpolate(double x, double y, double d) {
		return x + d * (y - x);
	}

}
