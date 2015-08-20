/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi.rest;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.config.yang.config.tsdr.northbound.api.TSDRNBIModule;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.getmetric.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRRestAdapter implements Runnable {
    // Server socket for the rest requests/response
    private ServerSocket restSocket = null;
    private static Logger logger = LoggerFactory.getLogger(TSDRRestAdapter.class);
    private TSDRNBIModule module = null;

    public TSDRRestAdapter(TSDRNBIModule _module) {
        this.module = _module;
        try {
            restSocket = new ServerSocket(8098);
            new Thread(this, "TSDR REST Adapter").start();
        } catch (Exception err) {
            logger.error("Failed to initialize TSDR Rest Adapter", err);
        }
    }

    public void shutdown() {
        try {
            this.restSocket.close();
        } catch (Exception err) {
        }
    }

    public void run() {
        while (this.module.isRunning()) {
            try {
                Socket s = restSocket.accept();
                TSDRRestRequest request = new TSDRRestRequest(
                        s.getInputStream());
                TSDRRestReply reply = new TSDRRestReply(s.getOutputStream());
                ITSDRRequest tsdrRequest = (ITSDRRequest) request
                        .getRequest(ITSDRRequest.class);
                reply.reply(executeRequest(tsdrRequest), 200);
                s.getOutputStream().close();
                s.close();
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public String executeRequest(ITSDRRequest request) {
        GetMetricInputBuilder input = new GetMetricInputBuilder();
        input.setName(request.getTarget());

        input.setFrom(getTimeFromString(request.getFrom()));
        input.setUntil(getTimeFromString(request.getUntil()));
        long maxDataPoints = 0;
        try {
            maxDataPoints = Long.parseLong(request.getMaxDataPoints());
        } catch (Exception err) {
        }
        if (maxDataPoints == 0)
            return "[]";
        Future<RpcResult<GetMetricOutput>> metric = module.getTSDRService()
                .getMetric(input.build());
        try {
            List<Metrics> metrics = metric.get().getResult().getMetrics();
            if (metrics == null || metrics.size() == 0) {
                return "[]";
            }
            int skip = 1;
            if (metrics.size() > maxDataPoints) {
                skip = (int) (metrics.size() / maxDataPoints);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[{\"target\": \"" + request.getTarget()
                    + "\", \"datapoints\": [[");
            int count = 0;
            for (Metrics m : metrics) {
                if (count % skip == 0) {
                    sb.append(m.getValue());
                    sb.append(", ");
                    String time = "" + m.getTime();
                    sb.append(time.substring(0, 10));
                }
                count++;
                if ((count - 1) % skip == 0) {
                    if (count < metrics.size() || count < maxDataPoints)
                        sb.append("],[");
                }
            }
            sb.append("]]}]");
            return sb.toString();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to execute request",e);
        }
        return "[]";
    }

    public long getTimeFromString(String t) {
        if (t == null)
            return System.currentTimeMillis();
        int index1 = t.indexOf("-");
        if (index1 != -1) {
            int index2 = t.indexOf("min");
            if (index2 != -1) {
                int min = Integer.parseInt(t.substring(index1 + 1, index2)
                        .trim());
                return System.currentTimeMillis() - (min * 60000);
            }
            index2 = t.indexOf("h");
            if (index2 != -1) {
                int h = Integer
                        .parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (h * 3600000);
            }
            index2 = t.indexOf("d");
            if (index2 != -1) {
                int d = Integer
                        .parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (d * 86400000);
            }
        } else if (t.equals("now")) {
            return System.currentTimeMillis();
        }
        return Long.parseLong(t) * 1000;
    }
}
