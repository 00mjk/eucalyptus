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
package com.eucalyptus.cloudwatch.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.internal.domain.InvalidTokenException;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Datapoint;
import com.eucalyptus.cloudwatch.common.msgs.Datapoints;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsResult;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsType;
import com.eucalyptus.cloudwatch.common.msgs.Metric;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.Metrics;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.msgs.Statistics;
import com.eucalyptus.cloudwatch.common.policy.CloudWatchPolicySpec;
import com.eucalyptus.cloudwatch.service.queue.metricdata.MetricDataQueue;
import com.eucalyptus.component.Faults;
import com.eucalyptus.context.Context;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.component.ComponentException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.backend.msgs.CloudWatchBackendMessage;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import com.google.common.base.Objects;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
public class CloudWatchService implements Callable {

  private static final Logger LOG = Logger.getLogger(CloudWatchService.class);
  public PutMetricDataResponseType putMetricData(PutMetricDataType request)
    throws CloudWatchException {
    PutMetricDataResponseType reply = request.getReply();
    long before = System.currentTimeMillis();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_PUTMETRICDATA, ctx);
      if (CloudWatchConfigProperties.isDisabledCloudWatchService()) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final List<MetricDatum> metricData = CloudWatchBackendServiceFieldValidator.validateMetricData(request.getMetricData());
      final String namespace = CloudWatchBackendServiceFieldValidator.validateNamespace(request.getNamespace(), true);
      final Boolean privileged = Contexts.lookup().isPrivileged();
      LOG.trace("Namespace=" + namespace);
      LOG.trace("metricData="+metricData);
      MetricType metricType = CloudWatchBackendServiceFieldValidator.getMetricTypeFromNamespace(namespace);
      if (metricType == MetricType.System && !privileged) {
        throw new InvalidParameterValueException("The value AWS/ for parameter Namespace is invalid.");
      }
      MetricDataQueue.getInstance().insertMetricData(ownerFullName.getAccountNumber(), namespace, metricData, metricType);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public ListMetricsResponseType listMetrics(ListMetricsType request)
    throws CloudWatchException {
    ListMetricsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_LISTMETRICS, ctx);

      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String namespace = CloudWatchBackendServiceFieldValidator.validateNamespace(request.getNamespace(), false);
      final String metricName = CloudWatchBackendServiceFieldValidator.validateMetricName(request.getMetricName(),
        false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionFiltersToMap.INSTANCE
        .apply(CloudWatchBackendServiceFieldValidator.validateDimensionFilters(request.getDimensions()));

      // take all stats updated after two weeks ago
      final Date after = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60
        * 60 * 1000L);
      final Date before = null; // no bound on time before stats are updated
      // (though maybe 'now')
      final Integer maxRecords = 500; // per the API docs
      final String nextToken = request.getNextToken();
      final List<ListMetric> results;
      try {
        results = ListMetricManager.listMetrics(
          ownerFullName.getAccountNumber(), metricName, namespace,
          dimensionMap, after, before, maxRecords, nextToken);
      } catch (InvalidTokenException e) {
        // not sure why, but this is the message AWS sends (different from the alarm case, different exception too)
        throw new InvalidParameterValueException("Invalid nextToken");
      }

      final Metrics metrics = new Metrics();
      metrics.setMember(Lists.newArrayList(Collections2
        .<ListMetric, Metric>transform(results,
          TransformationFunctions.ListMetricToMetric.INSTANCE)));
      final ListMetricsResult listMetricsResult = new ListMetricsResult();
      listMetricsResult.setMetrics(metrics);
      if (maxRecords != null && results.size() == maxRecords) {
        listMetricsResult.setNextToken(results.get(results.size() - 1)
          .getNaturalId());
      }
      reply.setListMetricsResult(listMetricsResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }
  public GetMetricStatisticsResponseType getMetricStatistics(
    GetMetricStatisticsType request) throws CloudWatchException {
    GetMetricStatisticsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    try {
      // IAM Action Check
      checkActionPermission(CloudWatchPolicySpec.CLOUDWATCH_GETMETRICSTATISTICS, ctx);

      // TODO: parse statistics separately()?
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      Statistics statistics = CloudWatchBackendServiceFieldValidator.validateStatistics(request.getStatistics());
      final String namespace = CloudWatchBackendServiceFieldValidator.validateNamespace(request.getNamespace(), true);
      final String metricName = CloudWatchBackendServiceFieldValidator.validateMetricName(request.getMetricName(),
        true);
      final Date startTime = MetricUtils.stripSeconds(CloudWatchBackendServiceFieldValidator.validateStartTime(request.getStartTime(), true));
      final Date endTime = MetricUtils.stripSeconds(CloudWatchBackendServiceFieldValidator.validateEndTime(request.getEndTime(), true));
      final Integer period = CloudWatchBackendServiceFieldValidator.validatePeriod(request.getPeriod(), true);
      CloudWatchBackendServiceFieldValidator.validateDateOrder(startTime, endTime, "StartTime", "EndTime", true,
        true);
      CloudWatchBackendServiceFieldValidator.validateNotTooManyDataPoints(startTime, endTime, period, 1440L);

      // TODO: null units here does not mean Units.NONE but basically a
      // wildcard.
      // Consider this case.
      final Units units = CloudWatchBackendServiceFieldValidator.validateUnits(request.getUnit(), false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionsToMap.INSTANCE
        .apply(CloudWatchBackendServiceFieldValidator.validateDimensions(request.getDimensions()));
      Collection<MetricStatistics> metrics;
      metrics = MetricManager.getMetricStatistics(
        ownerFullName.getAccountNumber(), metricName, namespace,
        dimensionMap, CloudWatchBackendServiceFieldValidator.getMetricTypeFromNamespace(namespace), units,
        startTime, endTime, period);
      reply.getGetMetricStatisticsResult().setLabel(metricName);
      ArrayList<Datapoint> datapoints = CloudWatchBackendServiceFieldValidator.convertMetricStatisticsToDatapoints(
        statistics, metrics);
      if (datapoints.size() > 0) {
        Datapoints datapointsReply = new Datapoints();
        datapointsReply.setMember(datapoints);
        reply.getGetMetricStatisticsResult().setDatapoints(datapointsReply);
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  private CloudWatchMessage dispatchAction( final CloudWatchMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );
    if ( !Permissions.perhapsAuthorized(CloudWatchPolicySpec.VENDOR_CLOUDWATCH, getIamActionByMessageType( request ), user ) ) {
      throw new CloudWatchAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    try {
      final CloudWatchBackendMessage backendRequest = (CloudWatchBackendMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final CloudWatchMessage response = (CloudWatchMessage) BaseMessages.deepCopy( backendResponse, request.getReply().getClass() );
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws ClassNotFoundException {
    return Class.forName( request.getClass( ).getName( ).replace( ".common.msgs.", ".common.backend.msgs." ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( CloudWatchBackend.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new CloudWatchUnavailableException( "Service Unavailable" );
    } catch ( ServiceDispatchException e ) {
      final ComponentException componentException = Exceptions.findCause( e, ComponentException.class );
      if ( componentException != null && componentException.getCause( ) instanceof Exception ) {
        throw (Exception) componentException.getCause( );
      }
      throw e;
    } catch ( final FailedRequestException e ) {
      if ( request.getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new CloudWatchException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final EucalyptusRemoteFault remoteFault = Exceptions.findCause( e, EucalyptusRemoteFault.class );
    if ( remoteFault != null ) {
      final HttpResponseStatus status = Objects.firstNonNull( remoteFault.getStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR );
      final String code = remoteFault.getFaultCode( );
      final String message = remoteFault.getFaultDetail( );
      switch( status.getCode( ) ) {
        case 400:
          throw new CloudWatchClientException( code, message );
        case 403:
          throw new CloudWatchAuthorizationException( code, message );
        case 404:
          throw new CloudWatchNotFoundException( code, message );
        case 503:
          throw new CloudWatchUnavailableException( message );
        default:
          throw new CloudWatchException( code, Role.Receiver, message );
      }
    }
  }

  private static final int DISABLED_SERVICE_FAULT_ID = 1500;
  private boolean alreadyFaulted = false;
  private void faultDisableCloudWatchServiceIfNecessary() {
    // TODO Auto-generated method stub
    if (!alreadyFaulted) {
      Faults.forComponent(CloudWatch.class).havingId(DISABLED_SERVICE_FAULT_ID).withVar("component", "cloudwatch").log();
      alreadyFaulted = true;
    }

  }

  private void checkActionPermission(final String actionType, final Context ctx)
    throws EucalyptusCloudException {
    if (!Permissions.isAuthorized(CloudWatchPolicySpec.VENDOR_CLOUDWATCH, actionType, "",
      ctx.getAccount(), actionType, ctx.getAuthContext())) {
      throw new EucalyptusCloudException("User does not have permission");
    }
  }

  private static void handleException(final Exception e)
    throws CloudWatchException {
    final CloudWatchException cause = Exceptions.findCause(e,
      CloudWatchException.class);
    if (cause != null) {
      throw cause;
    }

    final InternalFailureException exception = new InternalFailureException(
      String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }

  @Override
  public Object onCall(MuleEventContext muleEventContext) throws Exception {
    final CloudWatchMessage request = (CloudWatchMessage) muleEventContext.getMessage( ).getPayload( );
    LOG.debug(request.toSimpleString());
    return dispatchAction(request);
  }
}
