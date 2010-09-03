package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.GetLogsResponseType;
import edu.ucsb.eucalyptus.msgs.GetLogsType;

public class LogDataCallback extends MessageCallback<GetLogsType, GetLogsResponseType> {
  private static Logger  LOG  = Logger.getLogger( LogDataCallback.class );
  private final NodeInfo node;
  private final Cluster  cluster;
  private boolean        self = false;
  
  public LogDataCallback( Cluster cluster, NodeInfo node ) {
    this.node = node;
    this.cluster = cluster;
    this.self = ( null == node );
    if ( self ) {
      this.setRequest( new GetLogsType( "self" ) );
    } else {
      this.setRequest( new GetLogsType( node.getServiceTag( ) ) );
    }
  }
  
  @Override
  public void initialize( GetLogsType msg )  {}
  
  @Override
  public void fireException( Throwable t ) {
    LOG.error( t, t );
  }
  @Override
  public void fire( GetLogsResponseType msg )  {
    if ( msg == null || msg.getLogs( ) == null ) {
      EucalyptusClusterException error = new EucalyptusClusterException( "Failed to get log data from cluster." );
      LOG.error( error, error );
    } else {
      String log = "";
      if ( self ) {
        cluster.setLastLog( msg.getLogs( ) );
        try {
          log = new String( Base64.decode( msg.getLogs( ).getCcLog( ) ) ).replaceFirst( ".*\b", "" ).substring( 0, 1000 );
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }
      } else {
        node.setLogs( msg.getLogs( ) );
        try {
          log = new String( Base64.decode( msg.getLogs( ).getNcLog( ) ) ).replaceFirst( ".*\b", "" ).substring( 0, 1000 );
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }
      }
      LOG.debug( "LOG: " + log );
    }
  }
  
}
