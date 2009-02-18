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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GLCanvas;
import javax.swing.JFrame;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.w3c.dom.Element;

/**
 * Ptolemy3D example
 */
public class SimpleFrame
{
	public static void main(String[] args)
	{
		/*
		 * Ptolemy3D configuration
		 */
		String xmlFile = null;
		int width = 600, height = 400;
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-xmlfile")) {
					xmlFile = args[i + 1];
				}
				else if (args[i].equals("-w")) {
					width = Integer.parseInt(args[i + 1]);
				}
				else if (args[i].equals("-h")) {
					height = Integer.parseInt(args[i + 1]);
				}
			}
		}
		catch (Exception e) {
			xmlFile = null;
		}
		if (xmlFile == null) {
			usage();
		}

		/*
		 * Ptolemy3D instanciation
		 */
		final Ptolemy3D ptolemy3d = new Ptolemy3D();

		/*
		 * Ptolemy3D initialization
		 */
		//Build document from the XML configuration file
		//If null is used, parameters will be search inside the applet tag.
		Element docelem = Ptolemy3DConfiguration.buildXMLDocument(xmlFile);
		if (docelem == null) {
			usage();	//xml file not found
		}
		ptolemy3d.init(docelem);

		/*
		 * Ptolemy3D rendering area
		 */
		//Creating the rendering area for Ptolemy rendering
		final GLCanvas component3d = new GLCanvas();
		//GLJPanel component3d = new GLJPanel();
		component3d.addGLEventListener(ptolemy3d.events);
		component3d.setSize(new Dimension(width, height));

		/*
		 * Ptolemy3D Example Application
		 */
		//Creating the Frame and adding the Ptolemy3D rendering area
		final JFrame frame = new JFrame("Ptolemy3D Example - Simple Frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(component3d, BorderLayout.CENTER);
		frame.pack();

		//Register listeners
		frame.addMouseListener(new MouseAdapter() {
		    public void mouseEntered(MouseEvent e) {
		    	frame.requestFocus();
		    }
		});
		frame.addWindowListener(new WindowAdapter( ){
		    public void windowClosing(WindowEvent e) {
		    	frame.getInputContext().removeNotify(component3d);

		        //Ptolemy3D destruction
		        ptolemy3d.destroy();

		        frame.remove(component3d);
		        frame.dispose();
		    }
		});

		/*
		 * Show the frame and start Ptolemy3D Rendering
		 */
		frame.setVisible(true);
	}

	private final static void usage()
	{
		System.out.println("usage : "+SimpleFrame.class.getName()+" -xmlfile [xml file] -w [width] -h [height]");
		System.exit(1);
	}
}
