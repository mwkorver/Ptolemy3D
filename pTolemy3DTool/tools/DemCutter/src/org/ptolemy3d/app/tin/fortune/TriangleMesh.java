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
package org.ptolemy3d.app.tin.fortune;

public class TriangleMesh{
	private MyLine setLine = new MyLine(null , null);
	public MyLine[] edges = new MyLine[20];
	public int numEdges;

	public DelTriangle[] triangles = new DelTriangle[20];
	public int numTris;

	public TriangleMesh(){
		reset();
	}

	public void reset(){
		numEdges = 0;
		numTris = 0;
	}

	public void resetTriangles(){
		for(int i=0;i<numTris;i++){
			triangles[i].reset();
		}
	}

	public void insertEdge(MyLine newline){

		if(numEdges >= edges.length){
			MyLine[] narr = new MyLine[edges.length + 20];
			System.arraycopy(edges,0,narr,0,edges.length);
			edges = narr;
		}
		edges[numEdges] = newline;

		// check to see if this edge makes a triangle
		MyLine ed1,ed2;
		for(int i=0;i<numEdges;i++){
			ed1 = edges[i];

			if(hasLikeVertex(ed1 , newline)){
				for(int j=0;j<numEdges;j++){
					if(j==i) continue;
					ed2 = edges[j];
					if(matchSetLine(ed2)){
						insertTriangle(ed1,ed2);
					}
				}
			}
		}

		numEdges++;
	}

	public void insertTriangle(MyLine e1 , MyLine e2){
		int v1,v2,v3;

		v1 = e1.P1.index;
		v2 = e1.P2.index;

		if((e1.P1.index == e2.P1.index) || (e1.P2.index == e2.P1.index))
			v3= e2.P2.index;
		else
			v3= e2.P1.index;

		DelTriangle dt2,dt1 = new DelTriangle (numTris , v1 , v2 , v3 );

		if(triangleExists(dt1)) return;

		if(numTris >= triangles.length){
			DelTriangle[] narr = new DelTriangle[triangles.length + 20];
			System.arraycopy(triangles,0,narr,0,triangles.length);
			triangles = narr;
		}
		triangles[numTris] = dt1;

		// set connectivity
		int[] simv = new int[2];
		int nT = numTris;
		int sim;
		for(int u=nT-1;u>=0;u--){
			dt2 = triangles[u];
			if (dt2 == dt1) continue;
			sim = 0;
			for(int uu=0;uu<3;uu++){
				for(int uuu=0;uuu<3;uuu++){
					if(dt1.v[uu] == dt2.v[uuu]){
						simv[sim++] = dt1.v[uu];
					}
				}
			}
			if(sim == 2){
				dt1.addPartner(dt2 , simv);
				dt2.addPartner(dt1, simv);
			}
		}

		numTris++;
	}

	public boolean hasLikeVertex(MyLine line1 , MyLine line2){
		if(line1.P1.index == line2.P1.index){
			setLine.set(line1.P2 , line2.P2);
			return true;
		}
		if(line1.P2.index == line2.P1.index){
			setLine.set(line1.P1 , line2.P2);
			return true;
		}
		if(line1.P2.index == line2.P2.index){
			setLine.set(line1.P1 , line2.P1);
			return true;
		}

		return false;
	}

	public boolean matchSetLine(MyLine line){
		if((setLine.P1.index == line.P1.index) && (setLine.P2.index == line.P2.index)) return true;
		if((setLine.P1.index == line.P2.index) && (setLine.P2.index == line.P1.index)) return true;

		return false;
	}

	public boolean triangleExists(DelTriangle dt1){
		int common;
		for(int u=numTris-1;u>=0;u--){
			common = 0;
			for(int uu=0;uu<3;uu++){
				for(int uuu=0;uuu<3;uuu++){
					if(dt1.v[uu] == triangles[u].v[uuu]){
						common++;
					}
				}
			}
			if(common == 3) return true;
		}
		return false;
	}

	public void removeEdgeTriangles(double[][] pts){

		boolean remove;
		for(int i=0;i<numTris;i++){
			remove = false;

			if(pts[ triangles[i].v[0] ][0] == pts[ triangles[i].v[1] ][0])
				if(pts[ triangles[i].v[0] ][0] == pts[ triangles[i].v[2] ][0])
				{
					remove = true;
				}

			if(pts[ triangles[i].v[0] ][1] == pts[ triangles[i].v[1] ][1])
				if(pts[ triangles[i].v[0] ][1] == pts[ triangles[i].v[2] ][1])
				{
					remove = true;
				}

			if(remove){
				triangles[i].setOff();
			}

		}
	}

	public void faceUpTriangles(double[][] pts){

		double v1x,v1y,v2x,v2y,c,mag;
		int[] v;
		int tmp;
		for(int i=0;i<numTris;i++){

			v = triangles[i].v;

			v1x = pts[ v[2] ][0] - pts[ v[0] ][0];
			v1y = pts[ v[2] ][1] - pts[ v[0] ][1];

			mag = Math.pow( v1x*v1x + v1y*v1y , 0.5);

			v1x/=mag;
			v1y/=mag;

			v2x = pts[ v[1] ][0] - pts[ v[0] ][0];
			v2y = pts[ v[1] ][1] - pts[ v[0] ][1];

			mag = Math.pow( v2x*v2x + v2y*v2y , 0.5);

			v2x/=mag;
			v2y/=mag;

			c = ((v1y * v2x) - (v1x*v2y));

			if(c > 0){
				tmp = v[1];
				v[1] = v[2];
				v[2] = tmp;
			}
		}

	}
}