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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.run.AdmissionControl;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.run.VerifyMetadata;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstance.VmState;
import com.eucalyptus.cluster.VmInstance.VmStateSet;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.BundleCallback;
import com.eucalyptus.cluster.callback.CancelBundleCallback;
import com.eucalyptus.cluster.callback.ConsoleOutputCallback;
import com.eucalyptus.cluster.callback.PasswordDataCallback;
import com.eucalyptus.cluster.callback.RebootCallback;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.BundleInstanceChecker;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Request;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.CreatePlacementGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreatePlacementGroupType;
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DeletePlacementGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeletePlacementGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteTagsResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DescribePlacementGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePlacementGroupsType;
import edu.ucsb.eucalyptus.msgs.DescribeTagsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeTagsType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputResponseType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataResponseType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataType;
import edu.ucsb.eucalyptus.msgs.ModifyInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.MonitorInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.MonitorInstancesType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.ResetInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.StartInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.StartInstancesType;
import edu.ucsb.eucalyptus.msgs.StopInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.StopInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.msgs.UnmonitorInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.UnmonitorInstancesType;

public class VmControl {
  
  private static Logger LOG = Logger.getLogger( VmControl.class );
  
  public Allocation allocate( final Allocation allocInfo ) throws EucalyptusCloudException {
    return allocInfo;
  }
  
  public DescribeInstancesResponseType describeInstances( final DescribeInstancesType msg ) throws EucalyptusCloudException {
    final DescribeInstancesResponseType reply = ( DescribeInstancesResponseType ) msg.getReply( );
    final Context ctx = Contexts.lookup( );
    final boolean isAdmin = ctx.hasAdministrativePrivileges( );
    final boolean isVerbose = msg.getInstancesSet( ).remove( "verbose" );
    final ArrayList<String> instancesSet = msg.getInstancesSet( );
    final Map<String, ReservationInfoType> rsvMap = new HashMap<String, ReservationInfoType>( );
    Predicate<VmInstance> privileged = RestrictedTypes.filterPrivileged( );
    try {
      for ( final VmInstance vm : Iterables.filter( VmInstances.listValues( ), privileged ) ) {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance v = Entities.merge( vm );
          if ( VmState.TERMINATED.apply( v ) && v.getSplitTime( ) > VmInstances.SHUT_DOWN_TIME ) {
            VmInstances.terminate( v );
          } else if ( VmState.BURIED.apply( v ) && v.getSplitTime( ) > VmInstances.BURY_TIME ) {
            VmInstances.delete( v );
          }
          if ( VmState.BURIED.apply( v ) && !isVerbose ) {
            continue;
          }
          if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) {
            continue;
          }
          if ( rsvMap.get( v.getReservationId( ) ) == null ) {
            final ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwner( ).getNamespace( ), v.getNetworkNames( ) );
            rsvMap.put( reservation.getReservationId( ), reservation );
          }
          rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( VmInstances.transform( v ) );
          db.commit( );
        } catch ( Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
          try {
            rsvMap.get( vm.getReservationId( ) ).getInstancesSet( ).add( VmInstances.transform( vm ) );
          } catch ( Exception ex1 ) {
            LOG.error( ex1 , ex1 );
          }
        }
      }
      ArrayList<ReservationInfoType> vms = new ArrayList<ReservationInfoType>( rsvMap.values( ) );
      reply.setReservationSet( vms );
    } catch ( final Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }
  
  public TerminateInstancesResponseType terminateInstances( final TerminateInstancesType request ) throws EucalyptusCloudException {
    final TerminateInstancesResponseType reply = request.getReply( );
    try {
      final Context ctx = Contexts.lookup( );
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Iterables.all( request.getInstancesSet( ), new Predicate<String>( ) {
        @Override
        public boolean apply( final String instanceId ) {
          EntityTransaction db = Entities.get( VmInstance.class );
          try {
            VmInstance vm = null;
            try {
              try {
                vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.Lookup.INSTANCE );
              } catch ( NoSuchElementException ex ) {
                vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.Lookup.TERMINATED );
              }
              final int oldCode = vm.getState( ).getCode( ), newCode = VmState.SHUTTING_DOWN.getCode( );
              final String oldState = vm.getState( ).getName( ), newState = VmState.SHUTTING_DOWN.getName( );
              if ( VmStateSet.DONE.apply( vm ) ) {
                VmInstances.delete( vm );
              } else {
                VmInstances.terminate( vm );
              }
              results.add( new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState ) );
              db.commit( );
              return true;
            } catch ( final NoSuchElementException e ) {
              db.rollback( );
              return false;
            }
          } catch ( AuthException ex ) {
            db.rollback( );
            throw new AccessControlException( "Not authorized to terminate instance: " + instanceId + " because of: " + ex.getMessage( ) );
          } catch ( IllegalContextAccessException ex ) {
            db.rollback( );
            throw new RuntimeException( "Failed to terminate instance: " + instanceId + " becuase of: " + ex.getMessage( ), ex );
          } catch ( PersistenceException ex ) {
            db.rollback( );
            throw new RuntimeException( "Failed to terminate instance: " + instanceId + " becuase of: " + ex.getMessage( ), ex );
          } catch ( Exception ex ) {
            Logs.exhaust( ).error( ex, ex );
            db.rollback( );
            throw new RuntimeException( "Failed to terminate instance: " + instanceId + " becuase of: " + ex.getMessage( ), ex );
          }
        }
      } );
      reply.set_return( !reply.getInstancesSet( ).isEmpty( ) );
      return reply;
    } catch ( final Throwable e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public RebootInstancesResponseType rebootInstances( final RebootInstancesType request ) throws EucalyptusCloudException {
    final RebootInstancesResponseType reply = ( RebootInstancesResponseType ) request.getReply( );
    try {
      final Context ctx = Contexts.lookup( );
      final boolean result = Iterables.any( request.getInstancesSet( ), new Predicate<String>( ) {
        @Override
        public boolean apply( final String instanceId ) {
          try {
            final VmInstance v = VmInstances.lookup( instanceId );
            if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, instanceId, v.getOwner( ) ) ) {
              final Request<RebootInstancesType, RebootInstancesResponseType> req = AsyncRequests.newRequest( new RebootCallback( v.getInstanceId( ) ) );
              req.getRequest( ).regarding( request );
              req.dispatch( v.lookupClusterConfiguration( ) );
              return true;
            } else {
              return false;
            }
          } catch ( final NoSuchElementException e ) {
            return false;
          }
        }
      } );
      reply.set_return( result );
      return reply;
    } catch ( final Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public void getConsoleOutput( final GetConsoleOutputType request ) throws EucalyptusCloudException {
    VmInstance v = null;
    try {
      v = VmInstances.lookup( request.getInstanceId( ) );
    } catch ( final NoSuchElementException e2 ) {
      try {
        v = VmInstances.lookup( request.getInstanceId( ) );
        final GetConsoleOutputResponseType reply = request.getReply( );
        reply.setInstanceId( request.getInstanceId( ) );
        reply.setTimestamp( new Date( ) );
        reply.setOutput( v.getConsoleOutputString( ) );
        ServiceContext.response( reply );
      } catch ( final NoSuchElementException ex ) {
        throw new EucalyptusCloudException( "No such instance: " + request.getInstanceId( ) );
      }
    }
    if ( !RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, request.getInstanceId( ), v.getOwner( ) ) ) {
      throw new EucalyptusCloudException( "Permission denied for vm: " + request.getInstanceId( ) );
    } else if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
      final GetConsoleOutputResponseType reply = request.getReply( );
      reply.setInstanceId( request.getInstanceId( ) );
      reply.setTimestamp( new Date( ) );
      reply.setOutput( v.getConsoleOutputString( ) );
      ServiceContext.response( reply );
    } else {
      Cluster cluster = null;
      try {
        cluster = Clusters.getInstance( ).lookup( v.lookupPartition( ) );
      } catch ( final NoSuchElementException e1 ) {
        throw new EucalyptusCloudException( "Failed to find cluster info for '" + v.lookupPartition( ) + "' related to vm: " + request.getInstanceId( ) );
      }
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
      AsyncRequests.newRequest( new ConsoleOutputCallback( request ) ).dispatch( cluster.getConfiguration( ) );
    }
  }
  
  public DescribeBundleTasksResponseType describeBundleTasks( final DescribeBundleTasksType request ) throws EucalyptusCloudException {
    final Context ctx = Contexts.lookup( );
    final DescribeBundleTasksResponseType reply = request.getReply( );
    if ( request.getBundleIds( ).isEmpty( ) ) {
      for ( final VmInstance v : Iterables.filter( VmInstances.listValues( ), VmInstance.Filters.BUNDLING ) ) {
        if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, v.getInstanceId( ), v.getOwner( ) ) ) {
          reply.getBundleTasks( ).add( v.getBundleTask( ) );
        }
      }
    } else {
      for ( final String bundleId : request.getBundleIds( ) ) {
        try {
          final VmInstance v = VmInstances.lookupByBundleId( bundleId );
          if ( v.isBundling( )
               && ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, v.getInstanceId( ), v.getOwner( ) ) ) ) {
            reply.getBundleTasks( ).add( v.getBundleTask( ) );
          }
        } catch ( final NoSuchElementException e ) {}
      }
    }
    return reply;
  }
  
  public UnmonitorInstancesResponseType unmonitorInstances( final UnmonitorInstancesType request ) {
    final UnmonitorInstancesResponseType reply = request.getReply( );
    return reply;
  }
  
  public StartInstancesResponseType startInstances( final StartInstancesType request ) throws EucalyptusCloudException {
    Context ctx = Contexts.lookup( );
    final StartInstancesResponseType reply = request.getReply( );
    for ( String instanceId : request.getInstancesSet( ) ) {
      VmInstance vm = null;
      try {
        vm = VmInstances.lookup( instanceId );
      } catch ( NoSuchElementException ex ) {
        try {
          vm = Transactions.find( VmInstance.named( ctx.getUserFullName( ), instanceId ) );
        } catch ( Exception ex1 ) {
          throw new EucalyptusCloudException( "Failed to locate instance information for instance id: " + instanceId );
        }
        final VmInstance v = vm;
        try {
          RunInstancesType runRequest = new RunInstancesType( ) {
            {
              this.setMinCount( 1 );
              this.setMaxCount( 1 );
              this.setImageId( v.getImageId( ) );
              this.setAvailabilityZone( v.getPartition( ) );
              this.getGroupSet( ).addAll( v.getNetworkNames( ) );
              this.setInstanceType( v.getVmType( ).getName( ) );
            }
          };
          Allocation allocInfo = VerifyMetadata.handle( runRequest );
          allocInfo = AdmissionControl.handle( allocInfo );
          final int oldCode = v.getState( ).getCode( ), newCode = VmState.SHUTTING_DOWN.getCode( );
          final String oldState = v.getState( ).getName( ), newState = VmState.SHUTTING_DOWN.getName( );
          reply.getInstancesSet( ).add( new TerminateInstancesItemType( v.getInstanceId( ), oldCode, oldState, newCode, newState ) );
        } catch ( MetadataException ex1 ) {
          LOG.error( ex1, ex1 );
        }
      }
    }
    return reply;
  }
  
  public StopInstancesResponseType stopInstances( final StopInstancesType request ) throws EucalyptusCloudException {
    final StopInstancesResponseType reply = request.getReply( );
    try {
      final Context ctx = Contexts.lookup( );
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Iterables.all( request.getInstancesSet( ), new Predicate<String>( ) {
        @Override
        public boolean apply( final String instanceId ) {
          try {
            final VmInstance v = VmInstances.lookup( instanceId );
            if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, instanceId, v.getOwner( ) ) ) {
              final int oldCode = v.getState( ).getCode( ), newCode = VmState.SHUTTING_DOWN.getCode( );
              final String oldState = v.getState( ).getName( ), newState = VmState.SHUTTING_DOWN.getName( );
              results.add( new TerminateInstancesItemType( v.getInstanceId( ), oldCode, oldState, newCode, newState ) );
              if ( VmState.RUNNING.equals( v.getState( ) ) || VmState.PENDING.equals( v.getState( ) ) ) {
                v.setState( VmState.STOPPING, Reason.USER_STOPPED );
              }
            }
            return true;
          } catch ( final NoSuchElementException e ) {
            try {
              VmInstances.terminate( instanceId );
              return true;
            } catch ( final NoSuchElementException e1 ) {
              return false;
            }
          }
        }
      } );
      reply.set_return( !reply.getInstancesSet( ).isEmpty( ) );
      return reply;
    } catch ( final Throwable e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public ResetInstanceAttributeResponseType resetInstanceAttribute( final ResetInstanceAttributeType request ) {
    final ResetInstanceAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public MonitorInstancesResponseType monitorInstances( final MonitorInstancesType request ) {
    final MonitorInstancesResponseType reply = request.getReply( );
    return reply;
  }
  
  public ModifyInstanceAttributeResponseType modifyInstanceAttribute( final ModifyInstanceAttributeType request ) {
    final ModifyInstanceAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribeTagsResponseType describeTags( final DescribeTagsType request ) {
    final DescribeTagsResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribePlacementGroupsResponseType describePlacementGroups( final DescribePlacementGroupsType request ) {
    final DescribePlacementGroupsResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribeInstanceAttributeResponseType describeInstanceAttribute( final DescribeInstanceAttributeType request ) {
    final DescribeInstanceAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public DeleteTagsResponseType deleteTags( final DeleteTagsType request ) {
    final DeleteTagsResponseType reply = request.getReply( );
    return reply;
  }
  
  public DeletePlacementGroupResponseType deletePlacementGroup( final DeletePlacementGroupType request ) {
    final DeletePlacementGroupResponseType reply = request.getReply( );
    return reply;
  }
  
  public CreateTagsResponseType createTags( final CreateTagsType request ) {
    final CreateTagsResponseType reply = request.getReply( );
    return reply;
  }
  
  public CreatePlacementGroupResponseType createPlacementGroup( final CreatePlacementGroupType request ) {
    final CreatePlacementGroupResponseType reply = request.getReply( );
    return reply;
  }
  
  public CancelBundleTaskResponseType cancelBundleTask( final CancelBundleTaskType request ) throws EucalyptusCloudException {
    final CancelBundleTaskResponseType reply = request.getReply( );
    reply.set_return( true );
    final Context ctx = Contexts.lookup( );
    try {
      final VmInstance v = VmInstances.lookupByBundleId( request.getBundleId( ) );
      if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, v.getInstanceId( ), v.getOwner( ) ) ) {
        v.getBundleTask( ).setState( "canceling" );
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELING, ctx.getUserFullName( ).toString( ), v.getBundleTask( ).getBundleId( ),
                                    v.getInstanceId( ) ) );
        
        final Cluster cluster = Clusters.lookup( v.lookupPartition( ) );
        
        request.setInstanceId( v.getInstanceId( ) );
        reply.setTask( v.getBundleTask( ) );
        AsyncRequests.newRequest( new CancelBundleCallback( request ) ).dispatch( cluster.getConfiguration( ) );
        return reply;
      } else {
        throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
      }
    } catch ( final NoSuchElementException e ) {
      throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
    }
  }
  
  public BundleInstanceResponseType bundleInstance( final BundleInstanceType request ) throws EucalyptusCloudException {
    final BundleInstanceResponseType reply = request.getReply( );//TODO: check if the instance has platform windows.
    reply.set_return( true );
    Component walrus = Components.lookup( Walrus.class );
    NavigableSet<ServiceConfiguration> configs = walrus.lookupServiceConfigurations( );
    if ( configs.isEmpty( ) || !Component.State.ENABLED.equals( configs.first( ).lookupState( ) ) ) {
      throw new EucalyptusCloudException( "Failed to bundle instance because there is no available walrus service at the moment." );
    }
    final String walrusUrl = configs.first( ).getUri( ).toASCIIString( );
    final String instanceId = request.getInstanceId( );
    final Context ctx = Contexts.lookup( );
    final User user = ctx.getUser( );
    
    try {
      final VmInstance v = VmInstances.lookup( instanceId );
      if ( v.isBundling( ) ) {
        reply.setTask( v.getBundleTask( ) );
        return reply;
      } else if ( !"windows".equals( v.getPlatform( ) ) ) {
        throw new EucalyptusCloudException( "Failed to bundle requested vm because the platform is not 'windows': " + request.getInstanceId( ) );
      } else if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new EucalyptusCloudException( "Failed to bundle requested vm because it is not currently 'running': " + request.getInstanceId( ) );
      } else if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, instanceId, v.getOwner( ) ) ) {
        BundleInstanceChecker.check( request );
        final BundleTask bundleTask = new BundleTask( v.getInstanceId( ).replaceFirst( "i-", "bun-" ), v.getInstanceId( ), request.getBucket( ),
                                                      request.getPrefix( ) );
        if ( v.startBundleTask( bundleTask ) ) {
          reply.setTask( bundleTask );
        } else if ( v.getBundleTask( ) == null ) {
          v.resetBundleTask( );
          v.startBundleTask( bundleTask );
          reply.setTask( bundleTask );
        } else {
          throw new EucalyptusCloudException( "Instance is already being bundled: " + v.getBundleTask( ).getBundleId( ) );
        }
        LOG.info( EventRecord
                             .here( BundleCallback.class, EventType.BUNDLE_PENDING, ctx.getUserFullName( ).toString( ), v.getBundleTask( ).getBundleId( ),
                                    v.getInstanceId( ) ) );
        final BundleCallback callback = new BundleCallback( request );
        request.setUrl( walrusUrl );
        request.setAwsAccessKeyId( Accounts.getFirstActiveAccessKeyId( user ) );
        AsyncRequests.newRequest( callback ).dispatch( v.lookupClusterConfiguration( ) );
        return reply;
      } else {
        throw new EucalyptusCloudException( "Failed to find instance: " + request.getInstanceId( ) );
      }
    } catch ( EucalyptusCloudException e ) {
      throw e;
    } catch ( final Exception e ) {
      throw new EucalyptusCloudException( "Failed to find instance: " + request.getInstanceId( ) );
    }
  }
  
  public void getPasswordData( final GetPasswordDataType request ) throws Exception {
    try {
      final Context ctx = Contexts.lookup( );
      Cluster cluster = null;
      final VmInstance v = VmInstances.lookup( request.getInstanceId( ) );
      if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " is not in a running state." );
      }
      if ( RestrictedTypes.checkPrivilege( request, PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, request.getInstanceId( ), v.getOwner( ) ) ) {
        cluster = Clusters.lookup( v.lookupClusterConfiguration( ) );
      } else {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " does not exist." );
      }
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
      if ( v.getPasswordData( ) == null ) {
        AsyncRequests.newRequest( new PasswordDataCallback( request ) ).dispatch( cluster.getConfiguration( ) );
      } else {
        final GetPasswordDataResponseType reply = request.getReply( );
        reply.set_return( true );
        reply.setOutput( v.getPasswordData( ) );
        reply.setTimestamp( new Date( ) );
        reply.setInstanceId( v.getInstanceId( ) );
        ServiceContext.dispatch( "ReplyQueue", reply );
      }
    } catch ( final NoSuchElementException e ) {
      ServiceContext.dispatch( "ReplyQueue", new EucalyptusErrorMessageType( RequestContext.getEventContext( ).getService( ).getComponent( ).getClass( )
                                                                                           .getSimpleName( ), request, e.getMessage( ) ) );
    }
  }
  
}
