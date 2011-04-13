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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.system.SubDirectory;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_partition" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Partition extends AbstractPersistent {
  private static Logger LOG = Logger.getLogger( Partition.class );
  @Column(name="config_partition_name")
  String name;
  @Lob
  @Column(name="config_partition_x509_certificate")
  private String pemCertificate;
  @Lob
  @Column(name="config_partition_node_x509_certificate")
  private String pemNodeCertificate;
  
  public Partition( ) {
    super( );
  }

  public Partition( String name, X509Certificate certificate, X509Certificate nodeCertificate ) {
    super( );
    this.name = name;
    this.pemCertificate = PEMFiles.fromCertificate( certificate );
    this.pemNodeCertificate = PEMFiles.fromCertificate( nodeCertificate );
  }

  /**
   * This removes the key directory link for related components.  This is temporary, do not plan on using it.
   * @param config
   */
  @Deprecated
  public void link( ComponentConfiguration config ) {
    File keyLink = SubDirectory.KEYS.getChildFile( config.getName( ) );
    if( !keyLink.exists( ) ) {
      try {
        GroovyUtil.exec( "ln -sf " + SubDirectory.KEYS.getChildFile( this.name ).getAbsolutePath( ) + " " + keyLink.getAbsolutePath( ) );
        try {
          LOG.info( "Creating key directory link for " + config.getFullName( ) + " at " + keyLink.getAbsolutePath( ) + " -> " + keyLink.getCanonicalPath( ) );
        } catch ( IOException ex ) {
          LOG.info( "Creating key directory link for " + config.getFullName( ) + " at " + keyLink.getAbsolutePath( ) ); 
        } 
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex , ex );
      }
    }
  }

  /**
   * This removes the key directory link for related components.  This is temporary, do not plan on using it.
   * @param config
   */
  @Deprecated
  public void unlink( ComponentConfiguration config ) {
    LOG.info( "Removing key directory link for " + config ); 
    SubDirectory.KEYS.getChildFile( config.getName( ) ).delete( );
  }

  public String getPemCertificate( ) {
    return this.pemCertificate;
  }

  protected void setPemCertificate( String clusterCertificate ) {
    this.pemCertificate = clusterCertificate;
  }

  public String getPemNodeCertificate( ) {
    return this.pemNodeCertificate;
  }

  public X509Certificate getNodeCertificate( ) {
    return PEMFiles.toCertificate( this.getPemNodeCertificate( ) );
  }

  public X509Certificate getCertificate( ) {
    return PEMFiles.toCertificate( this.getPemCertificate( ) );
  }
  
  protected void setPemNodeCertificate( String nodeCertificate ) {
    this.pemNodeCertificate = nodeCertificate;
  }

  public String getName( ) {
    return this.name;
  }

  protected void setName( String name ) {
    this.name = name;
  }

}
