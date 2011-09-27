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
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster.callback;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.VmRunType;
import com.eucalyptus.cluster.NoSuchTokenException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;

public class VmRunCallback extends MessageCallback<VmRunType, VmRunResponseType> {
  
  private static Logger       LOG = Logger.getLogger( VmRunCallback.class );
  
  private final ResourceToken token;
  
  public VmRunCallback( final VmRunType msg, final ResourceToken token ) {
    super( msg );
    this.token = token;
  }
  
  @Override
  public void initialize( final VmRunType msg ) {
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance vm = VmInstances.lookup( msg.getInstanceId( ) );
      msg.setUserId( vm.getOwnerUserId( ) );
      msg.setOwnerId( vm.getOwnerUserId( ) );
      msg.setAccountId( vm.getOwnerAccountNumber( ) );
      if ( !VmState.PENDING.equals( vm.getState( ) ) ) {
        throw new EucalyptusClusterException( "Intercepted a RunInstances request for an instance which has meanwhile been terminated." );
      }
      try {
        this.token.submit( );
      } catch ( final NoSuchTokenException e2 ) {
        LOG.debug( e2, e2 );
      }
      db.commit( );
    } catch ( final Exception e ) {
      db.rollback( );
      LOG.debug( e, e );
      throw new EucalyptusClusterException( "Error while initializing request state: " + this.getRequest( ), e );
    }
  }
  
  @Override
  public void fire( final VmRunResponseType reply ) {
    if ( !reply.get_return( ) ) {
      throw new EucalyptusClusterException( "Failed to run instance: " + this.getRequest( ).getInstanceId( ) );
    }
    Logs.extreme( ).error( reply );
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      this.token.redeem( );
      for ( final VmInfo vmInfo : reply.getVms( ) ) {
        final VmInstance vm = VmInstances.lookup( vmInfo.getInstanceId( ) );
        vm.updateAddresses( vmInfo.getNetParams( ).getIpAddress( ), vmInfo.getNetParams( ).getIgnoredPublicIp( ) );
      }
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new EucalyptusClusterException( "Failed to run instance: " + this.getRequest( ).getInstanceId( ) + " because of: " + ex.getMessage( ), ex );
    }
  }
  
  @Override
  public void fireException( final Throwable e ) {
    
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      LOG.debug( "-> Release resource tokens for unused resources." );
      try {
        this.token.release( );
      } catch ( final Exception ex ) {
        LOG.error( ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      final Address addr = this.token.getAddress( );
      LOG.debug( "-> Release addresses from failed vm run allocation: " + addr );
      try {
        
        addr.release( );
      } catch ( final Exception ex ) {
        LOG.error( ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      LOG.debug( LogUtil.header( "Failing run instances because of: " + e.getMessage( ) ), e );
      LOG.debug( LogUtil.subheader( this.getRequest( ).toString( ) ) );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.commit( );
    }
  }
}
