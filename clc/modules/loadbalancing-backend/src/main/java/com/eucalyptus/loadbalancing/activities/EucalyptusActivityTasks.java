/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.loadbalancing.activities;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.CreateRoleResponseType;
import com.eucalyptus.auth.euare.CreateRoleType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.DeleteRoleType;
import com.eucalyptus.auth.euare.EuareMessage;
import com.eucalyptus.auth.euare.GetRolePolicyResponseType;
import com.eucalyptus.auth.euare.GetRolePolicyResult;
import com.eucalyptus.auth.euare.GetRolePolicyType;
import com.eucalyptus.auth.euare.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.GetServerCertificateType;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.ListRolePoliciesResponseType;
import com.eucalyptus.auth.euare.ListRolePoliciesType;
import com.eucalyptus.auth.euare.ListRolesResponseType;
import com.eucalyptus.auth.euare.ListRolesType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.RemoveRoleFromInstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationNames;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.DescribeImagesResponseType;
import com.eucalyptus.compute.common.DescribeImagesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseType;
import com.eucalyptus.compute.common.DescribeKeyPairsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.GroupIdSetType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupType;
import com.eucalyptus.compute.common.backend.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesType;
import com.eucalyptus.compute.common.backend.ModifyInstanceAttributeType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupIngressType;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

import com.eucalyptus.compute.common.backend.CreateTagsType;
import com.eucalyptus.compute.common.backend.DeleteTagsType;


/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EucalyptusActivityTasks {
	private static final Logger LOG = Logger.getLogger( EucalyptusActivityTask.class );
	private EucalyptusActivityTasks() {}
	private static  EucalyptusActivityTasks _instance = new EucalyptusActivityTasks();
	public static EucalyptusActivityTasks getInstance(){
		return _instance;
	}

	private interface ActivityContext<TM extends BaseMessage, TC extends ComponentId> {
	  DispatchingClient<TM, TC> getClient();
	}

	private abstract class ActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> implements ActivityContext<TM, TC>{
		private final Class<TC> componentIdClass;
		private ActivityContextSupport( final Class<TC> componentIdClass ) {
			this.componentIdClass = componentIdClass;
		}

		abstract String getUserId( );

		/**
		 * Account to use if user identifier not set, should not be called otherwise.
		 */
		abstract AccountFullName getAccount( );

		@Override
		public DispatchingClient<TM, TC> getClient() {
			try{
				final DispatchingClient<TM, TC> client =
						getUserId( ) != null ?
								new DispatchingClient<TM, TC>( this.getUserId( ), this.componentIdClass ) :
								new DispatchingClient<TM, TC>( this.getAccount(), this.componentIdClass );
				client.init();
				return client;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}

	private abstract class SystemActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> extends ActivityContextSupport<TM, TC>{
	  boolean useELBSystemAccount = true;
	  private SystemActivityContextSupport( final Class<TC> componentIdClass ) {
			super( componentIdClass );
		}
		
		private SystemActivityContextSupport( final Class<TC> componentIdClass, final boolean useELBSystemAccount ) {
      super( componentIdClass );
      this.useELBSystemAccount = useELBSystemAccount;
		}
    
		@Override
		final String getUserId() {
			return null;
		}

		@Override
		AccountFullName getAccount() {
			try{
				return AccountFullName.getInstance( Accounts.lookupAccountIdByAlias( this.useELBSystemAccount ?
						AccountIdentifiers.ELB_SYSTEM_ACCOUNT :
						AccountIdentifiers.SYSTEM_ACCOUNT
				) );
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
  }

	private abstract class UserActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> extends ActivityContextSupport<TM, TC>{
		private final String userId;
		private final AccountFullName accountFullName;

		private UserActivityContextSupport(
				final Class<TC> componentIdClass,
				final String userId
		) {
			super( componentIdClass );
			this.userId = userId;
			this.accountFullName = null;
		}

		private UserActivityContextSupport(
				final Class<TC> componentIdClass,
				final AccountFullName accountFullName
		) {
			super( componentIdClass );
			this.userId = null;
			this.accountFullName = accountFullName;
		}

		public final String getUserId() {
			return userId;
		}

		public final AccountFullName getAccount() {
			return accountFullName;
		}
	}

	private class EuareSystemActivity extends SystemActivityContextSupport<EuareMessage, Euare>{
		private EuareSystemActivity( ){ super( Euare.class ); }
		private EuareSystemActivity( final boolean useELBSystemAccount ){
		  super( Euare.class, useELBSystemAccount );
		}
	}
	
	private class EuareUserActivity extends UserActivityContextSupport<EuareMessage, Euare> {
	  private EuareUserActivity(final AccountFullName accountFullName){
	    super( Euare.class, accountFullName );
	  }
	}
	
	private class AutoScalingSystemActivity extends SystemActivityContextSupport<AutoScalingMessage, AutoScaling>{
		private AutoScalingSystemActivity(final boolean useELBSystemAccount){
		  super( AutoScaling.class, useELBSystemAccount );
		}
	}
	
	private class CloudWatchUserActivity extends UserActivityContextSupport<CloudWatchMessage, CloudWatch>{
		private CloudWatchUserActivity(final String userId){
			super( CloudWatch.class, userId );
		}
	}
	
	private class EmpyreanSystemActivity extends SystemActivityContextSupport<EmpyreanMessage, Empyrean>{
		private EmpyreanSystemActivity() { super( Empyrean.class, false ); }
	}
	
	public class EucalyptusSystemActivity extends SystemActivityContextSupport<ComputeMessage, Eucalyptus>{
    public EucalyptusSystemActivity() { super( Eucalyptus.class ); }
	  public EucalyptusSystemActivity(final boolean useELBSystemAccount) {
	    super(Eucalyptus.class, useELBSystemAccount);
	  }
	}

	private class EucalyptusUserActivity extends UserActivityContextSupport<ComputeMessage, Eucalyptus>{
		private EucalyptusUserActivity(final AccountFullName accountFullName){
			super( Eucalyptus.class, accountFullName );
		}
	}
	
	private class ComputeSystemActivity extends SystemActivityContextSupport<ComputeMessage, Compute>{
		private ComputeSystemActivity() { super( Compute.class ); }
		private ComputeSystemActivity(final boolean useELBSystemAccount) { super( Compute.class , useELBSystemAccount); }
 }

	private class ComputeUserActivity extends UserActivityContextSupport<ComputeMessage, Compute>{
		private ComputeUserActivity(final AccountFullName accountFullName){
			super( Compute.class, accountFullName );
		}
	}

	public List<RunningInstancesItemType> describeSystemInstancesWithVerbose(final List<String> instances){
    if(instances.size() <=0)
      return Lists.newArrayList();
    final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances, true);
    return resultOf( describeTask, new ComputeSystemActivity(false), "failed to describe the instances" );
  }

	public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances){
	  return describeSystemInstancesImpl(instances, true);
	}

	public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances, final boolean useELBSystemAccount){
	  return describeSystemInstancesImpl(instances, useELBSystemAccount);
	}
	private List<RunningInstancesItemType> describeSystemInstancesImpl(final List<String> instances, final boolean useELBSystemAccount){
	  if(instances.size() <=0)
	    return Lists.newArrayList();
	  final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
	  return resultOf( describeTask, new ComputeSystemActivity(useELBSystemAccount), "failed to describe the instances" );
	}

	public List<RunningInstancesItemType> describeUserInstances(final String accountNumber, final List<String> instances){
		if(instances.size() <=0)
			return Lists.newArrayList();
		final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
		return resultOf( describeTask, new ComputeUserActivity( AccountFullName.getInstance( accountNumber )), "failed to describe the instances" );
	}
	
	public List<ServiceStatusType> describeServices(final String componentType){
		//LOG.info("calling describe-services -T "+componentType);
		final EucalyptusDescribeServicesTask serviceTask = new EucalyptusDescribeServicesTask(componentType);
		return resultOf( serviceTask, new EmpyreanSystemActivity(), "failed to describe services" );
	}
	
  public List<ClusterInfoType> describeAvailabilityZonesWithVerbose(){
    return resultOf(
        new EucalyptusDescribeAvailabilityZonesTask(true),
        new EucalyptusSystemActivity(false),
        "failed to describe the availability zones"
    );
  }
  
  public List<ClusterInfoType> describeAvailabilityZones() {
    return describeAvailabilityZonesImpl(true);
  }
  public List<ClusterInfoType> describeAvailabilityZones(final boolean useELBSystemAccount) {
    return describeAvailabilityZonesImpl(useELBSystemAccount);
  }
	private List<ClusterInfoType> describeAvailabilityZonesImpl(final boolean useELBSystemAccount){
		return resultOf(
				new EucalyptusDescribeAvailabilityZonesTask(false),
				new EucalyptusSystemActivity(useELBSystemAccount),
				"failed to describe the availability zones"
		);
	}

	public void createSystemSecurityGroup( String groupName, String groupDesc ){
		final EucalyptusCreateGroupTask task = new EucalyptusCreateGroupTask(groupName, groupDesc);
		checkResult( task, new EucalyptusSystemActivity(), "failed to create the group "+groupName );
	}

	public void deleteSystemSecurityGroup( String groupName ){
	  deleteSystemSecurityGroupImpl(groupName, true);
	}
	public void deleteSystemSecurityGroup( String groupName , boolean useELBSystemAccount){
	  deleteSystemSecurityGroupImpl(groupName, useELBSystemAccount);
	}
	private void deleteSystemSecurityGroupImpl( String groupName, boolean useELBSystemAccount ){
	  final EucalyptusDeleteGroupTask task = new EucalyptusDeleteGroupTask(groupName);
	  checkResult( task, new EucalyptusSystemActivity(useELBSystemAccount), "failed to delete the group "+groupName );
	}
	
	public List<SecurityGroupItemType> describeSystemSecurityGroups( List<String> groupNames ){
		final EucalyptusDescribeSecurityGroupTask task = new EucalyptusDescribeSecurityGroupTask(null,groupNames,null,null);
		return resultOf( task, new ComputeSystemActivity(), "failed to describe security groups" );
	}
	
	public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum ){
	  this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new EucalyptusSystemActivity());
	}
	
  public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum, boolean useELBSystemAccount ){
    this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new EucalyptusSystemActivity(useELBSystemAccount));
  }
  
  private void authorizeSystemSecurityGroupImpl( String groupNameOrId, String protocol, int portNum, EucalyptusSystemActivity context){
    final EucalyptusAuthorizeIngressRuleTask task = new EucalyptusAuthorizeIngressRuleTask(groupNameOrId, protocol, portNum);
    checkResult(
        task,
        context,
        String.format("failed to authorize:%s, %s, %d ", groupNameOrId, protocol, portNum)
    );
  }
  
  public void revokeSystemSecurityGroup( String groupName, String protocol, int portNum) {
    revokeSystemSecurityGroupImpl(groupName, protocol, portNum, true);
  }
	
  public void revokeSystemSecurityGroup( String groupName, String protocol, int portNum, boolean useELBSystemAccount ){
    revokeSystemSecurityGroupImpl(groupName, protocol, portNum, useELBSystemAccount);
  }

	private void revokeSystemSecurityGroupImpl( String groupName, String protocol, int portNum, boolean useELBSystemAccount ){
		final EucalyptusRevokeIngressRuleTask task = new EucalyptusRevokeIngressRuleTask(groupName, protocol, portNum);
		checkResult(
				task,
				new EucalyptusSystemActivity(useELBSystemAccount),
				String.format("failed to revoke:%s, %s, %d ", groupName, protocol, portNum)
		);
	}

	public List<SecurityGroupItemType> describeUserSecurityGroupsByName( AccountFullName accountFullName, String vpcId, String groupNameFilter ){
		final EucalyptusDescribeSecurityGroupTask task =
				new EucalyptusDescribeSecurityGroupTask( null, null, Lists.newArrayList( groupNameFilter ), vpcId );
		return resultOf( task, new ComputeUserActivity( accountFullName ), "failed to describe security groups" );
	}

	public void createUserSecurityGroup( AccountFullName accountFullName, String groupName, String groupDesc ){
		final EucalyptusCreateGroupTask task = new EucalyptusCreateGroupTask( groupName, groupDesc );
		checkResult( task, new EucalyptusUserActivity( accountFullName ), "failed to create the group "+groupName );
	}

	public void putCloudWatchMetricData(final String userId, final String namespace, final MetricData data){
		final CloudWatchPutMetricDataTask task = new CloudWatchPutMetricDataTask(namespace, data);
		checkResult(
				task,
				new CloudWatchUserActivity(userId),
				"failed to put metric data"
		);
	}
	

  public void createLaunchConfiguration(
    final String imageId,
    final String instanceType,
    final String instanceProfileName,
    final String launchConfigName,
    final Collection<String> securityGroupNamesOrIds,
    final String keyName,
    final String userData,
    final Boolean associatePublicIp){
    createLaunchConfigurationImpl(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds,
        keyName, userData, associatePublicIp, true);
  }
	

  public void createLaunchConfiguration(
    final String imageId,
    final String instanceType,
    final String instanceProfileName,
    final String launchConfigName,
    final Collection<String> securityGroupNamesOrIds,
    final String keyName,
    final String userData,
    final Boolean associatePublicIp,
    final boolean useELBSystemAccount) {
    createLaunchConfigurationImpl(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds,
        keyName, userData, associatePublicIp, useELBSystemAccount);
  }
  
	private void createLaunchConfigurationImpl(
		final String imageId,
		final String instanceType,
		final String instanceProfileName,
		final String launchConfigName,
		final Collection<String> securityGroupNamesOrIds,
		final String keyName,
		final String userData,
		final Boolean associatePublicIp,
		final boolean useELBSystemAccount
	){
		final AutoScalingCreateLaunchConfigTask task = 
				new AutoScalingCreateLaunchConfigTask(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds, keyName, userData, associatePublicIp);
		checkResult(
				task,
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to create launch configuration"
		);
	}
	
	public void createAutoScalingGroup(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue){
	  createAutoScalingGroupImpl(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue, true);
	}
	public void createAutoScalingGroup(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue, final boolean useELBSystemAccount){
	  createAutoScalingGroupImpl(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue, useELBSystemAccount);
	}
	private void createAutoScalingGroupImpl(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue, final boolean useELBSystemAccount){
	  final AutoScalingCreateGroupTask task =
	      new AutoScalingCreateGroupTask(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue);
	  checkResult(
	      task,
	      new AutoScalingSystemActivity( useELBSystemAccount ),
	      "failed to create autoscaling group"
	      );
	}

	public void createOrUpdateAutoscalingTags(final String tagKey,
      final String tagValue, final String asgName){ 
	  createOrUpdateAutoscalingTagsImpl(tagKey, tagValue, asgName, true);
	}
	
	public void createOrUpdateAutoscalingTags(final String tagKey,
      final String tagValue, final String asgName, final boolean useELBSystemAccount){
	  createOrUpdateAutoscalingTagsImpl(tagKey, tagValue, asgName, useELBSystemAccount);
	}
	
  public void createOrUpdateAutoscalingTagsImpl(final String tagKey,
      final String tagValue, final String asgName, final boolean useELBSystemAccount) {
    final AutoscalingCreateOrUpdateTagsTask task = new AutoscalingCreateOrUpdateTagsTask(
        tagKey, tagValue, asgName);
    checkResult(
        task,
        new AutoScalingSystemActivity(useELBSystemAccount),
        "failed to create/update autoscaling tags"
    );
  }

  public LaunchConfigurationType describeLaunchConfiguration(final String launchConfigName){
    return describeLaunchConfigurationImpl(launchConfigName, true);
  }
  public LaunchConfigurationType describeLaunchConfiguration(final String launchConfigName, boolean useELBSystemAccount){
    return describeLaunchConfigurationImpl(launchConfigName, useELBSystemAccount);
  }
  private LaunchConfigurationType describeLaunchConfigurationImpl(final String launchConfigName, final boolean useELBSystemAccount){
    return resultOf(
        new AutoScalingDescribeLaunchConfigsTask(launchConfigName),
        new AutoScalingSystemActivity(useELBSystemAccount),
        "failed to describe launch configuration"
        );
  }
	
  public void deleteLaunchConfiguration(final String launchConfigName){
    deleteLaunchConfigurationImpl(launchConfigName, true);
  }
  public void deleteLaunchConfiguration(final String launchConfigName, final boolean useELBSystemAccount){
    deleteLaunchConfigurationImpl(launchConfigName, useELBSystemAccount);
  }
	private void deleteLaunchConfigurationImpl(final String launchConfigName, final boolean useELBSystemAccount){
		checkResult(
				new AutoScalingDeleteLaunchConfigTask(launchConfigName),
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to delete launch configuration"
		);
	}
	
  public void deleteAutoScalingGroup(final String groupName, final boolean terminateInstances){
    deleteAutoScalingGroupImpl(groupName, terminateInstances, true);
  }
  public void deleteAutoScalingGroup(final String groupName, final boolean terminateInstances, final boolean useELBSystemAccount){
    deleteAutoScalingGroupImpl(groupName, terminateInstances, useELBSystemAccount);
  }
	private void deleteAutoScalingGroupImpl(final String groupName, final boolean terminateInstances, final boolean useELBSystemAccount){
		checkResult(
				new AutoScalingDeleteGroupTask(groupName, terminateInstances),
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to delete autoscaling group"
		);
	}
	
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsWithVerbose(final List<String> groupNames){
	  final List<String> namesWithVerbose = Lists.newArrayList();
	  namesWithVerbose.addAll(groupNames);
	  namesWithVerbose.add("verbose");
	  return describeAutoScalingGroupsImpl(namesWithVerbose, false);
  }
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final List<String> groupNames){
	  return describeAutoScalingGroupsImpl(groupNames, true);
	}
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final List<String> groupNames, final boolean useELBSystemAccount){
	  return describeAutoScalingGroupsImpl(groupNames, useELBSystemAccount);
	}
	private DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsImpl(final List<String> groupNames, final boolean useELBSystemAccount){
		return resultOf(
				new AutoScalingDescribeGroupsTask(groupNames),
				new AutoScalingSystemActivity(useELBSystemAccount),
				"failed to describe autoscaling groups"
		);
	}

	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity){
	  updateAutoScalingGroup(groupName, zones, capacity, null, true);
	}
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity, final boolean useELBSystemAccount){
	  updateAutoScalingGroup(groupName, zones, capacity, null, useELBSystemAccount);
	}

	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName){
	  updateAutoScalingGroupImpl(groupName, zones, capacity, launchConfigName, true);
	}
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName, final boolean useELBSystemAccount){
	  updateAutoScalingGroupImpl(groupName, zones, capacity, launchConfigName, useELBSystemAccount);
	}   
	private void updateAutoScalingGroupImpl(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName, final boolean useELBSystemAccount){
	  checkResult(
	      new AutoScalingUpdateGroupTask(groupName, zones, capacity, launchConfigName),
	      new AutoScalingSystemActivity( useELBSystemAccount ),
	      "failed to update autoscaling group"
	      );
	}
	
	public List<RoleType> listRoles(final String pathPrefix){
		return resultOf(
				new EuareListRolesTask(pathPrefix),
				new EuareSystemActivity(),
				"failed to list IAM roles"
		);
	}
	
	public RoleType createRole(final String roleName, final String path, final String assumeRolePolicy){
		return resultOf(
				new EuareCreateRoleTask(roleName, path, assumeRolePolicy),
				new EuareSystemActivity(),
				"failed to create IAM role"
		);
	}
	
	public List<DescribeKeyPairsResponseItemType> describeKeyPairs(final List<String> keyNames){
		return resultOf(
				new EucaDescribeKeyPairsTask(keyNames),
				new ComputeSystemActivity(),
				"failed to describe keypairs"
		);
	}

	public List<SecurityGroupItemType> describeUserSecurityGroupsById(
			final AccountFullName accountFullName,
			final String vpcId,
			final Collection<String> securityGroupIds ){
		return resultOf(
				new EucaDescribeSecurityGroupsTask( vpcId, securityGroupIds ),
				new ComputeUserActivity( accountFullName ),
				"failed to describe security groups"
		);
	}

	public void modifySecurityGroups(
	    final String instanceId,
	    final Collection<String> securityGroupIds
	    ) {
	  modifySecurityGroupsImpl(instanceId, securityGroupIds, true);
	}
	public void modifySecurityGroups(
	    final String instanceId,
	    final Collection<String> securityGroupIds,
	    final boolean useELBSystemAccount
	    ){
	  modifySecurityGroupsImpl(instanceId, securityGroupIds, useELBSystemAccount);
	}
	private void modifySecurityGroupsImpl(
	    final String instanceId,
	    final Collection<String> securityGroupIds,
	    final boolean useELBSystemAccount
	    ) {
	  checkResult(
	      new EucalyptusModifySecurityGroupsTask( instanceId, securityGroupIds ),
	      new EucalyptusSystemActivity( useELBSystemAccount ),
	      "failed to modify security groups"
	      );
	}

	public Optional<VpcType> defaultVpc( final AccountFullName accountFullName ) {
		return Iterables.tryFind( resultOf(
				new EucaDescribeVpcsTask( true ),
				new ComputeUserActivity( accountFullName ),
				"failed to describe default vpc"
		), Predicates.alwaysTrue() );
	}

	public List<SubnetType> describeSubnets(final Collection<String> subnetIds ){
		return resultOf(
				new EucaDescribeSubnetsTask( subnetIds ),
				new ComputeSystemActivity(false),
				"failed to describe subnets"
		);
	}

	public List<SubnetType> describeSubnetsByZone(
			final String vpcId,
			final Boolean defaultSubnet,
			final Collection<String> zones ){
		return resultOf(
				new EucaDescribeSubnetsTask( vpcId, defaultSubnet, zones ),
				new ComputeSystemActivity(false),
				"failed to describe subnets"
		);
	}

	public List<InternetGatewayType> describeInternetGateways(final Collection<String> vpcIds ){
		return resultOf(
				new EucaDescribeInternetGatewaysTask(vpcIds),
				new ComputeSystemActivity(false),
				"failed to describe internet gateways"
		);
	}

  public ServerCertificateType getServerCertificate(final String accountNumber, final String certName){
		return resultOf(
				new EuareGetServerCertificateTask(certName),
				new EuareUserActivity( AccountFullName.getInstance( accountNumber ) ),
				"failed to get server certificate"
		);
	}

  public void deleteRole(final String roleName){
    deleteRoleImpl(roleName, true);
  }
  public void deleteRole(final String roleName, boolean useELBSystemAccount){
    deleteRoleImpl(roleName, useELBSystemAccount);
  }
	private void deleteRoleImpl(final String roleName, boolean useELBSystemAccount){
		checkResult(
				new EuareDeleteRoleTask(roleName),
				new EuareSystemActivity(useELBSystemAccount),
				"failed to delete IAM role"
		);
	}
	
	public List<InstanceProfileType> listInstanceProfiles(String pathPrefix){
		return resultOf(
				new EuareListInstanceProfilesTask(pathPrefix),
				new EuareSystemActivity(),
				"failed to list IAM instance profile"
		);
	}
	
	public InstanceProfileType createInstanceProfile(String profileName, String path){
		return resultOf(
				new EuareCreateInstanceProfileTask(profileName, path),
				new EuareSystemActivity(),
				"failed to create IAM instance profile"
		);
	}
	
  public void deleteInstanceProfile(String profileName){
    deleteInstanceProfileImpl(profileName, true);
  }
  public void deleteInstanceProfile(String profileName, boolean useELBSystemAccount){
    deleteInstanceProfileImpl(profileName, useELBSystemAccount);
  }
	private void deleteInstanceProfileImpl(String profileName, boolean useELBSystemAccount){
		checkResult(
				new EuareDeleteInstanceProfileTask(profileName),
				new EuareSystemActivity( useELBSystemAccount ),
				"failed to delete IAM instance profile"
		);
	}
	
	public void addRoleToInstanceProfile(String instanceProfileName, String roleName){
		checkResult(
				new EuareAddRoleToInstanceProfileTask(instanceProfileName, roleName),
				new EuareSystemActivity( ),
				"failed to add role to the instance profile"
		);
	}

	public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName){
	  removeRoleFromInstanceProfileImpl(instanceProfileName, roleName, true);
	}
	public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName, boolean useELBSystemAccount){
	  removeRoleFromInstanceProfileImpl(instanceProfileName, roleName, useELBSystemAccount);
	}
	private void removeRoleFromInstanceProfileImpl(String instanceProfileName, String roleName, boolean useELBSystemAccount){
	  checkResult(
	      new EuareRemoveRoleFromInstanceProfileTask(instanceProfileName, roleName),
	      new EuareSystemActivity( useELBSystemAccount ),
	      "failed to remove role from the instance profile"
	      );
	}
	
	public List<String> listRolePolicies(final String roleName){
		return resultOf(
				new EuareListRolePoliciesTask(roleName),
				new EuareSystemActivity(),
				"failed to list role's policies"
		);
	}
	
	public GetRolePolicyResult getRolePolicy(String roleName, String policyName){
		return resultOf(
				new EuareGetRolePolicyTask(roleName, policyName),
				new EuareSystemActivity(),
				"failed to get role's policy"
		);
	}
	
	public void putRolePolicy(String roleName, String policyName, String policyDocument){
	  putRolePolicyImpl(roleName, policyName, policyDocument, true);
	}

	public void putRolePolicy(String roleName, String policyName, String policyDocument, boolean useELBSystemAccount){
    putRolePolicyImpl(roleName, policyName, policyDocument, useELBSystemAccount);
	}
	
	private void putRolePolicyImpl(String roleName, String policyName, String policyDocument, boolean useELBSystemAccount){
		checkResult(
				new EuarePutRolePolicyTask(roleName, policyName, policyDocument),
				new EuareSystemActivity( useELBSystemAccount ),
				"failed to put role's policy"
		);
	}

	public void deleteRolePolicy(String roleName, String policyName){
	  deleteRolePolicyImpl(roleName, policyName, true);
	}
	public void deleteRolePolicy(String roleName, String policyName, boolean useELBSystemAccount){
	  deleteRolePolicyImpl(roleName, policyName, useELBSystemAccount);
	}
	private void deleteRolePolicyImpl(String roleName, String policyName, boolean useELBSystemAccount){
	  checkResult(
	      new EuareDeleteRolePolicyTask(roleName, policyName),
	      new EuareSystemActivity( useELBSystemAccount ),
	      "failed to delete role's policy"
	      );
	}
	
	public List<ImageDetails> describeImages(final List<String> imageIds){
		return resultOf(
				new EucaDescribeImagesTask(imageIds),
				new ComputeSystemActivity(),
				"failed to describe images"
		);
	}
	
	public List<ImageDetails> describeImagesWithVerbose(final List<String> imageIds){
	  final List<String> idsWithVerbose = Lists.newArrayList(imageIds);
	  idsWithVerbose.add("verbose");
    return resultOf(
        new EucaDescribeImagesTask(idsWithVerbose),
        new ComputeSystemActivity(false),
        "failed to describe images"
    );
  }
	
	public void createTags(final String tagKey, final String tagValue, final List<String> resources){
		checkResult(
				new EucaCreateTagsTask(tagKey, tagValue, resources),
				new EucalyptusSystemActivity( ),
				"failed to create tags"
		);
	}
	
	public void deleteTags(final String tagKey, final String tagValue, final List<String> resources){
		checkResult(
				new EucaDeleteTagsTask(tagKey, tagValue, resources),
				new EucalyptusSystemActivity( ),
				"failed to delete tags"
		);
	}
	
	private class EucaDescribeImagesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<ImageDetails>> {
		private List<String> imageIds = null;
		private EucaDescribeImagesTask(final List<String> imageIds){
			this.imageIds = imageIds;
		}
		
		DescribeImagesType getRequest(){
			final DescribeImagesType req = new DescribeImagesType();
			if(this.imageIds!=null && this.imageIds.size()>0){
				req.setImagesSet(new ArrayList<>(imageIds));
			}
			return req;
		}

		@Override
		List<ImageDetails> extractResult(ComputeMessage response) {
			final DescribeImagesResponseType resp = (DescribeImagesResponseType) response;
			return resp.getImagesSet();
		}

		@Override
		List<ImageDetails> failureResult( final String errorCode ) {
			return "InvalidAMIID.NotFound".equals( errorCode ) ?
					Lists.<ImageDetails>newArrayList( ) :
					null;
		}
	}
	
	private class EucaDeleteTagsTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;
		
		private EucaDeleteTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}
		
		DeleteTagsType getRequest(){
			final DeleteTagsType req = new DeleteTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final DeleteResourceTag tag = new DeleteResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
	}
	
	private class EucaCreateTagsTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;
		private EucaCreateTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}
		CreateTagsType getRequest(){
			final CreateTagsType req = new CreateTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final ResourceTag tag = new ResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
	}
	
	private class EucaDescribeKeyPairsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<DescribeKeyPairsResponseItemType>> {
		private List<String> keyNames = null;
		private EucaDescribeKeyPairsTask(final List<String> keyNames){
			this.keyNames = keyNames;
		}
		
		DescribeKeyPairsType getRequest(){
			final DescribeKeyPairsType req = new DescribeKeyPairsType();
			if(this.keyNames!=null){
				req.setKeySet(new ArrayList<>(this.keyNames));
			}
			return req;
		}
		
		@Override
		List<DescribeKeyPairsResponseItemType> extractResult(ComputeMessage response) {
			final DescribeKeyPairsResponseType resp = (DescribeKeyPairsResponseType) response;
			return resp.getKeySet();
		}
	}


	private class EucaDescribeSecurityGroupsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>> {
		private String vpcId = null;
		private Collection<String> securityGroupIds = null;
		private EucaDescribeSecurityGroupsTask( final String vpcId, final Collection<String> securityGroupIds){
			this.vpcId = vpcId;
			this.securityGroupIds = securityGroupIds;
		}

		DescribeSecurityGroupsType getRequest( ){
			final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType( );
			if ( vpcId != null ) {
				req.getFilterSet( ).add( filter( "vpc-id", vpcId ) );
			}
			req.getFilterSet( ).add( filter( "group-id", securityGroupIds ) );
			return req;
		}

		@Override
		List<SecurityGroupItemType> extractResult( ComputeMessage response ) {
			final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
			return resp.getSecurityGroupInfo( );
		}
	}

	private class EucaDescribeVpcsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<VpcType>> {
		private Collection<String> vpcIds;
		private final Boolean defaultVpc;
		private EucaDescribeVpcsTask(final Boolean defaultVpc ) {
			this( defaultVpc, null );
		}
		private EucaDescribeVpcsTask(final Collection<String> vpcIds){
			this( null, vpcIds );
		}
		private EucaDescribeVpcsTask(final Boolean defaultVpc, final Collection<String> vpcIds){
			this.defaultVpc = defaultVpc;
			this.vpcIds = vpcIds;
		}

		ComputeMessage getRequest( ){
			final DescribeVpcsType req = new DescribeVpcsType();
			if(this.defaultVpc!=null){
				req.getFilterSet().add( filter( "isDefault", String.valueOf( defaultVpc ) ) );
			}
			if(this.vpcIds!=null){
				req.getFilterSet().add( filter( "vpc-id", vpcIds ) );
			}
			return req;
		}

		@Override
		List<VpcType> extractResult( final ComputeMessage response ) {
			final DescribeVpcsResponseType resp = (DescribeVpcsResponseType) response;
			return resp.getVpcSet( ).getItem();
		}
	}

	private class EucaDescribeSubnetsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SubnetType>> {
		private String vpcId = null;
		private Collection<String> subnetIds = null;
		private Collection<String> zones = null;
		private Boolean defaultSubnet = null;
		private EucaDescribeSubnetsTask(final Collection<String> subnetIds){
			this.subnetIds = subnetIds;
		}
		private EucaDescribeSubnetsTask(final String vpcId, final Boolean defaultSubnet, final Collection<String> zones){
			this.vpcId = vpcId;
			this.defaultSubnet = defaultSubnet;
			this.zones = zones;
		}

		ComputeMessage getRequest( ){
			final DescribeSubnetsType req = new DescribeSubnetsType();
			req.setSubnetSet( new SubnetIdSetType(  ) );
			req.getSubnetSet( ).getItem( ).add( new SubnetIdSetItemType() );
			req.getSubnetSet( ).getItem( ).get( 0 ).setSubnetId( "verbose" );
			if(this.vpcId!=null){
				req.getFilterSet( ).add( filter( "vpc-id", vpcId ) );
			}
			if(this.subnetIds!=null){
				req.getFilterSet( ).add( filter( "subnet-id", subnetIds ) );
			}
			if(this.zones!=null){
				req.getFilterSet( ).add( filter( "availability-zone", zones ) );
			}
			if(this.defaultSubnet!=null){
				req.getFilterSet( ).add( filter( "default-for-az", String.valueOf( this.defaultSubnet ) ) );
			}
			return req;
		}

		@Override
		List<SubnetType> extractResult( final ComputeMessage response ) {
			final DescribeSubnetsResponseType resp = (DescribeSubnetsResponseType) response;
			return resp.getSubnetSet( ).getItem( );
		}
	}

	private class EucaDescribeInternetGatewaysTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<InternetGatewayType>> {
		private Collection<String> vpcIds = null;
		private EucaDescribeInternetGatewaysTask(final Collection<String> vpcIds){
			this.vpcIds = vpcIds;
		}

		ComputeMessage getRequest( ){
			final DescribeInternetGatewaysType req = new DescribeInternetGatewaysType();
			req.setInternetGatewayIdSet( new InternetGatewayIdSetType() );
			req.getInternetGatewayIdSet().getItem( ).add( new InternetGatewayIdSetItemType() );
			req.getInternetGatewayIdSet().getItem( ).get( 0 ).setInternetGatewayId( "verbose" );
			if(this.vpcIds!=null){
				req.getFilterSet( ).add( filter( "attachment.vpc-id", this.vpcIds ) );
			}
			return req;
		}

		@Override
		List<InternetGatewayType> extractResult( final ComputeMessage response ) {
			final DescribeInternetGatewaysResponseType resp = (DescribeInternetGatewaysResponseType) response;
			return resp.getInternetGatewaySet( ).getItem( );
		}
	}

	private class EuareGetServerCertificateTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, ServerCertificateType> {
	  private String certName = null;

	  private EuareGetServerCertificateTask(final String certName){
	    this.certName = certName;
	  }
	  
	  GetServerCertificateType getRequest( ){
	    final GetServerCertificateType req = new GetServerCertificateType();
	    req.setServerCertificateName(this.certName);
	    return req;
	  }

    @Override
		ServerCertificateType extractResult( EuareMessage response ) {
			final GetServerCertificateResponseType resp = (GetServerCertificateResponseType) response;
			if(resp.getGetServerCertificateResult()!= null)
				return resp.getGetServerCertificateResult().getServerCertificate();
			return null;
    }
	}
	
	private class EuareDeleteInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String profileName =null;
		private EuareDeleteInstanceProfileTask(String profileName){
			this.profileName = profileName;
		}
		
		DeleteInstanceProfileType getRequest(){
			final DeleteInstanceProfileType req = new DeleteInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			return req;
		}
	}
	
	private class EuareAddRoleToInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String instanceProfileName = null;
		private String roleName = null;
		
		private EuareAddRoleToInstanceProfileTask(final String instanceProfileName, final String roleName){
			this.instanceProfileName = instanceProfileName;
			this.roleName = roleName;
		}
		
		AddRoleToInstanceProfileType getRequest(){
			final AddRoleToInstanceProfileType req  = new AddRoleToInstanceProfileType();
			req.setRoleName(this.roleName);
			req.setInstanceProfileName(this.instanceProfileName);
			return req;
		}
	}
	
	private class EuareRemoveRoleFromInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
	  private String instanceProfileName = null;
	  private String roleName = null;
	  
	  private EuareRemoveRoleFromInstanceProfileTask(final String instanceProfileName, final String roleName){
	    this.instanceProfileName = instanceProfileName;
	    this.roleName = roleName;
	  }
	  
	  RemoveRoleFromInstanceProfileType getRequest(){
	    final RemoveRoleFromInstanceProfileType req = new RemoveRoleFromInstanceProfileType();
	    req.setRoleName(this.roleName);
	    req.setInstanceProfileName(this.instanceProfileName);
	    return req;
	  }
	}
	
	private class EuareListInstanceProfilesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<InstanceProfileType>> {
		private String pathPrefix = null;
		private EuareListInstanceProfilesTask(final String pathPrefix){
			this.pathPrefix = pathPrefix;
		}
		
		ListInstanceProfilesType getRequest(){
			final ListInstanceProfilesType req = new ListInstanceProfilesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}
		
		@Override
		List<InstanceProfileType> extractResult(EuareMessage response) {
			ListInstanceProfilesResponseType resp = (ListInstanceProfilesResponseType) response;
			try{
				return resp.getListInstanceProfilesResult().getInstanceProfiles().getMember();
			}catch(Exception  ex){
				return null;
			}
		}
	}
	
	private class EuareCreateInstanceProfileTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, InstanceProfileType>{
		private String profileName = null;
		private String path = null;
		private EuareCreateInstanceProfileTask(String profileName, String path){
			this.profileName = profileName;
			this.path = path;
		}
		
		CreateInstanceProfileType getRequest(){
			final CreateInstanceProfileType req = new CreateInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			req.setPath(this.path);
			return req;
		}
		
		@Override
		InstanceProfileType extractResult(EuareMessage response) {
			final CreateInstanceProfileResponseType resp = (CreateInstanceProfileResponseType) response;
			try{
				return resp.getCreateInstanceProfileResult().getInstanceProfile();
			}catch(Exception ex){
				return null;
			}
		}
	}
	
	private class EuareDeleteRoleTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private EuareDeleteRoleTask(String roleName){
			this.roleName = roleName;
		}
		DeleteRoleType getRequest(){
			final DeleteRoleType req = new DeleteRoleType();
			req.setRoleName(this.roleName);
			return req;
		}
	}
	
	private class EuareCreateRoleTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, RoleType> {
		String roleName = null;
		String path = null;
		String assumeRolePolicy = null;

		private EuareCreateRoleTask(String roleName, String path, String assumeRolePolicy){
			this.roleName = roleName;
			this.path = path;
			this.assumeRolePolicy = assumeRolePolicy;
		}

		CreateRoleType getRequest(){
			final CreateRoleType req = new CreateRoleType();
			req.setRoleName(this.roleName);
			req.setPath(this.path);
			req.setAssumeRolePolicyDocument(this.assumeRolePolicy);
			return req;
		}
		
		@Override
		RoleType extractResult( EuareMessage response) {
			CreateRoleResponseType resp = (CreateRoleResponseType) response;
			try{
				return resp.getCreateRoleResult().getRole();
			}catch(Exception ex){
				return null;
			}			
		}
	}
	
	private class EuareListRolesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<RoleType>> {
		private String pathPrefix = null;

		private EuareListRolesTask(String pathPrefix){
			this.pathPrefix = pathPrefix;
		}
		
		ListRolesType getRequest(){
			final ListRolesType req = new ListRolesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}
		
		@Override
		List<RoleType> extractResult(EuareMessage response) {
			ListRolesResponseType resp = (ListRolesResponseType) response;
			try{
				return resp.getListRolesResult().getRoles().getMember();
			}catch(Exception ex){
				return null;
			}
		}
	}
	
	private class EuarePutRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private String policyName = null;
		private String policyDocument = null;
		
		private EuarePutRolePolicyTask(String roleName, String policyName, String policyDocument){
			this.roleName = roleName;
			this.policyName = policyName;
			this.policyDocument = policyDocument;
		}
		
		PutRolePolicyType getRequest(){
			final PutRolePolicyType req = 
					new PutRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			req.setPolicyDocument(this.policyDocument);
			
			return req;
		}
	}
	
	private class EuareListRolePoliciesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<String>> {
		private String roleName = null;
		private EuareListRolePoliciesTask(final String roleName){
			this.roleName = roleName;
		}

		ListRolePoliciesType getRequest(){
			final ListRolePoliciesType req = new ListRolePoliciesType();
			req.setRoleName(this.roleName);
			return req;
		}

		@Override
		List<String> extractResult(EuareMessage response) {
			try{
				final ListRolePoliciesResponseType resp = (ListRolePoliciesResponseType) response;
				return resp.getListRolePoliciesResult().getPolicyNames().getMemberList();
			}catch(final Exception ex){
				return Lists.newArrayList();
			}
		}
	}
	
	private class EuareGetRolePolicyTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, GetRolePolicyResult> {
		private String roleName = null;
		private String policyName = null;
		
		private EuareGetRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}
		
		GetRolePolicyType getRequest(){
			final GetRolePolicyType req = new GetRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
		}

		@Override
		GetRolePolicyResult extractResult( EuareMessage response) {
		  try{
		    final GetRolePolicyResponseType resp = (GetRolePolicyResponseType) response;
		    return resp.getGetRolePolicyResult();
		  }catch(final Exception ex){
		    return null;
		  }
		}
	}
	
	private class EuareDeleteRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName= null;
		private String policyName = null;
		
		private EuareDeleteRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}
		
		DeleteRolePolicyType getRequest(){
			final DeleteRolePolicyType req = new DeleteRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
		}
	}
	
	private class AutoScalingUpdateGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private List<String> availabilityZones = null;
		private Integer capacity = null;
		private String launchConfigName = null;
		
		private AutoScalingUpdateGroupTask(final String groupName, final List<String> zones, 
				final Integer capacity, final String launchConfig){
			this.groupName = groupName;
			this.availabilityZones = zones;
			this.capacity = capacity;
			this.launchConfigName = launchConfig;
		}
		
		UpdateAutoScalingGroupType getRequest(){
			final UpdateAutoScalingGroupType req = new UpdateAutoScalingGroupType();
			req.setAutoScalingGroupName( this.groupName );
			
			if(this.availabilityZones!=null && this.availabilityZones.size()>0){
				AvailabilityZones zones = new AvailabilityZones();
				zones.setMember(Lists.<String>newArrayList());
				zones.getMember().addAll(this.availabilityZones);
				req.setAvailabilityZones(zones);
			}
			if(this.capacity!=null){
				req.setDesiredCapacity(this.capacity);
				req.setMaxSize(this.capacity);
				req.setMinSize(this.capacity);
			}
			if(this.launchConfigName!=null){
				req.setLaunchConfigurationName(this.launchConfigName);
			}
			return req;
		}
	}
	
	private class AutoScalingDescribeGroupsTask extends EucalyptusActivityTaskWithResult<AutoScalingMessage, AutoScaling, DescribeAutoScalingGroupsResponseType> {
		private List<String> groupNames = null;
		private AutoScalingDescribeGroupsTask(final List<String> groupNames){
			this.groupNames = groupNames;
		}
		
		DescribeAutoScalingGroupsType getRequest( ){
			final DescribeAutoScalingGroupsType req = new DescribeAutoScalingGroupsType();
			final AutoScalingGroupNames names = new AutoScalingGroupNames();
			names.setMember(Lists.<String>newArrayList());
			names.getMember().addAll(this.groupNames);
			req.setAutoScalingGroupNames(names);
			return req;
		}

		@Override
		DescribeAutoScalingGroupsResponseType extractResult( final AutoScalingMessage response ) {
			return (DescribeAutoScalingGroupsResponseType) response;
		}
	}
	
	private class AutoScalingCreateGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private List<String> availabilityZones = null;
		private String vpcZoneIdentifier;
		private int capacity = 1;
		private String launchConfigName = null;
		private String tagKey = null;
		private String tagValue = null;
		
		private AutoScalingCreateGroupTask(final String groupName, final List<String> zones, final String vpcZoneIdentifier,
				final int capacity, final String launchConfig, final String tagKey, final String tagValue){
			this.groupName = groupName;
			this.availabilityZones = zones;
			this.vpcZoneIdentifier = vpcZoneIdentifier;
			this.capacity = capacity;
			this.launchConfigName = launchConfig;
			this.tagKey = tagKey;
			this.tagValue = tagValue;
		}
		
		CreateAutoScalingGroupType getRequest(){
			final CreateAutoScalingGroupType req = new CreateAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			req.setAvailabilityZones( new AvailabilityZones( this.availabilityZones ) );
			req.setVpcZoneIdentifier( this.vpcZoneIdentifier );
			req.setDesiredCapacity(this.capacity);
			req.setMaxSize(this.capacity);
			req.setMinSize(this.capacity);
			req.setHealthCheckType("EC2");
			req.setLaunchConfigurationName(this.launchConfigName);
			final Tags tags = new Tags();
			final TagType tag = new TagType();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			tag.setPropagateAtLaunch(true);
			tag.setResourceType("auto-scaling-group");
			tag.setResourceId(this.groupName);
			tags.setMember(Lists.newArrayList(tag));
			req.setTags(tags);
			return req;
		}
	}

  private class AutoscalingCreateOrUpdateTagsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling> {
    private String tagKey = null;
    private String tagValue = null;
    private String asgName = null;

    private AutoscalingCreateOrUpdateTagsTask(final String tagKey, final String tagValue, final String asgName) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.asgName = asgName;
    }

    @Override
    AutoScalingMessage getRequest() {
      final CreateOrUpdateTagsType req = new CreateOrUpdateTagsType();
      final Tags tags = new Tags();
      final TagType tag = new TagType();
      tag.setKey(this.tagKey);
      tag.setValue(this.tagValue);
      tag.setPropagateAtLaunch(true);
      tag.setResourceType("auto-scaling-group");
      tag.setResourceId(this.asgName);
      tags.setMember(Lists.newArrayList(tag));
      req.setTags(tags);
      return req;
    }
  }

	private class AutoScalingDeleteGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private boolean terminateInstances = false;
		private AutoScalingDeleteGroupTask(final String groupName, final boolean terminateInstances){
			this.groupName = groupName;
			this.terminateInstances = terminateInstances;
		}
		
		DeleteAutoScalingGroupType getRequest(){
			final DeleteAutoScalingGroupType req = new DeleteAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			req.setForceDelete(this.terminateInstances);
			return req;
		}
	}
	
	private class AutoScalingDeleteLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String launchConfigName = null;
		private AutoScalingDeleteLaunchConfigTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}
		
		DeleteLaunchConfigurationType getRequest(){
			final DeleteLaunchConfigurationType req = new DeleteLaunchConfigurationType();
			req.setLaunchConfigurationName(this.launchConfigName);
			return req;
		}
	}

	private class AutoScalingDescribeLaunchConfigsTask extends EucalyptusActivityTaskWithResult<AutoScalingMessage, AutoScaling, LaunchConfigurationType> {
		private String launchConfigName = null;
		private AutoScalingDescribeLaunchConfigsTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}
		
		DescribeLaunchConfigurationsType getRequest(){
			final DescribeLaunchConfigurationsType req = new DescribeLaunchConfigurationsType();
			final LaunchConfigurationNames names = new LaunchConfigurationNames();
			names.setMember(Lists.newArrayList(this.launchConfigName));
			req.setLaunchConfigurationNames(names);
			return req;
		}
		
		@Override
		LaunchConfigurationType extractResult( AutoScalingMessage response) {
			final DescribeLaunchConfigurationsResponseType resp = (DescribeLaunchConfigurationsResponseType) response;
			try{
				return resp.getDescribeLaunchConfigurationsResult().getLaunchConfigurations().getMember().get(0);
			}catch(final Exception ex){
				LOG.error("Launch configuration is not found from the response");
				return null;
			}
		}
	}
	
	private class AutoScalingCreateLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private final String imageId;
		private final String instanceType;
		private final String instanceProfileName;
		private final String launchConfigName;
		private final Collection<String> securityGroupNamesOrIds;
		private final String keyName;
		private final String userData;
		private final Boolean associatePublicIp;
		private AutoScalingCreateLaunchConfigTask(final String imageId, final String instanceType, String instanceProfileName,
				final String launchConfigName, final Collection<String> securityGroupNamesOrIds, final String keyName, final String userData,
				final Boolean associatePublicIp){
			this.imageId = imageId;
			this.instanceType = instanceType;
			this.instanceProfileName = instanceProfileName;
			this.launchConfigName = launchConfigName;
			this.securityGroupNamesOrIds = securityGroupNamesOrIds;
			this.keyName = keyName; 
			this.userData = userData;
			this.associatePublicIp = associatePublicIp;
		}
		
		CreateLaunchConfigurationType getRequest(){
			final CreateLaunchConfigurationType req = new CreateLaunchConfigurationType();
			req.setImageId(this.imageId);
			req.setInstanceType(this.instanceType);
			req.setIamInstanceProfile(this.instanceProfileName);
			req.setKeyName(this.keyName);
			req.setLaunchConfigurationName(this.launchConfigName);
			req.setSecurityGroups( new SecurityGroups( this.securityGroupNamesOrIds ) );
			req.setUserData(userData);
			req.setAssociatePublicIpAddress( associatePublicIp );
			return req;
		}
	}

	private class CloudWatchPutMetricDataTask extends EucalyptusActivityTask<CloudWatchMessage, CloudWatch>{
		private MetricData metricData = null;
		private String namespace = null;
		private CloudWatchPutMetricDataTask( final String namespace, final MetricData data){
			this.namespace = namespace;
			this.metricData = data;
		}
		
		PutMetricDataType getRequest(){
			final PutMetricDataType request = new PutMetricDataType();
			request.setNamespace(this.namespace);
			request.setMetricData(this.metricData);
			return request;
		}
	}

	
	private class EucalyptusDescribeAvailabilityZonesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Eucalyptus, List<ClusterInfoType>> {
		private boolean verbose = false;
		private EucalyptusDescribeAvailabilityZonesTask(boolean verbose){
			this.verbose = verbose;
		}
		
		DescribeAvailabilityZonesType getRequest(){
			final DescribeAvailabilityZonesType req = new DescribeAvailabilityZonesType();
			if(this.verbose){
				req.setAvailabilityZoneSet(Lists.newArrayList("verbose"));
			}
			return req;
		}
		
		@Override
		List<ClusterInfoType> extractResult(ComputeMessage response) {
			final DescribeAvailabilityZonesResponseType resp = (DescribeAvailabilityZonesResponseType) response;
			return resp.getAvailabilityZoneInfo();
		}
	}

	private class EucalyptusDescribeServicesTask extends EucalyptusActivityTaskWithResult<EmpyreanMessage, Empyrean,List<ServiceStatusType>> {
		private String componentType = null;
		private EucalyptusDescribeServicesTask(final String componentType){
			this.componentType = componentType;
		}
		
		DescribeServicesType getRequest(){
			final DescribeServicesType req = new DescribeServicesType();
		    req.setByServiceType(this.componentType);
		    return req;
		}
		
		@Override
		List<ServiceStatusType> extractResult(EmpyreanMessage response) {
			final DescribeServicesResponseType resp = (DescribeServicesResponseType) response;
			return resp.getServiceStatuses();
		}
	}
		
	private class EucalyptusDescribeInstanceTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute,List<RunningInstancesItemType>> {
		private final List<String> instanceIds;
		private boolean verbose = false;
		private EucalyptusDescribeInstanceTask(final List<String> instanceId){
			this.instanceIds = instanceId;
		}
		
		private EucalyptusDescribeInstanceTask(final List<String> instanceId, final boolean verbose){
      this.instanceIds = instanceId;
      this.verbose = verbose;
    }
		
		DescribeInstancesType getRequest(){
			final DescribeInstancesType req = new DescribeInstancesType();
			final ArrayList<String> instances = Lists.newArrayList(this.instanceIds);
			if(this.verbose)
			  instances.add("verbose");
			req.setInstancesSet(instances);
			return req;
		}
		
		@Override
		List<RunningInstancesItemType> extractResult( ComputeMessage response) {
			final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
			final List<RunningInstancesItemType> resultInstances = Lists.newArrayList();
			for(final ReservationInfoType res : resp.getReservationSet()){
				resultInstances.addAll(res.getInstancesSet());
			}
			return resultInstances;
		}

		@Override
		List<RunningInstancesItemType> getFailureResult( final String errorCode ) {
			return "InvalidInstanceID.NotFound".equals( errorCode ) ?
					Lists.<RunningInstancesItemType>newArrayList( ) :
					null;
		}
	}
	
	//SPARK: TODO: SYSTEM, STATIC MODE?
	private class EucalyptusCreateGroupTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Eucalyptus, String> {
		private String groupName = null;
		private String groupDesc = null;
		EucalyptusCreateGroupTask(String groupName, String groupDesc){
			this.groupName = groupName;
			this.groupDesc = groupDesc;
		}
		CreateSecurityGroupType getRequest(){
			final CreateSecurityGroupType req = new CreateSecurityGroupType();
			req.setGroupName(this.groupName);
			req.setGroupDescription(this.groupDesc);
			return req;
		}

		@Override
		String extractResult(ComputeMessage response) {
			final CreateSecurityGroupResponseType resp = (CreateSecurityGroupResponseType) response;
			return resp.getGroupId();
		}
	}
	
	private class EucalyptusAuthorizeIngressRuleTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus> {
		String groupNameOrId=null;
		String protocol = null;
		int portNum = 1;
		
		EucalyptusAuthorizeIngressRuleTask(String groupNameOrId, String protocol, int portNum){
			this.groupNameOrId = groupNameOrId;
			this.protocol=protocol;
			this.portNum = portNum;
		}
		AuthorizeSecurityGroupIngressType getRequest(){
			AuthorizeSecurityGroupIngressType req = new AuthorizeSecurityGroupIngressType( );
			if ( this.groupNameOrId.matches( "sg-[0-9a-fA-F]{8}" ) ) {
				req.setGroupId( this.groupNameOrId );
			} else {
				req.setGroupName( this.groupNameOrId );
			}
			IpPermissionType perm = new IpPermissionType();
			perm.setFromPort(this.portNum);
			perm.setToPort(this.portNum);
			perm.setCidrIpRanges( Collections.singleton( "0.0.0.0/0" ) );
			perm.setIpProtocol(this.protocol); // udp too?
			req.getIpPermissions( ).add( perm );
			return req;
		}
	}
	private class EucalyptusRevokeIngressRuleTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus> {
		String groupName=null;
		String protocol=null;
		int portNum = 1;
		EucalyptusRevokeIngressRuleTask(String groupName, String protocol, int portNum){
			this.groupName = groupName;
			this.protocol = protocol;
			this.portNum = portNum;
		}
		RevokeSecurityGroupIngressType getRequest(){
			RevokeSecurityGroupIngressType req = new RevokeSecurityGroupIngressType();
			req.setGroupName(this.groupName);
			IpPermissionType perm = new IpPermissionType();
			perm.setFromPort(this.portNum);
			perm.setToPort(this.portNum);
			perm.setCidrIpRanges( Lists.newArrayList( Arrays.asList( "0.0.0.0/0" ) ) );
			perm.setIpProtocol(this.protocol);
			req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
			return req;
		}
	}
	
	private class EucalyptusDeleteGroupTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus>{
		private String groupName = null;
		EucalyptusDeleteGroupTask(String groupName){
			this.groupName = groupName;
		}
		DeleteSecurityGroupType getRequest(){
			final DeleteSecurityGroupType req = new DeleteSecurityGroupType();
			req.setGroupName(this.groupName);
			return req;
		}
	}
	
	private class EucalyptusDescribeSecurityGroupTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>>{
		@Nullable private List<String> groupIds = null;
		@Nullable private List<String> groupNames = null;
		@Nullable private List<String> groupNameFilters = null;
		@Nullable private String vpcId = null;

		EucalyptusDescribeSecurityGroupTask(
				@Nullable final List<String> groupIds,
				@Nullable final List<String> groupNames,
				@Nullable final List<String> groupNameFilters,
				@Nullable final String vpcId ){
			this.groupIds = groupIds;
			this.groupNames = groupNames;
			this.groupNameFilters = groupNameFilters;
			this.vpcId = vpcId;
		}

		DescribeSecurityGroupsType getRequest( ) {
			final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType( );
			if ( groupIds != null && !groupIds.isEmpty( ) ) {
				req.getFilterSet().add( filter( "group-id", groupIds ) );
			}
			if ( groupNames != null && !groupNames.isEmpty( ) ) {
				req.setSecurityGroupSet( Lists.newArrayList( groupNames ) );
			}
			if ( groupNameFilters != null && !groupNameFilters.isEmpty( ) ) {
				req.getFilterSet().add( filter( "group-name", groupNameFilters ) );
			}
			if ( vpcId != null ) {
				req.getFilterSet().add( filter( "vpc-id", vpcId ) );
			}
			return req;
		}
		
		@Override
		List<SecurityGroupItemType> extractResult(ComputeMessage response) {
			final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
			return resp.getSecurityGroupInfo();
		}
	}

	private class EucalyptusModifySecurityGroupsTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus> {
		private final String instanceId;
		private final Collection<String> securityGroupIds;

		EucalyptusModifySecurityGroupsTask(
				final String instanceId,
				final Collection<String> securityGroupIds
		) {
			this.instanceId = instanceId;
			this.securityGroupIds = securityGroupIds;
		}

		@Override
		ComputeMessage getRequest( ) {
			final ModifyInstanceAttributeType modifyInstanceAttribute = new ModifyInstanceAttributeType( );
			modifyInstanceAttribute.setInstanceId( instanceId );
			modifyInstanceAttribute.setGroupIdSet( new GroupIdSetType( ) );
			for ( final String securityGroupId : securityGroupIds ) {
				final SecurityGroupIdSetItemType id = new SecurityGroupIdSetItemType( );
				id.setGroupId( securityGroupId );
				modifyInstanceAttribute.getGroupIdSet().getItem().add( id );
			}
			return modifyInstanceAttribute;
		}
	}


	private static Filter filter( final String name, String value ) {
		return filter( name, Collections.singleton( value ) );
	}

	private static Filter filter( final String name, final Iterable<String> values ) {
		final Filter filter = new Filter( );
		filter.setName( name );
		filter.setValueSet( Lists.newArrayList( values ) );
		return filter;
	}

	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
		protected EucalyptusActivityTask(){}

		final CheckedListenableFuture<Boolean> dispatch( final ActivityContext<TM,TC> context ) {
			try {
				final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
				dispatchInternal( context, new Callback.Checked<TM>(){
					@Override
					public void fireException( final Throwable throwable ) {
						boolean result = false;
						try {
							result = dispatchFailure( context, throwable );
						} finally {
							future.set( result );
						}
					}

					@Override
					public void fire( final TM response ) {
						try {
							dispatchSuccess( context, response );
						} finally {
							future.set( true );
						}
					}
				} );
				return future;
			} catch ( Exception e ) {
				LOG.error( e, e );
			}
			return Futures.predestinedFuture( false );
		}
	
		/**
		 * Build the request message
		 */
		abstract TM getRequest( );

		final void dispatchInternal( final ActivityContext<TM,TC> context, final Callback.Checked<TM> callback) {
			final DispatchingClient<TM, TC> client = context.getClient( );
			client.dispatch( getRequest( ), callback );
		}

		boolean dispatchFailure( ActivityContext<TM,TC> context, Throwable throwable ) {
			LOG.error( "Loadbalancer activity error", throwable );
			return false;
		}

		void dispatchSuccess( ActivityContext<TM,TC> context, TM response ){ }
	}

	private abstract class EucalyptusActivityTaskWithResult<TM extends BaseMessage, TC extends ComponentId, R>
			extends EucalyptusActivityTask<TM,TC> {
		private final AtomicReference<R> r = new AtomicReference<>( );

		/**
		 * Extract/construct the result from the response message
		 */
		abstract R extractResult( TM response );

		R failureResult( String errorCode ) { return null; }

		final R getResult( ) {
			return r.get( );
		}

		R getFailureResult( final String errorCode ) {
			return null;
		}

		@Override
		void dispatchSuccess( final ActivityContext<TM,TC> context, final TM response) {
			r.set( extractResult( response ) );
		}

		@Override
		boolean dispatchFailure( final ActivityContext<TM, TC> context, final Throwable throwable ) {
			final Optional<AsyncWebServiceError> serviceErrorOptional = AsyncExceptions.asWebServiceError( throwable );
			if ( serviceErrorOptional.isPresent( ) ){
				final R result = getFailureResult( serviceErrorOptional.get( ).getCode( ) );
				if ( result != null ) {
					r.set( result );
					return true;
				} else {
					return super.dispatchFailure( context, throwable );
				}
			} else {
				return super.dispatchFailure( context, throwable );
			}
		}
	}

	private <TM extends BaseMessage, TC extends ComponentId> void checkResult(
			final EucalyptusActivityTask<TM,TC> task,
			final ActivityContext<TM,TC> context,
			final String errorMessage
	) {
		final CheckedListenableFuture<Boolean> result = task.dispatch( context );
		try{
			if ( !result.get( ) ) {
				throw new EucalyptusActivityException( errorMessage );
			}
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}

	private <TM extends BaseMessage, TC extends ComponentId, R> R resultOf(
			final EucalyptusActivityTaskWithResult<TM,TC,R> task,
			final ActivityContext<TM,TC> context,
			final String errorMessage
	) {
		final CheckedListenableFuture<Boolean> result = task.dispatch( context );
		try{
			if (result.get() ){
				return task.getResult();
			}else
				throw new EucalyptusActivityException( errorMessage );
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
}
