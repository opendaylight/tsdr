/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.nontested;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.dataquery.nontested.query.MetricsRequest;
import org.opendaylight.tsdr.dataquery.nontested.model.MetricId;
import org.opendaylight.tsdr.dataquery.nontested.model.MetricRecord;
import org.opendaylight.tsdr.dataquery.nontested.model.RecordKeysCombination;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR Data Query Service implementation
 *
 * This service contains methods to query the TSDR Data Storage Service for
 * data.  Currently two divisions are made, Metrics query and LogRecord query.
 * This pattern will evolve as we design the correct RESTful time series data
 * query service.
 *
 * This service is used as the demarcation point between the TSDR data model and
 * the Data Query model. This division is made to logically separate the two
 * models. One could argue that a single, correct data model is good enough, but
 * this separation is forced on TSDR by the JAX-RS implementation, Jersey, that
 * (for the time being) requires annotations on the classes that it needs to
 * marshal. Putting Jersey annotations on the current TSDR data model, while
 * possible, would violate good architectural design. Hence the new Data Query
 * Service data model that will be extensible to future data sets as well as
 * future methods of retrieval.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

public class TSDRQueryServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(TSDRQueryServiceImpl.class);
    private static TSDRService tsdrService = null;
    private static RpcProviderRegistry rpcRegistry = null;

    public TSDRQueryServiceImpl(TSDRService tsdrService, RpcProviderRegistry rpcRegistry) {
        TSDRQueryServiceImpl.tsdrService = tsdrService;
        TSDRQueryServiceImpl.rpcRegistry = rpcRegistry;
    }

    /**
     * Build and execute the TSDR Persistence SPI request to get the metrics
     * from TSDR. A list of request data in Jersey aware classes are converted
     * to the TSDR input. Marshal the metrics data into Data Query model objects
     * for return to the caller.
     *.
     * TSDRQueryServiceImpl is the demarcation between the TSDR data model and
     * the public Data Query model interface. This is necessary because Jersey
     * (at the moment) requires annotations on the data objects in order to
     * recognize them for the Jersey REST interface marshalling mechanisms.
     *
     * @param requests - Data Query model requests
     * @return metricsRecordList - results from the TSDR SPI calls.
     */
    public static List<MetricRecord> getTSDRMetrics(List<MetricsRequest> requests) {
        List<Metrics> tsdrMetricRecordLists = null;
        List<MetricRecord> metricRecordList = null;

        // Only process the first request for now.  MetricRequest contains the
        // start and end times for all queries, each of which are defined in
        // a MetricId instance.
        for (MetricId metricId : requests.get(0).getMetricIdList()) {
            GetTSDRMetricsInputBuilder inputBuilder = new GetTSDRMetricsInputBuilder();
            // TODO: need a good, common way to get the DataCategory enum from string
            // inputBuilder.setTSDRDataCategory(request.getCategoryName());
 
            inputBuilder.setTSDRDataCategory(requests.get(0).getMetricIdList().get(0).getCategoryName()); // <== temporary hack
            inputBuilder.setStartTime(requests.get(0).getStartTime());
            inputBuilder.setEndTime(requests.get(0).getEndTime());

            GetTSDRMetricsInput input = inputBuilder.build();

            // I know the TsdrPersistenceServiceUtil utility class does this
            // same null check mitigation.
            // What are the requirements for ensuring non-null TsdrService here?
            if (tsdrService == null) {
                tsdrService = rpcRegistry.getRpcService(TSDRService.class);
            }

            try {
                // Make the call to the TSDR Persistence SPI to get the TSDR metrics.
                Future<RpcResult<GetTSDRMetricsOutput>> output = tsdrService.getTSDRMetrics(input);

                // getTSDRMetricRecordList() throws Interrupted and Execution Exceptions.
                // Is there a need to distinguish between the two?
                tsdrMetricRecordLists = output.get().getResult().getMetrics();
            } catch (IllegalStateException ex) {
                log.error("TSDRQueryServiceImpl.getTSDRMetrics() caught unhandled IllegalStateException: " + ex
                        .getMessage());
            } catch (ExecutionException ex) {
                log.error("TSDRQueryServiceImpl.getTSDRMetrics() caught unhandled ExecutionException: " + ex
                        .getMessage());
            } catch (InterruptedException ex) {
                log.error("TSDRQueryServiceImpl.getTSDRMetrics() caught unhandled InterruptedException: " + ex
                        .getMessage());
            } catch (Exception ex) {
                log.error("TSDRQueryServiceImpl.getTSDRMetrics() caught unhandled Exception: " + ex.getMessage());
                // What exceptions are accepted/required to rethrow?
            }

            metricRecordList = convertTSDRMetricRecordListToMetricRecordList(tsdrMetricRecordLists);

            // Just process the first request for now.
            break;
        }

        return metricRecordList;
    }

    /**
     * Convert the TSDR data model, List<TSDRMetricRecordList>, to the Data
     * Query data model, List<MetricRecord>.
     *
     * @param tsdrMetricRecordLists TSDR Metric Records
     * @return metricRecordList - Data Query return
     */
    private static List<MetricRecord> convertTSDRMetricRecordListToMetricRecordList(List<Metrics> tsdrMetricRecordLists) {
        List<MetricRecord> metricRecordList = new ArrayList<MetricRecord>();

        for (Metrics tsdrMetricRecordList : tsdrMetricRecordLists) {
            MetricRecord metricRecord = new MetricRecord();
            metricRecord.setMetricName(tsdrMetricRecordList.getMetricName());
            metricRecord.setMetricValue(tsdrMetricRecordList.getMetricValue());
            metricRecord.setTimeStamp(BigDecimal.valueOf(System.currentTimeMillis()));
            metricRecord.setNodeId(tsdrMetricRecordList.getNodeID());
            metricRecord.setCategoryName(tsdrMetricRecordList.getTSDRDataCategory().name());

            // The following two data points are specific to a certain request, in this case, FlowTable statistics.
            // Is there a published list of these combinations?
            // metricRecord.setFlowId(tsdrMetricRecordList.get); // stored in RecordKeys for FlowTable statistics
            // metricRecord.setTableId(tsdrMetricRecordList.get); // stored in RecordKeys for FlowTable statistics

            List<RecordKeysCombination> recordKeysCombinationList = new ArrayList<RecordKeysCombination>();
            for (RecordKeys tsdrRecordKeys : tsdrMetricRecordList.getRecordKeys()) {
                RecordKeysCombination recordKeysCombination = new RecordKeysCombination(tsdrRecordKeys.getKeyName(),
                        tsdrRecordKeys.getKeyValue());
                recordKeysCombinationList.add(recordKeysCombination);
            }
            metricRecordList.add(metricRecord);
        }

        for (MetricRecord metricRecord : metricRecordList) {
            log.debug(metricRecord.toMetricRecordString());
        }
        return metricRecordList;
    }

    // public static List<MetricRecord> getTSDRLogRecords(List<LogRecordsRequest> requests)
}
