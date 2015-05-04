/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.util.List;

import org.opendaylight.tsdr.scheduler.Task;
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
    public CreateTableTask(){
        super();
    }

    @Override
    public void runTask(){
        Thread.currentThread().setName("TSDR HBase Data Store CreateTableTask-thread-" + Thread.currentThread().getId());
        createTables();
    }

    public void createTables(){
         log.debug("Entering createTables()");
             List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
             for ( String tableName: tableNames){
                 try{
                     HBaseDataStoreFactory.getHBaseDataStore().createTable(tableName);
                 }catch ( Throwable t){
                     log.error("Exception caught creating tables", t.getMessage());
                     log.trace("Exception caught creating tables", t);
                 }
             }
         return;
    }

}
