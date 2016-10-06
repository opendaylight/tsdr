/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import static org.mockito.Matchers.any;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * Test bundle activator
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
@RunWith(PowerMockRunner.class)
public class ActivatorTest {
    private ElasticsearchStore store = Mockito.spy(ElasticsearchStore.class);

    @Test
    public void init() throws Exception {
        Activator activator = Mockito.spy(Activator.class);
        Mockito.doReturn(new HashMap<String, String>()).when(activator).loadElasticsearchStore();
        activator.init(null, null);

    }

    @Test
    public void destroy() throws Exception {
        Activator activator = new Activator();
        Service service = Mockito.mock(Service.class);
        Mockito.doNothing().when(service).awaitTerminated(any(Long.class), any(TimeUnit.class));
        PowerMockito.doReturn(service).when((AbstractScheduledService) store).stopAsync();
        activator.setStore(store);
        activator.destroy(null, null);
    }
}
