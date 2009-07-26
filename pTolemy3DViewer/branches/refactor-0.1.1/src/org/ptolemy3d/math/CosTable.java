/* $LICENSE$ */
package org.ptolemy3d.math;

import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.Config;

/**
 * Table to speed up cosinus/ sinus computation.<BR>
 * <BR>
 * The unit uset is DD (digital degree).<BR>
 * By default, the precision is set to 0.0004 degrees. Below this precision, the standard Math.cos is used.<BR>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CosTable {
	/*
	 * MaxLevels~4, MaxPrecision=3200, MemUsage= 215ko
	 * MaxLevels~5, MaxPrecision=1600, MemUsage= 430ko
	 * MaxLevels~5, MaxPrecision= 800, MemUsage= 860ko
	 * MaxLevels~5, MaxPrecision= 400, MemUsage=1720ko
	 * MaxLevels~ , MaxPrecision= 200, MemUsage=3440ko
	 */
	
	/** Angles values */
	private final static int angle90 = 90 * Unit.DEGREE_TO_DD_FACTOR;
	private final static int angle180 = 180 * Unit.DEGREE_TO_DD_FACTOR;
	private final static int angle360 = 360 * Unit.DEGREE_TO_DD_FACTOR;
	
	/** Pre-calculate cos */
	private static double[] cosTable;	//Memory usage (in octets): cosTable.length * 8;
	
	/** Angular precision */
	public final static int MAX_PRECISION = 400;
	
	/** If the angle precision is less than the maximum precision; use Math.cos instead.
	 * Reduce a little the speed of cos computation. */
	private final static boolean DONT_LOOSE_PRECISION = true;
	
	static {
		//Calculate the number of entries in the cosinus table
		final int numEntry = angle90 / MAX_PRECISION;

		//Fill the cos table
		cosTable = new double[numEntry + 1];
		for (int i = 0, index = 0, incr = MAX_PRECISION; i <= angle90; i += incr, index++) {
			final double angle = i * Unit.DD_TO_RADIAN;
			cosTable[index] = Math.cos(angle);
		}
	}
	
	/**
	 * @param angle in dd
	 * @return the cosinus of the angle
	 */
	public static final double cos(int angle) {
		/* To reduce the table size, we must fit to [0°;90°].
		 * Most time consumed here is due to the if test, but it considerably improve Math.cos. */
		if(DONT_LOOSE_PRECISION) {
			if((angle % MAX_PRECISION) != 0) {
				if(Config.DEBUG) {
					//org.ptolemy3d.debug.IO.printfRenderer("Loose of precision: %d\n", (angle / MAX_PRECISION));
				}
				return Math.cos(angle * Unit.DD_TO_RADIAN);
			}
		}
		if(angle < 0) {
			angle = -angle;
		}
		if(angle > angle180) {
			angle = angle360-angle;
		}
		if(angle > angle90) {
			return -cosTable[(angle180-angle) / MAX_PRECISION];
		}
		else {
			return cosTable[angle / MAX_PRECISION];
		}
	}
	
	/**
	 * @param angle in dd
	 * @return the sinus of the angle
	 */
	public static final double sin(int angle) {
		return cos(angle90 - angle);
	}
	
	/** @return the memory usage of the internal cos table, in bytes */
	public static int getCosTableUsage() {
		return cosTable.length * 8;
	}
}
