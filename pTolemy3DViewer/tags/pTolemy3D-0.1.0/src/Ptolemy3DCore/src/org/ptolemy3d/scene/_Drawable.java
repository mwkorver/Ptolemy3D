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

/**
 *
 */
interface _Drawable
{
	/** Initialize all non OpenGL datas.<BR>
	 * This is called just after the object instanciation.<BR>
	 * <BR>
	 * <B>OpenGL Context is NOT current here, initialize all OpenGL related datas in <code>initGL</code>
	 * @see #initGL(GL) */
	public void init(Ptolemy3D ptolemy);

	/** Initialize all OpenGL datas.<BR>
	 * <I>OpenGL Context is current.</I>
	 * @see #init(Ptolemy3D) */
	public void initGL(GL gl);

	/** Render OpenGL geometry. */
	public void draw(GL gl);

	/** Destroy all OpenGL Related Datas.<BR>
	 * <I>OpenGL Context is current.</I> */
	public void destroyGL(GL gl);
}
