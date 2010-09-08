package com.eucalyptus.address;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callbacks;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class DynamicSystemAddressManager extends AbstractSystemAddressManager {
  private static Logger LOG = Logger.getLogger( DynamicSystemAddressManager.class );
  
  @Override
  public List<Address> allocateSystemAddresses( String cluster, int count ) throws NotEnoughResourcesAvailable {
    List<Address> addressList = Lists.newArrayList( );
    if ( Addresses.getInstance( ).listDisabledValues( ).size( ) < count ) throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    for ( Address addr : Addresses.getInstance( ).listDisabledValues( ) ) {
      if ( cluster.equals( addr.getCluster( ) ) ) {
        addressList.add( addr.allocate( Component.eucalyptus.name( ) ) );
        addr.pendingAssignment( );
        if ( --count == 0 ) {
          break;
        }
      }
    }
    if ( count != 0 ) {
      for( Address addr : addressList ) {
        addr.release( );
      }
      throw new NotEnoughResourcesAvailable( "Not enough resources available: addresses (try --addressing private)" );
    } 
    return addressList;
  }
  @Override
  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesAvailable {
    final Address addr = this.allocateSystemAddresses( vm.getPlacement( ), 1 ).get( 0 );
    Callbacks.newClusterRequest( addr.assign( vm ).getCallback( ) ).then( new Callback.Success<BaseMessage>() {
      public void fire( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    }).dispatch( addr.getCluster( ) );
  }
    
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( Iterables.filter( Addresses.getInstance( ).listValues( ), new Predicate<Address>( ) {
      @Override
      public boolean apply( Address arg0 ) {
        return arg0.isSystemOwned( );
      }
    } ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void inheritReservedAddresses( List<Address> previouslyReservedAddresses ) {
    for ( final Address addr : previouslyReservedAddresses ) {
      if( !addr.isAssigned( ) ) {
        Addresses.release( addr );
      }
    }
  }
  @Override public void releaseSystemAddress( Address addr ) {
    try {
      addr.release( );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
    }
  }
  
}
