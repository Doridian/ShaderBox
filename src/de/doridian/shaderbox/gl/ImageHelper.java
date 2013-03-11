package de.doridian.shaderbox.gl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageHelper {
	public static BufferedImage toImage(ByteBuffer byteBuffer, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				int i = (x + (width * y)) * 4;
				int r = byteBuffer.get(i) & 0xFF;
				int g = byteBuffer.get(i + 1) & 0xFF;
				int b = byteBuffer.get(i + 2) & 0xFF;
				int a = byteBuffer.get(i + 3) & 0xFF;
				image.setRGB(x, height - (y + 1), (a << 24) | (r << 16) | (g << 8) | b);
			}
		}
		return image;
	}
}
