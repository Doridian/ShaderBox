package de.doridian.shaderbox.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class RenderTarget {
	public int framebuffer;
	public int renderbuffer;
	public int texture;

	public RenderTarget(int width, int height) {
		this.framebuffer = GL30.glGenFramebuffers();
		this.renderbuffer = GL30.glGenRenderbuffers();
		this.texture = GL11.glGenTextures();

		// set up texture
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

		// set up framebuffer
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.texture, 0);

		// set up renderbuffer
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.renderbuffer);

		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT16, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, this.renderbuffer);

		// clean up
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public void delete() {
		GL30.glDeleteFramebuffers(framebuffer);
		GL30.glDeleteRenderbuffers(renderbuffer);
		GL11.glDeleteTextures(texture);
	}
}
