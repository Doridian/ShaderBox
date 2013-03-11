package de.doridian.shaderbox.al;

import de.doridian.shaderbox.gl.OpenGLMain;
import jouvieje.bass.Bass;
import jouvieje.bass.BassInit;
import jouvieje.bass.defines.BASS_ATTRIB;
import jouvieje.bass.defines.BASS_DATA;
import jouvieje.bass.structures.HSTREAM;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class BASSMain {
	public static File loadWaveFile = null;
	public static boolean doClearSound = false;
	public static boolean doPlay = true;
	static boolean isPlaying = false;

	static int bassChannel = -1;
	static HSTREAM bassStream = null;

	public static final int FFT_RAW_BUFFER_SIZE = 512 * 4;
	public static final int FFT_BUFFER_SIZE = 128;

	public static boolean newFFTAvail = false;

	static ByteBuffer fft1024_raw_back = BufferUtils.createByteBuffer(FFT_RAW_BUFFER_SIZE);
	//static ByteBuffer fft1024_raw = BufferUtils.createByteBuffer(FFT_RAW_BUFFER_SIZE);
	static ByteBuffer fft1024_back = BufferUtils.createByteBuffer(FFT_BUFFER_SIZE * 4);
	//static ByteBuffer fft1024 = BufferUtils.createByteBuffer(FFT_BUFFER_SIZE * 4);
	static ByteBuffer fft1024_smoothed_back = BufferUtils.createByteBuffer(FFT_BUFFER_SIZE * 4);
	public static ByteBuffer fft1024_smoothed = BufferUtils.createByteBuffer(FFT_BUFFER_SIZE * 4);

	public static boolean isChannelPlaying() {
		return isPlaying;
	}

	public static void main(String[] args) {
		try {
			BassInit.loadLibraries();
			if(!Bass.BASS_Init(-1, 44100, 0, null, null))
				throw new Exception("Could not init BASS");
			Bass.BASS_SetVolume(1);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while(OpenGLMain.isRunning) {
			if(loadWaveFile != null) {
				if(bassStream != null) {
					Bass.BASS_ChannelStop(bassChannel);
					Bass.BASS_StreamFree(bassStream);
				}
				bassStream = Bass.BASS_StreamCreateFile(false, loadWaveFile.getPath(), 0, 0, 0);
				bassChannel = bassStream.asInt();
				Bass.BASS_ChannelSetAttribute(bassChannel, BASS_ATTRIB.BASS_ATTRIB_VOL, 1.0f);
				Bass.BASS_ChannelSetAttribute(bassChannel, BASS_ATTRIB.BASS_ATTRIB_PAN, 0.0f);

				loadWaveFile = null;
			}

			if(doClearSound) {
				if(bassStream != null) {
					Bass.BASS_ChannelStop(bassChannel);
					Bass.BASS_StreamFree(bassStream);
				}
				bassStream = null;
				doPlay = false;
				isPlaying = false;
				doClearSound = false;
			}

			if(doPlay != isPlaying && bassStream != null) {
				isPlaying = doPlay;
				if(doPlay) {
					Bass.BASS_ChannelPlay(bassChannel, false);
				} else {
					Bass.BASS_ChannelPause(bassChannel);
				}
			}

			if(isPlaying) {
				fft1024_raw_back.rewind();
				Bass.BASS_ChannelGetData(bassChannel, fft1024_raw_back, BASS_DATA.BASS_DATA_FFT1024);

				FloatBuffer fftf_raw = fft1024_raw_back.asFloatBuffer();
				FloatBuffer fftf = fft1024_back.asFloatBuffer();
				FloatBuffer fftf_smooth = fft1024_smoothed_back.asFloatBuffer();
				FloatBuffer fftf_last = fft1024_smoothed.asFloatBuffer();

				int b1, b0, sc;
				double sum, y;
				b0 = 0;
				for(int i=0;i<FFT_BUFFER_SIZE;i++) {
					sum = 0.0f;
					b1 = (int)Math.pow(2.0, i * 10.0 / (FFT_BUFFER_SIZE - 1.0));

					if(b1 > 511) b1 = 511;
					if(b1 <= b0) b1 = b0 + 1;
					sc = 10 + b1 - b0;
					while(b0 < b1) {
						b0++;
						if(b0 > 511) continue;
						sum = sum + fftf_raw.get(b0);
					}
					y = Math.sqrt(sum / Math.log10(sc)) * 2.7;
					if(y < 0) y = 0;
					if(y > 1) y = 1;
					fftf.put((float)y);
					fftf_smooth.put((float)((y * 0.20) + (fftf_last.get(i) * 0.80)));
				}
				fftf.flip();
				fftf_smooth.flip();

				/*ByteBuffer tmp = fft1024_raw;
				fft1024_raw = fft1024_raw_back;
				fft1024_raw_back = tmp;

				tmp = fft1024;
				fft1024 = fft1024_back;
				fft1024_back = tmp;*/

				ByteBuffer tmp = fft1024_smoothed;
				fft1024_smoothed = fft1024_smoothed_back;
				fft1024_smoothed_back = tmp;

				newFFTAvail = true;
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
		}

		Bass.BASS_Free();
	}
}
