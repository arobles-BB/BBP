package com.bloobirds.bbp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

/**
 * Bean to throw at the server
 */
public class CallRecord implements Serializable {
    private Date datetime;
    private String duration;
    private String toNumber;
    private String callType;
    private String direction;

    public CallRecord(Date datetime, String duration, String toNumber, String callType, String direction) {
        this.datetime = datetime;
        this.duration = duration;
        this.toNumber = toNumber;
        this.callType = callType;
        this.direction = direction;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}