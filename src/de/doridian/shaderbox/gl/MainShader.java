package de.doridian.shaderbox.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class MainShader extends ShaderProgram {
	int vertexPosition;

	public static final String FSH_DONOTHING =
			"#version 130\n" +
			"//Default Fragment Shader rendering backbuffer => front (not manipulating anything)\n" +
			"uniform vec2 resolution;\n" +
			"uniform sampler2D backbuffer;\n" +
			"out vec4 FragColor;\n" +
			"void main( void ) {\n" +
			"	FragColor = texture(backbuffer, (gl_FragCoord.xy / resolution.xy));\n" +
			"}";

	public final int uniformMouse;
	public final int uniformResolution;
	public final int uniformBackbuffer;
	public final int uniformTime;
	public final int uniformOriginal;

	public MainShader(String vsh, String gsh, String fsh) {
		super((vsh == null || vsh.isEmpty()) ? ShaderProgram.VSH_DONOTHING : vsh, gsh, (fsh == null || fsh.isEmpty()) ? FSH_DONOTHING : fsh);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, OpenGLMain.buffer);
		GL20.glVertexAttribPointer(vertexPosition, 2, GL11.GL_FLOAT, false, 0, 0);
		GL20.glEnableVertexAttribArray(vertexPosition);

		uniformMouse = getUniformLocation("mouse");
		uniformResolution = getUniformLocation("resolution");
		uniformBackbuffer = getUniformLocation("backbuffer");
		uniformOriginal = getUniformLocation("original");
		uniformTime = getUniformLocation("time");
	}
}
