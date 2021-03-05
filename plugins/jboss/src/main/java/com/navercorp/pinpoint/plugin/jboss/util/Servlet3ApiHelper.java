package com.navercorp.pinpoint.plugin.jboss.util;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet3ApiHelper implements ServletApiHelper {
    public Servlet3ApiHelper() {
    }

    @Override
    public boolean isAsyncDispatcherBefore(HttpServletRequest request) {
        return request.isAsyncStarted() || request.getDispatcherType() == DispatcherType.ASYNC;
    }

    @Override
    public boolean isAsyncDispatcherAfter(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ASYNC;
    }

    @Override
    public boolean isAsyncStarted(HttpServletRequest request) {
        return request.isAsyncStarted();
    }

    @Override
    public String getDispatcherTypeString(HttpServletRequest request) {
        return request.getDispatcherType().toString();
    }

    @Override
    public int getStatus(HttpServletResponse response) {
        return response.getStatus();
    }
}
