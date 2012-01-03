/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import org.hibernate.annotations.Parent;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;

@Embeddable
public class VmVolumeAttachment implements Comparable<VmVolumeAttachment> {
  @Parent
  private VmInstance vmInstance;
  @Column( name = "metadata_vm_volume_id" )
  private String     volumeId;
  @Column( name = "metadata_vm_volume_device" )
  private String     device;
  @Lob
  @Column( name = "metadata_vm_volume_remove_device" )
  private String     remoteDevice;
  @Column( name = "metadata_vm_volume_status" )
  private String     status;
  @Column( name = "metadata_vm_volume_attach_time" )
  private Date       attachTime = new Date( );
  
//  @OneToOne
//  @JoinTable( name = "metadata_vm_has_volume", joinColumns = { @JoinColumn( name = "metadata_vm_id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_volume_id" ) } )
//  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
//  private Volume     volume;
  
  VmVolumeAttachment( ) {
    super( );
  }
  
  public VmVolumeAttachment( VmInstance vmInstance, String volumeId, String device, String remoteDevice, String status, Date attachTime ) {
    super( );
    this.vmInstance = vmInstance;
    this.volumeId = volumeId;
    this.device = device;
    this.remoteDevice = remoteDevice;
    this.status = status;
    this.attachTime = attachTime;
  }
  
  public static Function<AttachedVolume, VmVolumeAttachment> fromAttachedVolume( final VmInstance vm ) {
    return new Function<AttachedVolume, VmVolumeAttachment>( ) {
      @Override
      public VmVolumeAttachment apply( AttachedVolume vol ) {
        return new VmVolumeAttachment( vm, vol.getVolumeId( ), vol.getDevice( ), vol.getRemoteDevice( ), vol.getStatus( ), vol.getAttachTime( ) );
      }
    };
  }
  
  public static Function<VmVolumeAttachment, AttachedVolume> asAttachedVolume( final VmInstance vm ) {
    return new Function<VmVolumeAttachment, AttachedVolume>( ) {
      @Override
      public AttachedVolume apply( VmVolumeAttachment vol ) {
        return new AttachedVolume( vol.getVolumeId( ), vm.getInstanceId( ), vol.getDevice( ), vol.getRemoteDevice( ) );
      }
    };
  }
  
//  Volume getVolume( ) {
//    return this.volume;
//  }
  
  VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public String getVolumeId( ) {
    return this.volumeId;
  }
  
  void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }
  
  String getDevice( ) {
    return this.device;
  }
  
  void setDevice( String device ) {
    this.device = device;
  }
  
  String getRemoteDevice( ) {
    return this.remoteDevice;
  }
  
  void setRemoteDevice( String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }
  
  String getStatus( ) {
    return this.status;
  }
  
  void setStatus( String status ) {
    this.status = status;
  }
  
  Date getAttachTime( ) {
    return this.attachTime;
  }
  
  void setAttachTime( Date attachTime ) {
    this.attachTime = attachTime;
  }
  
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || !getClass( ).equals( o.getClass( ) ) ) return false;
    VmVolumeAttachment that = ( VmVolumeAttachment ) o;
    if ( this.volumeId != null
      ? !this.volumeId.equals( that.getVolumeId( ) )
      : that.getVolumeId( ) != null ) return false;
    return true;
  }
  
  public int hashCode( ) {
    return ( this.volumeId != null
      ? this.volumeId.hashCode( )
      : 0 );
  }
  
  public int compareTo( VmVolumeAttachment that ) {
    return this.volumeId.compareTo( that.getVolumeId( ) );
  }
  
  /**
   * @param instanceId
   */
  public void setInstanceId( String instanceId ) {}
  
  private void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }

  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "VmVolumeAttachment:" );
    if ( this.volumeId != null ) builder.append( "volumeId=" ).append( this.volumeId ).append( ":" );
    if ( this.device != null ) builder.append( "device=" ).append( this.device ).append( ":" );
    if ( this.remoteDevice != null ) builder.append( "remoteDevice=" ).append( this.remoteDevice ).append( ":" );
    if ( this.status != null ) builder.append( "status=" ).append( this.status ).append( ":" );
    if ( this.attachTime != null ) builder.append( "attachTime=" ).append( this.attachTime );
    return builder.toString( );
  }

  static Predicate<VmVolumeAttachment> volumeDeviceFilter( final String deviceName ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getDevice( ).replaceAll( "unknown,requested:", "" ).equals( deviceName );
      }
    };
  }

  static Predicate<VmVolumeAttachment> volumeIdFilter( final String volumeId ) {
    return new Predicate<VmVolumeAttachment>( ) {
      @Override
      public boolean apply( VmVolumeAttachment input ) {
        return input.getVolumeId( ).equals( volumeId );
      }
    };
  }
  
}
