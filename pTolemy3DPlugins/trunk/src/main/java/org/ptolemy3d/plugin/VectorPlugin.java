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
package org.ptolemy3d.plugin;

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.util.ByteReader;
import org.ptolemy3d.util.IntBuffer;
import org.ptolemy3d.plugin.util.GMLDoc;
import org.ptolemy3d.plugin.util.VectorClass;
import org.ptolemy3d.plugin.util.VectorNode;
import org.ptolemy3d.plugin.util.XsltVector;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.LatLonAlt;

public class VectorPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
	/**  */
	private GL gl;
	
	  /** Plugin index*/
    private int pindex;

	public static final int WKB_MULTILINESTRING = 5;
	public static final int WKB_MULTIPOLYGON = 6;
	public static final int WKB_GEOMETRYCOLLECTION = 7;

	//WFS
	private boolean WFS = false;//use WFS or not;
	private String wfs_filter = null;
	private String typename = null;
	private XsltVector xsltOutput;
	private String wfs_max_features = "20";
	private String stylesheet = null;
	
	private boolean STATUS = false;
	private String GetUrl = "";
	private String ENC_LAYER,  LAYER = "";
	private String TextLabel = null;
	private String TextLabelENC = "UTF-8";
	private POILabelPlugin labelPart = null;
	private String ClassAttribute = null;
	private boolean setDeleteLists = false;
	private int DISP_MAXZ = Integer.MAX_VALUE;
	private int DISP_MINZ = 0;
	private float VECTOR_RAISE = 0;    // this number describes what to factor our int len by so we can interpolate progressively.
	private final short START_INTP_LVL = 5; // for res levels of interpolation
	private int DisplayRadius = 0;
	private int feature_minx = Integer.MAX_VALUE,  feature_minz = Integer.MAX_VALUE,  feature_maxx = Integer.MIN_VALUE,  feature_maxz = Integer.MIN_VALUE;
	private int numgeoms = 0;
	private int draw_type = WKB_MULTIPOLYGON;
	private int QueryWidth = 10000;
	private int selected_id = -1;
	private int[] vec_ids;  // id in database
	private boolean[][] isFixed;
	private VectorNode[][][][] shape;
	private double[][] ctds; // centroids
	IntBuffer hatchedLists = null;
	IntBuffer selectedShapes = null;
	IntBuffer hatchedQue = null;
	private float[] stroke = { 0, 0, 0 };
	private float LineWidth = 1.0f;
	private float interp_len;
	private boolean forceDataLoad;
	private String TextLabelMD = "65";
	private String TextLabelFontColor = "255:255:255:255";
	private String TextLabelBgColor = "0:0:0:0";
	private int line_style = 1;
	private boolean clickEnabled;
	private Vector<VectorClass> Classes;

	public VectorPlugin() {}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		this.interp_len = ptolemy.scene.landscape.levels[0].getTileSize();
		this.clickEnabled = false;
		this.Classes = null;
	}

	public void setPluginIndex(int index)
	{
		this.pindex = index;
	}
    public void setPluginParameters(String param)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;

		StringTokenizer stok;

		stok = new StringTokenizer(param, "***");
		// set some defaults

		String parameters;
		while (stok.hasMoreTokens())
		{
			parameters = stok.nextToken();
			if (parameters.substring(0, 1).equals("P"))
			{

				StringTokenizer ptok = new StringTokenizer(parameters, " ");
				String ky;
				while (ptok.hasMoreTokens())
				{
					try
					{
						ky = ptok.nextToken();
						if (ky.equalsIgnoreCase("-status"))
						{
							STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
						}
						else if (ky.equalsIgnoreCase("-jspurl"))
						{
							GetUrl = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-layer"))
						{
							LAYER = ptok.nextToken();
							ENC_LAYER = java.net.URLEncoder.encode(LAYER, "UTF-8");
						}
						else if (ky.equalsIgnoreCase("-querywidth"))
						{
							QueryWidth = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-displayradius"))
						{
							DisplayRadius = (int) (Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio);
						}
						else if (ky.equalsIgnoreCase("-minalt"))
						{
							DISP_MINZ = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-maxalt"))
						{
							DISP_MAXZ = Integer.parseInt(ptok.nextToken());
							if (DISP_MAXZ == 0)
							{
								DISP_MAXZ = Integer.MAX_VALUE;
							}
						}
						else if (ky.equalsIgnoreCase("-vectorraise"))
						{
							VECTOR_RAISE = Float.parseFloat(ptok.nextToken()) * unit.coordSystemRatio;
						}
						else if (ky.equalsIgnoreCase("-textlabel"))
						{
							TextLabel = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-labelencoding"))
						{
							TextLabelENC = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-labelfontcolor"))
						{
							TextLabelFontColor = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-labelbgcolor"))
						{
							TextLabelBgColor = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-labelmindistance"))
						{
							TextLabelMD = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-classattribute"))
						{
							ClassAttribute = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-class"))
						{
							addClass(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-enableSelect"))
						{
							clickEnabled = true;
						}
						else if (ky.equalsIgnoreCase("-style"))
						{
							line_style = Integer.parseInt(ptok.nextToken());
							if ((line_style < 1) || (line_style > 4))
							{
								line_style = 1;
							}
						}
						else if (ky.equalsIgnoreCase("-stroke"))
						{
							StringTokenizer colstok = new StringTokenizer(ptok.nextToken(), ":");
							stroke[0] = (float) Integer.parseInt(colstok.nextToken()) / 255;
							stroke[1] = (float) Integer.parseInt(colstok.nextToken()) / 255;
							stroke[2] = (float) Integer.parseInt(colstok.nextToken()) / 255;
						}
						else if (ky.equalsIgnoreCase("-linew"))
						{
							LineWidth = Float.parseFloat(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-interpolation"))
						{
							interp_len = Float.parseFloat(ptok.nextToken());
						}
						//adding wfs support
						else if (ky.equalsIgnoreCase("WFS")){
							WFS = true;
						}
						else if (ky.equalsIgnoreCase("-typename")){
							typename = ptok.nextToken();
							LAYER = typename;
						}
						else if (ky.equalsIgnoreCase("-wfsurl")){
							GetUrl = ptok.nextToken()+"&typename="+typename;
						}
						else if (ky.equalsIgnoreCase("-stylesheet")){
							stylesheet = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-maxFeatures")){
							wfs_max_features = ptok.nextToken();
						}
					}
					catch (Exception e)
					{
					}

				}
			}
			else
			{  // support for old way
				StringTokenizer ptok = new StringTokenizer(parameters, ",");


				try
				{
					STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
					GetUrl = ptok.nextToken();
					if (GetUrl.indexOf("?") == -1)
					{
						GetUrl += "?";
					}
					LAYER = ptok.nextToken();
					ENC_LAYER = java.net.URLEncoder.encode(LAYER, "UTF-8");
					QueryWidth = Integer.parseInt(ptok.nextToken());
					DisplayRadius = (int) (Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio);
					DISP_MAXZ = Integer.parseInt(ptok.nextToken());
					DISP_MINZ = Integer.parseInt(ptok.nextToken());
					VECTOR_RAISE = Float.parseFloat(ptok.nextToken()) * unit.coordSystemRatio;

					StringTokenizer cstok = new StringTokenizer(ptok.nextToken(), ":");
					stroke[0] = (float) Integer.parseInt(cstok.nextToken()) / 255;
					stroke[1] = (float) Integer.parseInt(cstok.nextToken()) / 255;
					stroke[2] = (float) Integer.parseInt(cstok.nextToken()) / 255;

					ptok.nextToken();
					//cstok = new StringTokenizer(stok.nextToken(),":");
					//fill[0] = (float)Integer.parseInt(cstok.nextToken())/255;
					//fill[1] = (float)Integer.parseInt(cstok.nextToken())/255;
					//fill[2] = (float)Integer.parseInt(cstok.nextToken())/255;

					LineWidth = Float.parseFloat(ptok.nextToken());

					if (ptok.hasMoreTokens())
					{
						interp_len = Float.parseFloat(ptok.nextToken());
					}
					// check to see if there are any wall images
				}
				catch (Exception e)
				{
				}
			}
		}
//		setLabelAttribute();

		if (Classes == null) {
			resetClasses();
		}
	}

	/**

    color;regex

	 ***/
	private void addClass(String clstr)
	{
		if (Classes == null) {
			resetClasses();
		}

		try {
			StringTokenizer stok = new StringTokenizer(clstr, ";");
			VectorClass vc = new VectorClass(stok.nextToken(), stok.nextToken(), stok.nextToken());
			Classes.add(vc);
		}
		catch (Exception e) {
			IO.printStack(e);
		}
	}

	private void resetClasses()
	{
		if (Classes != null) {
			Classes.clear();
			Classes = null;
		}

		Classes = new Vector<VectorClass>(5, 3);
//		// add null class to draw default shapes
//		Classes.add(new VectorClass(
//				String.valueOf(LineWidth),
//				(int) (stroke[0] * 255) + ":" + (int) (stroke[1] * 255) + ":" + (int) (stroke[2] * 255) + ":255", ""));
	}

	private void setLabelAttribute()
	{
		if (TextLabel != null) {
//			String QryStr = GetUrl.substring(GetUrl.indexOf("?") + 1, GetUrl.length());

			labelPart = new POILabelPlugin();
			labelPart.init(ptolemy);
			labelPart.setPluginIndex(pindex);
//			labelPart.setPluginParameters("P -status " + ((STATUS) ? "1" : "0") + " -jspquery 0 -numicons 1 -iconprefix /JetStreamLib/common/icon/bdot -mindistance 65 -encoding " + TextLabelENC + " -fontcolor " + TextLabelFontColor + "  -bgcolor " + TextLabelBgColor + " -minalt " + DISP_MINZ + " -maxalt " + DISP_MAXZ + " -position RM -mindistance " + TextLabelMD + "");
			labelPart.setPluginParameters("P -status " + ((STATUS) ? "1" : "0") + " -jspquery 0 -numicons 1 -iconprefix "+xsltOutput.getLabelIcon()+"  -encoding "+xsltOutput.getLabelEncoding()+" -fontcolor "+xsltOutput.getLabelFontColor()+"  -bgcolor "+xsltOutput.getLabelBgColor()+" -minalt "+xsltOutput.getLabelMinAlt()+" -maxalt "+xsltOutput.getLabelMaxAlt()+" -position "+xsltOutput.getLabelPosition()+" -mindistance "+xsltOutput.getLabelMD()+" -kanji "+xsltOutput.getKanji()+" -fontFile "+xsltOutput.getLabelFontFile()+" -font_h "+xsltOutput.getLabelFontHeight()+" -font_w "+xsltOutput.getLabelFontWidth());
		}
	}

	public void initGL(GL gl)
	{
	}

	// called when motion stops.
	public void motionStop(GL gl)
	{
		if (!STATUS) {
			return;
		}

		try {
			interpolateVectors();
		} catch (Exception e) {
			IO.printStackRenderer(e);
		}
	}

	private final void interpolateVectors()
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();

		if (isFixed == null) {
			return;
		}

		int i, j, k, m, n;
		int fix_id = -1;
		float closest = Float.MAX_VALUE;
		float dist;

		double approxht = landscape.groundHeight(latLonAlt.getLongitudeDD(), latLonAlt.getLatitudeDD(), 0) / unit.coordSystemRatio;

		boolean fixNonVis = false;

		for (n = (START_INTP_LVL - 1); n >= 0; n--)
		{
			fix_id = -1;
			closest = Float.MAX_VALUE;
			for (i = 0; i < numgeoms; i++)
			{
				if (!isFixed[i][n])
				{
					dist = (float) Math3D.distance2D(latLonAlt.getLongitudeDD(), ctds[i][ 0], latLonAlt.getLatitudeDD(), ctds[i][ 1]);
					if (dist < closest)
					{
						closest = dist;
						fix_id = i;
					}
				}
			}
			if (fix_id != -1)
			{
				if (fixNonVis || Math3D.realPointInView(ctds[fix_id][ 0], approxht, ctds[fix_id][ 1], 40))
				{
					for (j = 0; j < shape[fix_id].length; j++)
					{
						for (k = 0; k < shape[fix_id][j].length; k++)
						{
							for (m = 0; m < shape[fix_id][j][k].length; m++)
							{
								if ((m != (shape[fix_id][j][k].length - 1)) && (n > 0))
								{
									shape[fix_id][j][k][m].interpolate(shape[fix_id][j][k][m + 1], (interp_len * n), VECTOR_RAISE, 0);
								}
								else
								{
									shape[fix_id][j][k][m].setPointElevations();
								}
							}
						}
					}
					isFixed[fix_id][n] = true;
					return;
				}
			}

			if (!fixNonVis && (n == 0))
			{
				fixNonVis = true;
				n = START_INTP_LVL - 1;
			}
		}

		// if nothing was fixed reset the bottom level
		for (i = 0; i < isFixed.length; i++)
		{
			isFixed[i][0] = false;
		}

	}
	// To be called in the rendering loop
	public synchronized void draw(GL gl)
	{
		final Camera camera = ptolemy.camera;
		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
		}

		this.gl = gl;

		gl.glDisable(GL.GL_TEXTURE_2D);

		if (setDeleteLists) {
			deleteLists();
			setDeleteLists = false;
		}

		int do_hatch = -1;
		if (hatchedQue != null) {
			if (hatchedQue.size() > 0) {
				do_hatch = hatchedQue.popBuf();
			}
		}

		int clsz = 0;
		if ((Classes != null) && (numgeoms > 0)) {
			clsz = Classes.size();
		}

		for (int ctx = 0; ctx < clsz; ctx++)
		{
			VectorClass tmpvc = Classes.elementAt(ctx);
			int shpsize = tmpvc.shapes.size();

			gl.glColor3f(tmpvc.color[0], tmpvc.color[1], tmpvc.color[2]);
			gl.glLineWidth(tmpvc.lineW);

			for (int h = 0; h < shpsize; h++)
			{
				int i = tmpvc.shapes.get(h);
				//for(i=0;i<numgeoms;i++)
				//{
					if (selected_id == i) {
						gl.glLineWidth(tmpvc.lineW + 2);
					}

					for (int j = 0; j < shape[i].length; j++)
					{
						for (int k = 0; k < shape[i][j].length; k++)
						{
							if ((do_hatch != -1) && (i == do_hatch)) {
								drawHatchedPoly(i, j, k, tmpvc);
							}

							gl.glBegin(GL.GL_LINE_STRIP);
							for (int m = 0; m < shape[i][j][k].length; m++) {
								shape[i][j][k][m].draw(gl, true);
							}
							gl.glEnd();
						}
					}

					if (selected_id == i)
					{
						gl.glLineWidth(tmpvc.lineW);
					}
				//}
			}
		}

		if (hatchedLists != null)
		{
			gl.glDisable(GL.GL_DEPTH_TEST);
			int hlen = hatchedLists.size();
			for (int hlptr = 0; hlptr < hlen; hlptr++) {
				gl.glCallList(hatchedLists.get(hlptr));
			}
			gl.glEnable(GL.GL_DEPTH_TEST);
		}

		gl.glColor3f(0, 0, 0);
		gl.glEnable(GL.GL_TEXTURE_2D);

		try {
			drawVectorLabels();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		this.gl = null;
	}

	public void drawVectorLabels()
	{
		if (labelPart != null) {
			labelPart.draw(gl);
		}
	}

	public void destroyGL(GL gl)
	{
		deleteLists();
	}
	
	/* set parameters using XSLT out*/
	private void setParamsXSLT(){
		addClass(xsltOutput.getLineWidth()+";"+xsltOutput.getStroke()+";255");
		VECTOR_RAISE = Float.parseFloat(xsltOutput.getVectorRaise()) * ptolemy.unit.coordSystemRatio;
		DISP_MAXZ = Integer.parseInt(xsltOutput.getMaxAlt());
		if(DISP_MAXZ == 0) DISP_MAXZ = Integer.MAX_VALUE;
		DISP_MINZ = Integer.parseInt(xsltOutput.getMinAlt());
		interp_len = Float.parseFloat(xsltOutput.getInterpolation());
		clickEnabled = xsltOutput.getEnableSelect().equalsIgnoreCase("true") ? true :false;
		setLabelAttribute();

	}

	private final void loadDataWFS(boolean force, Communicator JC)  throws IOException{
		int tx,ty;
		
		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();
		
		tx = (int)latLonAlt.getLongitudeDD();
		ty = (int)latLonAlt.getLatitudeDD();
		
		int dispr = (int)(DisplayRadius/2);
		int factor = ptolemy.unit.DD;
		if((((tx - dispr) <  feature_minx) || ((tx + dispr) > feature_maxx) || ((ty + dispr) > feature_maxz) || ((ty - dispr) < feature_minz)) || (force)){
			cleanListsData();
			
			feature_minx = tx - (QueryWidth/2);
			feature_minz = ty - (QueryWidth/2);
			feature_maxx = tx + (QueryWidth/2);
			feature_maxz = ty + (QueryWidth/2);


			double x1 = (double)(feature_minx)/factor;
			double z1 = (double)(feature_minz)/factor;
			double x2 = (double)(feature_maxx)/factor;
			double z2 = (double)(feature_maxz)/factor;
			String wfs_url = GetUrl;
			JC.setServer("WFS_SERVER");//this tells JC that we're dealing with a WFS so it will know that the server is in the GetUrl
			if((wfs_filter==null)||(wfs_filter.length() == 0)) {
				wfs_url += "&bbox=" +x1+","+z1+","+x2+","+z2;
			}
			else{
				String bbox = "<BBox><PropertyName>DUMMY</PropertyName><gml:Box><gml:coordinates>"+x1+","+z1+"%20"+x2+","+z2+"</gml:coordinates></gml:Box></BBox>";
				wfs_url += "&filter=<Filter><And>" + wfs_filter + bbox + "</And></Filter>";
			}
			if(wfs_max_features!=null && wfs_max_features.length() != 0){
				wfs_url += "&maxFeatures="+wfs_max_features;
			}
			JC.requestData(wfs_url);

			GMLDoc gmlDoc = new GMLDoc(JC.getInputStream());
			xsltOutput = new XsltVector(gmlDoc.getDocument(),stylesheet);
			//xsltOutput = new XsltVector(JC.getInputStream(),stylesheet);
			JC.setServer(ptolemy.configuration.server);//need to reset the server otherwise label icons won't be found


			xsltOutput.transform();
			xsltOutput.getNodes();

			setParamsXSLT();

			
			int numgeoms = xsltOutput.getNumberOfFeatures();

			// layer block
//			if(data[cur[0]++] != 0) return;
//			if(ByteArrayReader.readInt(data,cur) != WKB_GEOMETRYCOLLECTION) return;
			// loop through geometries
			this.numgeoms = 0; // to prevent drawing 
//			int numgeoms = ByteArrayReader.readInt(data,cur);
			
			if(TextLabel != null){
				labelPart.pluginAction("removeAll", null);
			}
			
			int[] vec_ids = new int[numgeoms];
			boolean[][] isFixed = new boolean[numgeoms][START_INTP_LVL];
			double[][] ctds = new double[numgeoms][2];
			VectorNode[][][][] shape = new VectorNode[numgeoms][][][];
			
			selected_id = -1;
			int i,j,k,m,numpts,numobj,numrings;
			boolean isFirstPoint;
			int prev_x=0,prev_z=0;
			int clsz = 0;
			VectorClass tmpvc;
			if(Classes != null){
				clsz = Classes.size();
				for(i=0;i<clsz;i++) Classes.elementAt(i).shapes.reset();
			}
			boolean addedToClass;
			double len,tlen,cwx,cwz;
			for(i=0;i<numgeoms;i++){
				isFirstPoint = true;
				ctds[i][0] = ctds[i][1] = 0;
				tlen = cwx = cwz = 0;
//				vec_ids[i] = ByteArrayReader.readInt(data,cur);
				vec_ids[i] = Integer.parseInt(xsltOutput.getId(i));
//				System.out.println("vecids[i]: " + vec_ids[i]);
				Arrays.fill(isFixed[i],false);
//				if(data[cur[0]++] != 0) return;
//				draw_type = ByteArrayReader.readInt(data,cur);
//				System.out.println("draw_type: " + draw_type);
				numobj = 1;//ByteArrayReader.readInt(data,cur);
//				System.out.println("numobj: " + numobj);
				shape[i] = new VectorNode[numobj][][];
				
				for(j=0;j<numobj;j++){
					//if(data[cur[0]++] != 0) return;
					//ByteArrayReader.readInt(data,cur); // this is just a type int, not really needed.
					/*if(draw_type == WKB_MULTIPOLYGON)
						numrings = ByteArrayReader.readInt(data,cur);
					else
						numrings = 1;
					*/
					numrings = 1;
//					System.out.println("numrings: " + numrings);
					shape[i][j] = new VectorNode[numrings][];
					for(m=0;m<numrings;m++){
						//numpts = ByteArrayReader.readInt(data,cur);
						String coords = xsltOutput.getCoords(i);
						if (coords==null) return;
						StringTokenizer stok = new StringTokenizer(coords," ");
						numpts = stok.countTokens();
						//System.out.println("numpts: " + numpts);
						shape[i][j][m] = new VectorNode[numpts];
						k=0;
						while(stok.hasMoreTokens()){
							
						//for(k=0;k<numpts;k++){
							//shape[i][j][m][k] = new VectorNode(ByteArrayReader.readInt(data,cur) , ByteArrayReader.readInt(data,cur) , VECTOR_RAISE, true);//
							String[] lonlat = stok.nextToken().split(",");
							double lon = Double.valueOf(lonlat[0]).doubleValue();
							double lat = Double.valueOf(lonlat[1]).doubleValue();

							shape[i][j][m][k] = new VectorNode((int)(lon*factor) , (int)(lat*factor) , VECTOR_RAISE, true);
							if(!isFirstPoint){
								len = Math3D.distance2D(prev_x , shape[i][j][m][k].rp[0] , prev_z , shape[i][j][m][k].rp[1]);
								cwx += len * ((prev_x + shape[i][j][m][k].rp[0])/2);
								cwz += len * ((prev_z + shape[i][j][m][k].rp[1])/2);
								tlen += len;
							} else isFirstPoint = false;
							prev_x = shape[i][j][m][k].rp[0];
							prev_z = shape[i][j][m][k].rp[1];
							k++;//added for while loop
						}
					}
					ctds[i][0] = (float)(cwx / tlen);
					ctds[i][1] = (float)(cwz / tlen);
				}
			
				
				TextLabel = xsltOutput.getLabel(i);


				if(TextLabel != null && TextLabel != "" && !(TextLabel.equalsIgnoreCase("NO_LABEL_NAME")))
					labelPart.pluginAction("putIcon", vec_ids[i] + ",1," + TextLabel + "," + Double.valueOf(xsltOutput.getX(i)).doubleValue() + "," + Double.valueOf(xsltOutput.getY(i)).doubleValue());

				addedToClass = false;
				
			/*	
				if(ClassAttribute != null){
					// label
					llen = ByteArrayReader.readInt(data,cur);
					if(llen > 0){
						cltext =  new String(data  ,  cur[0] , llen , "UTF-8");
						for(int clx = 1;clx < clsz;clx++){
							tmpvc = (VectorClass) (Classes.elementAt(clx));
							if(cltext.indexOf(tmpvc.regex) != -1){
								tmpvc.shapes.append(i);
								addedToClass = true;
								break;
							}
						}
						cur[0] += llen;
					}
					
				}
				*/
				if((!addedToClass) && (clsz > 0)) {
					tmpvc = Classes.elementAt(0);
					tmpvc.shapes.append(i);
				}
			}
			
			this.numgeoms = numgeoms;
			this.vec_ids = vec_ids;
			this.isFixed = isFixed;
			this.ctds = ctds;
			this.shape = shape;
		}
	}

	private final void loadDataServlet(boolean force, Communicator JC) throws IOException
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();

		int tx, ty;
		tx = (int) latLonAlt.getLongitudeDD();
		ty = (int) latLonAlt.getLatitudeDD();
		int dispr = (DisplayRadius / 2);
		int factor = unit.DD;
		if ((((tx - dispr) < feature_minx) || ((tx + dispr) > feature_maxx) || ((ty + dispr) > feature_maxz) || ((ty - dispr) < feature_minz)) || (force))
		{
			cleanListsData();
			byte[] data;

			feature_minx = tx - (QueryWidth / 2);
			feature_minz = ty - (QueryWidth / 2);
			feature_maxx = tx + (QueryWidth / 2);
			feature_maxz = ty + (QueryWidth / 2);
			String req = GetUrl + "&LAYER=" + ENC_LAYER + "&BBOX=" + feature_minx + "," + feature_minz + "," + feature_maxx + "," + feature_maxz + "&FACTOR=" + String.valueOf(factor);
			if (TextLabel != null)
			{
				req += "&TEXTATTR=" + TextLabel;
			}
			if (ClassAttribute != null)
			{
				req += "&CLASSATTR=" + ClassAttribute;
			}
			JC.requestData(req);
			data = JC.getData();
//			int ulx = (int) unit.virtualX(feature_minx);
//			int ulz = (int) unit.virtualY(feature_maxz);
			int[] cur = { 0 };
			// layer block
			if (data[cur[0]++] != 0)
			{
				return;
			}
			if (ByteReader.readInt(data, cur) != WKB_GEOMETRYCOLLECTION)
			{
				return;
				// loop through geometries
			}
			this.numgeoms = 0; // to prevent drawing
			int numgeoms = ByteReader.readInt(data, cur);

			if (TextLabel != null)
			{
				labelPart.pluginAction("removeAll", null);
			}

			int[] vec_ids = new int[numgeoms];
			boolean[][] isFixed = new boolean[numgeoms][START_INTP_LVL];
			double[][] ctds = new double[numgeoms][2];
			VectorNode[][][][] shape = new VectorNode[numgeoms][][][];

			selected_id = -1;

			int i, j, k, m, numpts, numobj, numrings, llen;
			boolean isFirstPoint;
			int prev_x = 0, prev_z = 0;
			int clsz = 0;
			String cltext;
			VectorClass tmpvc;
			if (Classes != null)
			{
				clsz = Classes.size();
				for (i = 0; i < clsz; i++)
				{
					(Classes.elementAt(i)).shapes.reset();
				}
			}
			boolean addedToClass;

			double len, tlen, cwx, cwz;
			String newalbel;
			for (i = 0; i < numgeoms; i++)
			{
				isFirstPoint = true;
				ctds[i][0] = ctds[i][1] = 0;
				tlen = cwx = cwz = 0;
				vec_ids[i] = ByteReader.readInt(data, cur);
				Arrays.fill(isFixed[i], false);
				if (data[cur[0]++] != 0)
				{
					return;
				}
				draw_type = ByteReader.readInt(data, cur);
				numobj = ByteReader.readInt(data, cur);
				shape[i] = new VectorNode[numobj][][];

				for (j = 0; j < numobj; j++)
				{
					if (data[cur[0]++] != 0)
					{
						return;
					}
					ByteReader.readInt(data, cur); // this is just a type int, not really needed.
					if (draw_type == WKB_MULTIPOLYGON)
					{
						numrings = ByteReader.readInt(data, cur);
					}
					else
					{
						numrings = 1;
					}
					shape[i][j] = new VectorNode[numrings][];
					for (m = 0; m < numrings; m++)
					{
						numpts = ByteReader.readInt(data, cur);
						shape[i][j][m] = new VectorNode[numpts];
						for (k = 0; k < numpts; k++)
						{
							shape[i][j][m][k] = new VectorNode(ByteReader.readInt(data, cur), ByteReader.readInt(data, cur), VECTOR_RAISE, true);
							if (!isFirstPoint)
							{
								len = Math3D.distance2D(prev_x, shape[i][j][m][k].rp[0], prev_z, shape[i][j][m][k].rp[1]);
								cwx += len * ((prev_x + shape[i][j][m][k].rp[0]) / 2);
								cwz += len * ((prev_z + shape[i][j][m][k].rp[1]) / 2);
								tlen += len;
							}
							else
							{
								isFirstPoint = false;
							}
							prev_x = shape[i][j][m][k].rp[0];
							prev_z = shape[i][j][m][k].rp[1];
						}
					}
					ctds[i][0] = (float) (cwx / tlen);
					ctds[i][1] = (float) (cwz / tlen);
				}
				if (TextLabel != null)
				{
					// label
					llen = ByteReader.readInt(data, cur);
					if (llen > 0)
					{
						newalbel = new String(data, cur[0], llen, TextLabelENC);
						labelPart.pluginAction("putIcon", vec_ids[i] + ",1," + newalbel + "," + (ctds[i][0] / factor) + "," + (ctds[i][1] / factor));
						cur[0] += llen;
					}
					else
					{
						labelPart.pluginAction("putIcon", vec_ids[i] + ",1, ," + (ctds[i][0] / factor) + "," + (ctds[i][1] / factor));
					}
				}

				addedToClass = false;

				if (ClassAttribute != null)
				{
					// label
					llen = ByteReader.readInt(data, cur);
					if (llen > 0)
					{
						cltext = new String(data, cur[0], llen, "UTF-8");
						for (int clx = 1; clx < clsz; clx++)
						{
							tmpvc = (Classes.elementAt(clx));
							if (cltext.indexOf(tmpvc.regex) != -1)
							{
								tmpvc.shapes.append(i);
								addedToClass = true;
								break;
							}
						}
						cur[0] += llen;
					}

				}

				if ((!addedToClass) && (clsz > 0))
				{
					tmpvc = (Classes.elementAt(0));
					tmpvc.shapes.append(i);
				}



			}

			this.numgeoms = numgeoms;
			this.vec_ids = vec_ids;
			this.isFixed = isFixed;
			this.ctds = ctds;
			this.shape = shape;

		}
	}

	private final void setStatus(String pm) throws Exception
	{
		boolean prev = STATUS;
		STATUS = (Integer.parseInt(pm) == 1) ? true : false;
		if ((!prev) && (STATUS))
		{
			forceDataLoad = true;
		}
		if (labelPart != null)
		{
			labelPart.pluginAction("status", pm);
		}
	}

	public String pluginAction(String commandname, String command_params)
	{
		try
		{
			final Ptolemy3DUnit unit = ptolemy.unit;

			if (commandname.equalsIgnoreCase("refresh"))
			{
				forceDataLoad = true;
			}
			else if (commandname.equalsIgnoreCase("status"))
			{
				setStatus(command_params);
			}
			else if (commandname.equalsIgnoreCase("resetClasses"))
			{
				resetClasses();
				forceDataLoad = true;
			}
			else if (commandname.equalsIgnoreCase("addClass"))
			{
				addClass(command_params);
				forceDataLoad = true;
			}
			else if (commandname.equalsIgnoreCase("setClassAttribute"))
			{
				ClassAttribute = command_params;
				forceDataLoad = true;
			}
			else if (commandname.equalsIgnoreCase("getInfo"))
			{
				return "vector," + STATUS + "," + QueryWidth + "," + (DisplayRadius / unit.coordSystemRatio) + "," + DISP_MAXZ + "," + DISP_MINZ + "," + (VECTOR_RAISE / unit.coordSystemRatio) + "," +
				+(int) (stroke[0] * 255) + ":" + (int) (stroke[1] * 255) + ":" + (int) (stroke[2] * 255) + "," + LineWidth + "," + interp_len;
			}
			else if (commandname.equalsIgnoreCase("getLayerName"))
			{
				return LAYER;
			}
			else if (commandname.equalsIgnoreCase("clearSelectedPoly"))
			{
				cleanListsData();
			}
			else if (commandname.equalsIgnoreCase("setSelectedPoly"))
			{
				if (hatchedQue == null)
				{
					hatchedQue = new IntBuffer(5, 5);
				}

				StringTokenizer stok = new StringTokenizer(command_params, ",");
				while (stok.hasMoreTokens())
				{
					int gid = Integer.parseInt(stok.nextToken());
					if (vec_ids != null)
					{
						for (int i = 0; i < vec_ids.length; i++)
						{
							if (vec_ids[i] == gid)
							{
								hatchedQue.append(i);
							}
						}
					}
				}
			}
			else if (commandname.equalsIgnoreCase("setInfo"))
			{
				StringTokenizer stok = new StringTokenizer(command_params, ",");
				setStatus(stok.nextToken());
				QueryWidth = Integer.parseInt(stok.nextToken());
				DisplayRadius = (int) (Float.parseFloat(stok.nextToken()) * unit.coordSystemRatio);
				DISP_MAXZ = Integer.parseInt(stok.nextToken());
				DISP_MINZ = Integer.parseInt(stok.nextToken());
				VECTOR_RAISE = Float.parseFloat(stok.nextToken()) * unit.coordSystemRatio;
				StringTokenizer cstok = new StringTokenizer(stok.nextToken(), ":");
				stroke[0] = (float) Integer.parseInt(cstok.nextToken()) / 255;
				stroke[1] = (float) Integer.parseInt(cstok.nextToken()) / 255;
				stroke[2] = (float) Integer.parseInt(cstok.nextToken()) / 255;
				LineWidth = Float.parseFloat(stok.nextToken());
				interp_len = Float.parseFloat(stok.nextToken());
				VectorClass tmpvc;
				if ((Classes != null) && (Classes.size() > 0))
				{
					tmpvc = (Classes.elementAt(0));
					tmpvc.color = stroke;
					tmpvc.lineW = LineWidth;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public boolean onPick(double[] intersectPoint)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;
		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS) || (!clickEnabled))
		{
			return false;
		}

		int i, j, k;
		double tx, ty;
		{
			double[] out = new double[3];
			Math3D.setMapCoord(intersectPoint, out);
			ty = out[0] * unit.DD; // y
			tx = out[1] * unit.DD; // x
		}
		for (i = 0; i < numgeoms; i++)
		{
			for (j = 0; j < shape[i].length; j++)
			{
				for (k = 0; k < shape[i][j].length; k++)
				{
					if (draw_type == WKB_MULTIPOLYGON)
					{
						if (VectorNode.pointInPoly(shape[i][j][k], tx, ty, shape[i][j][k].length))
						{
							selected_id = i;
							if (selectedShapes == null)
							{
								selectedShapes = new IntBuffer(5, 5);
							}
							if (selectedShapes.find(selected_id) == -1)
							{
								cleanListsData();
								selectedShapes.append(selected_id);
								if (hatchedQue == null)
								{
									hatchedQue = new IntBuffer(5, 5);
								}
								hatchedQue.append(selected_id);
							}

							ptolemy.callJavascript("actionById", String.valueOf(vec_ids[i]), String.valueOf(LAYER), null);
							return false;
						}
					}
					else
					{
						return false;
					}
				}
			}
		}
		selected_id = -1;
		return false;
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{
		if (!STATUS) {
			return;
		}
		
   		if(WFS==true){
			loadDataWFS(forceDataLoad, JC);
		}
		else{
			loadDataServlet(forceDataLoad, JC);
		}
		forceDataLoad = false;

		if (labelPart != null) {
			labelPart.tileLoaderAction(JC);
		}
	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{
		forceDataLoad = true;
	}

	private void drawHatchedPoly(int a1, int a2, int a3, VectorClass vc)
	{
		final Landscape landscape = ptolemy.scene.landscape;

		VectorNode[] ring = shape[a1][a2][a3];

		int lspacer = 30;
		int MAX_LINES = 100;
		int i, z;
		int nPoints = ring.length;
		// find height
		int maxx, minx, minz, maxz;
		minz = maxz = ring[0].rp[1];
		maxx = minx = ring[0].rp[0];
		for (i = 1; i < nPoints; i++)
		{
			if (ring[i].rp[1] < minz) {
				minz = ring[i].rp[1];
			}
			if (ring[i].rp[1] > maxz) {
				maxz = ring[i].rp[1];
				///  added by me
			}
			if (ring[i].rp[0] < minx) {
				minx = ring[i].rp[0];
			}
			if (ring[i].rp[0] > maxx) {
				maxx = ring[i].rp[0];
			}
		}
		int numlines = 0;

		int l_count = (maxz - minz) / lspacer;
		if (l_count > MAX_LINES)
		{
			lspacer = (maxz - minz) / MAX_LINES;
			l_count = MAX_LINES;
		}
		int[][] ints = new int[l_count + 1][nPoints];
		int[] ictr = new int[l_count + 1];
		int ind1, ind2, u, v, line;
		int x1, z1, x2, z2;

		for (line = 0            , z=

			minz   ; ((z < maxz) && (line <= l_count)) ; z+=lspacer,line++) {
			ictr[line] = 0;
			for (i = 0; i < nPoints; i++)
			{
				if (i == 0)
				{
					ind1 = nPoints - 1;
					ind2 = 0;
				}
				else
				{
					ind1 = i - 1;
					ind2 = i;
				}
				z1 = ring[ind1].rp[1];
				z2 = ring[ind2].rp[1];
				if (z1 < z2)
				{
					x1 = ring[ind1].rp[0];
					x2 = ring[ind2].rp[0];
				}
				else if (z1 > z2)
				{
					z2 = ring[ind1].rp[1];
					z1 = ring[ind2].rp[1];
					x2 = ring[ind1].rp[0];
					x1 = ring[ind2].rp[0];
				}
				else
				{
					continue;
				}
				if ((z1 <= z && z < z2) || (z == maxz && z1 < z && z <= z2))
				{
					v = (z - z1) * (x2 - x1) / (z2 - z1) + x1;
					if ((ictr[line] > 0) && (v < ints[line][ictr[line] - 1]))
					{
						for (u = ictr[line]; u >= 2; u--)
						{
							ints[line][u] = ints[line][u - 1];
							if (v > ints[line][u - 2])
							{
								ints[line][u - 1] = v;
								break;
							}
						}
						if (u == 1)
						{
							ints[line][u] = ints[line][0];
							ints[line][0] = v;
						}
						ictr[line]++;
					}
					else
					{
						ints[line][ictr[line]++] = v;
					}
				}
			}

			for (int r = 0; r < (ictr[line] - 1); r += 2)
			{
				numlines += 1;
			}
		}
		// create list

		if (hatchedLists == null) {
			hatchedLists = new IntBuffer(5, 5);
		}

		int listid = gl.glGenLists(1);
		hatchedLists.append(listid);
		gl.glNewList(listid, GL.GL_COMPILE);
		gl.glColor3f(vc.color[0], vc.color[1], vc.color[2]);
		gl.glLineWidth(1.0f);
		gl.glBegin(GL.GL_LINES);
		int zp;
		double[] gpt = new double[3];
		for (line = 0, i=0, zp = minz; ((zp<=maxz) && (line <= l_count)) ;line++,zp+=lspacer){
			for(z=0; z < (ictr[line] - 1); z += 2, i += 2)
			{
				double ht;
				Math3D.setSphericalCoord(ints[line][z], zp, gpt);
				ht = landscape.groundHeight(ints[line][z], zp, 0) + VECTOR_RAISE;
				gl.glVertex3d(gpt[0] * (EARTH_RADIUS + ht), gpt[1] * (EARTH_RADIUS + ht), gpt[2] * (EARTH_RADIUS + ht));

				Math3D.setSphericalCoord(ints[line][z + 1], zp, gpt);
				ht = landscape.groundHeight(ints[line][z + 1], zp, 0) + VECTOR_RAISE;
				gl.glVertex3d(gpt[0] * (EARTH_RADIUS + ht), gpt[1] * (EARTH_RADIUS + ht), gpt[2] * (EARTH_RADIUS + ht));
			}
		}
		gl.glEnd();
		gl.glEndList();
	}

	private void deleteLists()
	{
		if (hatchedLists != null)
		{
			int hlen = hatchedLists.size();
			for (int hlptr = 0; hlptr < hlen; hlptr++)
			{
				gl.glDeleteLists(hatchedLists.get(hlptr), 1);
			}
			hatchedLists.reset();
		}
	}

	private void cleanListsData()
	{
		setDeleteLists = true;
		if (selectedShapes != null)
		{
			selectedShapes.reset();
		}
		if (hatchedQue != null)
		{
			hatchedQue.reset();
		}
	}
}