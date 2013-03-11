package de.doridian.shaderbox.gui;

import de.doridian.shaderbox.Utils;
import de.doridian.shaderbox.al.BASSMain;
import de.doridian.shaderbox.gl.ImageHelper;
import de.doridian.shaderbox.gl.OpenGLMain;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditor {
	private RSyntaxTextArea codeEditor;
	private JPanel rootPanel;
	private RTextScrollPane codeScrollPane;
	private JButton btnRun;
	private JCheckBox cbRunning;
	private JLabel labelStatus;
	private JSpinner spinnerFPS;
	private JButton btnStep;
	private JButton btnLoadTexture;
	private JButton btnSaveTexture;
	private JButton btnSaveShader;
	private JButton btnLoadShader;
	private JButton btnClearTexture;
	private JButton btnClearSound;
	private JButton btnLoadSound;
	private JButton btnPlayPauseSound;

	private File lastSelectedFolder = new File("shaders");

	private Gutter codeEditorGutter;

	private final ImageIcon errorIcon = Utils.createImageIcon("icons/exclamation.png", "error");
	private final ImageIcon warningIcon = Utils.createImageIcon("icons/error.png", "warning");

	//0(18) : error C0000: syntax error, unexpected identifier, expecting ',' or ')' at token "resolution"
	private static final Pattern errorParserPattern = Pattern.compile("([^:]+: )?([0-9]+)\\(([0-9]+)\\) : ([a-zA-Z]+) C[0-9a-fA-F]+: (.+)");

	private void parseShaderError(String errorRaw) {
		if(errorRaw.isEmpty()) {
			labelStatus.setForeground(Color.GREEN);
			labelStatus.setText("BUILD SUCCESS | 0 errors, 0 warnings");
			return;
		}

		System.out.println(errorRaw);

		HashMap<Integer, Boolean> isErrorMap = new HashMap<Integer, Boolean>();
		HashMap<Integer, String> errorsMap = new HashMap<Integer, String>();

		int errorC = 0;
		int warningC = 0;
		StringBuilder errorString = new StringBuilder();
		String[] errors = errorRaw.split("\n");
		for(String errorX : errors) {
			String error = errorX;
			Matcher matcher = errorParserPattern.matcher(error);
			if(matcher.matches()) {
				int numChar = Integer.parseInt(matcher.group(2));
				int numLine = Integer.parseInt(matcher.group(3)) - 1;
				String errorType = matcher.group(4).toLowerCase();
				error = matcher.group(5);

				try {
					numChar += codeEditor.getLineStartOffset(numLine);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

				try {
					String oldError = errorsMap.get(numLine);
					if(oldError == null) {
						oldError = error;
					} else {
						oldError += " | " + error;
					}
					errorsMap.put(numLine, oldError);
					if(errorType.equals("warning")) {
						warningC++;
					} else if(errorType.equals("error")) {
						isErrorMap.put(numLine, true);
						errorC++;
					}
				} catch (Exception e) { }
			}

			if(errorString.length() > 0)
				errorString.append(", ");
			errorString.append(error);
		}

		for(Map.Entry<Integer, String> errorEntry : errorsMap.entrySet()) {
			int line = errorEntry.getKey();
			String error = errorEntry.getValue();
			if(error.length() > 128) {
				error = error.substring(0, 128) + "...";
			}
			try {
				if(Boolean.TRUE.equals(isErrorMap.get(line))) {
					codeEditorGutter.addLineTrackingIcon(line, errorIcon, error);
				} else {
					codeEditorGutter.addLineTrackingIcon(line, warningIcon, error);
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		if(errorC > 0) {
			labelStatus.setForeground(Color.RED);
			labelStatus.setText("BUILD FAILURE | " + errorC + " errors, " + warningC + " warnings");
		} else if(warningC > 0) {
			labelStatus.setForeground(Color.ORANGE);
			labelStatus.setText("BUILD SUCCESS | 0 errors, " + warningC + " warnings");
		}
	}

	public CodeEditor() {
		lastSelectedFolder.mkdirs();

		((RSyntaxDocument)codeEditor.getDocument()).setTokenMakerFactory(new GLSLTokenMakerFactory());
		codeEditor.setSyntaxEditingStyle("text/glsl");

		codeEditor.setCodeFoldingEnabled(true);
		codeEditor.setAntiAliasingEnabled(true);
		codeScrollPane.setLineNumbersEnabled(true);
		codeScrollPane.setFoldIndicatorEnabled(true);
		codeScrollPane.setIconRowHeaderEnabled(true);

		codeEditorGutter = codeScrollPane.getGutter();

		try {
			codeEditor.setText(Utils.readFileAsString(new File("autosave.fsh")));
		} catch (Exception e) {
			codeEditor.setText("");
		}
		refreshCode();

		cbRunning.setSelected(OpenGLMain.useCurrentProgram);
		spinnerFPS.setValue(OpenGLMain.getFPSLimit());

		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					FileWriter fileWriter = new FileWriter("autosave.fsh");
					fileWriter.write(codeEditor.getText());
					fileWriter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				refreshCode();
			}
		});

		cbRunning.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OpenGLMain.useCurrentProgram = cbRunning.isSelected();
			}
		});

		spinnerFPS.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OpenGLMain.setFPSLimit((Integer)spinnerFPS.getValue());
			}
		});

		btnStep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OpenGLMain.useCurrentProgramSingleSteps++;
			}
		});

		//TEXTURE CONTROLS
		btnLoadTexture.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				FileFilter fileFilter = new FileNameExtensionFilter("Image file (*.png;*.jpg;*.jpeg;*.bmp)", "png", "jpg", "jpeg", "bmp");
				jFileChooser.addChoosableFileFilter(fileFilter);
				jFileChooser.setFileFilter(fileFilter);
				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					try {
						lastSelectedFolder = jFileChooser.getSelectedFile().getParentFile();
						OpenGLMain.loadTexture = jFileChooser.getSelectedFile();
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}
				} else {
					return;
				}
				OpenGLMain.doReinitialize = true;
			}
		});

		btnClearTexture.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OpenGLMain.loadTexture = null;
				OpenGLMain.doReinitialize = true;
			}
		});

		btnSaveTexture.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = null;
				JFileChooser jFileChooser = new JFileChooser();
				FileFilter fileFilter = new FileNameExtensionFilter("Texture file (*.png)", "png");
				jFileChooser.addChoosableFileFilter(fileFilter);
				jFileChooser.setFileFilter(fileFilter);
				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showSaveDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					file = jFileChooser.getSelectedFile();
				}

				if(file == null) {
					return;
				}

				lastSelectedFolder = file.getParentFile();

				OpenGLMain.DumpContent data = OpenGLMain.dumpRenderedContent();
				BufferedImage image = ImageHelper.toImage(data.byteBuffer, data.width, data.height);

				try {
					ImageIO.write(image, "PNG", file);
				} catch (IOException ex) { ex.printStackTrace(); }
			}
		});

		//SHADER CONTROLS
		btnLoadShader.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				FileFilter fileFilter = new FileNameExtensionFilter("Shader file (*.fsh, *.shader)", "fsh", "shader");
				jFileChooser.addChoosableFileFilter(fileFilter);
				jFileChooser.setFileFilter(fileFilter);
				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					try {
						lastSelectedFolder = jFileChooser.getSelectedFile().getParentFile();
						codeEditor.setText(Utils.readFileAsString(jFileChooser.getSelectedFile()));
						refreshCode();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		btnSaveShader.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = null;
				JFileChooser jFileChooser = new JFileChooser();
				FileFilter fileFilter = new FileNameExtensionFilter("Shader file (*.fsh, *.shader)", "fsh", "shader");
				jFileChooser.addChoosableFileFilter(fileFilter);
				jFileChooser.setFileFilter(fileFilter);
				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showSaveDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					file = jFileChooser.getSelectedFile();
				}

				if(file == null) {
					return;
				}

				lastSelectedFolder = file.getParentFile();

				try {
					FileWriter fileWriter = new FileWriter(file);
					fileWriter.write(codeEditor.getText());
					fileWriter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		//SOUND CONTROLS
		btnLoadSound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				FileFilter fileFilter = new FileNameExtensionFilter("Sound file (*.ogg, *.mp3, *.wav)", "ogg", "mp3", "wav");
				jFileChooser.addChoosableFileFilter(fileFilter);
				jFileChooser.setFileFilter(fileFilter);
				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					try {
						lastSelectedFolder = jFileChooser.getSelectedFile().getParentFile();
						BASSMain.loadWaveFile = jFileChooser.getSelectedFile();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		btnClearSound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BASSMain.doClearSound = true;
			}
		});

		btnPlayPauseSound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BASSMain.doPlay = !BASSMain.doPlay;
			}
		});
	}

	private void refreshCode() {
		codeEditorGutter.removeAllTrackingIcons();
		labelStatus.setForeground(Color.YELLOW);
		labelStatus.setText("Compiling");
		OpenGLMain.newCurrentProgramFragment = codeEditor.getText();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		final JFrame frame = new JFrame("CodeEditor");
		final CodeEditor codeEditor = new CodeEditor();
		frame.setContentPane(codeEditor.rootPanel);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				OpenGLMain.shouldRun = false;
			}
		});

		Thread thread = new Thread() {
			public void run() {
				while(OpenGLMain.isRunning) {
					try {
						Thread.sleep(100);
					} catch (Exception e) { }

					if(OpenGLMain.newCurrentProgramFragment == null && OpenGLMain.lastShaderError != null) {
						codeEditor.parseShaderError(OpenGLMain.lastShaderError);
						OpenGLMain.lastShaderError = null;
					}
				}

				frame.dispose();
			}
		};
		thread.setName("OpenGLFrameDisposePoll");
		thread.start();
	}
}
