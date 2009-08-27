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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.bootstrap;


import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.cloud.ws.DNSControl;

@Provides(resource=Resource.PrivilegedContext,component=Component.dns)
@Depends(local=Component.dns)
public class DNSBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DNSBootstrapper.class );
  private static DNSBootstrapper singleton;

  public static Bootstrapper getInstance( ) {
    synchronized ( DNSBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new DNSBootstrapper( );
        LOG.info( "Creating DNS Bootstrapper instance." );
      } else {
        LOG.info( "Returning DNS Bootstrapper instance." );
      }
    }
    return singleton;
  }

  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return true;
  }

  @Override
  public boolean load(Resource current ) throws Exception {
	  LOG.info("Initializing DNS");
	  DNSControl.initialize();
	  return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

}
