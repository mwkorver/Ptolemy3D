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

public class XsltPOI extends Xslt{
	private NodeList pois;
	private NamedNodeMap attributes;

	public XsltPOI (Document doc, String file){
		super(doc,file);
	}

	public XsltPOI (InputStream in, String file){
		super(in,file);
	}

	public void transform(){
		super.transform();
	}

	public boolean getNodes(){
		Node poi_root = super.outputResult.getNode().getFirstChild();
		pois = poi_root.getChildNodes();
		attributes = poi_root.getAttributes();
		return true;
	}

	public int getNumberOfFeatures(){
		return pois.getLength();
	}

	public String getLabel(int i){
		return pois.item(i).getAttributes().getNamedItem("label").getNodeValue();
	}
	
	public String getLat(int i){
		return pois.item(i).getAttributes().getNamedItem("lat").getNodeValue();
	}

	public String getLon(int i){
		return pois.item(i).getAttributes().getNamedItem("lon").getNodeValue();
	}

	public String getId(int i){
		return pois.item(i).getAttributes().getNamedItem("id").getNodeValue();
	}

	public String getObjId(int i){
		return pois.item(i).getAttributes().getNamedItem("obj_id").getNodeValue();
	}

	public String getIconPrefix(){
		Node node = attributes.getNamedItem("iconprefix");
		if(node!=null) return node.getNodeValue();
		else return "/JetStreamLib/common/icon/bdot";
	}

	public String getBgColor(){
		Node node = attributes.getNamedItem("bgcolor");
		if(node!=null) return node.getNodeValue();
		else return "0:204:153:150";
	}

	public String getFColor(){
		Node node = attributes.getNamedItem("fcolor");
		if(node!=null) return node.getNodeValue();
		else return "255:255:255:255";
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

	public String getPosition(){
		Node node = attributes.getNamedItem("position");
		if(node!=null) return node.getNodeValue();
		else return "RM";
	}

	public String getKanji(){
		Node node = attributes.getNamedItem("kanji");
		if(node!=null) return node.getNodeValue();
		else return "true";
	}

	public String getFontHeight(){
		Node node = attributes.getNamedItem("fontheight");
		if(node!=null) return node.getNodeValue();
		else return "1.8";
	}

	public String getFontWidth(){
		Node node = attributes.getNamedItem("fontwidth");
		if(node!=null) return node.getNodeValue();
		else return "1.3";
	}

	public String getLabelFontFile(){
		Node node = attributes.getNamedItem("fontfile");
		if(node!=null) return node.getNodeValue();
		else return "/fonts/verdana36both.fnt";
	}

	public String getNumIcons(){
		Node node = attributes.getNamedItem("numicons");
		if(node!=null) return node.getNodeValue();
		else return "0";
	}

	public String getMinDistance(){
		Node node = attributes.getNamedItem("mindistance");
		if(node!=null) return node.getNodeValue();
		else return "85";
	}

	public String getEncoding(){
		Node node = attributes.getNamedItem("encoding");
		if(node!=null) return node.getNodeValue();
		else return "UTF-8";
	}

	public String getBorderColor(){
		Node node = attributes.getNamedItem("bordercolor");
		if(node!=null) return node.getNodeValue();
		else return "0:0:0:0";
	}

	public String getPinpoint(){
		Node node = attributes.getNamedItem("pinpoint");
		if(node!=null) return node.getNodeValue();
		else return "false";
	}



}

