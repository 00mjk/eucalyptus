package com.eucalyptus.www;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.server.EucalyptusManagement;
import edu.ucsb.eucalyptus.admin.server.EucalyptusWebBackendImpl;
import edu.ucsb.eucalyptus.admin.server.SessionInfo;
import edu.ucsb.eucalyptus.msgs.NodeLogInfo;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

@ConfigurableClass( root = "reporting", description = "Parameters controlling the generation of system reports." )
public class Reports extends HttpServlet {
  @ConfigurableField( description = "The number of seconds which a generated report should be cached.", initial = "60" )
  public static Integer REPORT_CACHE_SECS = 1200;
  private static Logger LOG               = Logger.getLogger( Reports.class );
  
  enum Param {
    name, type, session, /*page,*/flush( false ), start( false ), end( false );
    private String  value    = null;
    private Boolean required = Boolean.TRUE;
    
    public Boolean isRequired( ) {
      return this.required;
    }
    
    private Param( String value, Boolean required ) {
      this.value = value;
      this.required = required;
    }
    
    private Param( ) {}
    
    private Param( Boolean required ) {
      this.required = required;
    }
    
    public String get( ) throws NoSuchFieldException {
      if ( this.value == null ) {
        throw new NoSuchFieldException( );
      } else {
        return this.value;
      }
    }
    
    public String get( HttpServletRequest req ) throws IllegalArgumentException {
      if ( req.getParameter( this.name( ) ) == null ) {
        throw new IllegalArgumentException( "'" + this.name( ) + "' is a required argument." );
      } else {
        this.value = req.getParameter( this.name( ) );
        LOG.debug( "Found parameter: " + this.name( ) + "=" + this.value );
        return this.value;
      }
    }
  }
  
  @Override
  protected void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
    for ( Param p : Param.values( ) ) {
      try {
        p.get( req );
      } catch ( IllegalArgumentException e ) {
        if ( p.isRequired( ) ) {
          LOG.debug( e, e );
          throw new RuntimeException( e );
        }
      }
    }
    
    try {
      this.verifyUser( Param.session.get( ) );
    } catch ( NoSuchFieldException e ) {
      LOG.debug( e, e );
      throw new RuntimeException( e );
    }
    try {
      if ( Param.name.get( ).startsWith( "service-" ) ) {
        this.exportLogs( req, res );
      } else {
        Date startTime = this.parseTime( Param.start );
        Date endTime = this.parseTime( Param.end );
        for ( Param p : Param.values( ) ) {
          try {
            LOG.debug( String.format( "REPORT: %10.10s=%s", p.name( ), p.get( ) ) );
          } catch ( NoSuchFieldException e1 ) {
            LOG.debug( String.format( "REPORT: %10.10s=%s", p.name( ), e1.getMessage( ) ) );
          }
        }
        this.exportReport( req, res );
      }
    } catch ( NoSuchFieldException e ) {
      LOG.debug( e, e );
    }
  }
  
  private void exportLogs( HttpServletRequest req, HttpServletResponse res ) {
    try {
      String serviceFq = Param.name.get( req ).replaceAll( "service-", "" );
      String type = serviceFq.replaceAll( "@.*", "" );
      String host = serviceFq.replaceAll( ".*@", "" );
      LOG.debug( String.format( "LOG:  serviceFq=%s  type=%s  host=%s", serviceFq, type, host ) );
      final PrintWriter out = res.getWriter( );
      try {
        res.setContentType( "text/plain" );
        for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
          out.println( "Looking for CC info: " + c.toString( ) );
          if ( c.getHostName( ).equals( host ) ) {
            Cluster cluster = Clusters.getInstance( ).lookup( c.getName( ) );
            out.println( "Sending request to: " + c.toString( ) );
            try {
              NodeLogInfo logInfo = cluster.getLastLog( );
              if ( logInfo != null ) {
                out.write( new String( Base64.decode( logInfo.getCcLog( ) ) ) );
                out.flush( );
              } else {
                out.println( "ERROR getting log information for " + host );
                out.println( logInfo.toString( ) );
              }
            } catch ( Exception e ) {
              LOG.debug( e, e );
              e.printStackTrace( out );
            }
          }
        }
        out.close( );
      } catch ( EucalyptusCloudException e ) {
        LOG.debug( e, e );
        e.printStackTrace( out );
        out.close( );
      } catch ( Exception e ) {
        LOG.debug( e, e );
        e.printStackTrace( out );
        out.close( );
      }
    } catch ( IllegalArgumentException e ) {
      LOG.debug( e, e );
    } catch ( IOException e ) {
      LOG.debug( e, e );
    }
  }
  
  private void exportReport( HttpServletRequest req, HttpServletResponse res ) {
    try {
      Type reportType = Type.valueOf( Param.type.get( ) );
      try {
        Boolean doFlush = this.doFlush( );
        final JRExporter exporter = reportType.setup( req, res, Param.name.get( ) );
        ReportCache reportCache = getReportManager( Param.name.get( req ), doFlush );
        JasperPrint jasperPrint = reportCache.getJasperPrint( );
        exporter.setParameter( JRExporterParameter.JASPER_PRINT, jasperPrint );
        //        exporter.setParameter( JRExporterParameter.PAGE_INDEX, new Integer( Param.page.get( ) ) );
        exporter.exportReport( );
      } catch ( Throwable ex ) {
        LOG.error( ex, ex );
        res.setContentType( "text/plain" );
        LOG.error( "Could not create the report stream " + ex.getMessage( ) + " " + ex.getLocalizedMessage( ) );
        ex.printStackTrace( res.getWriter( ) );
      } finally {
        reportType.close( res );
      }
    } catch ( NoSuchFieldException e ) {
      LOG.debug( e, e );
      this.hasError( "Failed to generate report: " + e.getMessage( ), res );
    } catch ( IOException e ) {
      LOG.debug( e, e );
      this.hasError( "Failed to generate report: " + e.getMessage( ), res );
    }
  }
  
  private Date parseTime( Param p ) {
    Date time = new Date( );
    try {
      String str = p.get( );
      time = new Date( Long.parseLong( str ) );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    } catch ( NoSuchFieldException e ) {
      LOG.error( e, e );
    } catch ( Exception e ) {
      LOG.debug( e, e );
    }
    return time;
  }
  
  private Boolean doFlush( ) {
    try {
      return Boolean.parseBoolean( Param.flush.get( ) );
    } catch ( IllegalArgumentException e ) {
      LOG.debug( e, e );
    } catch ( Exception e ) {
      LOG.debug( e, e );
    }
    return Boolean.FALSE;
  }
  
  private void verifyUser( String sessionId ) {
    SessionInfo session;
    try {
      session = EucalyptusWebBackendImpl.verifySession( sessionId );
      User user = null;
      try {
        user = Users.lookupUser( session.getUserId( ) );
      } catch ( Exception e ) {
        throw new RuntimeException( "User does not exist" );
      }
      if ( !user.isAdministrator( ) ) {
        throw new RuntimeException( "Only administrators can view reports." );
      }
    } catch ( SerializableException e1 ) {
      throw new RuntimeException( "Error obtaining session info." );
    }
    session.setLastAccessed( System.currentTimeMillis( ) );
  }
  
  public static class ReportCache {
    private final long            timestamp;
    private final String          name;
    private final String          reportName;
    private final String          reportGroup;
    private final AtomicBoolean         pending     = new AtomicBoolean( false );
    private Future<JasperPrint>   pendingPrint;
    private JasperPrint           jasperPrint = null;
    private final JasperDesign    jasperDesign;
    private final JasperReport    jasperReport;
    
    private Callable<JasperPrint> async;
    
    public ReportCache( String name, JasperDesign jasperDesign ) throws JRException {
      this.timestamp = System.currentTimeMillis( ) / 1000;
      this.reportName = jasperDesign.getName( );
      this.jasperDesign = jasperDesign;
      this.reportGroup = jasperDesign.getProperty( "euca.report.group" );
      this.name = name;
      this.async = new Callable<JasperPrint>( ) {
        @Override
        public JasperPrint call( ) throws Exception {
          try {
            ReportCache.this.jasperPrint = prepareReport( ReportCache.this );
            return ReportCache.this.jasperPrint;
          } catch ( Exception e ) {
            LOG.error( e, e );
            throw e;
          } finally {
            ReportCache.this.pending.set( false );
          }
        }
      };
      try {
        this.jasperReport = JasperCompileManager.compileReport( jasperDesign );
      } catch ( JRException e1 ) {
        LOG.debug( e1, e1 );
        throw e1;
      }
    }
    
    public String getReportGroup( ) {
      return this.reportGroup;
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.name == null ) ? 0 : this.name.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      ReportCache other = ( ReportCache ) obj;
      if ( this.name == null ) {
        if ( other.name != null ) return false;
      } else if ( !this.name.equals( other.name ) ) return false;
      return true;
    }
    
    public String getReportName( ) {
      return this.reportName;
    }
    
    public long getTimestamp( ) {
      return this.timestamp;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public void rerun( ) {
      try {
        if( this.pending.compareAndSet( false, true ) ) {
//          this.pendingPrint = Threads.lookup( "reporting" ).limitTo( 1 ).getPool( ).submit( this.async );
          this.jasperPrint = this.pendingPrint.get( );
          
        }
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    
    public boolean isDone( ) {
      return this.jasperPrint != null || ( !this.pending.get() && this.pendingPrint != null && this.pendingPrint.isDone( ) );
    }
    
    public JasperPrint getJasperPrint( ) {
      if( this.jasperPrint == null ) {
        this.rerun( );
      }
      return this.jasperPrint;
    }
    
    public boolean isExpired( ) {
      return ( System.currentTimeMillis( ) / 1000l ) - this.timestamp > REPORT_CACHE_SECS;
    }
    
    public JasperDesign getJasperDesign( ) {
      return this.jasperDesign;
    }
    
    public JasperReport getJasperReport( ) {
      return this.jasperReport;
    }
  }
  
  private static Map<String, ReportCache> reportCache = new ConcurrentHashMap<String, ReportCache>( );
  
  private static GroovyScriptEngine       gse         = makeScriptEngine( );
  
  public static ReportCache getReportManager( final String name, boolean flush ) throws JRException, SQLException {
    try {
      if ( !flush && reportCache.containsKey( name ) && !reportCache.get( name ).isExpired( ) ) {
        return reportCache.get( name );
      } else if ( reportCache.containsKey( name ) && ( reportCache.get( name ).isExpired( ) || flush ) ) {
        ReportCache r = reportCache.get( name );
        r.rerun( );
        return r;
      } else {
        ReportCache r = reportCache.get( name );
        final JasperDesign jasperDesign = JRXmlLoader.load( SubDirectory.REPORTS.toString( ) + File.separator + name + ".jrxml" );
        reportCache.put( name, new ReportCache( name, jasperDesign ) );
      }
      return reportCache.get( name );
    } catch ( Throwable t ) {
      LOG.error( t, t );
      throw new JRException( t );
    }
  }
  
  private static JasperPrint prepareReport( final ReportCache reportCache ) throws JRException, SQLException, IOException {
    JasperPrint jasperPrint;
    final boolean jdbc = !( new File( SubDirectory.REPORTS.toString( ) + File.separator + reportCache.getName( ) + ".groovy" ).exists( ) );
    if ( jdbc ) {
      String url = String.format( "jdbc:%s_%s", Component.db.getUri( ).toString( ), "records" );
      Connection jdbcConnection = DriverManager.getConnection( url, "eucalyptus", Hmacs.generateSystemSignature( ) );
      jasperPrint = JasperFillManager.fillReport( reportCache.getJasperReport( ), null, jdbcConnection );
    } else {
      FileReader fileReader = null;
      try {
        final List results = new ArrayList( );
        fileReader = new FileReader( SubDirectory.REPORTS + File.separator + reportCache.getName( ) + ".groovy" );
        Binding binding = new Binding( new HashMap( ) {
          {
            put( "results", results );
          }
        } );
        try {
          makeScriptEngine( ).run( reportCache.getName( ) + ".groovy", binding );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
        JRBeanCollectionDataSource data = new JRBeanCollectionDataSource( results );
        jasperPrint = JasperFillManager.fillReport( reportCache.getJasperReport( ), null, data );
      } catch ( Throwable e ) {
        LOG.debug( e, e );
        throw new RuntimeException( e );
      } finally {
        if ( fileReader != null ) try {
          fileReader.close( );
        } catch ( IOException e ) {
          LOG.error( e, e );
          throw e;
        }
      }
    }
    return jasperPrint;
  }
  
  private static GroovyScriptEngine makeScriptEngine( ) {
    if ( gse != null ) {
      return gse;
    } else {
      synchronized ( Reports.class ) {
        if ( gse != null ) {
          return gse;
        } else {
          try {
            return gse = new GroovyScriptEngine( SubDirectory.REPORTS.toString( ) );
          } catch ( IOException e ) {
            LOG.debug( e, e );
            throw new RuntimeException( e );
          }
        }
      }
    }
  }
  
  public static void hasError( String message, HttpServletResponse response ) {
    try {
      response.getWriter( ).print( EucalyptusManagement.getError( message ) );
      response.getWriter( ).flush( );
    } catch ( IOException e ) {
      e.printStackTrace( );
    }
  }
  
  enum Type {
    pdf {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/pdf" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".pdf" );
        JRExporter exporter = new JRPdfExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    },
    csv {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "text/plain" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".csv" );
        JRExporter exporter = new JRCsvExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    },
    html {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        PrintWriter out = res.getWriter( );
        res.setContentType( "text/html" );
        JRExporter exporter = new JRHtmlExporter( );
        exporter.setParameter( new JRExporterParameter( "EUCA_WWW_DIR" ) {}, "/" );
        exporter.setParameter( JRExporterParameter.OUTPUT_WRITER, res.getWriter( ) );
        exporter.setParameter( JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE );
        exporter.setParameter( JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE );
        return exporter;
      }
      
      @Override
      public void close( HttpServletResponse res ) throws IOException {
        res.getWriter( ).close( );
      }
    },
    xls {
      @Override
      public JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException {
        res.setContentType( "application/vnd.ms-excel" );
        res.setHeader( "Content-Disposition", "file; filename=" + name + ".xls" );
        JRExporter exporter = new JRXlsExporter( );
        exporter.setParameter( JRExporterParameter.OUTPUT_STREAM, res.getOutputStream( ) );
        return exporter;
      }
    };
    public abstract JRExporter setup( HttpServletRequest request, HttpServletResponse res, String name ) throws IOException;
    
    public void close( HttpServletResponse res ) throws IOException {
      res.getOutputStream( ).close( );
    }
  }
  
}
