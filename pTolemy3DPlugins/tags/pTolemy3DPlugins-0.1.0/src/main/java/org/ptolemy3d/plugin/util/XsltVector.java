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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.io.InputStream;


public class XsltVector extends Xslt{
	private NodeList polys;
	private NamedNodeMap attributes;

	public XsltVector(Document doc, String file){
		super(doc,file);
	}

	public XsltVector(InputStream in, String file){
		super(in,file);
	}
	public void getNodes(){
		Node poly_root = super.outputResult.getNode().getFirstChild();
		polys = poly_root.getChildNodes();
		attributes = poly_root.getAttributes();
	}

	public int getNumberOfFeatures(){
		return polys.getLength();
	}

	public String getCoords(int i){
		Node poly_coords = polys.item(i).getFirstChild().getFirstChild();
		if(poly_coords != null) 
			return poly_coords.getNodeValue();
		else 
			return null;
	}

	public String getStroke(){
		Node node = attributes.getNamedItem("stroke");
		if(node!=null) return node.getNodeValue();
		else return "220:220:220";
	}

	public String getLineWidth(){
		Node node = attributes.getNamedItem("linew");
		if(node!=null) return node.getNodeValue();
		else return "2";
	}
	
	public String getDisplayRadius(){
		Node node = attributes.getNamedItem("dispr");
		if(node!=null) return node.getNodeValue();
		else return "9000";
	}

	public String getVectorRaise(){
		Node node = attributes.getNamedItem("vectorraise");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getMaxAlt(){
		Node node = attributes.getNamedItem("maxalt");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getMinAlt(){
		Node node = attributes.getNamedItem("minalt");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getInterpolation(){
		Node node = attributes.getNamedItem("interpolation");
		if(node!=null) return node.getNodeValue();
		else return "150";
	}

	public String getLabelEncoding(){
		Node node = attributes.getNamedItem("labelenc");
		if(node!=null) return node.getNodeValue();
		else return "UTF-8";
	}

	public String getLabelIcon(){
		Node node = attributes.getNamedItem("labelicon");
		if(node!=null) return node.getNodeValue();
		else return "/JetStreamLib/common/icon/bdot";
	}
	
	public String getLabelPosition(){
		Node node = attributes.getNamedItem("labelposition");
		if(node!=null) return node.getNodeValue();
		else return "RM";
	}

	public String getLabelMD(){
		Node node = attributes.getNamedItem("labelmindistance");
		if(node!=null) return node.getNodeValue();
		else return "65";
	}

	public String getKanji(){
		Node node = attributes.getNamedItem("kanji");
		if(node!=null) return node.getNodeValue();
		else return "true";
	}

	public String getLabelFontFile(){
		Node node = attributes.getNamedItem("labelfontfile");
		if(node!=null) return node.getNodeValue();
		else return "/fonts/verdana36both.fnt";
	}

	public String getLabelFontHeight(){
		Node node = attributes.getNamedItem("labelfontheight");
		if(node!=null) return node.getNodeValue();
		else return "1.9";
	}

	public String getLabelFontWidth(){
		Node node = attributes.getNamedItem("labelfontwidth");
		if(node!=null) return node.getNodeValue();
		else return "1.45";
	}

	public String getLabelFontColor(){
		Node node = attributes.getNamedItem("labelfontcolor");
		if(node!=null) return node.getNodeValue();
		else return "255:255:255:255";
	}

	public String getLabelBgColor(){
		Node node = attributes.getNamedItem("labelbgcolor");
		if(node!=null) return node.getNodeValue();
		else return "100:100:250:0";
	}

	public String getLabelMinAlt(){
		Node node = attributes.getNamedItem("labelminalt");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getLabelMaxAlt(){
		Node node = attributes.getNamedItem("labelmaxalt");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getEnableSelect(){
		Node node = attributes.getNamedItem("enableSelect");
		if(node!=null) return node.getNodeValue();
		else return "false";

	}

	public String getLabel(int i){
		return polys.item(i).getAttributes().getNamedItem("label").getNodeValue();
	}

	public String getId(int i){
		return polys.item(i).getAttributes().getNamedItem("id").getNodeValue();
	}

	public String getX(int i){
		return polys.item(i).getAttributes().getNamedItem("x").getNodeValue();
	}

	public String getY(int i){
		return polys.item(i).getAttributes().getNamedItem("y").getNodeValue();
	}

}

