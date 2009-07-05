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
import org.ptolemy3d.DrawContext;

/**
 * Light settings
 */
public class Light {

    //Ambient light
    protected float[] lightAmbient = {0.8f, 0.8f, 0.8f, 1.0f};

    //Diffuse light
    protected float[] light0Diffuse = {0.8f, 0.8f, 0.8f, 1.0f};
    protected float[] light1Diffuse = {0.8f, 0.8f, 0.8f, 1.0f};

    //Light position
    public float[] light0Position = {0.5f, 0.8f, 0.5f, 0.0f};
    public float[] light1Position = {-0.5f, 0.8f, -0.5f, 0.0f};

    public Light() {
    }

    public void initGL(DrawContext drawContext) {
        GL gl = drawContext.getGL();

        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light0Diffuse, 0);

        gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, light1Diffuse, 0);
    }

    public void draw(DrawContext drawContext) {
        GL gl = drawContext.getGL();
        
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light0Position, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light1Position, 0);
        
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        gl.glEnable(GL.GL_LIGHT1);        
    }

    public void destroyGL(DrawContext drawContext) {
    }
}
