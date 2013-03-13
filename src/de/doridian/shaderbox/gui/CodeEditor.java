package de.doridian.shaderbox.gui;

import de.doridian.shaderbox.Utils;
import de.doridian.shaderbox.al.BASSMain;
import de.doridian.shaderbox.gl.ImageHelper;
import de.doridian.shaderbox.gl.MainShader;
import de.doridian.shaderbox.gl.OpenGLMain;
import de.doridian.shaderbox.gl.ShaderProgram;
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
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditor {
	private RSyntaxTextArea GSHcodeEditor;
	private JPanel rootPanel;
	private RTextScrollPane GSHcodeScrollPane;
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
	private JTabbedPane codeEditorTabs;
	private RTextScrollPane FSHcodeScrollPane;
	private RTextScrollPane VSHcodeScrollPane;
	private RSyntaxTextArea FSHcodeEditor;
	private RSyntaxTextArea VSHcodeEditor;

	private File lastSelectedFolder = new File("shaders");

	private Gutter GSHcodeEditorGutter;
	private Gutter FSHcodeEditorGutter;
	private Gutter VSHcodeEditorGutter;

	private final ImageIcon errorIcon = Utils.createImageIcon("icons/exclamation.png", "error");
	private final ImageIcon warningIcon = Utils.createImageIcon("icons/error.png", "warning");

	//0(18) : error C0000: syntax error, unexpected identifier, expecting ',' or ')' at token "resolution"
	private static final Pattern errorParserPattern = Pattern.compile("([^:]+: )?([a-zA-Z]+):([0-9]+)\\(([0-9]+)\\) : ([a-zA-Z]+) C[0-9a-fA-F]+: (.+)");

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
				//int numChar = Integer.parseInt(matcher.group(3));
				int numLine = Integer.parseInt(matcher.group(4)) - 1;
				String errorType = matcher.group(5).toLowerCase();
				error = matcher.group(6);

				final String eWhere = matcher.group(2).toLowerCase();
				if(eWhere.equals("gsh")) {
					numLine += 1000000;
				} else if(eWhere.equals("fsh")) {
					numLine += 2000000;
				} else if(eWhere.equals("vsh")) {
					//NOTHING!
				} else {
					numLine += 3000000;
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
				Gutter curGutter = VSHcodeEditorGutter;
				if(line >= 3000000) {
					continue;
				} else if(line >= 2000000) {
					curGutter = FSHcodeEditorGutter;
				} else if(line >= 1000000) {
					curGutter = GSHcodeEditorGutter;
				}

				boolean isError = Boolean.TRUE.equals(isErrorMap.get(line));

				line %= 1000000;

				if(isError) {
					curGutter.addLineTrackingIcon(line, errorIcon, error);
				} else {
					curGutter.addLineTrackingIcon(line, warningIcon, error);
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
		} else {
			labelStatus.setForeground(Color.GREEN);
			labelStatus.setText("BUILD SUCCESS | 0 errors, 0 warnings");
		}
	}

	private Gutter initializeEditor(RSyntaxTextArea rSyntaxTextArea, RTextScrollPane rTextScrollPane, String shType, String defaultContent) {
		((RSyntaxDocument) rSyntaxTextArea.getDocument()).setTokenMakerFactory(new GLSLTokenMakerFactory());
		rSyntaxTextArea.setSyntaxEditingStyle("text/glsl");
		rSyntaxTextArea.setCodeFoldingEnabled(true);
		rSyntaxTextArea.setAntiAliasingEnabled(true);
		rTextScrollPane.setLineNumbersEnabled(true);
		rTextScrollPane.setFoldIndicatorEnabled(true);
		rTextScrollPane.setIconRowHeaderEnabled(true);

		try {
			rSyntaxTextArea.setText(Utils.readFileAsString(new File("autosave." + shType)));
		} catch (Exception e) {
			rSyntaxTextArea.setText(defaultContent);
		}

		return rTextScrollPane.getGutter();
	}

	public CodeEditor() {
		lastSelectedFolder.mkdirs();

		GSHcodeEditorGutter = initializeEditor(GSHcodeEditor, GSHcodeScrollPane, "gsh", "");
		FSHcodeEditorGutter = initializeEditor(FSHcodeEditor, FSHcodeScrollPane, "fsh", MainShader.FSH_DONOTHING);
		VSHcodeEditorGutter = initializeEditor(VSHcodeEditor, VSHcodeScrollPane, "vsh", ShaderProgram.VSH_DONOTHING);

		refreshCode();

		cbRunning.setSelected(OpenGLMain.useCurrentProgram);
		spinnerFPS.setValue(OpenGLMain.getFPSLimit());

		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					FileWriter fileWriter = new FileWriter("autosave.fsh");
					fileWriter.write(FSHcodeEditor.getText());
					fileWriter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try {
					FileWriter fileWriter = new FileWriter("autosave.gsh");
					fileWriter.write(GSHcodeEditor.getText());
					fileWriter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try {
					FileWriter fileWriter = new FileWriter("autosave.vsh");
					fileWriter.write(VSHcodeEditor.getText());
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

				FileFilter fragmentFileFilter = new FileNameExtensionFilter("Fragment shader file (*.fsh)", "fsh");
				jFileChooser.addChoosableFileFilter(fragmentFileFilter);
				FileFilter geometryFileFilter = new FileNameExtensionFilter("Geometry shader file (*.gsh)", "gsh");
				jFileChooser.addChoosableFileFilter(geometryFileFilter);
				FileFilter vertexFileFilter = new FileNameExtensionFilter("Vertex shader file (*.vsh)", "vsh");
				jFileChooser.addChoosableFileFilter(vertexFileFilter);
				FileFilter shaderboxFileFilter = new FileNameExtensionFilter("ShaderBox file (*.sbox)", "sbox");
				jFileChooser.addChoosableFileFilter(shaderboxFileFilter);

				jFileChooser.setFileFilter(shaderboxFileFilter);
				jFileChooser.setAcceptAllFileFilterUsed(false);

				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					try {
						final File file = jFileChooser.getSelectedFile();
						lastSelectedFolder = file.getParentFile();
						switch (getShaderTypeFromFile(file)) {
							case VERTEX:
								VSHcodeEditor.setText(Utils.readFileAsString(file));
								break;
							case GEOMETRY:
								GSHcodeEditor.setText(Utils.readFileAsString(file));
								break;
							case FRAGMENT:
								FSHcodeEditor.setText(Utils.readFileAsString(file));
								break;
							case SHADERBOX:
								FileInputStream fileInputStream = new FileInputStream(file);
								ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
								VSHcodeEditor.setText((String)objectInputStream.readObject());
								GSHcodeEditor.setText((String)objectInputStream.readObject());
								FSHcodeEditor.setText((String)objectInputStream.readObject());
								objectInputStream.close();
								fileInputStream.close();
								break;
							default:
								JOptionPane.showMessageDialog(rootPanel, "Invalid extension", "ShaderBox", JOptionPane.ERROR_MESSAGE);
								return;
						}
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

				FileFilter fragmentFileFilter = new FileNameExtensionFilter("Fragment shader file (*.fsh)", "fsh");
				jFileChooser.addChoosableFileFilter(fragmentFileFilter);
				FileFilter geometryFileFilter = new FileNameExtensionFilter("Geometry shader file (*.gsh)", "gsh");
				jFileChooser.addChoosableFileFilter(geometryFileFilter);
				FileFilter vertexFileFilter = new FileNameExtensionFilter("Vertex shader file (*.vsh)", "vsh");
				jFileChooser.addChoosableFileFilter(vertexFileFilter);
				FileFilter shaderboxFileFilter = new FileNameExtensionFilter("ShaderBox file (*.sbox)", "sbox");
				jFileChooser.addChoosableFileFilter(shaderboxFileFilter);

				jFileChooser.setFileFilter(shaderboxFileFilter);
				jFileChooser.setAcceptAllFileFilterUsed(false);

				jFileChooser.setCurrentDirectory(lastSelectedFolder);
				if(jFileChooser.showSaveDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
					file = jFileChooser.getSelectedFile();
				}

				if(file == null) {
					return;
				}

				lastSelectedFolder = file.getParentFile();

				try {
					switch (getShaderTypeFromFile(file)) {
						case VERTEX:
							FileWriter fileWriter1 = new FileWriter(file);
							fileWriter1.write(VSHcodeEditor.getText());
							fileWriter1.close();
							break;
						case GEOMETRY:
							FileWriter fileWriter2 = new FileWriter(file);
							fileWriter2.write(GSHcodeEditor.getText());
							fileWriter2.close();
							break;
						case FRAGMENT:
							FileWriter fileWriter3 = new FileWriter(file);
							fileWriter3.write(FSHcodeEditor.getText());
							fileWriter3.close();
							break;
						case SHADERBOX:
							FileOutputStream fileOutputStream = new FileOutputStream(file);
							ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
							objectOutputStream.writeObject(VSHcodeEditor.getText());
							objectOutputStream.writeObject(GSHcodeEditor.getText());
							objectOutputStream.writeObject(FSHcodeEditor.getText());
							objectOutputStream.flush();
							fileOutputStream.flush();
							objectOutputStream.close();
							fileOutputStream.close();
							break;
						default:
							JOptionPane.showMessageDialog(rootPanel, "Invalid extension", "ShaderBox", JOptionPane.ERROR_MESSAGE);
							break;
					}
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

	enum ShaderType {
		FRAGMENT, VERTEX, GEOMETRY, SHADERBOX, UNKNOWN
	}

	private ShaderType getShaderTypeFromFile(File file) {
		String fileName = file.getName();
		final int fileExtPos = fileName.lastIndexOf('.');
		if(fileExtPos < 0) {
			return ShaderType.UNKNOWN;
		}
		fileName = fileName.substring(fileExtPos + 1).toLowerCase();
		if(fileName.equals("fsh")) {
			return ShaderType.FRAGMENT;
		} else if(fileName.equals("gsh")) {
			return ShaderType.GEOMETRY;
		} else if(fileName.equals("vsh")) {
			return ShaderType.VERTEX;
		} else if(fileName.equals("sbox")) {
			return ShaderType.SHADERBOX;
		} else {
			return ShaderType.UNKNOWN;
		}
	}

	private void refreshCode() {
		GSHcodeEditorGutter.removeAllTrackingIcons();
		VSHcodeEditorGutter.removeAllTrackingIcons();
		FSHcodeEditorGutter.removeAllTrackingIcons();
		labelStatus.setForeground(Color.YELLOW);
		labelStatus.setText("Compiling");

		OpenGLMain.compileNewProgramNow = false;
		OpenGLMain.newCurrentProgramFragment = FSHcodeEditor.getText();
		OpenGLMain.newCurrentProgramVertex = VSHcodeEditor.getText();
		OpenGLMain.newCurrentProgramGeometry = GSHcodeEditor.getText();
		OpenGLMain.compileNewProgramNow = true;
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

					if((!OpenGLMain.compileNewProgramNow) && OpenGLMain.lastShaderError != null) {
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
