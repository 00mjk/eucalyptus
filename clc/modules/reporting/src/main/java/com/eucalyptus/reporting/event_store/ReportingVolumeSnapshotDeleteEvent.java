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
package com.eucalyptus.reporting.event_store;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_snapshot_delete_events")
public class ReportingVolumeSnapshotDeleteEvent
	extends AbstractPersistent
{
	@Column(name="uuid", nullable=false)
	private String uuid;
	@Column(name="timestamp_ms", nullable=false)
	private Long timestampMs;
	@Column(name="username", nullable=false)
	private String username;
	@Column(name="snapshotid", nullable=false)
	private String snapshotid;
	
	protected ReportingVolumeSnapshotDeleteEvent(String uuid, String snapshotid, String username, Long timestampMs)
	{
		super();
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.snapshotid = snapshotid;
		this.username = username;
	}

	protected ReportingVolumeSnapshotDeleteEvent()
	{
		super();
		this.uuid = null;
		this.timestampMs = null;
		this.snapshotid = null;
		this.username = null;
	}

	public String getUuid()
	{
		return uuid;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public String getUsername() {
	    return username;
	}

	public String getSnapshotid() {
	    return snapshotid;
	}
	
}
