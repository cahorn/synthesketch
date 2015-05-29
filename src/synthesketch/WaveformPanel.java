package synthesketch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class WaveformPanel extends JPanel implements MouseListener,
		MouseMotionListener {

	public WaveformPanel() {
		setPreferredSize(SIZE);
		addMouseListener(this);
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
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		for (ChangeListener l : listeners) {
			l.stateChanged(new ChangeEvent(this));
		}
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
