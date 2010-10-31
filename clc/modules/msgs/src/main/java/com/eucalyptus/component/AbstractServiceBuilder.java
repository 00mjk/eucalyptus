package com.eucalyptus.component;

import java.util.List;
import org.apache.log4j.Logger;

import com.eucalyptus.component.event.StartComponentEvent;
import com.eucalyptus.component.event.StopComponentEvent;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.NetworkUtil;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {
  private static Logger LOG = Logger.getLogger( AbstractServiceBuilder.class );

  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    try {
      this.lookupByName( name );
      return true;
    } catch ( Exception e ) {
      return false;
    }
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet( config.getComponent( ).name( ) );
      for ( ConfigurableProperty prop : props ) {
        ConfigurableProperty addProp = null;
        if ( prop instanceof SingletonDatabasePropertyEntry ) {
          addProp = prop;
        } else if ( prop instanceof MultiDatabasePropertyEntry ) {
          addProp = ( ( MultiDatabasePropertyEntry ) prop ).getClone( config.getName( ) );
        }
        PropertyDirectory.addProperty( addProp );
      }
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
    }

    StartComponentEvent e = null;
    if ( config.isLocal( ) ) {
      e = StartComponentEvent.getLocal( config );
    } else {
      e = StartComponentEvent.getRemote( config );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponent( ), e );
    } catch ( EventVetoedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    StopComponentEvent e = null;
    if ( NetworkUtil.testLocal( config.getHostName( ) ) ) {
      e = StopComponentEvent.getLocal( config );
    } else {
      e = StopComponentEvent.getRemote( config );
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( config.getComponent( ), e );
    } catch ( EventVetoedException e1 ) {
      LOG.error( e1, e1 );
      throw new ServiceRegistrationException( e1.getMessage( ), e1 );
    }
    
    try {
      List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet( config.getComponent( ).name( ) );
      for ( ConfigurableProperty prop : props ) {
        if ( prop instanceof SingletonDatabasePropertyEntry ) {
          //noop
        } else if ( prop instanceof MultiDatabasePropertyEntry ) {
          ( ( MultiDatabasePropertyEntry ) prop ).setIdentifierValue( config.getName( ) );
        }
        PropertyDirectory.removeProperty( prop );
      }
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
    }
  }

  /**
   * @see com.eucalyptus.component.ServiceBuilder#fireEnable(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  /**
   * @see com.eucalyptus.component.ServiceBuilder#fireDisable(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.component.ServiceBuilder#fireCheck(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}

  
}
