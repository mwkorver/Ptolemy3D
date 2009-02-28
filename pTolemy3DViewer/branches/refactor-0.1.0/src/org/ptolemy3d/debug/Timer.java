/**
 * Graphic Engine
 * Copyright © 2004-2008 Jérôme JOUVIE (Jouvieje)
 * 
 * PROJECT INFORMATIONS
 * ====================
 * Author   Jérôme JOUVIE (Jouvieje)
 * Email    jerome.jouvie@gmail.com
 * Site     http://jerome.jouvie.free.fr/
 * Homepage http://jerome.jouvie.free.fr/OpenGl/Projects/GraphicEngineCore.php
 * Version  GraphicEngineCore v0.1.5 Build 16-11-2008
 * 
 * LICENSE
 * =======
 * 
 * GNU GENERAL PUBLIC LICENSE (GPL)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.ptolemy3d.debug;

class Timer
{
	/*
	 * Time passed calculation
	 */
	private long timePassedNanos = 0;
	private long lastTime        = -1;		//Previous time

	/*
	 * FPS calculation
	 */
	private float fps = 0;
	private int frames = 0;
	private long firstFrameTime = 0;
	public int FPS_REFRESH_TIME_NANOS = 500 * 1000 * 1000;	//each 500ms
	
	public Timer(){}
	
	/* Timer interface */
	
	public final long getTimeNanos()
	{
		return System.nanoTime();
	}
	
	public final long getTimeMicros()
	{
		return getTimeNanos() / 1000;
	}
	
	public final long getTimeMillis()
	{
		return getTimeNanos() / 1000000;
	}
	
	/* Counter interface */

	/**
	 * This method calculates the time that OpenGl takes to draw frames.
	 */
	public final void update()
	{
		if(lastTime == -1) {
			//Initialization of the counter
			lastTime = getTimeNanos();
			timePassedNanos = 0;

			//Initialization for FPS calculation
			fps = 0;
			frames = 0;
			firstFrameTime = lastTime;
		}
		else {
			//Get the current time
			long currentTime = getTimeNanos();
			//Time passed
			timePassedNanos = currentTime-lastTime;
			//Update last time, it is now the current for next frame calculation
			lastTime = currentTime;

			//FPS
			{
				//update frame count for the fps counter
				frames++;

				//Calculate fps
				long dt = currentTime-firstFrameTime;
				if(dt >= FPS_REFRESH_TIME_NANOS)
				{
					fps = (float)(1000*frames)/(float)(dt / 1000000);
					frames = 0;
					firstFrameTime = currentTime;
				}
			}
		}
	}

	/**
	 * Get the time to draw last frame
	 * @return the time in milliseconds that the last frame takes to be drawn
	 */
	public final long getTimePassedMillis()
	{
		return getTimePassedNanos() / 1000000;
	}
	public final long getTimePassedMicros()
	{
		return getTimePassedNanos() / 1000;
	}
	public final long getTimePassedNanos()
	{
		return timePassedNanos;
	}

	/**
	 * @return the number of frames per seconds
	 * @see #isFPSEnabled
	 */
	public final float getFPS()
	{
		return fps;
	}

	/**
	 * Stop the counter and
	 */
	public void reset()
	{
		//reset time

		lastTime = -1;
		timePassedNanos = 0;

		fps = 0;
		frames = 0;
		firstFrameTime = lastTime;
	}
}