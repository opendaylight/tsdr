/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.util;

import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class provides formatting related utilities
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *         <p>
 *         Created: May 13, 2015
 */
public class FormatUtil {
    public static final String QUERY_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
    private static final Logger log = LoggerFactory.getLogger(FormatUtil.class);
    public static final String COMMAND_OUT_TIMESTAMP = "MM/dd/yyyy HH:mm:ss";

    public static String getFormattedTimeStamp(long timestamp, String formatString) {
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat(formatString, Locale.US);
        return (formatter.format(date));
    }


    /**
     * Convert the object keys in TSDR Model as Metric Details in JSON format to be
     * output in Karaf console
     *
     * @param objectKeys
     * @param category
     * @return formatted JSON string
     */
    public static String convertToMetricDetailsJSON(String objectKeys, String category) {
        String[] keys = objectKeys.split(TSDRConstants.ROWKEY_SPLIT);

        if (category.equalsIgnoreCase(TSDRConstants.FLOW_STATS_CATEGORY_NAME)) {
            if (keys == null || keys.length < 3){
                return null;
            }
            return "{ 'Node':'" + keys[0] + "'," + "'Table':'" + keys[1] + "',"
                    + "'Flow':'" + keys[2] + "'}";
        }

        if (category.equalsIgnoreCase(TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME)) {
            if (keys == null || keys.length < 2) {
                return null;
            }
            return "{ 'Node':'" + keys[0] + "'," + "'Table':'" + keys[1] + "'}";
        }

        if (category.equalsIgnoreCase(TSDRConstants.PORT_STATS_CATEGORY_NAME)) {
            if (keys == null || keys.length < 2) {
                return null;
            }
            return "{ 'Node':'" + keys[0] + "'," + "'InterfaceName':'" + keys[1] + "'}";
        }

        if (category.equalsIgnoreCase(TSDRConstants.QUEUE_STATS_CATEGORY_NAME)) {
            if ( keys == null || keys.length < 3){
                return null;
            }
            return "{ 'Node':'" + keys[0] + "'," + "'InterfaceName':'" + keys[1] + "',"
                    + "'QueueName':'" + keys[2] + "'}";
        }

        if (category.equalsIgnoreCase(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME)) {
            if ( keys == null || keys.length < 3){
                 return null;
             }
            return "{ 'Node':'" + keys[0] + "'," + "'GroupName':'" + keys[1] + "',"
                    + "'BucketID':'" + keys[2] + "'}";
        }
        if (category.equalsIgnoreCase(TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME)) {
            if ( keys == null || keys.length < 3){
                 return null;
             }
            return "{ 'Node':'" + keys[0] + "'," + "'GroupName':'" + keys[1] + "',"
                    + "'MeterName':'" + keys[2] + "'}";
        } else {
            log.warn("The table name is not supported:{}", category);
            return null;
        }
    }



    /**
     * Get Metrics Details (which is used as key string in non-sql stores) from TSDRMetric data.
     * @param metricData
     * @return
     */
    public static String getMetricsDetails(TSDRMetric metricData){
        StringBuffer keyBuffer = new StringBuffer();
        List<RecordKeys> recordKeys = metricData.getRecordKeys();
        if ( recordKeys != null && recordKeys.size() != 0){
            for(RecordKeys key: recordKeys){
                if (key.getKeyValue() != null && key.getKeyValue().length() != 0){
                    keyBuffer = keyBuffer.append(TSDRConstants.ROWKEY_SPLIT)
                            .append(key.getKeyValue());
                }

            }
        }
        String keyString = keyBuffer.toString();
        if (keyString != null && keyString.length() != 0 &&
                keyString.startsWith(TSDRConstants.ROWKEY_SPLIT)){
            keyString = keyString.substring(1);
        }
        return keyString;
    }

    public static String getLogID(TSDRLog m){
        StringBuilder sb = new StringBuilder();
        sb.append(m.getTSDRDataCategory());
        sb.append(TSDRConstants.ID_SPLIT);
        sb.append(m.getNodeID());
        return sb.toString();
    }

    public static String getMetricID(TSDRMetric m){
        StringBuilder sb = new StringBuilder();
        sb.append(m.getTSDRDataCategory());
        sb.append(TSDRConstants.ID_SPLIT);
        sb.append(m.getMetricName());
        sb.append(TSDRConstants.ID_SPLIT);
        sb.append(m.getNodeID());
        List<RecordKeys> keys = m.getRecordKeys();
        if(keys!=null){
            for(RecordKeys key:keys){
                sb.append(TSDRConstants.ID_SPLIT);
                sb.append(key.getKeyName());
                sb.append(TSDRConstants.ID_SPLIT_ARG);
                sb.append(key.getKeyValue());
            }
        }
        return sb.toString();
    }

    public static final BigDecimal toMetricValue(Counter32 counter32){
        if(counter32!=null)
            return new BigDecimal(counter32.getValue());
        return BigDecimal.ZERO;
    }

    public static final BigDecimal toMetricValue(Counter64 counter64){
        if(counter64!=null)
            return new BigDecimal(counter64.getValue());
        return BigDecimal.ZERO;
    }

    public static final BigDecimal toMetricValue(BigInteger bigInteger){
        if(bigInteger!=null)
            return new BigDecimal(bigInteger);
        return BigDecimal.ZERO;
    }

}
