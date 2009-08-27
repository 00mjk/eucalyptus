/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package com.eucalyptus.util;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.WalrusConfiguration;

public class StorageProperties {

	private static Logger LOG = Logger.getLogger( StorageProperties.class );

    public static final String SERVICE_NAME = "StorageController";
    public static String NAME = "StorageController";
    public static String SC_ID = SERVICE_NAME + UUID.randomUUID();
    public static final String EUCALYPTUS_OPERATION = "EucaOperation";
    public static final String EUCALYPTUS_HEADER = "EucaHeader";
    public static String storageRootDirectory = BaseDirectory.VAR.toString() + "/volumes";
    public static String WALRUS_URL = "http://localhost:8773/services/Walrus";
    public static int MAX_TOTAL_VOLUME_SIZE = 50;
    public static int MAX_TOTAL_SNAPSHOT_SIZE = 50;
    public static int MAX_VOLUME_SIZE = 10;
    public static int MAX_SNAPSHOT_SIZE = 10;
    public static final long GB = 1024*1024*1024;
    public static final long MB = 1024*1024;
    public static final long KB = 1024;
    public static final int TRANSFER_CHUNK_SIZE = 102400;
    public static boolean enableSnapshots = false;
    public static boolean enableStorage = false;
    public static boolean shouldEnforceUsageLimits = true;
    public static final String ETHERD_PREFIX = "/dev/etherd/e";
    public static String iface = "eth0";
    public static boolean zeroFillVolumes = false;
	public static boolean trackUsageStatistics = true;

	static {
		updateWalrusUrl();
	}

	public static void updateWalrusUrl() {
		if(!WalrusProperties.sharedMode) {
			List<WalrusConfiguration> walrusConfigs;
			try {
				walrusConfigs = Configuration.getWalrusConfigurations();
				if(walrusConfigs.size() > 0) {
					WalrusConfiguration walrusConfig = walrusConfigs.get(0);
					WALRUS_URL = walrusConfig.getUri();
					StorageProperties.enableSnapshots = true;
				}
			} catch (EucalyptusCloudException e) {
				LOG.warn("Could not obtain walrus information. Snapshot functionality may be disabled.");
				StorageProperties.enableSnapshots = false;
			}
		}
	}

	public static void update() {
		LOG.warn("update is deprecated.");
	}

	public enum Status {
		creating, available, pending, completed, failed
	}

	public enum StorageParameters {
		EucaCert, EucaSignature, EucaSnapSize
	}
}
