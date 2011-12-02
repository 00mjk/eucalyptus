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

package com.eucalyptus.cloud;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.cloud.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.NoSuchTokenException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import edu.ucsb.eucalyptus.msgs.StartInstancesType;

public class ResourceToken implements VmInstanceMetadata, Comparable<ResourceToken> {
  private static Logger       LOG    = Logger.getLogger( ResourceToken.class );
  private final Allocation    allocation;
  private final Integer       launchIndex;
  private final String        instanceId;
  private final String        instanceUuid;
  @Nullable
  private Address             address;
  @Nullable
  private ExtantNetwork       extantNetwork;
  @Nullable
  private PrivateNetworkIndex networkIndex;
  private final Date          creationTime;
  private final Integer       resourceAllocationSequenceNumber;
  private final Integer       amount = 1;
  private final Date          expirationTime;
  @Nullable
  private VmInstance          vmInst;
  
  public ResourceToken( final Allocation allocInfo, final int resourceAllocationSequenceNumber, final int launchIndex ) {
    this.allocation = allocInfo;
    Contract<Date> expiry = allocInfo.getContext( ).getContracts( ).get( Contract.Type.EXPIRATION );
    this.expirationTime = ( expiry == null ? new Date( 32503708800000l ) : expiry.getValue( ) );
    this.launchIndex = launchIndex;
    String tempVmId = VmInstances.getId( allocInfo.getReservationIndex( ), launchIndex );
    try {//GRZE:ugly hack.
      if ( Contexts.lookup( ).getRequest( ) instanceof StartInstancesType ) {
        tempVmId = ( ( StartInstancesType ) Contexts.lookup( ).getRequest( ) ).getInstancesSet( ).get( launchIndex );
      }
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    this.instanceId = tempVmId;
    this.instanceUuid = UUID.randomUUID( ).toString( );
    this.resourceAllocationSequenceNumber = resourceAllocationSequenceNumber;
    this.creationTime = Calendar.getInstance( ).getTime( );
  }
  
  public Allocation getAllocationInfo( ) {
    return this.allocation;
  }
  
  public String getInstanceId( ) {
    return this.instanceId;
  }
  
  public Address getAddress( ) {
    return this.address;
  }
  
  public Integer getAmount( ) {
    return this.amount;
  }
  
  public Date getCreationTime( ) {
    return this.creationTime;
  }
  
  public Integer getSequenceNumber( ) {
    return this.resourceAllocationSequenceNumber;
  }
  
  @Override
  public int compareTo( final ResourceToken that ) {
    return this.resourceAllocationSequenceNumber - that.resourceAllocationSequenceNumber;
  }
  
  public String getInstanceUuid( ) {
    return this.instanceUuid;
  }
  
  public PrivateNetworkIndex getNetworkIndex( ) {
    return this.networkIndex != null
      ? this.networkIndex
      : PrivateNetworkIndex.bogus( );
  }
  
  public Integer getLaunchIndex( ) {
    return this.launchIndex;
  }
  
  public void abort( ) {
    try {
      final ServiceConfiguration config = Topology.lookup( ClusterController.class, this.getAllocationInfo( ).getPartition( ) );
      final Cluster cluster = Clusters.lookup( config );
      cluster.getNodeState( ).releaseToken( this );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
    if ( this.networkIndex != null ) {
      try {
        this.networkIndex.release( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    if ( this.address != null ) {
      try {
        this.address.release( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    if ( this.vmInst != null ) {
      try {
        this.vmInst.release( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.instanceUuid == null )
      ? 0
      : this.instanceUuid.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    ResourceToken other = ( ResourceToken ) obj;
    if ( this.instanceUuid == null ) {
      if ( other.instanceUuid != null ) {
        return false;
      }
    } else if ( !this.instanceUuid.equals( other.instanceUuid ) ) {
      return false;
    }
    return true;
  }
  
  static Logger getLOG( ) {
    return LOG;
  }
  
  Allocation getAllocation( ) {
    return this.allocation;
  }
  
  Integer getResourceAllocationSequenceNumber( ) {
    return this.resourceAllocationSequenceNumber;
  }
  
  public void submit( ) throws NoSuchTokenException {
    Clusters.lookup( Topology.lookup( ClusterController.class, this.getAllocationInfo( ).getPartition( ) ) ).getNodeState( ).submitToken( this );
  }
  
  public void redeem( ) throws NoSuchTokenException {
    Clusters.lookup( Topology.lookup( ClusterController.class, this.getAllocationInfo( ).getPartition( ) ) ).getNodeState( ).redeemToken( this );
  }
  
  public void release( ) throws NoSuchTokenException {
    Clusters.lookup( Topology.lookup( ClusterController.class, this.getAllocationInfo( ).getPartition( ) ) ).getNodeState( ).releaseToken( this );
  }
  
  public void setNetworkIndex( PrivateNetworkIndex networkIndex ) {
    this.networkIndex = networkIndex;
  }
  
  public void setAddress( Address address ) {
    this.address = address;
  }
  
  public void setExtantNetwork( ExtantNetwork exNet ) {
    this.extantNetwork = exNet;
  }
  
  public ExtantNetwork getExtantNetwork( ) {
    return this.extantNetwork;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "ResourceToken:" );
    if ( this.instanceId != null ) {
      builder.append( this.instanceId ).append( ":" );
    }
    if ( this.address != null ) {
      builder.append( this.address.getName( ) ).append( ":" );
    }
    if ( this.extantNetwork != null ) {
      builder.append( "tag=" ).append( this.extantNetwork.getTag( ) ).append( ":" );
    }
    if ( this.networkIndex != null ) {
      builder.append( "idx=" ).append( this.networkIndex.getIndex( ) );
    }
    return builder.toString( );
  }
  
  public void setVmInstance( VmInstance vmInst ) {
    this.vmInst = vmInst;
  }
  
  public VmInstance getVmInstance( ) {
    return this.vmInst;
  }
  
  @Override
  public String getDisplayName( ) {
    return this.getInstanceId( );
  }
  
  @Override
  public OwnerFullName getOwner( ) {
    return this.allocation.getOwnerFullName( );
  }

  public Date getExpirationTime( ) {
    return this.expirationTime;
  }
  
}
