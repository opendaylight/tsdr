/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.opendaylight.tsdr.spi.scheduler.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class is a task that create HBase tables during the initialization
 * time of HBase data store. It extends the TSDR Task, which is schedulable
 * by TSDR Scheduler.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: May 4th, 2015
 *
 */
public class CreateTableTask extends Task{
    private static final Logger log = LoggerFactory.getLogger(CreateTableTask.class);
    private ScheduledFuture future = null;
    private List<String> pendingTableNames = new ArrayList<String>();
    public CreateTableTask(){
        super();
        pendingTableNames = new ArrayList<String>(HBasePersistenceUtil.getTSDRHBaseTables());
    }

    @Override
    public void runTask(){
        Thread.currentThread().setName("TSDR HBase Data Store CreateTableTask-thread-" + Thread.currentThread().getId());
        createTables();
    }
/**
 * Calls the CreateTable function for the tableName passed in the DataStore.
 * @param tableName
 * @throws Throwable
 */
    public void runCreateTable (String tableName) throws Throwable{
              HBaseDataStoreFactory.getHBaseDataStore().createTable(tableName);
    }
    public void createTables(){
         log.debug("Entering createTables()");
         Iterator<String> tableNameIter = pendingTableNames.iterator();
         while( tableNameIter.hasNext()){
             String  tableName = tableNameIter.next();
             try{
                 runCreateTable(tableName);
                 tableNameIter.remove();
             }catch ( Throwable t){
                 log.error("Exception caught creating tables", t);
                 log.trace("Exception caught creating tables", t);
             }
         }
         log.info("Exiting createTables()..pending tables count:" + pendingTableNames.size());
         synchronized(future){
             if(pendingTableNames.size() == 0){
                 future.cancel(true);
             }
         }
         return;
    }

    public void setScheduledFuture(ScheduledFuture scheduledFuture){
       future = scheduledFuture; 
    }

}
