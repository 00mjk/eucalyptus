/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package edu.ucsb.eucalyptus.util;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;

import java.io.File;

public class SystemUtil {
	private static Logger LOG = Logger.getLogger(SystemUtil.class);

	public static String run(String[] command) {
		try
		{
			String commandString = "";
			for(String part : command) {
				commandString += part + " ";
			}
			LOG.debug("Running command: " + commandString);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command);
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			int returnValue = proc.waitFor();
			output.join();
			if(returnValue != 0) {
				throw new EucalyptusCloudException(error.getReturnValue());
			}
			return output.getReturnValue();
		} catch (Exception t) {
			LOG.error(t, t);
		}
		return "";
	}

	public static int runAndGetCode(String[] command) {
		try
		{
			String commandString = "";
			for(String part : command) {
				commandString += part + " ";
			}
			LOG.debug("Running command: " + commandString);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command);
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			int returnValue = proc.waitFor();
			return returnValue;
		} catch (Exception t) {
			LOG.error(t, t);
		}
		return -1;
	}

	 public static class CommandOutput {
	   public int returnValue;
	   public String output;
	   public String error;
	   public CommandOutput(int returnValue, String output, String error) {
	     this.returnValue = returnValue;
	     this.output = output;
	     this.error = error;
	   }
	 }
	 
	 public static CommandOutput runWithRawOutput(String[] command) throws Exception {
	   //System.out.println(Joiner.on(" ").skipNulls().join(command));
	   Runtime rt = Runtime.getRuntime();
	   Process proc = rt.exec(command);
	   StreamConsumer error = new StreamConsumer(proc.getErrorStream());
	   StreamConsumer output = new StreamConsumer(proc.getInputStream());
	   error.start();
	   output.start();
	   int returnValue = proc.waitFor();
	   output.join();
	   return new CommandOutput(returnValue, output.getReturnValue(), error.getReturnValue());
	 }

	public static void shutdownWithError(String errorMessage) {
		LOG.fatal(errorMessage);
		throw new IllegalStateException(errorMessage);
	}        

	public static void setEucaReadWriteOnly(String filePath) throws EucalyptusCloudException {
		File file = new File(filePath);
		try {
			file.setReadable(false, false);
			file.setWritable(false, false);
			file.setExecutable(false, false);
			file.setReadable(true, true);
			file.setWritable(true, true);
			file.setExecutable(true, true);
		} catch(SecurityException ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException(ex);
		}

	}
}
