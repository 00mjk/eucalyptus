package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.cloud.ws.VMwareBrokerProperties;
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Dmitrii Zagorodnov <dmitrii@eucalyptus.com>
 */

// parent to all requests/replies used by VMwareBroker/NC,
// which inherit correlationId and userId from EucalyptusMessage

public class VMwareBrokerRequestType extends EucalyptusMessage {
	String nodeName;

  	@Override
	public String toString() {
		toString("");
	}

  	@Override
    public String toString(String msg) {
  		String paddedMsg = (msg.size()>0 && !msg.endsWith(" "))?(msg+" "):(msg);
    	return "VMwareBrokerRequest {" + paddedMsg + "userId=" + getUserId() + " nodeName=" + this.nodeName + "}";
    }
}

public class VMwareBrokerResponseType extends EucalyptusMessage {

	public static final String STATUS_MSG_NOT_SUPPORTED = "method not supported";
	public static final String STATUS_MSG_SUCCESS = "method succeeded";
	public static final String STATUS_MSG_FAILURE = "method failed";
	
  	def VMwareBrokerResponseType() {}

  	@Override
  	public void setStatusMessage (String msg) {
  		// TODO: don't suppress the message once CC can accept statusMessage 
  	}

  	// unused (handler must cast the response)
  	public VMwareBrokerResponseType withSuccess () {
  		withSuccess (STATUS_MSG_SUCCESS); // default OK message
  	}
  	
  	public VMwareBrokerResponseType withSuccess (String msg) {
  		this.setStatusMessage (msg);
  		this.set_return (false);
  		return this;
  	}

  	// unused
  	public VMwareBrokerResponseType withFailure () {
  		withFailure (STATUS_MSG_NOT_SUPPORTED); // default error message
  	}
  	
  	public VMwareBrokerResponseType withFailure (String msg) {
  		this.setStatusMessage (msg);
  		this.set_return (false);
  		return this;
  	}
}

// fields used by {Run|Describe}Instance

public class VirtualMachineType extends EucalyptusData {
	Integer memory;
	Integer cores;
	Integer disk;
	
	VirtualMachineType () {}
	
	VirtualMachineType (int cores, int disk, int memory) {
		this.memory = memory;
		this.cores = cores;
		this.disk = disk;
	}
}

public class NetConfigType extends EucalyptusData {
	String macAddress;
	String ignoredMacAddress;
	int vlan;
	String ipAddress;
	String ignoredPublicIp;
	
	NetConfigType () {}
	
	NetConfigType (String macAddress, String ignoredMacAddress, int vlan, String ipAddress, String ignoredPublicIp) {
		this.macAddress = macAddress;
		this.ignoredMacAddress = ignoredMacAddress;
		this.vlan = vlan;
		this.ipAddress = ipAddress;
		this.ignoredPublicIp = ignoredPublicIp;
	}
}

public class VolumeType extends EucalyptusData {
	String volumeId;
	String remoteDevice;
	String device;
	String status;
	
	VolumeType () {}
	
	VolumeType (String volumeId, String remoteDevice, String device, String status) {
		this.volumeId = volumeId;
		this.remoteDevice = remoteDevice;
		this.device = device;
		this.status = status;
	}
}

public class InstanceType extends EucalyptusData {
	String reservationId;
	String instanceId;
	String imageId;
	String imageURL; // extra
	String kernelId;
	String kernelURL; // extra
	String ramdiskId;
	String ramdiskURL; // extra
	String userId;
	String keyName;
	VirtualMachineType instanceType;
	NetConfigType netParams;
	String stateName;
	String launchTime;
	String userData;
	String launchIndex;
	ArrayList<String> groupNames = new ArrayList<String>();
	ArrayList<VolumeType> volumes = new ArrayList<VolumeType>();
	String serviceTag;
}

// DescribeResource

public class EucalyptusNCNcDescribeResourceType extends VMwareBrokerRequestType {		
    String resourceType;
    def EucalyptusNCNcDescribeResourceType() {}
   
  	@Override
    public String toString() {
    	return super.toString ("DescribeResource: resourceType=" + this.resourceType);
    }
}

public class EucalyptusNCNcDescribeResourceResponseType extends VMwareBrokerResponseType {
    String nodeStatus;
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    String publicSubnets;
    def EucalyptusNCNcDescribeResourceResponseType() {}
}

// GetConsoleOutput

public class EucalyptusNCNcGetConsoleOutputType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcGetConsoleOutputType() {}

  	@Override
    public String toString() {
    	return super.toString ("GetConsoleOutput: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcGetConsoleOutputResponseType extends VMwareBrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcGetConsoleOutputResponseType() {}
}

// DescribeInstances

public class EucalyptusNCNcDescribeInstancesType extends VMwareBrokerRequestType {
    ArrayList<String> instanceIds = new ArrayList<String>();
    def EucalyptusNCNcDescribeInstancesType() {}

  	@Override
    public String toString() {
    	return super.toString ("DescribeInstances instances=" + this.instanceIds);
    }
}

public class EucalyptusNCNcDescribeInstancesResponseType extends VMwareBrokerResponseType {
	ArrayList<InstanceType> instances = new ArrayList<InstanceType>();
    def EucalyptusNCNcDescribeInstancesResponseType() {}
}

// RunInstance

public class EucalyptusNCNcRunInstanceType extends VMwareBrokerRequestType {
	String imageId;
	String kernelId;
	String ramdiskId;
	String imageURL;
	String kernelURL;
	String ramdiskURL;
	String instanceId;
	VirtualMachineType instanceType;
	String keyName;
	String publicMacAddress;
	String privateMacAddress;
	String reservationId;
	Integer vlan;
	String userData;
	String launchIndex;
	ArrayList<String> groupNames = new ArrayList<String>();
    def EucalyptusNCNcRunInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("RunInstance instanceId=" + this.instanceId + " imageId=" + this.imageId);
    }
}
	
public class EucalyptusNCNcRunInstanceResponseType extends VMwareBrokerResponseType {
    InstanceType instance;
    def EucalyptusNCNcRunInstanceResponseType() {}
}

// TerminateInstance

public class EucalyptusNCNcTerminateInstanceType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcTerminateInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("TerminateInstance: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcTerminateInstanceResponseType extends VMwareBrokerResponseType {
    String instanceId;
	String shutdownState;
	String previousState;
    def EucalyptusNCNcTerminateInstanceResponseType() {}
}

// StartNetwork

public class EucalyptusNCNcStartNetworkType extends VMwareBrokerRequestType {
	ArrayList<String> remoteHosts = new ArrayList<String>();
    Integer remoteHostPort;
    Integer vlan;
    def EucalyptusNCNcStartNetworkType() {}

  	@Override
    public String toString() {
    	return super.toString ("StartNetwork: remoteHosts=" + this.remoteHosts + " port=" + this.remoteHostPort + " vlan= "+ this.vlan);
    }
}

public class EucalyptusNCNcStartNetworkResponseType extends VMwareBrokerResponseType {
    String networkStatus;
    def EucalyptusNCNcStartNetworkResponseType() {}
}

// RebootInstance

public class EucalyptusNCNcRebootInstanceType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcRebootInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("RebootInstance: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcRebootInstanceResponseType extends VMwareBrokerResponseType {
    Boolean	status;
    def EucalyptusNCNcRebootInstanceResponseType() {}
}

// AttachVolume

public class EucalyptusNCNcAttachVolumeType extends VMwareBrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    def EucalyptusNCNcAttachVolumeType() {}

  	@Override
    public String toString() {
    	return super.toString ("AttachVolume: instanceId=" + this.instanceId + " volumeId=" + this.volumeId);
    }
}

public class EucalyptusNCNcAttachVolumeResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcAttachVolumeResponseType() {}
}

// DetachVolume

public class EucalyptusNCNcDetachVolumeType extends VMwareBrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    Boolean force;
    def EucalyptusNCNcDetachVolumeType() {}

  	@Override
    public String toString() {
    	return super.toString ("DetachVolume: instanceId=" + this.instanceId + " volumeId=" + this.volumeId);
    }
}

public class EucalyptusNCNcDetachVolumeResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcDetachVolumeResponseType() {}
}

// PowerDown

public class EucalyptusNCNcPowerDownType extends VMwareBrokerRequestType {
    def EucalyptusNCNcPowerDownType() {}

  	@Override
    public String toString() {
    	return super.toString ("PowerDown: ");
    }
}

public class EucalyptusNCNcPowerDownResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcPowerDownResponseType() {}
}

// Template 
/*
public class EucalyptusNCNcXXXType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcXXXType() {}
}

public class EucalyptusNCNcXXXResponseType extends VMwareBrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcXXXResponseType() {}
}
*/

