package com.eucalyptus.cluster.callback;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.SystemState;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger LOG = Logger.getLogger( VmStateCallback.class );
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ) {
      {
        regarding( );
      }
    } );
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    reply.setOriginCluster( this.getSubject( ).getConfiguration( ).getName( ) );
    for ( VmInfo vmInfo : reply.getVms( ) ) {
      vmInfo.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
      VmTypeInfo typeInfo = vmInfo.getInstanceType( );
      if ( typeInfo.getName( ) == null || "".equals( typeInfo.getName( ) ) ) {
        for ( VmType t : VmTypes.list( ) ) {
          if ( t.getCpu( ).equals( typeInfo.getCores( ) ) && t.getDisk( ).equals( typeInfo.getDisk( ) ) && t.getMemory( ).equals( typeInfo.getMemory( ) ) ) {
            typeInfo.setName( t.getName( ) );
          }
        }
      }
    }
    
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      SystemState.handle( reply );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
  }

  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
}
