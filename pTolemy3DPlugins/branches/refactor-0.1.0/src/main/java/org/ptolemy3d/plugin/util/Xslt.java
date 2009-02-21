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
package org.ptolemy3d.plugin.util;

import java.io.InputStream;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;


public class Xslt {
	private static Map<String, Templates> CACHE = new Hashtable<String, Templates>(); // map of Templates objects
	private DOMSource gmlSource;
	private StreamSource gmlStreamSource;

	protected Transformer transformer;
	protected DOMResult outputResult;

	
	public Xslt(Document doc, String file){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc_out = db.newDocument();

			Templates templates = CACHE.get(file);
			if (templates == null)
			{
				TransformerFactory factory = TransformerFactory.newInstance();
				InputStream xslt_in = this.getClass().getResourceAsStream(file);
				templates = factory.newTemplates(new StreamSource(xslt_in));
				CACHE.put(file, templates);
				xslt_in.close();
			}
			transformer  = templates.newTransformer();
			gmlSource = new DOMSource(doc);
			outputResult = new DOMResult(doc_out);
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			pce.printStackTrace();
		}
		catch(TransformerConfigurationException tce){
			tce.printStackTrace();
		}
		catch(TransformerException te){
			te.printStackTrace();
		}
		catch(Exception ie){
			ie.printStackTrace();
		}
	}


	public Xslt(InputStream stream, String file){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc_out = db.newDocument();

			Templates templates = CACHE.get(file);
			if (templates == null)
			{
				TransformerFactory factory = TransformerFactory.newInstance();
				InputStream xslt_in = this.getClass().getResourceAsStream(file);
				templates = factory.newTemplates(new StreamSource(xslt_in));
				CACHE.put(file, templates);
				xslt_in.close();
			}
			transformer  = templates.newTransformer();
			gmlStreamSource = new StreamSource(stream);
			outputResult = new DOMResult(doc_out);
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			pce.printStackTrace();
		}
		catch(TransformerConfigurationException tce){
			tce.printStackTrace();
		}
		catch(TransformerException te){
			te.printStackTrace();
		}
		catch(Exception ie){
			ie.printStackTrace();
		}
	}


	public void transform(){
		try
		{
			transformer.transform(gmlSource, outputResult);
			//StreamResult sr = new StreamResult(System.out);
			//transformer.transform(gmlSource,sr);
		}
		catch(TransformerException te){
			te.printStackTrace();
		}
	}

	public void transformStream(){
		try
		{
			transformer.transform(gmlStreamSource, outputResult);
			//StreamResult sr = new StreamResult(System.out);
			//transformer.transform(gmlStreamSource,sr);
		}
		catch(TransformerException te){
			te.printStackTrace();
		}
	}


}

