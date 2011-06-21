import com.eucalyptus.system.DirectoryBootstrapper;

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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

import java.util.Properties
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration
import com.eucalyptus.bootstrap.MysqlDatabaseBootstrapper
import com.eucalyptus.bootstrap.ServiceJarDiscovery
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentDiscovery
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilders
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceBuilders.ServiceBuilderDiscovery
import com.eucalyptus.component.auth.SystemCredentialProvider
import com.eucalyptus.component.id.Database;
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.entities.PersistenceContextDiscovery
import com.eucalyptus.entities.PersistenceContexts
import com.eucalyptus.util.Internets
import com.mysql.management.MysqldResource


Logger LOG = Logger.getLogger( initialize_cloud.class );

[ new ComponentDiscovery( ), new ServiceBuilderDiscovery( ), new PersistenceContextDiscovery( ) ].each{
  ServiceJarDiscovery.runDiscovery(  it );
}
new DirectoryBootstrapper( ).load( );
SystemCredentialProvider.initialize( );
Component dbComp = Components.lookup( Database.class );
try {
  MysqldResource mysql = MysqlDatabaseBootstrapper.initialize( );
  try {
    props = [
          "hibernate.archive.autodetection": "jar, class, hbm",
          "hibernate.show_sql": "false",
          "hibernate.format_sql": "false",
          "hibernate.connection.autocommit": "true",
          "hibernate.hbm2ddl.auto": "update",
          "hibernate.generate_statistics": "true",
          "hibernate.connection.driver_class": "com.mysql.jdbc.Driver",
          "hibernate.connection.username": "eucalyptus",
          "hibernate.connection.password": SystemIds.databasePassword( ),
          "hibernate.bytecode.use_reflection_optimizer": "true",
          "hibernate.cglib.use_reflection_optimizer": "true",
          "hibernate.dialect": "org.hibernate.dialect.MySQLInnoDBDialect",
          "hibernate.cache.provider_class": "org.hibernate.cache.TreeCache",
          "hibernate.cache.region.factory_class": "org.hibernate.cache.jbc2.SharedJBossCacheRegionFactory",
          "hibernate.cache.region.jbc2.cfg.shared": "eucalyptus_jboss_cache.xml",
          "hibernate.cache.use_second_level_cache": "true",
          "hibernate.cache.use_query_cache": "true",
          "hibernate.cache.use_structured_entries": "true",
        ]
    for ( String ctx : PersistenceContexts.list( ) ) {
      Properties p = new Properties( );
      p.putAll( props );
      String ctxUrl = String.format( "jdbc:%s_%s?createDatabaseIfNotExist=true", dbComp.getUri( ).toString( ), ctx.replaceAll( "eucalyptus_": "" ) );
      p.put( "hibernate.connection.url", ctxUrl );
      p.put("hibernate.cache.region_prefix", "eucalyptus_" + ctx + "_cache" );
      Ejb3Configuration config = new Ejb3Configuration( );
      config.setProperties( p );
      for ( Class c : PersistenceContexts.listEntities( ctx ) ) {
        config.addAnnotatedClass( c );
      }
      PersistenceContexts.registerPersistenceContext( ctx, config );
    }
    final ServiceConfiguration newComponent = ServiceBuilders.lookup( Eucalyptus.class ).add( Eucalyptus.INSTANCE.name( ), Internets.localHostAddress( ), Internets.localHostAddress( ), 8773 );
    LOG.info( "Added registration for this cloud controller: " + newComponent.toString() );
  } catch( Exception ex ) {
    LOG.error( ex, ex );
    System.exit( 1 );
  } finally {
    mysql.shutdown( );
  }
  System.exit( 0 );
} catch( Exception ex ) {
  LOG.error( ex, ex );
  System.exit( 1 );
}
