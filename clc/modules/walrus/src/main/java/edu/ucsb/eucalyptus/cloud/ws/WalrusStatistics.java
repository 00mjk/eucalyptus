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
 ************************************************************************/

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.entities.WalrusStatsInfo;
import edu.ucsb.eucalyptus.msgs.WalrusUsageStatsRecord;

public class WalrusStatistics {
	private Logger LOG = Logger.getLogger( WalrusStatistics.class );
	private Long totalBytesIn;
	private Long totalBytesOut;
	private Long totalSpaceUsed;
	private Integer numberOfBuckets;

	public WalrusStatistics() {
		totalBytesIn = 0L;
		totalBytesOut = 0L;
		totalSpaceUsed = 0L;
		numberOfBuckets = 0;
		getStateInfo();
		dump();
	}

	public void updateBytesIn(Long bytes) {
		totalBytesIn += bytes;
		dump();
	}

	public void updateBytesOut(Long bytes) {
		totalBytesOut += bytes;
		dump();
	}

	public void updateSpaceUsed(Long bytes) {
		totalSpaceUsed += bytes;
		updateStateInfo();
		dump();
	}

	public void incrementBucketCount() {
		numberOfBuckets++;
		updateStateInfo();
		dump();
	}

	public void decrementBucketCount() {
		numberOfBuckets--;
		updateStateInfo();
		dump();
	}

	public void resetBytesIn() {
		totalBytesIn = 0L;
	}

	public void resetBytesOut() {
		totalBytesOut = 0L;
	}

	public void dump() {
		LOG.info(WalrusUsageStatsRecord.create(totalBytesIn, totalBytesOut, numberOfBuckets, totalSpaceUsed));
	}

	private void getStateInfo() {
		EntityWrapper<WalrusStatsInfo> db = EntityWrapper.get(WalrusStatsInfo.class);
		try {
			WalrusStatsInfo walrusStats = db.getUnique(new WalrusStatsInfo(WalrusProperties.NAME));
			numberOfBuckets = walrusStats.getNumberOfBuckets();
			totalSpaceUsed = walrusStats.getTotalSpaceUsed();
		} catch(EucalyptusCloudException ex) {
			WalrusStatsInfo walrusStats = new WalrusStatsInfo(WalrusProperties.NAME,
					numberOfBuckets,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		db.commit();
	}
	
	private void updateStateInfo() {
		EntityWrapper<WalrusStatsInfo> db = EntityWrapper.get(WalrusStatsInfo.class);
		try {
			WalrusStatsInfo walrusStats = db.getUnique(new WalrusStatsInfo(WalrusProperties.NAME));
			walrusStats.setNumberOfBuckets(numberOfBuckets);
			walrusStats.setTotalSpaceUsed(totalSpaceUsed);			
		} catch(EucalyptusCloudException ex) {
			WalrusStatsInfo walrusStats = new WalrusStatsInfo(WalrusProperties.NAME,
					numberOfBuckets,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		try {
		    db.commit();
		} catch(Exception ex) {
			//log it and ignore it
			LOG.error(ex);
		}
	}
}
