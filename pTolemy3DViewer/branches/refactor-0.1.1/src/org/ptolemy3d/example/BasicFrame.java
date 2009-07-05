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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.ptolemy3d.Configuration;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.plugin.CityGmlPlugin;
import org.ptolemy3d.view.CameraMovement;

/**
 * Ptolemy3D example.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class BasicFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private Ptolemy3DGLCanvas canvas = null;
	private Configuration config = null;

	public BasicFrame(String configFile) {
		super("pTolemy3D Basic Frame");

		// Load the configuration file.
		config = new Configuration(configFile);

		// Initialize ptolemy system with the config file.
		Ptolemy3D.initialize(config);

		// Create the canvas and register it into Ptolemy3D system.
		canvas = new Ptolemy3DGLCanvas();
		Ptolemy3D.registerCanvas(canvas);
		Ptolemy3D.start();

		// Register key listener for the canvas.
		canvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !e.isControlDown()) {
					System.out.println("Closing ptolemy...");
					Ptolemy3D.shutDown();
					System.exit(1);
				}
			}
		});

		CityGmlPlugin cgp = new CityGmlPlugin();
		cgp.setAltitude(100000);
		cgp.setFileUrl("file:///opt/servers/jetty-6.1.14/webapps/Ptolemy_test/gml_big_test.xml");
		Ptolemy3D.getScene().getPlugins().addPlugin(cgp);
		
		
		// Add canvas and the menu panel
		this.getRootPane().setLayout(new BorderLayout());
		this.getRootPane().add(canvas, BorderLayout.CENTER);
		JPanel menuPanel = new JPanel(new FlowLayout());
		this.getRootPane().add(menuPanel, BorderLayout.NORTH);

		// Listener to close the frame
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("Closing ptolemy...");
				Ptolemy3D.shutDown();
				System.exit(1);
			}
		});

		// Add menu buttons
		JButton quit = new JButton("Quit");
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Closing ptolemy...");
				Ptolemy3D.shutDown();
				System.exit(1);
			}
		});

		JButton flyto = new JButton("Fly to...");
		flyto.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				CameraMovement cm = canvas.getCameraMovement();
				cm.flyTo(52331100, 13045000, 10);
			}
		});
		
		JButton flyto2 = new JButton("Fly to 2...");
		flyto2.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				CameraMovement cm = canvas.getCameraMovement();
				cm.flyTo(56293092, 14783110, 10);
			}
		});

		menuPanel.add(flyto);
		menuPanel.add(flyto2);
		menuPanel.add(quit);

		// TODO - Using pack I can't see any globe.
//		this.pack();
	}

	public static void main(String[] args) {

		// Parse command line
		String xml = "config/local_config.xml";
		int width = 640;
		int height = 500;
		if (args.length > 0) {
			xml = args[0];
		}
		if (args.length > 2) {
			try {
				int w = Integer.parseInt(args[1]);
				int h = Integer.parseInt(args[2]);
				width = w;
				height = h;
			} catch (NumberFormatException e) {
			}
		}

		BasicFrame frame = new BasicFrame(xml);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setSize(width, height);
		frame.setVisible(true);
	}
}
