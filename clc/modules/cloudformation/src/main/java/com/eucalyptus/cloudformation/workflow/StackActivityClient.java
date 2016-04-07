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
package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

/**
 * StackActivity but more promising.
 *
 * @see StackActivity
 */
@ActivityRegistrationOptions(
    defaultTaskScheduleToStartTimeoutSeconds = 900,
    defaultTaskStartToCloseTimeoutSeconds = 450,
    defaultTaskHeartbeatTimeoutSeconds = 450,
    defaultTaskScheduleToCloseTimeoutSeconds = 1350
)
@Activities(version="1.0")
public interface StackActivityClient {
  @Activity(name = "StackActivity.initCreateResource")
  Promise<String> initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int stackVersion);

  @Activity(name = "StackActivity.validateAWSParameterTypes")
  Promise<String> validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int stackVersion);

  @Activity(name = "StackActivity.performCreateStep")
  Promise<Boolean> performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);

  @Activity(name = "StackActivity.performDeleteStep")
  Promise<Boolean> performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion);

  @Activity(name = "StackActivity.determineCreateResourceFailures")
  Promise<String> determineCreateResourceFailures(String stackId, String accountId, int createdResourceVersion);

  @Activity(name = "StackActivity.determineUpdateResourceFailures")
  Promise<String> determineUpdateResourceFailures(String stackId, String accountId, int updatedResourceVersion);

  @Activity(name = "StackActivity.determineDeleteResourceFailures")
  Promise<String> determineDeleteResourceFailures(String stackId, String accountId, int deletedResourceVersion);

  @Activity(name = "StackActivity.initDeleteResource")
  Promise<String> initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion);

  @Activity(name = "StackActivity.createGlobalStackEvent")
  Promise<String> createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int stackVersion);

  @Activity(name = "StackActivity.finalizeCreateStack")
  Promise<String> finalizeCreateStack(String stackId, String accountId, String effectiveUserId, int createdStackVersion);

  @Activity(name = "StackActivity.finalizeUpdateStack")
  Promise<String> finalizeUpdateStack(String stackId, String accountId, String effectiveUserId, int updatedStackVersion);

  @Activity(name = "StackActivity.deleteAllStackRecords")
  Promise<String> deleteAllStackRecords(String stackId, String accountId);

  @Activity(name = "StackActivity.getResourceType")
  Promise<String> getResourceType(String stackId, String accountId, String resourceId, int resourceVersion);

  @Activity(name = "StackActivity.finalizeCreateResource")
  Promise<String> finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);

  @Activity(name = "StackActivity.finalizeDeleteResource")
  Promise<String> finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.failDeleteResource")
  Promise<String> failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int deletedResourceVersion);

  @Activity(name = "StackActivity.getCreateWorkflowExecutionCloseStatus")
  Promise<String> getCreateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getUpdateWorkflowExecutionCloseStatus")
  Promise<String> getUpdateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getStackStatus")
  Promise<String> getStackStatus(String stackId, String accountId, int stackVersion);

  @Activity(name = "StackActivity.setStackStatus")
  Promise<String> setStackStatus(String stackId, String accountId, String status, String statusReason, int stackVersion);

  @Activity(name = "StackActivity.cancelCreateAndMonitorWorkflows")
  Promise<String> cancelCreateAndMonitorWorkflows(String stackId);

  @Activity(name = "StackActivity.verifyCreateAndMonitorWorkflowsClosed")
  Promise<String> verifyCreateAndMonitorWorkflowsClosed(String stackId);

  @Activity(name = "StackActivity.cancelOutstandingCreateResources")
  Promise<String> cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage, int createdResourceVersion);

  @Activity(name = "StackActivity.cancelOutstandingUpdateResources")
  Promise<String> cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage, int updatedResourceVersion);

  @Activity(name = "StackActivity.initUpdateResource")
  Promise<String> initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateResource")
  Promise<String> finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.performUpdateStep")
  Promise<Boolean> performUpdateStep(String updateTypeAndDirectionStr, String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateCleanupStack")
  Promise<String> finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updatedStackVersion);

  @Activity(name = "StackActivity.initUpdateCleanupResource")
  Promise<String> initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.failUpdateCleanupResource")
  Promise<String> failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateCleanupResource")
  Promise<String> finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);


  @Activity(name = "StackActivity.initUpdateRollbackResource")
  Promise<String> initUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateRollbackResource")
  Promise<String> finalizeUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateRollbackStack")
  Promise<String> finalizeUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion);
  @Activity(name = "StackActivity.failUpdateRollbackStack")
  Promise<String> failUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion, String errorMessage);


  @Activity(name = "StackActivity.finalizeUpdateRollbackCleanupStack")
  Promise<String> finalizeUpdateRollbackCleanupStack(String stackId, String accountId, String statusMessage, int rolledBackStackVersion);
  @Activity(name = "StackActivity.initUpdateRollbackCleanupResource")
  Promise<String> initUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.failUpdateRollbackCleanupResource")
  Promise<String> failUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateRollbackCleanupResource")
  Promise<String> finalizeUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  @Activity(name = "StackActivity.rollbackStackState")
  Promise<String> rollbackStackState(String stackId, String accountId, int rolledBackStackVersion);

  @Activity(name = "StackActivity.recordUpdateRollbackInfo")
  Promise<String> recordUpdateRollbackInfo(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, Integer rolledBackStackVersion);

  @Activity(name = "StackActivity.flattenStackForDelete")
  Promise<String> flattenStackForDelete(String stackId, String accountId);

  @Activity(name = "StackActivity.checkResourceAlreadyRolledBackOrStartedRollback")
  Promise<String> checkResourceAlreadyRolledBackOrStartedRollback(String stackId, String accountId, String resourceId);

  @Activity(name = "StackActivity.addCompletedUpdateRollbackResource")
  Promise<String> addCompletedUpdateRollbackResource(String stackId, String accountId, String resourceId);

}
