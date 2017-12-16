/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides formatting related utilities.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public final class FormatUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FormatUtil.class);

    public static final String QUERY_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
    public static final String COMMAND_OUT_TIMESTAMP = "MM/dd/yyyy HH:mm:ss";

    public static final String KEY_NODEID = "[NID=";
    public static final String KEY_CATEGORY = "[DC=";
    public static final String KEY_METRICNAME = "[MN=";
    public static final String KEY_RECORDKEYS = "[RK=";
    public static final String KEY_TIMESTAMP = "[TS=";
    public static final String KEY_RECORD_ATTRIBUTES = "[RA=";

    private static final Set<String> DATA_CATEGORY_STRINGS = new HashSet<>();

    static {
        for (DataCategory c:DataCategory.values()) {
            DATA_CATEGORY_STRINGS.add(c.name());
        }
    }

    private FormatUtil() {
    }

    public static boolean isDataCategory(String str) {
        return DATA_CATEGORY_STRINGS.contains(str);
    }

    /**
     * Check if the tsdrKey is a valid tsdrKey and only contains DC section.
     */
    public static boolean isDataCategoryKey(String tsdrKey) {
        if (isValidTSDRKey(tsdrKey)) {
            /*
             * if it's a valid metircs key, getNodeId, getMetriName,
             * getRecordKeys, and getDataCategory from the tsdrkey would not be
             * null.
             */
            if (getNodeIdFromTSDRKey(tsdrKey).length() == 0 && getMetriNameFromTSDRKey(tsdrKey).length() == 0
                    && getRecordKeysFromTSDRKey(tsdrKey).size() == 0
                    && getDataCategoryFromTSDRKey(tsdrKey).length() != 0) {
                return true;
            } else {
                return false;
            }
        } else if (isValidTSDRLogKey(tsdrKey)) {
            /*
             * if it's valid log key, getNodeId, getRecordKeys, and
             * getDataCategory from the tsdrKey would not be null.
             */
            if (getNodeIdFromTSDRKey(tsdrKey).length() == 0 && getRecordKeysFromTSDRKey(tsdrKey).size() == 0
                    && getDataCategoryFromTSDRKey(tsdrKey).length() != 0) {
                return true;
            } else {
                return false;
            }
        } else {
            // not a valid tsdrkey
            return false;
        }
    }

    public static String getFormattedTimeStamp(long timestamp, String formatString) {
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat(formatString, Locale.US);
        return formatter.format(date);
    }

    public static String getTSDRLogKeyWithTimeStamp(TSDRLog log) {
        StringBuilder sb = new StringBuilder(getTSDRLogKey(log));
        sb.append(KEY_TIMESTAMP);
        sb.append(log.getTimeStamp());
        sb.append("]");
        return sb.toString();
    }

    public static String getTSDRLogKey(TSDRLog log) {
        StringBuilder sb = new StringBuilder();
        sb.append(KEY_NODEID);
        if (log.getNodeID() != null) {
            sb.append(log.getNodeID());
        }
        sb.append("]");
        sb.append(KEY_CATEGORY);
        if (log.getTSDRDataCategory() != null) {
            sb.append(log.getTSDRDataCategory().name());
        }
        sb.append("]");
        sb.append(KEY_RECORDKEYS);
        if (log.getRecordKeys() != null) {
            boolean isFirst = true;
            for (RecordKeys rec : log.getRecordKeys()) {
                if (!isFirst) {
                    sb.append(",");
                }
                if (rec.getKeyName() != null) {
                    sb.append(rec.getKeyName());
                }
                sb.append(":");
                if (rec.getKeyValue() != null) {
                    sb.append(rec.getKeyValue());
                }
                isFirst = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getTSDRBinaryKey(TSDRBinaryRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(KEY_NODEID);
        if (record.getNodeID() != null) {
            sb.append(record.getNodeID());
        }
        sb.append("]");
        sb.append(KEY_CATEGORY);
        if (record.getTSDRDataCategory() != null) {
            sb.append(record.getTSDRDataCategory().name());
        }
        sb.append("]");
        sb.append(KEY_RECORDKEYS);
        if (record.getRecordKeys() != null) {
            boolean isFirst = true;
            for (RecordKeys rec : record.getRecordKeys()) {
                if (!isFirst) {
                    sb.append(",");
                }
                if (rec.getKeyName() != null) {
                    sb.append(rec.getKeyName());
                }
                sb.append(":");
                if (rec.getKeyValue() != null) {
                    sb.append(rec.getKeyValue());
                }
                isFirst = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static boolean isValidTSDRKey(String str) {
        if (getDataCategoryFromTSDRKey(str) == null) {
            return false;
        }
        if (getMetriNameFromTSDRKey(str) == null) {
            return false;
        }
        if (getNodeIdFromTSDRKey(str) == null) {
            return false;
        }
        if (getRecordKeysFromTSDRKey(str) == null) {
            return false;
        }
        return true;
    }

    public static boolean isValidTSDRLogKey(String str) {
        if (getDataCategoryFromTSDRKey(str) == null) {
            return false;
        }
        if (getNodeIdFromTSDRKey(str) == null) {
            return false;
        }
        if (getRecordKeysFromTSDRKey(str) == null) {
            return false;
        }
        return true;
    }

    public static String getDataCategoryFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_CATEGORY);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        return tsdrKey.substring(index1 + KEY_CATEGORY.length(), index2);
    }

    public static Long getTimeStampFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_TIMESTAMP);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        if (index2 == -1) {
            return null;
        }
        return Long.parseLong(tsdrKey.substring(index1 + KEY_TIMESTAMP.length(), index2));
    }

    public static String getMetriNameFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_METRICNAME);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        return tsdrKey.substring(index1 + KEY_METRICNAME.length(), index2);
    }

    public static String getNodeIdFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_NODEID);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        return tsdrKey.substring(index1 + KEY_NODEID.length(), index2);
    }

    public static List<RecordKeys> getRecordKeysFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_RECORDKEYS);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        String recs = tsdrKey.substring(index1 + KEY_RECORDKEYS.length(), index2);
        StringTokenizer tokens = new StringTokenizer(recs, ",");
        List<RecordKeys> result = new ArrayList<>();
        while (tokens.hasMoreTokens()) {
            String recKey = tokens.nextToken();
            int index3 = recKey.indexOf(":");
            RecordKeysBuilder rb = new RecordKeysBuilder();
            if (index3 == -1) {
                rb.setKeyName(recKey);
                rb.setKeyValue(recKey);
            } else {
                rb.setKeyName(recKey.substring(0, index3));
                rb.setKeyValue(recKey.substring(index3 + 1));
            }
            result.add(rb.build());
        }
        return result;
    }

    public static List<RecordAttributes> getRecordAttributesFromTSDRKey(String tsdrKey) {
        int index1 = tsdrKey.indexOf(KEY_RECORD_ATTRIBUTES);
        if (index1 == -1) {
            return null;
        }
        int index2 = tsdrKey.indexOf("]", index1);
        String recs = tsdrKey.substring(index1 + KEY_RECORD_ATTRIBUTES.length(), index2);
        StringTokenizer tokens = new StringTokenizer(recs, ",");
        List<RecordAttributes> result = new ArrayList<>();
        while (tokens.hasMoreTokens()) {
            String recKey = tokens.nextToken();
            int index3 = recKey.indexOf(":");
            RecordAttributesBuilder rb = new RecordAttributesBuilder();
            if (index3 == -1) {
                rb.setName(recKey);
                rb.setValue(recKey);
            } else {
                rb.setName(recKey.substring(0, index3));
                rb.setValue(recKey.substring(index3 + 1));
            }
            result.add(rb.build());
        }
        return result;
    }

    public static String getTSDRMetricKeyWithTimeStamp(TSDRMetric metric) {
        StringBuilder sb = new StringBuilder(getTSDRMetricKey(metric));
        sb.append(KEY_TIMESTAMP);
        sb.append(metric.getTimeStamp());
        sb.append("]");
        return sb.toString();
    }

    public static String getTSDRLogKeyWithRecordAttributes(TSDRLog log) {
        StringBuilder sb = new StringBuilder(getTSDRLogKey(log));
        sb.append(KEY_RECORD_ATTRIBUTES);
        if (log.getRecordAttributes() != null) {
            boolean isFirst = true;
            for (RecordAttributes rec : log.getRecordAttributes()) {
                if (!isFirst) {
                    sb.append(",");
                }
                if (rec.getName() != null && rec.getValue() != null) {
                    if (rec.getName().equals(rec.getValue())) {
                        sb.append(rec.getName());
                    } else {
                        sb.append(rec.getName());
                        sb.append(":");
                        sb.append(rec.getValue());
                    }
                }
                isFirst = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getTSDRMetricKey(TSDRMetric metric) {
        StringBuilder sb = new StringBuilder();
        sb.append(KEY_NODEID);
        if (metric.getNodeID() != null) {
            sb.append(metric.getNodeID());
        }
        sb.append("]");
        sb.append(KEY_CATEGORY);
        if (metric.getTSDRDataCategory() != null) {
            sb.append(metric.getTSDRDataCategory());
        }
        sb.append("]");
        sb.append(KEY_METRICNAME);
        if (metric.getMetricName() != null) {
            sb.append(metric.getMetricName());
        }
        sb.append("]");
        sb.append(KEY_RECORDKEYS);
        if (metric.getRecordKeys() != null) {
            boolean isFirst = true;
            for (RecordKeys rec : metric.getRecordKeys()) {
                if (!isFirst) {
                    sb.append(",");
                }
                if (rec.getKeyName() != null && rec.getKeyValue() != null) {
                    if (rec.getKeyName().equals(rec.getKeyValue())) {
                        sb.append(rec.getKeyName());
                    } else {
                        sb.append(rec.getKeyName());
                        sb.append(":");
                        sb.append(rec.getKeyValue());
                    }
                }
                isFirst = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static BigDecimal toMetricValue(Counter32 counter32) {
        if (counter32 != null) {
            return new BigDecimal(counter32.getValue());
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal toMetricValue(Counter64 counter64) {
        if (counter64 != null) {
            return new BigDecimal(counter64.getValue());
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal toMetricValue(BigInteger bigInteger) {
        if (bigInteger != null) {
            return new BigDecimal(bigInteger);
        }
        return BigDecimal.ZERO;
    }

    public static String getFixedFormatString(String value, long length) {
        return String.format("%1$" + length + "s", value);
    }
}
