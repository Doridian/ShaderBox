package de.doridian.shaderbox.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.util.HashMap;

public abstract class ShaderProgram {
	public static final String VSH_DONOTHING =
			"//Default Vertex shader outputting unmanipulated vertices\n" +
			"attribute vec3 position;\n" +
			"void main() {\n" +
			"	gl_Position = vec4( position, 1.0 );\n" +
			"}";

	private final HashMap<String, Integer> uniformCache = new HashMap<String, Integer>();
	private int program;

	protected ShaderProgram(String vsh, String gsh, String fsh) {
		init(ShaderHelper.createShader(vsh, GL20.GL_VERTEX_SHADER), ShaderHelper.createShader(gsh, GL32.GL_GEOMETRY_SHADER), ShaderHelper.createShader(fsh, GL20.GL_FRAGMENT_SHADER));
	}

	private void init(int vsh, int gsh, int fsh) {
		if(vsh == 0 || fsh == 0) {
			ShaderHelper.deleteShader(fsh);
			ShaderHelper.deleteShader(vsh);
			ShaderHelper.deleteShader(gsh);
			throw new RuntimeException();
		}

		program = GL20.glCreateProgram();
		if(program == 0)
			throw new RuntimeException();

		GL20.glAttachShader(program, vsh);
		if(gsh != 0)
			GL20.glAttachShader(program, gsh);
		GL20.glAttachShader(program, fsh);

		GL20.glLinkProgram(program);
		if(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			throw new RuntimeException(getLogInfo(program));
		} else {
			OpenGLMain.lastShaderError += getLogInfo(program);
		}

		GL20.glValidateProgram(program);
		if(GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
			throw new RuntimeException(getLogInfo(program));
		} else {
			OpenGLMain.lastShaderError += getLogInfo(program);
		}

		ShaderHelper.deleteShader(fsh);
		ShaderHelper.deleteShader(gsh);
		ShaderHelper.deleteShader(vsh);
	}

	public static String getLogInfo(int obj) {
		return GL20.glGetProgramInfoLog(obj, GL20.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
	}

	public void delete() {
		GL20.glDeleteProgram(program);
		program = 0;
		uniformCache.clear();
	}

	public int getProgram() {
		return program;
	}

	public int getUniformLocation(String name) {
		Integer ret = uniformCache.get(name);
		if(ret == null) {
			ret = GL20.glGetUniformLocation(program, name);
			uniformCache.put(name, ret);
		}
		return ret;
	}
}
