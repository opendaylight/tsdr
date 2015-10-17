/**
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

//{
//"metrics:results": {
//  {
//      "ActiveFlows": "12",
//      "timestamp": "15 Oct 2015 13:15:20 PST"
//  },
//  {
//      "PacketMatched": "0",
//      "timestamp": "15 Oct 2015 13:15:20 PST"
//  },
//  {
//      "PacketLookedup": "0",
//       "timestamp": "15 Oct 2015 13:15:20 PST"
//  }
//}
//}

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@XmlRootElement(name = "TSDRLogRecordsReply")
public class TSDRLogRecordsReply {
}
