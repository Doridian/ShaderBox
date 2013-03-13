package de.doridian.shaderbox.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.util.regex.Pattern;

public class ShaderHelper {
	public static void deleteShader(int shader) {
		if(shader != 0)
			GL20.glDeleteShader(shader);
	}

	/*
     * With the exception of syntax, setting up vertex and fragment shaders
     * is the same.
     * @param the content of the shader
     */
	public static int createShader(String content, int shaderType) {
		if(content == null || content.isEmpty()) return 0;

		String linePrefix;
		switch (shaderType) {
			case GL20.GL_VERTEX_SHADER:
				linePrefix = "vsh:";
				break;
			case GL20.GL_FRAGMENT_SHADER:
				linePrefix = "fsh:";
				break;
			case GL32.GL_GEOMETRY_SHADER:
				linePrefix = "gsh:";
				break;
			default:
				linePrefix = "gen:";
				break;
		}

		int shader = 0;
		try {
			shader = GL20.glCreateShader(shaderType);
			if (shader == 0) return 0;
			GL20.glShaderSource(shader, content);
			GL20.glCompileShader(shader);
			if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
				throw new RuntimeException(getLogInfo(shader, linePrefix));
			else
				OpenGLMain.lastShaderError += getLogInfo(shader, linePrefix);
			return shader;
		} catch (Exception exc) {
			deleteShader(shader);
			throw new RuntimeException(exc.getMessage());
		}
	}

	private static final Pattern lineStarts = Pattern.compile("\r?\n");
	public static String getLogInfo(int obj, String linePrefix) {
		return linePrefix + lineStarts.matcher(GL20.glGetShaderInfoLog(obj, GL20.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH)).trim()).replaceAll("\n" + linePrefix) + "\n";
	}

}
