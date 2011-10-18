package com.eucalyptus.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.AnonymousMessage;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.UniqueIds;
import com.eucalyptus.util.UniqueIds.UniqueIdProducer;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Internets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ComponentId implements HasName<ComponentId>, HasFullName<ComponentId>, Serializable {
  private static Logger       LOG         = Logger.getLogger( ComponentId.class );
  private static final String EMPTY_MODEL = "  <mule xmlns=\"http://www.mulesource.org/schema/mule/core/2.0\"\n"
                                            +
                                            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                            +
                                            "      xmlns:spring=\"http://www.springframework.org/schema/beans\"\n"
                                            +
                                            "      xmlns:vm=\"http://www.mulesource.org/schema/mule/vm/2.0\"\n"
                                            +
                                            "      xmlns:euca=\"http://www.eucalyptus.com/schema/cloud/1.6\"\n"
                                            +
                                            "      xsi:schemaLocation=\"\n"
                                            +
                                            "       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\n"
                                            +
                                            "       http://www.mulesource.org/schema/mule/core/2.0 http://www.mulesource.org/schema/mule/core/2.0/mule.xsd\n" +
                                            "       http://www.mulesource.org/schema/mule/vm/2.0 http://www.mulesource.org/schema/mule/vm/2.0/mule-vm.xsd\n" +
                                            "       http://www.eucalyptus.com/schema/cloud/1.6 http://www.eucalyptus.com/schema/cloud/1.6/euca.xsd\">\n" +
                                            "</mule>\n";
  private final String        name;
  private final String        capitalizedName;
  private final String        entryPoint;
  private final Integer       port;
  private final String        modelContent;
  private String              uriPattern;
  private String              externalUriPattern;
  private String              uriLocal;
  
  protected ComponentId( String name ) {
    this.capitalizedName = name;
    this.name = this.capitalizedName.toLowerCase( );
    this.entryPoint = this.capitalizedName + "RequestQueueEndpoint";
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    // NOTE: this pattern is overwritten by some subclasses in getExternalUriPattern()
    this.externalUriPattern = "%s://%s:%d/services/" + this.capitalizedName;
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
    this.modelContent = loadModel( );
  }
  
  protected ComponentId( ) {
    this.capitalizedName = this.getClass( ).getSimpleName( );
    this.name = this.capitalizedName.toLowerCase( );
    this.entryPoint = this.capitalizedName + "RequestQueueEndpoint";
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    // NOTE: this pattern is overwritten by some subclasses in getExternalUriPattern()
    this.externalUriPattern = "%s://%s:%d/services/" + this.capitalizedName;
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
    this.modelContent = loadModel( );
  }

  public String getVendorName( ) {
    return "euca";
  }
  
  private String loadModel( ) {
    StringWriter out = new StringWriter( );
    try {
      InputStream in = Thread.currentThread( ).getContextClassLoader( ).getResourceAsStream( this.getServiceModelFileName( ) );
      if ( in == null ) {
        return EMPTY_MODEL;
      } else {
        IOUtils.copy( in, out );
        in.close( );
        out.flush( );
        String outString = out.toString( );
        Logs.extreme( ).trace( "Loaded model for: " + this.name );
        Logs.extreme( ).trace( outString );
        return outString;
      }
    } catch ( IOException ex ) {
      LOG.error( ex, ex );
      throw BootstrapException.throwError( "BUG! BUG! Failed to load configuration specified for Component: " + this.name, ex );
    }
  }
  
  public final String name( ) {
    return this.name;
  }
  
  @Override
  public final String getName( ) {
    return this.name;
  }
  
  @Override
  public final FullName getFullName( ) {
    return new ComponentFullName( this, this.tryForPartionName( ), this.name );
  }
  
  @Override
  public String getPartition( ) {
    return this.tryForPartionName( );//TODO:GRZE:OMGFIXME
  }
  
  private final String tryForPartionName( ) {
    return ( this.isPartitioned( ) )
      ? ComponentIds.lookup( Empyrean.class ).name( )
      : ( ( Unpartioned ) this ).getPartition( );
  }
  
  public final boolean isRootService( ) {
    return this.serviceDependencies( ).isEmpty( );
  }
  
  public final boolean isPartitioned( ) {
    return !Unpartioned.class.isAssignableFrom( this.getClass( ) );
  }
  
  public FullName makeFullName( ServiceConfiguration config, String... parts ) {
    return new ComponentFullName( config, parts );
  }

  public List<Class<? extends ComponentId>> serviceDependencies( ) {
    return Lists.newArrayList( );
  }
  
  public final Boolean isCloudLocal( ) {
    return this.serviceDependencies( ).contains( Eucalyptus.class ) || Eucalyptus.class.equals( this.getClass( ) );
  }
  
  public final Boolean isAlwaysLocal( ) {
    return this.serviceDependencies( ).contains( Empyrean.class ) || Empyrean.class.equals( this.getClass( ) );
  }
  
  public Boolean hasCredentials( ) {
    return false;
  }
  
  private static final ConcurrentMap<String, Class<ChannelPipelineFactory>> clientPipelines = Maps.newConcurrentMap( );
  
  public ChannelPipelineFactory getClientPipeline( ) {
    return helpGetClientPipeline( defaultClientPipelineClass );//TODO:GRZE:URGENT: fix handling of internal pipeline
  }
  
  private static final String defaultClientPipelineClass = "com.eucalyptus.ws.client.pipeline.InternalClientPipeline";
  
  protected static ChannelPipelineFactory helpGetClientPipeline( String fqName ) {
    if ( clientPipelines.containsKey( fqName ) ) {
      try {
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( IllegalAccessException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      try {
        clientPipelines.putIfAbsent( fqName, ( Class<ChannelPipelineFactory> ) ClassLoader.getSystemClassLoader( ).loadClass( fqName ) );
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( IllegalAccessException ex ) {
        LOG.error( ex, ex );
      } catch ( ClassNotFoundException ex ) {
        LOG.error( ex, ex );
      }
    }
    return new ChannelPipelineFactory( ) {
      
      @Override
      public ChannelPipeline getPipeline( ) throws Exception {
        return Channels.pipeline( );
      }
    };
  }
  
  /**
   * @return the entryPoint
   */
  public String getEntryPoint( ) {
    return this.entryPoint;
  }
  
  /**
   * @return the capitalizedName
   */
  public final String getCapitalizedName( ) {
    return this.capitalizedName;
  }
  
  public Class<? extends BaseMessage> lookupBaseMessageType( ) {
    try {
      return ComponentMessages.lookup( this.getClass( ) );
    } catch ( NoSuchElementException ex ) {
      return AnonymousMessage.class;
    }
  }
  
  public Integer getPort( ) {
    return this.port;
  }
  
  public String getLocalEndpointName( ) {
    return this.uriLocal;
  }
  
  public URI getLocalEndpointUri( ) {
    URI uri = URI.create( this.getLocalEndpointName( ) );
    try {
      uri.parseServerAuthority( );
    } catch ( URISyntaxException ex ) {
      LOG.error( ex, ex );
    }
    return uri;
  }
  
  public final String getServiceModel( ) {
    return this.modelContent;
  }
  
  public String getServiceModelFileName( ) {
    return String.format( "%s-model.xml", this.getName( ) );
  }
  
  public String getUriPattern( ) {
    return this.uriPattern;
  }
  
  /**
   * Get the HTTP service path
   */
  public final URI makeInternalRemoteUri( String hostName, Integer port ) {
    String uri;
    try {
      uri = String.format( this.getUriPattern( ), hostName, port );
    } catch ( MissingFormatArgumentException e ) {
      uri = String.format( this.getUriPattern( ) + "Internal", hostName, port, this.getCapitalizedName( ) );
    }
    try {
      URI u = new URI( uri );
      u.parseServerAuthority( );
      return u;
    } catch ( URISyntaxException e ) {
      return URI.create( uri );
    }
  }
  
  /**
   * Create URI for external endpoint
   * @param scheme URI scheme to be used, see StackConfiguration fields for configurable properties
   */
  public final URI makeExternalRemoteUri( String hostName, Integer port, String scheme ) {
    String uri;
    URI u = null;
    String pattern;

    port = ( port == -1 )
      ? this.getPort( )
      : port;
    hostName = ( port == -1 )
      ? Internets.localHostAddress( )
      : hostName;
    pattern = this.getExternalUriPattern();

    try {
      if(pattern.startsWith("%s"))
	    uri = String.format( this.getExternalUriPattern( ), scheme, hostName, port );
      else
	    uri = String.format( this.getExternalUriPattern( ), hostName, port );
      u = new URI( uri );
      u.parseServerAuthority( );
    } catch ( URISyntaxException e ) {
        if(pattern.startsWith("%s"))
	    uri = String.format( this.getExternalUriPattern( ), scheme, Internets.localHostAddress( ), this.getPort( ) );
	else
	    uri = String.format( this.getExternalUriPattern( ), Internets.localHostAddress( ), this.getPort( ) );
      try {
        u = new URI( uri );
        u.parseServerAuthority( );
      } catch ( URISyntaxException ex ) {
        u = URI.create( uri );
      }
    } catch ( MissingFormatArgumentException e ) {
	if(pattern.startsWith("%s"))
	    uri = String.format( this.getExternalUriPattern( ), scheme, hostName, port, this.getCapitalizedName( ) );
	else 
	    uri = String.format( this.getExternalUriPattern( ), hostName, port, this.getCapitalizedName( ) );
      try {
        u = new URI( uri );
        u.parseServerAuthority( );
      } catch ( URISyntaxException ex ) {
        u = URI.create( uri );
      }
    }
    return u;
  }
  
  @Override
  public final int compareTo( ComponentId that ) {
    return this.name.compareTo( that.name );
  }
  
  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name == null )
      ? 0
      : this.name.hashCode( ) );
    return result;
  }
  
  @Override
  public final boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    ComponentId other = ( ComponentId ) obj;
    if ( this.name == null ) {
      if ( other.name != null ) return false;
    } else if ( !this.name.equals( other.name ) ) return false;
    return true;
  }
  
  public String getExternalUriPattern( ) {
    return this.externalUriPattern;
  }
  
//  @Override
//  public String toString( ) {
//    return String.format( "ComponentId:%s:parent=%s:%spartitioned:disp=%s:alwaysLocal=%s:cloudLocal=%s:creds=%s",
//                          this.name( ), this.serviceDependencies( ), this.isPartitioned( ) ? "" : "un", this.lookupBaseMessageType( ).getSimpleName( ), this.hasDispatcher( ), this.isAlwaysLocal( ), this.isCloudLocal( ),
//                          this.isPartitioned( ), this.hasCredentials( ) );
//  }
  
  public static abstract class Unpartioned extends ComponentId {
    
    public Unpartioned( ) {
      super( );
    }
    
    public Unpartioned( String name ) {
      super( name );
    }
    
    @Override
    public String getPartition( ) {
      return this.isCloudLocal( )
        ? Eucalyptus.INSTANCE.name( )
        : ( this.isAlwaysLocal( )
          ? Empyrean.INSTANCE.name( )
          : this.name( ) );
    }
    
  }
  
  public boolean runLimitedServices( ) {
    return false;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( this.getFullName( ) ).append( " " );
    builder.append( this.name( ) ).append( ":" );
    if ( this.isPartitioned( ) ) {
      builder.append( "partitioned:" );
    } else {
      builder.append( "unpartitioned:" );
    }
    if ( !this.serviceDependencies( ).isEmpty( ) ) {
      builder.append( "deps=" ).append( Lists.transform( this.serviceDependencies( ), new Function<Class, String>( ) {
        
        @Override
        public String apply( Class arg0 ) {
          return arg0.getSimpleName( );
        }
      } ) ).append( ":" );
    }
    if ( this.isCloudLocal( ) ) {
      builder.append( "cloudLocal:" );
    } else if ( this.isAlwaysLocal( ) ) {
      builder.append( "alwaysLocal:" );
    }
    if ( this.runLimitedServices( ) ) {
      builder.append( "runs-limited-services:" );
    }
    return builder.toString( );
  }
  
  public final boolean isInternal( ) {
    return !this.isAdminService( ) && !this.isUserService( );
  }
  
  public boolean isUserService( ) {
    return false;
  }
  
  public boolean isAdminService( ) {
    return false;
  }
  
  public boolean isRegisterable( ) {
    return !( ServiceBuilders.lookup( this ) instanceof DummyServiceBuilder );
  }

  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @param component TODO
   * @return true if the component could be run locally.
   */
  public Boolean isAvailableLocally( ) {
    return this.isAlwaysLocal( ) || ( this.isCloudLocal( ) && BootstrapArgs.isCloudController( ) );
  }

  protected static ServiceConfiguration setupServiceState( InetAddress addr, ComponentId compId ) throws ServiceRegistrationException, ExecutionException {
    try {
      Component comp = Components.lookup( compId );
      ServiceConfiguration config = ( Internets.testLocal( addr ) )
        ? comp.initRemoteService( addr )
        : comp.initRemoteService( addr );//TODO:GRZE:REVIEW: use of initRemote
      if ( Component.State.INITIALIZED.ordinal( ) >= config.lookupState( ).ordinal( ) ) {
        comp.loadService( config ).get( );
      }
      Topology.enable( config );
      return config;
    } catch ( InterruptedException ex ) {
      Thread.currentThread( ).interrupt( );
      return null;
    }
  }
  
}
