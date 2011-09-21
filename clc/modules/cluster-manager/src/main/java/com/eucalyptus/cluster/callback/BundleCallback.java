package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.vm.BundleInstanceResponseType;
import com.eucalyptus.vm.BundleInstanceType;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;

public class BundleCallback extends MessageCallback<BundleInstanceType,BundleInstanceResponseType> {

  private static Logger LOG = Logger.getLogger( BundleCallback.class );
  public BundleCallback( BundleInstanceType request ) {
    super( request );
  }
  

  @Override
  public void initialize( BundleInstanceType msg ) {}

  @Override
  public void fire( BundleInstanceResponseType reply ) {
    if ( !reply.get_return( ) ) {
      LOG.info( "Attempt to bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
      try {
        VmInstance vm = VmInstances.lookup( this.getRequest().getInstanceId( ) );
        vm.resetBundleTask( );
      } catch ( NoSuchElementException e1 ) {
      }
    } else {
      try {
        VmInstance vm = VmInstances.lookup( this.getRequest().getInstanceId( ) );
        vm.clearPendingBundleTask( );
        EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTED, this.getRequest( ).toSimpleString( ), vm.getBundleTask( ).getBundleId( ), vm.getInstanceId( ) ).info( );
      } catch ( NoSuchElementException e1 ) {
      }
    }

  }

  @Override
  public void fireException( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
