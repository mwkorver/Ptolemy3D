/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ptolemy3d.scene;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DEvents;
import org.ptolemy3d.util.Texture;

class LoadingScreen
{
	private Ptolemy3D ptolemy;
	private Texture texture = null;
	private int textureID = 0;
	
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}
	
	public void initGL(GL gl)
	{
		texture = Texture.load(getClass().getResourceAsStream("/org/ptolemy3d/pTolemy3D.png"));
		if(texture != null) {
			textureID = ptolemy.textureManager.load(gl, texture.data, texture.width, texture.height, texture.format, true, -1);
			texture = texture.removeData();
		}
	}
	
	public void destroyGL(GL gl) {
		if(textureID > 0) {
			ptolemy.textureManager.unload(gl, textureID);
		}
		textureID = 0;
		texture = null;
	}
	
	public boolean isDrawLoadingScreen() {
		return textureID > 0 && ptolemy.scene.landscape.numVisibleJp2 < 4;
	}
	
	public void drawLoadingScreen(GL gl) {
		if((textureID <= 0) || (texture == null)) {
			return;
		}
		
        final Ptolemy3DEvents events = ptolemy.events;
        
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, events.drawWidth, 0.0f, events.drawHeight, -1.0f, 1.0f);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // Bind texture and draw icon
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);
        if(texture.format == GL.GL_RGBA) {
        	gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        float x = (events.drawWidth - texture.width) / 2;
        float y = (events.drawHeight - texture.height);
        gl.glColor4f(1, 1, 1, 1);
        gl.glBegin(GL.GL_QUADS);
	        gl.glTexCoord2f(0.0f, 0.0f);
	        gl.glVertex2f(x, y);
	
	        gl.glTexCoord2f(1.0f, 0.0f);
	        gl.glVertex2f(x + texture.width, y);
	
	        gl.glTexCoord2f(1.0f, 1.0f);
	        gl.glVertex2f(x + texture.width, y + texture.height);
	
	        gl.glTexCoord2f(0.0f, 1.0f);
	        gl.glVertex2f(x, y + texture.height);
        gl.glEnd();
        
        if(texture.format == GL.GL_RGBA) {
            gl.glDisable(GL.GL_BLEND);
        }

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
	}
}
