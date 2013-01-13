/**
 * Project: otter.canal.protocol-1.0.0
 * 
 * File Created at 2012-6-20
 * $Id$
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.otter.canal.protocol;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.alibaba.otter.canal.common.utils.CanalToStringStyle;

/**
 * @author zebin.xuzb @ 2012-6-20
 * @version 1.0.0
 */
public class ClientIdentity {

    private String destination;
    private short  clientId;
    private String filter;

    public ClientIdentity(String destination, short clientId){
        this.clientId = clientId;
        this.destination = destination;
    }

    public ClientIdentity(String destination, short clientId, String filter){
        this.clientId = clientId;
        this.destination = destination;
        this.filter = filter;
    }

    public Boolean hasFilter() {
        if (filter == null) {
            return false;
        }
        return StringUtils.isNotBlank(filter);
    }

    // ======== setter =========

    public String getDestination() {
        return destination;
    }

    public short getClientId() {
        return clientId;
    }

    public void setClientId(short clientId) {
        this.clientId = clientId;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, CanalToStringStyle.DEFAULT_STYLE);
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + clientId;
        result = prime * result + ((destination == null) ? 0 : destination.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ClientIdentity)) {
            return false;
        }
        ClientIdentity other = (ClientIdentity) obj;
        if (clientId != other.clientId) {
            return false;
        }
        if (destination == null) {
            if (other.destination != null) {
                return false;
            }
        } else if (!destination.equals(other.destination)) {
            return false;
        }
        return true;
    }

}
