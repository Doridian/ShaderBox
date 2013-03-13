package de.doridian.shaderbox.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public class ScreenShader extends ShaderProgram {
	public final int uniformResolution;
	public final int uniformOffset;
	public final int uniformTexture;

	public ScreenShader() {
		super(ShaderProgram.VSH_DONOTHING, null,
				"#version 130\n" +
				"uniform vec2 offset;\n" +
				"uniform vec2 resolution;\n" +
				"uniform sampler2D backtexture;\n" +
				"out vec4 FragColor;\n" +
				"void main() {\n" +
				"	vec2 uv = (gl_FragCoord.xy  + offset.xy) / resolution.xy;\n" +
				"	if(uv.x < 0 || uv.x > 1 || uv.y < 0 || uv.y > 1)\n" +
				"		FragColor = vec4(0.0,0.0,0.0,0.0);\n" +
				"	else\n" +
				"		FragColor = texture(backtexture, uv);\n" +
				"}"
		);

		FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(12);
		float[] floats = new float[] { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
		floatBuffer.put(floats);
		floatBuffer.flip();

		OpenGLMain.buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, OpenGLMain.buffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);

		uniformResolution = getUniformLocation("resolution");
		uniformOffset = getUniformLocation("offset");
		uniformTexture = getUniformLocation("backtexture");
	}
}
