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
package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.ucsb.eucalyptus.cloud.AbstractNamedRegistry;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Clusters extends AbstractNamedRegistry<Cluster> {
  private static Clusters singleton = getInstance( );

  public static Clusters getInstance( ) {
    synchronized ( Clusters.class ) {
      if ( singleton == null ) singleton = new Clusters( );
    }
    return singleton;
  }

  public List<RegisterClusterType> getClusters( ) {
    List<RegisterClusterType> list = new ArrayList<RegisterClusterType>( );
    for ( Cluster c : this.listValues( ) )
      list.add( c.getWeb( ) );
    return list;
  }

  public List<String> getClusterAddresses( ) {
    List<String> list = new ArrayList<String>( );
    for ( Cluster c : this.listValues( ) )
      list.add( c.getConfiguration( ).getHostName( ) + ":" + c.getConfiguration( ).getPort( ) );
    return list;
  }

}
