package de.doridian.shaderbox;

import de.doridian.shaderbox.al.BASSMain;
import de.doridian.shaderbox.gl.OpenGLMain;
import de.doridian.shaderbox.gui.CodeEditor;

public class Main {
	public static void main(final String[] args) {
		Thread thread = new Thread() {
			public void run() {
				OpenGLMain.main(args);
			}
		};
		thread.setName("OpenGL");
		thread.start();

		thread = new Thread() {
			public void run() {
				BASSMain.main(args);
			}
		};
		thread.setName("BASS");
		thread.start();

		CodeEditor.main(args);
	}
}
