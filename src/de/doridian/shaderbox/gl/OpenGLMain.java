package de.doridian.shaderbox.gl;

import de.doridian.shaderbox.al.BASSMain;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class OpenGLMain {
	static int screenWidth = 1024;
	static int screenHeight = 768;
	static int backgroundWidth = 0;
	static int backgroundHeight = 0;
	static ByteBuffer backgroundImageBuffer = null;
	static int backgroundImageRCount = 0;


	static int backgroundImageWidth = 0;
	static int backgroundImageHeight = 0;
	static ByteBuffer backgroundImageImageBuffer = null;

	static int renderAreaWidth = 1024;
	static int renderAreaHeight = 768;

	public static boolean doReinitialize = true;
	public static File loadTexture = new File("autosave.png");

	public static boolean isRunning = true;
	public static boolean shouldRun = true;

	static int fpsLimit = 0;
	static long minFrameTime = 0;

	public static String newCurrentProgramFragment = null;
	public static String newCurrentProgramVertex = null;
	public static String newCurrentProgramGeometry = null;
	public static boolean compileNewProgramNow = false;

	public static boolean useCurrentProgram = true;
	public static int useCurrentProgramSingleSteps = 0;
	public static String lastShaderError = null;

	public static void setFPSLimit(final int fps) {
		if(fps <= 0) {
			fpsLimit = 0;
			minFrameTime = 0;
		} else {
			fpsLimit = fps;
			minFrameTime = 1000000000L / fps;
		}
	}

	public static int getFPSLimit() {
		return fpsLimit;
	}

	private static void reinitialize() {
		if(loadTexture != null && loadTexture.exists()) {
			try {
				BufferedImage backgroundImage = ImageIO.read(loadTexture);
				renderAreaWidth = backgroundImage.getWidth();
				renderAreaHeight = backgroundImage.getHeight();
				backgroundWidth = renderAreaWidth;
				backgroundHeight = renderAreaHeight;

				int[] pixels = new int[backgroundImage.getWidth() * backgroundImage.getHeight()];
				backgroundImage.getRGB(0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), pixels, 0, backgroundImage.getWidth());

				backgroundImageBuffer = BufferUtils.createByteBuffer(backgroundImage.getWidth() * backgroundImage.getHeight() * 4); //4 for RGBA, 3 for RGB

				for(int y = backgroundImage.getHeight() - 1; y >= 0; y--){
					for(int x = 0; x < backgroundImage.getWidth(); x++){
						int pixel = pixels[y * backgroundImage.getWidth() + x];
						backgroundImageBuffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
						backgroundImageBuffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
						backgroundImageBuffer.put((byte) (pixel & 0xFF));               // Blue component
						backgroundImageBuffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
					}
				}

				backgroundImageBuffer.flip(); //FOR THE LOVE OF GOD DO NOT FORGET THIS

				backgroundImageImageBuffer = backgroundImageBuffer;
				backgroundImageHeight = backgroundHeight;
				backgroundImageWidth = backgroundWidth;

				try {
					ImageIO.write(backgroundImage, "PNG", new File("autosave.png"));
				} catch (IOException ex) { ex.printStackTrace(); }

				refreshBackgroundImage();
			} catch(Exception e) {
				e.printStackTrace();
				backgroundImageBuffer = null;
				renderAreaWidth = screenWidth;
				renderAreaHeight = screenHeight;
				refreshBackgroundImage();
				backgroundWidth = 0;
				backgroundHeight = 0;
				backgroundImageWidth = 0;
				backgroundImageHeight = 0;
				backgroundImageImageBuffer = null;
				try {
					new File("autosave.png").delete();
				} catch (Exception ex) { }
			}
		} else {
			backgroundImageBuffer = null;
			renderAreaWidth = screenWidth;
			renderAreaHeight = screenHeight;
			refreshBackgroundImage();
			backgroundWidth = 0;
			backgroundHeight = 0;
			backgroundImageWidth = 0;
			backgroundImageHeight = 0;
			backgroundImageImageBuffer = null;
			try {
				new File("autosave.png").delete();
			} catch (Exception ex) { }
		}
	}

	public static void main(String[] args) {
		setFPSLimit(60);

		isRunning = true;
		shouldRun = true;

		reinitialize();

		try {
			Display.setDisplayMode(new DisplayMode(screenWidth, screenHeight));
			Display.create();
			Display.setTitle("ShaderBox");
			Display.setResizable(true);
		} catch(LWJGLException e) {
			e.printStackTrace();
		}

		initDisplay();

		screenProgram = new ScreenShader();
		currentProgram = new MainShader(ShaderProgram.VSH_DONOTHING, null, MainShader.FSH_DONOTHING);

		inputThread.start();

		long lastFrameSampleTime = System.nanoTime();
		long thisFrameTime;
		long frameCount = 0;
		long fps; long frameTimeTmpDiff;

		long lastShadedFrameTime = 0;

		while(shouldRun && !Display.isCloseRequested()) {
			if(compileNewProgramNow) {
				try {
					lastShaderError = "";
					MainShader newProgram = new MainShader(newCurrentProgramVertex, newCurrentProgramGeometry, newCurrentProgramFragment);
					currentProgram.delete();
					currentProgram = newProgram;
				} catch (Exception e) {
					lastShaderError = e.getMessage();
				}
				newCurrentProgramFragment = null;
				newCurrentProgramGeometry = null;
				newCurrentProgramVertex = null;
				compileNewProgramNow = false;
			}

			if(doReinitialize) {
				reinitialize();
				initDisplay();
				doReinitialize = false;
			}

			if(Display.wasResized()) {
				initDisplay();
			}

			thisFrameTime = System.nanoTime();
			frameTimeTmpDiff = thisFrameTime - lastFrameSampleTime;
			frameCount++;
			if(frameTimeTmpDiff > 1000000000) {
				fps = (long)((1000000000.0f / frameTimeTmpDiff) * frameCount);
				frameCount = 0;
				lastFrameSampleTime = thisFrameTime;
				Display.setTitle("ShaderBox (" + fps + " FPS)");
			}

			if(fpsLimit > 0 && fpsLimit < 60) {
				frameTimeTmpDiff = thisFrameTime - lastShadedFrameTime;
				if(frameTimeTmpDiff > minFrameTime) {
					draw(true);
					lastShadedFrameTime = thisFrameTime;
				} else {
					draw(false);
				}
			} else {
				draw(true);
			}
			Display.update();
			if(fpsLimit > 0) {
				if(fpsLimit < 60)
					Display.sync(60);
				else
					Display.sync(fpsLimit);
			}

			if(backgroundImageRCount == 0 && dumpNextFrame) {
				dumpNextFrame = false;
				dumpTarget = __dumpRenderedContent();
			}
		}

		try {
			runInputThread = false;
			inputThread.join();
		} catch (InterruptedException e) { }

		Display.destroy();

		isRunning = false;
	}

	public static class DumpContent {
		public final ByteBuffer byteBuffer;
		public final int width;
		public final int height;

		private DumpContent(ByteBuffer byteBuffer, int width, int height) {
			this.byteBuffer = byteBuffer;
			this.width = width;
			this.height = height;
		}
	}

	private static boolean dumpNextFrame = false;
	private static DumpContent dumpTarget = null;

	public static synchronized DumpContent dumpRenderedContent() {
		dumpNextFrame = true;
		while(dumpTarget == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
		}
		DumpContent buffer = dumpTarget;
		dumpTarget = null;
		return buffer;
	}

	private static DumpContent __dumpRenderedContent() {
		return new DumpContent(__dumpRenderedContentToByteBuffer(), renderAreaWidth, renderAreaHeight);
	}

	private static ByteBuffer __dumpRenderedContentToByteBuffer() {
		ByteBuffer buffer = BufferUtils.createByteBuffer(renderAreaWidth * renderAreaHeight * 4);
		GL20.glUseProgram(0);
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, backTarget.texture);
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		return buffer;
	}

	private static void initDisplay() {
		if(fftBufferSmooth != 0)
			GL15.glDeleteBuffers(fftBufferSmooth);
		fftBufferSmooth = GL15.glGenBuffers();
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, fftBufferSmooth);
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, BASSMain.FFT_BUFFER_SIZE * 4, GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

		if(backTarget != null && backgroundImageRCount == 0) {
			backgroundImageBuffer = __dumpRenderedContentToByteBuffer();

			backgroundWidth = renderAreaWidth;
			backgroundHeight = renderAreaHeight;
		}

		screenWidth = Display.getWidth();
		screenHeight = Display.getHeight();

		if(backgroundImageBuffer != null) {
			renderAreaWidth = Math.max(backgroundWidth, screenWidth);
			renderAreaHeight = Math.max(backgroundHeight, screenHeight);
		} else {
			renderAreaWidth = screenWidth;
			renderAreaHeight = screenHeight;
		}

		viewportX = 0;
		viewportY = 0;
		zoomFactor = 1;
		resX = renderAreaWidth;
		resY = renderAreaHeight;
		startTime = System.currentTimeMillis();
		GL11.glViewport(0, 0, renderAreaWidth, renderAreaHeight);
		createRenderTargets();

		refreshBackgroundImage();
	}

	private static void refreshBackgroundImage() {
		backgroundImageRCount = 2;
	}

	static boolean runInputThread = true;

	private static Thread inputThread = new Thread() {
		public void run() {
			while(runInputThread) {
				parseInput();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }
			}
		}
	};

	private static void createRenderTargets() {
		if(frontTarget != null) frontTarget.delete();
		if(backTarget != null) backTarget.delete();
		frontTarget = new  RenderTarget(renderAreaWidth, renderAreaHeight);
		backTarget = new RenderTarget(renderAreaWidth, renderAreaHeight);
	}

	static RenderTarget frontTarget, backTarget;
	static int originalTexture = 0;

	static MainShader currentProgram = null;
	static ScreenShader screenProgram = null;
	static int buffer;
	static long startTime = 0;

	static float mouseX = 0;
	static float mouseY = 0;
	static float mouseConvX = 0;
	static float mouseConvY = 0;
	static float mouseLeftDown = 0;

	static int viewportX = 0;
	static int viewportY = 0;
	static int resX = 0;
	static int resY = 0;
	static float zoomFactor = 1;

	private static void parseInput() {
		float newMouseX = Mouse.getX();
		float newMouseY = Mouse.getY();

		if(Mouse.isButtonDown(1)) {
			float mouseDX = newMouseX - mouseX;
			float mouseDY = newMouseY - mouseY;
			if(mouseDX != 0 || mouseDY != 0) {
				viewportX -= mouseDX;
				viewportY -= mouseDY;
			}
		}

		float wheelDiff = Mouse.getDWheel();
		if(wheelDiff != 0) {
			viewportX -= resX / 2;
			viewportY -= resY / 2;
			zoomFactor += (wheelDiff / 1000.0f);
			resX = (int)(renderAreaWidth * zoomFactor);
			resY = (int)(renderAreaHeight * zoomFactor);
			viewportX += resX / 2;
			viewportY += resY / 2;
		}

		while(Keyboard.next()) {
			if(!Keyboard.getEventKeyState()) continue;
			if(Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
				viewportX = 0;
				viewportY = 0;
				resX = renderAreaWidth;
				resY = renderAreaHeight;
				zoomFactor = 1;
			} else if(Keyboard.getEventKey() == Keyboard.KEY_D) {
				doReinitialize = true;
			}
		}

		mouseX = newMouseX;
		mouseY = newMouseY;

		mouseConvX = (mouseX + viewportX) / resX;
		mouseConvY = (mouseY + viewportY) / resY;

		mouseLeftDown = Mouse.isButtonDown(0) ? 1.0f : 0.0f;
	}

	private static int fftBufferSmooth = 0;

	private static void draw(boolean mayShade) {
		if((mayShade && (useCurrentProgramSingleSteps > 0 || useCurrentProgram)) || backgroundImageRCount > 0) {
			RenderTarget tmp = frontTarget;
			frontTarget = backTarget;
			backTarget = tmp;

			if(useCurrentProgramSingleSteps > 0)
				useCurrentProgramSingleSteps--;

			GL20.glUseProgram(currentProgram.getProgram());

			GL20.glUniform3f(currentProgram.uniformMouse, mouseConvX, mouseConvY, mouseLeftDown);
			GL20.glUniform2f(currentProgram.uniformResolution, renderAreaWidth, renderAreaHeight);
			GL20.glUniform1i(currentProgram.uniformBackbuffer, 0);
			GL20.glUniform1i(currentProgram.uniformOriginal, 11);
			GL20.glUniform1f(currentProgram.uniformTime, (System.currentTimeMillis() - startTime) / 1000.0f);

			//
			if(BASSMain.newFFTAvail) {
				GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, fftBufferSmooth);
				GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, BASSMain.fft1024_smoothed);
				GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

				GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 1, fftBufferSmooth);

				int blockFFT = GL31.glGetUniformBlockIndex(currentProgram.getProgram(), "fftdata");
				GL31.glUniformBlockBinding(currentProgram.getProgram(), blockFFT, 1);

				BASSMain.newFFTAvail = false;
			}
			//

			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, backTarget.texture);

			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frontTarget.framebuffer);

			if(backgroundImageRCount > 0) {
				if(backgroundImageRCount == 2) {
					if(originalTexture != 0) GL11.glDeleteTextures(originalTexture);
					originalTexture = GL11.glGenTextures();

					GL13.glActiveTexture(GL13.GL_TEXTURE11);

					GL11.glBindTexture(GL11.GL_TEXTURE_2D, originalTexture);
					GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, renderAreaWidth, renderAreaHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);

					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

					if(backgroundImageImageBuffer != null) {
						GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, backgroundImageWidth, backgroundImageHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, backgroundImageImageBuffer);
					}

					GL13.glActiveTexture(GL13.GL_TEXTURE0);
				}
				backgroundImageRCount--;
				if(backgroundImageBuffer != null) {
					GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, backgroundWidth, backgroundHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, backgroundImageBuffer);
				}
			}

			// Render custom shader to front buffer
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
		}

		// Set uniforms for screen shader

		GL20.glUseProgram(screenProgram.getProgram());

		GL20.glUniform2f(screenProgram.uniformResolution, resX, resY);
		GL20.glUniform1i(screenProgram.uniformTexture, 1);
		GL20.glUniform2f(screenProgram.uniformOffset, viewportX, viewportY);

		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frontTarget.texture);

		// Render front buffer to screen
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
	}
}