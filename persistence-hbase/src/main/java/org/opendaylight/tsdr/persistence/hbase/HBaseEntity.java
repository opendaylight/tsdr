/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.util.List;
import java.io.Serializable;

/**
 * This class is part of HBase data model for HBase persistence data store.
 * HBase tables contain table name, rowkey and columns.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class HBaseEntity implements Serializable{
        private static final long serialVersionUID = 1L;
        private String rowKey;
        private String tableName;
        private List<HBaseColumn> columns;

        public String getRowKey() {
                return rowKey;
        }
        public void setRowKey(String rowKey) {
                this.rowKey = rowKey;
        }
        public List<HBaseColumn> getColumns() {
                return columns;
        }
        public void setColumns(List<HBaseColumn> columns) {
                this.columns = columns;
        }
        public String getPrimaryKey(){
                return rowKey;
        }
        public String getTableName() {
                return tableName;
        }
        public void setTableName(String tableName) {
                this.tableName = tableName;
        }

        @Override
        public String toString(){
            StringBuffer stringBuffer=new StringBuffer();
            stringBuffer.append("table:").append(tableName);
            stringBuffer.append(" rowkey:").append(rowKey);
            for(HBaseColumn currentColumn:columns){
                    stringBuffer.append(" family:").append(currentColumn.getColumnFamily())
                    .append(" qualifier:").append(currentColumn.getColumnQualifier())
                    .append(" value:").append(currentColumn.getValue());
            }
            return stringBuffer.toString();
         }
}
