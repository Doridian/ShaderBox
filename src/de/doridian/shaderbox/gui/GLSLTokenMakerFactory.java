package de.doridian.shaderbox.gui;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;

public class GLSLTokenMakerFactory extends AbstractTokenMakerFactory {
	@Override
	protected void initTokenMakerMap() {
		putMapping("text/glsl", GLSLTokenMaker.class.getName());
	}
}
