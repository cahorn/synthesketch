package synthesketch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class WaveformPanel extends JPanel implements MouseMotionListener {

	public WaveformPanel() {
		setPreferredSize(SIZE);
		addMouseMotionListener(this);
	}

	double[] waveform;
	double sampleWidth, sampleHeight;

	public void setWaveform(double[] waveform) {
		int maxLength = (SIZE.width - 2 * BORDER.width) / 2;
		if (waveform.length > maxLength) {
			this.waveform = Waveforms.resample(waveform, maxLength);
		} else {
			this.waveform = Arrays.copyOf(waveform, waveform.length);
		}
		sampleWidth = (double) (SIZE.width - 2 * BORDER.width)
				/ this.waveform.length;
		sampleHeight = SIZE.height - 2 * BORDER.height;
		repaint();
	}

	public double[] getWaveform() {
		return waveform;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (waveform != null && BORDER.width <= e.getX()
				&& e.getX() < SIZE.width - BORDER.width
				&& BORDER.height <= e.getY()
				&& e.getY() < SIZE.height - BORDER.height) {
			int start = (int) ((e.getX() - BORDER.width) / sampleWidth);
			for (int i = start; i < start + 1 / sampleWidth; ++i) {
				waveform[i] = -((e.getY() - BORDER.height) - sampleHeight / 2)
						/ (0.5 * sampleHeight);
			}
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	static final Dimension SIZE = new Dimension(600, 400);
	static final Dimension BORDER = new Dimension(100, 25);

	@Override
	public void paint(Graphics g) {
		((Graphics2D) g).setBackground(Color.BLACK);
		g.clearRect(0, 0, SIZE.width, SIZE.height);
		if (waveform != null) {
			int borderSamples = (int) (Math.ceil(BORDER.width / sampleWidth));
			g.setColor(Color.WHITE);
			for (int i = -borderSamples; i < waveform.length + borderSamples; ++i) {
				int x1 = BORDER.width + (int) (i * sampleWidth);
				int y1 = BORDER.height
						+ (int) (sampleHeight / 2 - waveform[(i + waveform.length)
								% waveform.length]
								* sampleHeight / 2);
				int x2 = BORDER.width + (int) ((i + 1) * sampleWidth);
				int y2 = BORDER.height
						+ (int) (sampleHeight / 2 - waveform[(i + 1 + waveform.length)
								% waveform.length]
								* sampleHeight / 2);
				g.drawLine(x1, y1, x2, y2);
			}
			g.setColor(Color.BLUE);
			g.drawLine(BORDER.width, SIZE.height / 2 - SIZE.height / 10,
					BORDER.width, SIZE.height / 2 + SIZE.height / 10);
			g.drawLine(SIZE.width - BORDER.width, SIZE.height / 2 - SIZE.height
					/ 10, SIZE.width - BORDER.width, SIZE.height / 2
					+ SIZE.height / 10);
			g.setColor(Color.WHITE);
			g.drawString("Drag mouse to edit waveform.", 10, 20);
		}
	}

}
