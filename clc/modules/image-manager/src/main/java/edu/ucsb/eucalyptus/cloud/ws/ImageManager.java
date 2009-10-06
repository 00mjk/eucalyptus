/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.eucalyptus.images.util.ImageUtil;
import com.eucalyptus.images.util.WalrusUtil;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.ServiceDispatcher;

import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ProductCode;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.UserGroupInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class ImageManager {
  public static Logger LOG = Logger.getLogger( ImageManager.class );

  public VmImageInfo resolve( VmInfo vmInfo ) throws EucalyptusCloudException {
    String walrusUrl = ImageUtil.getWalrusUrl( );
    ArrayList<String> productCodes = Lists.newArrayList( );
    ImageInfo diskInfo = null, kernelInfo = null, ramdiskInfo = null;
    String diskUrl = null, kernelUrl = null, ramdiskUrl = null;

    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      diskInfo = db.getUnique( ImageInfo.named( vmInfo.getImageId( ) ) );
      for ( ProductCode p : diskInfo.getProductCodes( ) ) {
        productCodes.add( p.getValue( ) );
      }
      diskUrl = ImageUtil.getImageUrl( walrusUrl, diskInfo );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
    }
    VmImageInfo vmImgInfo = new VmImageInfo( vmInfo.getImageId( ), vmInfo.getKernelId( ), vmInfo.getRamdiskId( ), diskUrl, null, null, productCodes );
    ArrayList<String> ancestorIds = ImageUtil.getAncestors( vmInfo.getOwnerId( ), diskInfo.getImageLocation( ) );
    vmImgInfo.setAncestorIds( ancestorIds );
    return vmImgInfo;
  }

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    String walrusUrl = ImageUtil.getWalrusUrl( );

    RunInstancesType msg = vmAllocInfo.getRequest( );
    ImageInfo searchDiskInfo = new ImageInfo( msg.getImageId( ) );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    ImageInfo diskInfo = null;
    ArrayList<String> productCodes = Lists.newArrayList( );
    try {
      diskInfo = db.getUnique( searchDiskInfo );
      for ( ProductCode p : diskInfo.getProductCodes( ) ) {
        productCodes.add( p.getValue( ) );
      }
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to find disk image: " + msg.getImageId( ) );
    }
    UserInfo user = null;
    try {
      user = db.recast( UserInfo.class ).getUnique( new UserInfo( msg.getUserId( ) ) );
    } catch ( Exception e1 ) {
      db.rollback( );
      throw new EucalyptusCloudException( "You do not have permissions to run this image." );
    }
    if ( !diskInfo.isAllowed( user ) ) {
      db.rollback( );
      throw new EucalyptusCloudException( "You do not have permissions to run this image." );
    }
    if ( "deregistered".equals( diskInfo.getImageState() ) ) {
      db.delete( diskInfo );
      db.rollback( );
      throw new EucalyptusCloudException( "The requested image is deregistered." );
    }

    String defaultKernelId = null;
    String defaultRamdiskId = null;
    try {
      defaultKernelId = EucalyptusProperties.getSystemConfiguration( ).getDefaultKernel( );
      defaultRamdiskId = EucalyptusProperties.getSystemConfiguration( ).getDefaultRamdisk( );
    } catch ( Exception e1 ) {}
    String kernelId = ImageUtil.getImageInfobyId( msg.getKernelId( ), diskInfo.getKernelId( ), defaultKernelId );
    if ( kernelId == null ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Unable to determine required kernel image." );
    }

    String ramdiskId = ImageUtil.getImageInfobyId( msg.getRamdiskId( ), diskInfo.getRamdiskId( ), defaultRamdiskId );

    ImageInfo kernelInfo = null;
    try {
      kernelInfo = db.getUnique( new ImageInfo( kernelId ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to find kernel image: " + kernelId );
    }
    if ( !diskInfo.isAllowed( user ) ) {
      db.rollback( );
      throw new EucalyptusCloudException( "You do not have permission to launch: " + diskInfo.getImageId( ) );
    }
    if ( !kernelInfo.isAllowed( user ) ) {
      db.rollback( );
      throw new EucalyptusCloudException( "You do not have permission to launch: " + kernelInfo.getImageId( ) );
    }
    ImageInfo ramdiskInfo = null;
    if ( ramdiskId != null ) {
      try {
        ramdiskInfo = db.getUnique( new ImageInfo( ramdiskId ) );
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        throw new EucalyptusCloudException( "Failed to find ramdisk image: " + ramdiskId );
      }
      if ( !ramdiskInfo.isAllowed( user ) ) {
        db.rollback( );
        throw new EucalyptusCloudException( "You do not have permission to launch: " + ramdiskInfo.getImageId( ) );
      }
    }
    db.commit( );
    // :: quietly add the ancestor and size information to the vm info object...
    // this should never fail noisily :://
    ArrayList<String> ancestorIds = ImageUtil.getAncestors( msg.getUserId( ), diskInfo.getImageLocation( ) );
    Long imgSize = ImageUtil.getSize( msg.getUserId( ), diskInfo.getImageLocation( ) );
    if( !"kernel".equals( kernelInfo.getImageType( ) ) ) {
      throw new EucalyptusCloudException( "Image specified is not a kernel: " + kernelInfo.toString( ) );
    }
    if( !"ramdisk".equals( ramdiskInfo.getImageType( ) ) ) {
      throw new EucalyptusCloudException( "Image specified is not a ramdisk: " + ramdiskInfo.toString( ) );
    }
    ImageUtil.checkStoredImage( kernelInfo );
    ImageUtil.checkStoredImage( diskInfo );
    ImageUtil.checkStoredImage( ramdiskInfo );

    // :: get together the required URLs ::/
    VmImageInfo vmImgInfo = ImageUtil.getVmImageInfo( walrusUrl, diskInfo, kernelInfo, ramdiskInfo, productCodes );
    vmImgInfo.setAncestorIds( ancestorIds );
    vmImgInfo.setSize( imgSize );
    vmAllocInfo.setImageInfo( vmImgInfo );
    return vmAllocInfo;
  }

  public DescribeImagesResponseType describe( DescribeImagesType request ) throws EucalyptusCloudException {
    DescribeImagesResponseType reply = ( DescribeImagesResponseType ) request.getReply( );
    ImageUtil.cleanDeregistered( );
    List<ImageInfo> imgList = Lists.newArrayList( );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    for( String imageId : request.getImagesSet( ) ) {
      try {
        imgList.add( db.getUnique( ImageInfo.named( imageId ) ) );
      } catch ( Throwable e ) {}
    }
    UserInfo user = null;
    try {
      user = db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId( ) ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.commit( );
      throw new EucalyptusCloudException( "Failed to find user information for: " + request.getUserId( ), e );
    }
    ArrayList<String> imageList = request.getImagesSet( );
    ArrayList<String> owners = request.getOwnersSet( );
    ArrayList<String> executable = request.getExecutableBySet( );
    if ( owners.isEmpty( ) && executable.isEmpty( ) ) {
      executable.add( "self" );
    }
    Set<ImageDetails> repList = Sets.newHashSet( );
    if ( !owners.isEmpty( ) ) {
      repList.addAll( ImageUtil.getImagesByOwner( imgList, user, owners ) );
    }
    if ( !executable.isEmpty( ) ) {
      if ( executable.remove( "self" ) ) {
        repList.addAll( ImageUtil.getImageOwnedByUser( imgList, user ) );
      }
      repList.addAll( ImageUtil.getImagesByExec( user, executable ) );
    }
    reply.getImagesSet( ).addAll( repList );
    return reply;
  }

  public RegisterImageResponseType register( RegisterImageType request ) throws EucalyptusCloudException {
    String imageLocation = request.getImageLocation( );
    String[] imagePathParts;
    try {
      imagePathParts = ImageUtil.getImagePathParts( imageLocation );
      ImageUtil.checkBucketAcl( request, imagePathParts );
    } catch ( EucalyptusCloudException e ) {
      LOG.trace( e, e );
      throw e;
    }
    ImageInfo imageInfo = new ImageInfo( imageLocation, request.getUserId( ), "available", true );
    try {
      WalrusUtil.verifyManifestIntegrity( imageInfo );
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Image registration failed because the manifest referenced is invalid or unavailable." );
    }
    String userName = request.getUserId( );
    // FIXME: wrap this manifest junk in a helper class.
    Document inputSource = ImageUtil.getManifestDocument( imagePathParts, userName );
    XPath xpath = XPathFactory.newInstance( ).newXPath( );
    String arch = ImageUtil.extractArchitecture( inputSource, xpath );
    imageInfo.setArchitecture( ( arch == null ) ? "i386" : arch );
    String kernelId = ImageUtil.extractKernelId( inputSource, xpath );
    String ramdiskId = ImageUtil.extractRamdiskId( inputSource, xpath );
    List<ProductCode> prodCodes = extractProductCodes( inputSource, xpath );
    imageInfo.getProductCodes( ).addAll( prodCodes );

    if ( "yes".equals( kernelId ) || "true".equals( kernelId ) || imagePathParts[1].startsWith( "vmlinuz" ) ) {
      if ( !request.isAdministrator( ) ) throw new EucalyptusCloudException( "Only administrators can register kernel images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_KERNEL );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_KERNEL_PREFIX, imageInfo.getImageLocation( ) ) );
    } else if ( "yes".equals( ramdiskId ) || "true".equals( ramdiskId ) || imagePathParts[1].startsWith( "initrd" ) ) {
      if ( !request.isAdministrator( ) ) throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_RAMDISK );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_RAMDISK_PREFIX, imageInfo.getImageLocation( ) ) );
    } else {
      if ( kernelId != null ) {
        try {
          ImageUtil.getImageInfobyId( kernelId );
        } catch ( EucalyptusCloudException e ) {
          throw new EucalyptusCloudException( "Referenced kernel id is invalid: " + kernelId );
        }
      }
      if ( ramdiskId != null ) {
        try {
          ImageUtil.getImageInfobyId( ramdiskId );
        } catch ( EucalyptusCloudException e ) {
          throw new EucalyptusCloudException( "Referenced ramdisk id is invalid: " + ramdiskId );
        }
      }
      imageInfo.setImageType( EucalyptusProperties.IMAGE_MACHINE );
      imageInfo.setKernelId( kernelId );
      imageInfo.setRamdiskId( ramdiskId );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_MACHINE_PREFIX, imageInfo.getImageLocation( ) ) );
    }

    String signature = null;
    try {
      signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage( ) );
    }
    imageInfo.setSignature( signature );

    // FIXME: this is sorely in need of an update and clean up.
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      db.add( imageInfo );
      UserInfo user = db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId( ) ) );
      UserGroupInfo group = db.recast( UserGroupInfo.class ).getUnique( new UserGroupInfo( "all" ) );
      imageInfo.getPermissions( ).add( user );
      imageInfo.getUserGroups( ).add( group );
      db.commit( );
      LOG.info( "Registering image pk=" + imageInfo.getId( ) + " ownerId=" + user.getUserName( ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw e;
    }

    LOG.info( "Triggering cache population in Walrus for: " + imageInfo.getId( ) );
    WalrusUtil.checkValid( imageInfo );
    WalrusUtil.triggerCaching( imageInfo );

    RegisterImageResponseType reply = ( RegisterImageResponseType ) request.getReply( );
    reply.setImageId( imageInfo.getImageId( ) );
    return reply;
  }

  private List<ProductCode> extractProductCodes( Document inputSource, XPath xpath ) {
    List<ProductCode> prodCodes = Lists.newArrayList( );
    NodeList productCodes = null;
    try {
      productCodes = ( NodeList ) xpath.evaluate( "/manifest/machine_configuration/product_codes/product_code/text()", inputSource, XPathConstants.NODESET );
      for ( int i = 0; i < productCodes.getLength( ); i++ ) {
        for ( String productCode : productCodes.item( i ).getNodeValue( ).split( "," ) ) {
          prodCodes.add( new ProductCode( productCode ) );
        }
      }
    } catch ( XPathExpressionException e ) {
      LOG.error( e, e );
    }
    return prodCodes;
  }

  public DeregisterImageResponseType deregister( DeregisterImageType request ) throws EucalyptusCloudException {
    DeregisterImageResponseType reply = ( DeregisterImageResponseType ) request.getReply( );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );

    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( new ImageInfo( request.getImageId( ) ) );
      if ( !imgInfo.getImageOwnerId( ).equals( request.getUserId( ) ) && !request.isAdministrator( ) ) throw new EucalyptusCloudException( "Only the owner of a registered image or the administrator can deregister it." );
      WalrusUtil.invalidate( imgInfo );
      db.commit( );
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      reply.set_return( false );
      db.rollback( );
    }
    return reply;
  }

  public ConfirmProductInstanceResponseType confirmProductInstance( ConfirmProductInstanceType request ) throws EucalyptusCloudException {
    ConfirmProductInstanceResponseType reply = ( ConfirmProductInstanceResponseType ) request.getReply( );
    reply.set_return( false );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
      EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
      try {
        ImageInfo found = db.getUnique( new ImageInfo( vm.getImageInfo( ).getImageId( ) ) );
        if ( found.getProductCodes( ).contains( new ProductCode( request.getProductCode( ) ) ) ) {
          reply.set_return( true );
          reply.setOwnerId( found.getImageOwnerId( ) );
        }
        db.commit( );
      } catch ( EucalyptusCloudException e ) {
        db.commit( );
      }
    } catch ( NoSuchElementException e ) {}
    return reply;
  }

  public DescribeImageAttributeResponseType describeImageAttribute( DescribeImageAttributeType request ) throws EucalyptusCloudException {
    DescribeImageAttributeResponseType reply = ( DescribeImageAttributeResponseType ) request.getReply( );
    reply.setImageId( request.getImageId( ) );

    if ( request.getAttribute( ) != null ) request.applyAttribute( );

    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      ImageInfo imgInfo = db.getUnique( new ImageInfo( request.getImageId( ) ) );
      if ( !imgInfo.isAllowed( db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId( ) ) ) ) ) throw new EucalyptusCloudException( "image attribute: not authorized." );
      if ( request.getKernel( ) != null ) {
        reply.setRealResponse( reply.getKernel( ) );
        if ( imgInfo.getKernelId( ) != null ) {
          reply.getKernel( ).add( imgInfo.getKernelId( ) );
        }
      } else if ( request.getRamdisk( ) != null ) {
        reply.setRealResponse( reply.getRamdisk( ) );
        if ( imgInfo.getRamdiskId( ) != null ) {
          reply.getRamdisk( ).add( imgInfo.getRamdiskId( ) );
        }
      } else if ( request.getLaunchPermission( ) != null ) {
        reply.setRealResponse( reply.getLaunchPermission( ) );
        for ( UserGroupInfo userGroup : imgInfo.getUserGroups( ) )
          reply.getLaunchPermission( ).add( LaunchPermissionItemType.getGroup( userGroup.getName( ) ) );
        for ( UserInfo user : imgInfo.getPermissions( ) )
          reply.getLaunchPermission( ).add( LaunchPermissionItemType.getUser( user.getUserName( ) ) );
      } else if ( request.getProductCodes( ) != null ) {
        reply.setRealResponse( reply.getProductCodes( ) );
        for ( ProductCode p : imgInfo.getProductCodes( ) ) {
          reply.getProductCodes( ).add( p.getValue( ) );
        }
      } else if ( request.getBlockDeviceMapping( ) != null ) {
        reply.setRealResponse( reply.getBlockDeviceMapping( ) );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EMI );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EPHEMERAL );
        reply.getBlockDeviceMapping( ).add( ImageUtil.SWAP );
        reply.getBlockDeviceMapping( ).add( ImageUtil.ROOT );
      } else {
        throw new EucalyptusCloudException( "invalid image attribute request." );
      }
    } catch ( EucalyptusCloudException e ) {
      db.commit( );
      throw e;
    }
    return reply;
  }

  public ModifyImageAttributeResponseType modifyImageAttribute( ModifyImageAttributeType request ) throws EucalyptusCloudException {
    ModifyImageAttributeResponseType reply = ( ModifyImageAttributeResponseType ) request.getReply( );

    if ( request.getAttribute( ) != null ) request.applyAttribute( );

    if ( request.getProductCodes( ).isEmpty( ) ) {
      reply.set_return( ImageUtil.modifyImageInfo( request.getImageId( ), request.getUserId( ), request.isAdministrator( ), request.getAdd( ), request.getRemove( ) ) );
    } else {
      EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
      ImageInfo imgInfo = null;
      try {
        imgInfo = db.getUnique( new ImageInfo( request.getImageId( ) ) );
        for ( String productCode : request.getProductCodes( ) ) {
          ProductCode prodCode = new ProductCode( productCode );
          if ( !imgInfo.getProductCodes( ).contains( prodCode ) ) {
            imgInfo.getProductCodes( ).add( prodCode );
          }
        }
        db.commit( );
        reply.set_return( true );
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        reply.set_return( false );
      }
    }
    return reply;
  }

  public ResetImageAttributeResponseType resetImageAttribute( ResetImageAttributeType request ) throws EucalyptusCloudException {
    ResetImageAttributeResponseType reply = ( ResetImageAttributeResponseType ) request.getReply( );
    reply.set_return( true );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      ImageInfo imgInfo = db.getUnique( new ImageInfo( request.getImageId( ) ) );

      if ( !request.getUserId( ).equals( imgInfo.getImageOwnerId( ) ) && !request.isAdministrator( ) ) throw new EucalyptusCloudException( "Not allowed to modify image: " + imgInfo.getImageId( ) );
      imgInfo.getPermissions( ).clear( );
      imgInfo.getUserGroups( ).clear( );
      imgInfo.getUserGroups( ).add( db.recast( UserGroupInfo.class ).getUnique( UserGroupInfo.named( "all" ) ) );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      reply.set_return( false );
    }
    return reply;
  }
}
