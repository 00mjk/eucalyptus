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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.StaticDiskImage;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.VerificationException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class Emis {
  enum VBRTypes {
    MACHINE( "walrus://" ),
    EBS,
    KERNEL( "walrus://" ),
    RAMDISK( "walrus://" ),
    EPHEMERAL,
    SWAP;
    String prefix;
    
    private VBRTypes( ) {
      this( "" );
    }
    
    private VBRTypes( String prefix ) {
      this.prefix = prefix;
    }
    
  }
  
  public enum LookupBlockStorage implements Function<String, BlockStorageImageInfo> {
    INSTANCE;
    @Override
    public BlockStorageImageInfo apply( String identifier ) {
      //Added check to ensure image is available
      BlockStorageImageInfo ret = EntityWrapper.get( BlockStorageImageInfo.class ).lookupAndClose( Images.exampleBlockStorageWithImageId( identifier ) );
      if (!ImageMetadata.State.available.equals(ret.getState())) {
	      throw new NoSuchElementException("Unable to start instance with deregistered image : " + ret);
      } else { 
        return ret;
      }
    }
  }
  
  public enum LookupMachine implements Function<String, MachineImageInfo> {
    INSTANCE;
    @Override
    public MachineImageInfo apply( String identifier ) {
      //Added check to ensure image is availabe
      MachineImageInfo ret = EntityWrapper.get( MachineImageInfo.class ).lookupAndClose( Images.exampleMachineWithImageId( identifier ) );
      if (!ImageMetadata.State.available.equals(ret.getState())) {
	      throw new NoSuchElementException("Unable to start instance with deregistered image : " + ret);
      } else {
        return ret;
      }
    }
  }
  
  public enum LookupKernel implements Function<String, KernelImageInfo> {
    INSTANCE;
    @Override
    public KernelImageInfo apply( String identifier ) {
     //Added check to ensure the image is available
     KernelImageInfo ret = EntityWrapper.get( KernelImageInfo.class ).lookupAndClose( Images.exampleKernelWithImageId( identifier ) );
     if (!ImageMetadata.State.available.equals(ret.getState())) {
	      throw new NoSuchElementException("Unable to start instance with deregistered image : " + ret);
     } else {
       return ret;
     }	
    }
  }
  
  public enum LookupRamdisk implements Function<String, RamdiskImageInfo> {
    INSTANCE;
    @Override
    public RamdiskImageInfo apply( String identifier ) { 
     //Added check to ensure the image is available 
     RamdiskImageInfo ret = EntityWrapper.get( RamdiskImageInfo.class ).lookupAndClose( Images.exampleRamdiskWithImageId( identifier ) );
     if (!ImageMetadata.State.available.equals(ret.getState())) {
	      throw new NoSuchElementException("Unable to start instance with deregistered image : " + ret);
     } else {
       return ret;
     }
    }
  }
  
  private static Logger LOG = Logger.getLogger( Emis.class );
  
  public static class BootableSet {
    private final BootableImageInfo disk;
    
    private BootableSet( BootableImageInfo bootableImageInfo ) {
      this.disk = bootableImageInfo;
    }
    
    public BootableImageInfo getMachine( ) {
      return this.disk;
    }
    
    public RamdiskImageInfo getRamdisk( ) {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a kernel." );
    }
    
    public KernelImageInfo getKernel( ) {
      throw new NoSuchElementException( "BootableSet:machine=" + this.getMachine( ) + " does not have a kernel." );
    }
    
    public boolean hasKernel( ) {
      try {
        this.getKernel( );
        return true;
      } catch ( NoSuchElementException ex ) {
        return false;
      }
    }
    
    public boolean hasRamdisk( ) {
      try {
        this.getRamdisk( );
        return true;
      } catch ( NoSuchElementException ex ) {
        return false;
      }
    }
    
    public boolean isLinux( ) {
      return ImageMetadata.Platform.linux.equals( this.getMachine( ).getPlatform( ) ) || this.getMachine( ).getPlatform( ) == null;
    }
    
    @Override
    public String toString( ) {
      return String.format( "BootableSet:machine=%s:ramdisk=%s:kernel=%s:isLinux=%s", this.getMachine( ),
                            this.hasRamdisk( )
                              ? this.getRamdisk( )
                              : "false",
                              this.hasKernel( )
                                ? this.getKernel( )
                                : "false",
                                this.isLinux( ) );
    }
    
    public VmTypeInfo populateVirtualBootRecord( VmType vmType ) throws MetadataException {
      VmTypeInfo vmTypeInfo = VmTypes.asVmTypeInfo( vmType, this.getMachine( ) );
      if ( this.isLinux( ) ) {
        if ( this.hasKernel( ) ) {
          vmTypeInfo.setKernel( this.getKernel( ).getDisplayName( ), this.getKernel( ).getManifestLocation( ) );
        }
        if ( this.hasRamdisk( ) ) {
          vmTypeInfo.setRamdisk( this.getRamdisk( ).getDisplayName( ), this.getRamdisk( ).getManifestLocation( ) );
        }
      }
      return vmTypeInfo;
    }
  }
  
  static class NoRamdiskBootableSet extends BootableSet {
    private final KernelImageInfo kernel;
    
    private NoRamdiskBootableSet( BootableImageInfo bootableImageInfo, KernelImageInfo kernel ) {
      super( bootableImageInfo );
      this.kernel = kernel;
    }
    
    @Override
    public KernelImageInfo getKernel( ) {
      return this.kernel;
    }
  }
  
  static class TrifectaBootableSet extends NoRamdiskBootableSet {
    private final RamdiskImageInfo ramdisk;
    
    public TrifectaBootableSet( BootableImageInfo bootableImageInfo, KernelImageInfo kernel, RamdiskImageInfo ramdisk ) {
      super( bootableImageInfo, kernel );
      this.ramdisk = ramdisk;
    }
    
    @Override
    public RamdiskImageInfo getRamdisk( ) {
      return this.ramdisk;
    }
  }

  /**
   * Temporary: Don't even think of referencing this symbol.
   */
  @Deprecated 
  public static BootableSet newBootableSet( VmType vmType, Partition partition, String imageId, String kernelId, String ramdiskId ) throws MetadataException, AuthException {
    BootableSet bootSet = null;
    try {
      bootSet = new BootableSet( LookupMachine.INSTANCE.apply( imageId ) );
    } catch ( Exception e ) {
      try {
        bootSet = new BootableSet( LookupBlockStorage.INSTANCE.apply( imageId ) );
      } catch ( IllegalContextAccessException ex ) {
        throw new VerificationException( ex );
      } catch ( NoSuchElementException ex ) {
        throw new NoSuchMetadataException( ex );
      } catch ( PersistenceException ex ) {
        throw new InvalidMetadataException( ex );
      }
    }
    KernelImageInfo kernel = ( kernelId == null ? null : LookupKernel.INSTANCE.apply( kernelId ) );
    RamdiskImageInfo ramdisk = ( ramdiskId == null ? null : LookupRamdisk.INSTANCE.apply( ramdiskId ) );
    if( kernel != null && ramdisk != null ) {
      return new TrifectaBootableSet( bootSet.getMachine( ), kernel, ramdisk );
    } else if ( kernel != null ) {
      return new NoRamdiskBootableSet( bootSet.getMachine( ), kernel );
    } else {
      return bootSet;
    }

  }
  public static BootableSet newBootableSet( VmType vmType, Partition partition, String imageId ) throws MetadataException, AuthException {
    BootableSet bootSet = null;
    try {
      bootSet = new BootableSet( RestrictedTypes.doPrivileged( imageId, LookupMachine.INSTANCE ) );
    } catch ( Exception e ) {
      try {
        bootSet = new BootableSet( RestrictedTypes.doPrivileged( imageId, LookupBlockStorage.INSTANCE ) );
      } catch ( IllegalContextAccessException ex ) {
        throw new VerificationException( ex );
      } catch ( NoSuchElementException ex ) {
        throw new NoSuchMetadataException( ex );
      } catch ( PersistenceException ex ) {
        throw new InvalidMetadataException( ex );
      }
    }
    if ( bootSet.isLinux( ) ) {
      bootSet = Emis.bootsetWithKernel( bootSet );
      bootSet = Emis.bootsetWithRamdisk( bootSet );
    }
    Emis.checkStoredImage( bootSet );
    return bootSet;
  }
  
  private static BootableSet bootsetWithKernel( BootableSet bootSet ) throws MetadataException {
    String kernelId = determineKernelId( bootSet );
    LOG.debug( "Determined the appropriate kernelId to be " + kernelId + " for " + bootSet.toString( ) );
    try {
      KernelImageInfo kernel = RestrictedTypes.doPrivileged( kernelId, LookupKernel.INSTANCE );
      return new NoRamdiskBootableSet( bootSet.getMachine( ), kernel );
    } catch ( Exception ex ) {
      throw new NoSuchMetadataException( "Failed to lookup kernel image information: " + kernelId + " because of: " + ex.getMessage( ), ex );
    }
  }
  
  private static BootableSet bootsetWithRamdisk( BootableSet bootSet ) throws MetadataException {
    String ramdiskId = determineRamdiskId( bootSet );
    LOG.debug( "Determined the appropriate ramdiskId to be " + ramdiskId + " for " + bootSet.toString( ) );
    if ( ramdiskId == null ) {
      return bootSet;
    } else {
      try {
        RamdiskImageInfo ramdisk = RestrictedTypes.doPrivileged( ramdiskId, LookupRamdisk.INSTANCE );
        return new TrifectaBootableSet( bootSet.getMachine( ), bootSet.getKernel( ), ramdisk );
      } catch ( Exception ex ) {
        throw new NoSuchMetadataException( "Failed to lookup ramdisk image information: " + ramdiskId + " because of: " + ex.getMessage( ), ex );
      }
    }
  }
  
  private static String determineKernelId( BootableSet bootSet ) throws MetadataException {
    BootableImageInfo disk = bootSet.getMachine( );
    String kernelId = null;
    Context ctx = null;
    try {
      ctx = Contexts.lookup( );
      if ( ctx.getRequest( ) instanceof RunInstancesType ) {
        kernelId = ( ( RunInstancesType ) ctx.getRequest( ) ).getKernelId( );
      }
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
    }
    if ( kernelId == null || "".equals( kernelId ) ) {
      kernelId = disk.getKernelId( );
    }
    if ( kernelId == null || "".equals( kernelId ) ) {
      kernelId = Images.lookupDefaultKernelId( );
    }
    Preconditions.checkNotNull( kernelId, "Attempt to resolve a kerneId for " + bootSet.toString( ) + " during request " + ( ctx != null
      ? ctx.getRequest( ).toSimpleString( )
      : "UNKNOWN" ) );
    if ( kernelId == null ) {
      throw new NoSuchMetadataException( "Unable to determine required kernel image for " + disk.getDisplayName( ) );
    } else if ( !kernelId.startsWith( ImageMetadata.Type.kernel.getTypePrefix( ) ) ) {
      throw new InvalidMetadataException( "Image specified is not a kernel: " + kernelId );
    }
    return kernelId;
  }
  
  private static String determineRamdiskId( BootableSet bootSet ) throws MetadataException {
    if ( !bootSet.hasKernel( ) ) {
      throw new InvalidMetadataException( "Image specified does not have a kernel: " + bootSet );
    }
    boolean skipRamdisk = false;
    String ramdiskId = null;
    Context ctx = null;
    try {
      ctx = Contexts.lookup( );
      if ( ctx.getRequest( ) instanceof RunInstancesType ) {
        RunInstancesType msg = ( RunInstancesType ) ctx.getRequest( );
        if ( ImageUtil.isSet( msg.getKernelId( ) ) ) {
          skipRamdisk |= ( !ImageUtil.isSet( msg.getRamdiskId( ) ) );//explicit kernel given w/o rd
        } else {
          skipRamdisk |= ( ImageUtil.isSet( bootSet.getMachine( ).getKernelId( ) )
                           && !ImageUtil.isSet( bootSet.getMachine( ).getRamdiskId( ) )
                         && !ImageUtil.isSet( msg.getRamdiskId( ) ) );//no explicit kernel, skip default ramdiskId if none other given
        }
        ramdiskId = ( ( RunInstancesType ) ctx.getRequest( ) ).getRamdiskId( );
      }
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
    }
    if ( ramdiskId == null || "".equals( ramdiskId ) ) {
      ramdiskId = bootSet.getMachine( ).getRamdiskId( );
    }
    if ( ramdiskId == null || "".equals( ramdiskId ) ) {
      ramdiskId = ImageConfiguration.getInstance( ).getDefaultRamdiskId( );
    }
    Preconditions.checkNotNull( ramdiskId, "Attempt to resolve a ramdiskId for " + bootSet.toString( ) + " during request " + ( ctx != null
        ? ctx.getRequest( ).toSimpleString( )
        : "UNKNOWN" ) );
    if ( ramdiskId == null ) {
      throw new InvalidMetadataException( "Unable to determine required ramdisk image for " + bootSet.toString( ) );
    } else if ( !ramdiskId.startsWith( ImageMetadata.Type.ramdisk.getTypePrefix( ) ) ) {
      throw new InvalidMetadataException( "Image specified is not a ramdisk: " + ramdiskId );
    }
    return ramdiskId;
  }
  
  public static void checkStoredImage( BootableSet bootSet ) {
    try {
      if ( bootSet.getMachine( ) instanceof StaticDiskImage ) {
        ImageUtil.checkStoredImage( ( StaticDiskImage ) bootSet.getMachine( ) );
      }
      if ( bootSet.hasKernel( ) ) {
        ImageUtil.checkStoredImage( bootSet.getKernel( ) );
      }
      if ( bootSet.hasRamdisk( ) ) {
        ImageUtil.checkStoredImage( bootSet.getRamdisk( ) );
      }
    } catch ( EucalyptusCloudException ex ) {
      LOG.error( ex, ex );
    }
  }
  
}
