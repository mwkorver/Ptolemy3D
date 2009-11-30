/**
 * visual-fortune-algorithm
 * http://github.com/sorbits/visual-fortune-algorithm/tree/master
 *
 * ABOUT
 * This Java applet implements a visualization of Fortunes plane-sweep algorithm
 * for creating a voronoi diagram.
 * Here is a live demo of the applet: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 * The applet was created by Benny Kjor Nielsen and Allan Odgaard in spring of 2000
 * following a course in Computational Geometry taught by Pawel Winter at DIKU.
 *
 * LICENSE
 * Permission to copy, use, modify, sell and distribute this software is granted.
 * This software is provided as is without express or implied warranty,
 * and with no claim as to its suitability for any purpose.
 *
 * NOTES
 * The purpose of the source is to visualize the algorithm, it is not a good base
 * for an efficient implementation of the algorithm (it does not run in O(n log n) time).
 * The original source was initially lost and recovered using a Java decompiler
 * so most variable names are nonsensical.
 */
package fortune;

public class DelTriangle{
	public int[] v;
	public int conn = 0;
	public int numpartners = 0;
	public boolean striped = false;
	public int id = 0;

	public DelTriangle[] partners = new DelTriangle[3];
	public int[] partner_ov = new int[3];
	
	private boolean on = true;

	public DelTriangle(int id , int v1 , int v2 , int v3 ){
		this.v = new int[3];
		this.id = id;
		v[0] = v1;
		v[1] = v2;
		v[2] = v3;
	}
	
	public void reset(){
		if(on){
			conn = numpartners;
			striped = false;
		}
	}
	
	public boolean edgeMatch(int v1 , int v2){
		if(  ((v[0] == v1) && (v[1] == v2)) ||
			((v[1] == v1) && (v[2] == v2)) ||
			((v[2] == v1) && (v[0] == v2))   )
				return true;
		else return false;		
	}
	
	public void setOff(){
		on = false;
		striped = true;
		decrementAllPartners();
	}

	public boolean match(int v1 , int v2 , int v3){
		int mct = 0;
		for(int i=0;i<3;i++){
			if(v1 == v[i]) mct++;
			else if(v2 == v[i]) mct++;
			else if(v3 == v[i]) mct++;
		}

		if(mct == 3) return true;
		else return false;
	}

	public void addPartner(DelTriangle dt, int[] simv){
		for(int i=0;i<conn;i++){
			if(dt == partners[i]) return;
		}

		if(conn >= partners.length){
			DelTriangle[] npart = new DelTriangle[partners.length + 3];
			System.arraycopy(partners , 0 , npart , 0 , partners.length);
			partners = npart;

			int[] n_ov = new int[partner_ov.length + 3];
			System.arraycopy(partner_ov , 0 , n_ov , 0 , partner_ov.length);
			partner_ov = n_ov;
		}

		for(int i=0;i<3;i++){
			if((v[i] != simv[0]) && (v[i] != simv[1])){
				partner_ov[conn] = v[i];
				break;
			}
		}
		partners[conn++] = dt;
		numpartners++;
	}

	public int setOddVert(DelTriangle dt){
		int ov = 0;
		if(conn > 0)conn--;

		for(int i=0;i<numpartners;i++){
			if(dt == partners[i]) ov = partner_ov[i];
			else{
				partners[i].conn--;
			}
		}

		return ov;
	}
	
	public int setOddVertLp(DelTriangle dt){
		int ov = 0;
		for(int i=0;i<numpartners;i++){
			if(dt == partners[i]) ov = partner_ov[i];
		}
		return ov;
	}
	
	public void decrementAllPartners(){
		for(int i=0;i<numpartners;i++){
			partners[i].conn--;
		}
	}
	
}
