package de.doridian.shaderbox.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class MainShader extends ShaderProgram {
	int vertexPosition;

	public static final String FSH_DONOTHING =
			"#version 130\n" +
			"uniform vec2 resolution;\n" +
			"uniform sampler2D backbuffer;\n" +
			"out vec4 FragColor;\n" +
			"void main( void ) {\n" +
			"	vec2 position = ( gl_FragCoord.xy / resolution.xy );\n" +
			"	FragColor = texture(backbuffer, position);\n" +
			"}";

	public final int uniformMouse;
	public final int uniformResolution;
	public final int uniformBackbuffer;
	public final int uniformTime;
	public final int uniformOriginal;

	public MainShader(String contents) {
		super(ShaderProgram.VSH_DONOTHING, contents);

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
