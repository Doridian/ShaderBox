package de.doridian.shaderbox.gl;

import de.doridian.shaderbox.Utils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.File;

public class ShaderHelper {
	public static void deleteShader(int shader) {
		if(shader != 0)
			GL20.glDeleteShader(shader);
	}

	/*
	 * With the exception of syntax, setting up vertex and fragment shaders
	 * is the same.
	 * @param the name and path to the shader
	 */
	public static int createShader(File filename, int shaderType) {
		try {
			return createShader(Utils.readFileAsString(filename), shaderType);
		} catch(Exception exc) {
			exc.printStackTrace();
			return 0;
		}
	}

	/*
     * With the exception of syntax, setting up vertex and fragment shaders
     * is the same.
     * @param the content of the shader
     */
	public static int createShader(String content, int shaderType) {
		int shader = 0;
		try {
			shader = GL20.glCreateShader(shaderType);
			if (shader == 0) return 0;
			GL20.glShaderSource(shader, content);
			GL20.glCompileShader(shader);
			if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
				throw new RuntimeException(getLogInfo(shader));
			else
				OpenGLMain.lastShaderError += getLogInfo(shader);
			return shader;
		} catch (Exception exc) {
			deleteShader(shader);
			throw new RuntimeException(exc.getMessage());
		}
	}

	public static String getLogInfo(int obj) {
		return GL20.glGetShaderInfoLog(obj, GL20.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH));
	}

}
