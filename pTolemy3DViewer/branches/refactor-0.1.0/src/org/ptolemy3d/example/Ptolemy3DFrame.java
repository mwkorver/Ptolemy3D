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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.w3c.dom.Element;

/**
 * Ptolemy3D viewer in a Frame
 */
public class Ptolemy3DFrame extends JFrame implements KeyListener, WindowListener, MouseListener
{
	private static final long serialVersionUID = 1L;

	public static void main(String[] args)
	{
		//Read program arguments
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

		//Build document from the XML configuration file
		//If null is used, parameters will be search inside the applet tag.
		Element docelem = Ptolemy3DConfiguration.buildXMLDocument(xmlFile);
		if (docelem == null) {
			usage();	//xml file not found
		}

		//Ptolemy3D instanciation
		Ptolemy3D ptolemy3d = new Ptolemy3D();

		//Ptolemy3D initialization
		ptolemy3d.init(docelem);

		//Ptolemy3D rendering area
		Ptolemy3DGLCanvas canvas = new Ptolemy3DGLCanvas(ptolemy3d);
		canvas.setSize(new Dimension(width, height));

		//Ptolemy3D Frame
		Ptolemy3DFrame frame = new Ptolemy3DFrame(ptolemy3d, canvas);
		frame.setVisible(true);
	}

	private final static void usage()
	{
		System.out.println("usage : "+Ptolemy3DFrame.class.getName()+" -xmlfile [xml file] -w [screen width] -h [screen height]");
		System.exit(1);
	}

	private Ptolemy3D ptolemy3d;
	private Component component3d;

	private Ptolemy3DFrame(Ptolemy3D ptolemy3d, Component component3d)
	{
		super("Ptolemy3D Boundless Image Server");
		this.ptolemy3d = ptolemy3d;
		this.component3d = component3d;

		component3d.addKeyListener(this);
		component3d.addMouseListener(this);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(this);
		this.setLayout(new BorderLayout());
		this.add(component3d, BorderLayout.CENTER);
		this.pack();
	}

	private final void destroy()
	{
		getInputContext().removeNotify(component3d);

		if(ptolemy3d != null) {
			ptolemy3d.destroy();
			ptolemy3d = null;
		}

		remove(component3d);

		System.exit(0);
	}

	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		component3d.requestFocus();
	}

	/* Events */

	public void mouseEntered(MouseEvent e) {
		requestFocus();
	}
	public void mouseClicked(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}

	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE && !e.isControlDown()) {
			destroy();
		}
	}

	public void windowClosing(WindowEvent e) {
		destroy();
	}
	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
}
