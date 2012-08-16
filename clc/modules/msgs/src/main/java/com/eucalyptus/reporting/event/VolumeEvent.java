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

package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class VolumeEvent implements Event {
  public enum VolumeAction {
    VOLUMECREATE, VOLUMEDELETE, VOLUMEATTACH, VOLUMEDETACH
  }

  private final EventActionInfo<VolumeAction> actionInfo;
  private final String uuid;
  private final String volumeId;
  private final long sizeGB;
  private final OwnerFullName ownerFullName;
  private final String availabilityZone;

  public static EventActionInfo<VolumeAction> forVolumeCreate() {
    return new EventActionInfo<VolumeAction>( VolumeAction.VOLUMECREATE );
  }

  public static EventActionInfo<VolumeAction> forVolumeDelete() {
    return new EventActionInfo<VolumeAction>( VolumeAction.VOLUMEDELETE );
  }

  public static InstanceEventActionInfo<VolumeAction> forVolumeAttach( final String instanceUuid,
                                                    final String instanceId ) {
    return new InstanceEventActionInfo<VolumeAction>( VolumeAction.VOLUMEATTACH, instanceUuid, instanceId );
  }

  public static InstanceEventActionInfo<VolumeAction> forVolumeDetach( final String instanceUuid,
                                                    final String instanceId ) {
    return new InstanceEventActionInfo<VolumeAction>( VolumeAction.VOLUMEDETACH, instanceUuid, instanceId );
  }

  public static VolumeEvent with( final EventActionInfo<VolumeAction> actionInfo,
                                  final String uuid,
                                  final String volumeId,
                                  final long sizeGB,
                                  final OwnerFullName ownerFullName,
                                  final String availabilityZone ) {
    return new VolumeEvent( actionInfo, uuid, volumeId, sizeGB, ownerFullName, availabilityZone );
  }

  private VolumeEvent( final EventActionInfo<VolumeAction> actionInfo,
                       final String uuid,
                       final String volumeId,
                       final long sizeGB,
                       final OwnerFullName ownerFullName,
                       final String availabilityZone ) {
    assertThat(actionInfo, notNullValue());
    assertThat(uuid, notNullValue());
    assertThat(sizeGB, notNullValue());
    assertThat(volumeId, notNullValue());
    assertThat(availabilityZone, notNullValue());
    assertThat(ownerFullName.getUserId(), notNullValue());
    assertThat(ownerFullName.getAccountNumber(), notNullValue());
    assertThat(ownerFullName.getUserName(), notNullValue());
    this.ownerFullName = ownerFullName;
    this.actionInfo = actionInfo;
    this.uuid = uuid;
    this.sizeGB = sizeGB;
    this.volumeId = volumeId;
    this.availabilityZone = availabilityZone;
  }

  public String getVolumeId() {
    return volumeId;
  }

  public long getSizeGB() {
    return sizeGB;
  }

  public OwnerFullName getOwner() {
    return ownerFullName;
  }

  public EventActionInfo<VolumeAction> getActionInfo() {
    return actionInfo;
  }

  public String getUuid() {
    return uuid;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  @Override
  public String toString() {
    return "VolumeEvent [actionInfo=" + actionInfo + ", uuid=" + uuid
        + ", sizeGB=" + sizeGB
        + ", ownerName=" + ownerFullName.getUserName() + ", volumeId="
        + volumeId + ", availabilityZone=" + availabilityZone + "]";
  }

}
