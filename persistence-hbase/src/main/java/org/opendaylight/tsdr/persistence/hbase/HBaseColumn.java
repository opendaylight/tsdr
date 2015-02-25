/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.Serializable;

/**
 * This class is part of HBase data model for HBase persistence data store.
 * HBase tables contain table name, rowkey and columns. Each column contains
 * column family, column qualifier, cell value, and the timestamp associated
 * with the data. This class models such data structure in HBase tales.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class HBaseColumn implements Serializable{

      private static final long serialVersionUID = 1L;
      private String columnFamily;
      private String columnQualifier;
      private String Value;
      private long timeStamp;

      /**
       * Default constructor.
       */
      public HBaseColumn(){
          super();
      }

      /**
       * Constructor with specified parameters.
       * @param columnFamily
       * @param columnQualifier
       * @param value
       */
      public HBaseColumn(String columnFamily, String columnQualifier, String value){
              this.columnFamily=columnFamily;
              this.columnQualifier=columnQualifier;
              this.Value=value;
      }

      public String getColumnFamily() {
              return columnFamily;
      }

      public void setColumnFamily(String collumnFamily) {
              this.columnFamily = collumnFamily;
      }
      public String getColumnQualifier() {
          return columnQualifier;
      }

      public void setColumnQualifier(String collumnQualifier) {
          this.columnQualifier = collumnQualifier;
      }

      public String getValue() {
          return Value;
      }
      public void setValue(String value) {
          Value = value;
      }
      public long getTimeStamp() {
          return timeStamp;
      }

      public void setTimeStamp(long timeStamp) {
          this.timeStamp = timeStamp;
      }
}
