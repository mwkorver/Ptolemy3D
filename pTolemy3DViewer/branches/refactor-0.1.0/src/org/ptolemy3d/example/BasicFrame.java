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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

        String configFile = "/home/antonio/AllTogether/Projects/ASI/pTolemy3D-Project/ptolemy-refactor/config/local_test.xml";

        // Load the configuration file.
        Configuration config = new Configuration(configFile);

        // Initialize ptolemy system with the config file.
        Ptolemy3D.initialize(config);

        // Create the canvas and register it into Ptolemy3D system.
        Ptolemy3DGLCanvas canvas = new Ptolemy3DGLCanvas();
        Ptolemy3D.registerCanvas(canvas);

        Ptolemy3D.start();
        
        this.getRootPane().setLayout(new BorderLayout());
        this.getRootPane().add(canvas, BorderLayout.CENTER);
        this.pack();

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Clossing ptolemy...");
                Ptolemy3D.shutDown();
            }
        });
    }

    public static void main(String[] args) {
        BasicFrame frame = new BasicFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(600, 500);
        frame.setVisible(true);
    }
}
