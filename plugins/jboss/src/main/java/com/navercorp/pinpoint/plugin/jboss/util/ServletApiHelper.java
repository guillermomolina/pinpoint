package com.navercorp.pinpoint.plugin.jboss.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ServletApiHelper {

    boolean isAsyncDispatcherBefore(HttpServletRequest request);

    boolean isAsyncDispatcherAfter(HttpServletRequest request);

    public boolean isAsyncStarted(HttpServletRequest request);

    public String getDispatcherTypeString(HttpServletRequest request);

    int getStatus(HttpServletResponse response);
}
