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
package org.ptolemy3d.example;

import javax.swing.JFrame;

import org.ptolemy3d.Configuration;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;

/**
 * Ptolemy3D example.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class BasicFrame extends JFrame {

    public BasicFrame() {

        Ptolemy3DGLCanvas canvas = new Ptolemy3DGLCanvas();
        Configuration config = new Configuration(canvas, "some_xml_file");
        Ptolemy3D.initialize(config);

        this.getRootPane().add(canvas);
        this.pack();

    // TODO - Look if is necessary the destroy call.
    // frame.addWindowListener(new WindowAdapter( ){
    // public void windowClosing(WindowEvent e) {
    // frame.getInputContext().removeNotify(component3d);
    //
    // //Ptolemy3D destruction
    // ptolemy3d.destroy();
    //
    // frame.remove(component3d);
    // frame.dispose();
    // }
    // });
    }

    public static void main(String[] args) {
        BasicFrame frame = new BasicFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(600, 500);
        frame.setVisible(true);
    }
}
