/*
 * Copyright 2016 Pinpoint contributors and NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.jboss.interceptor;

import java.lang.reflect.Method;
import org.jboss.remoting.InvokerLocator;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.jboss.JbossConstants;
import com.navercorp.pinpoint.plugin.jboss.ProxyMethodDescriptor;
import com.navercorp.pinpoint.plugin.jboss.interceptor.getter.JbossRemoteProxyUriGetter;
import com.navercorp.pinpoint.plugin.jboss.util.JbossUtility;

/**
 * The Class ProxyInterceptor.
 *
 * @author <a href="mailto:guillermoadrianmolina@hotmail.com">Guillermo Adrián Molina</a>
 * @author guillermoadrianmolina
 */
public class ProxyInvokeInterceptor implements AroundInterceptor {

    /** The Constant PROXY_API_TAG. */
    public static final ProxyMethodDescriptor PROXY_API_TAG = new ProxyMethodDescriptor();

    /** The logger. */
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    /** The is debug. */
    private final boolean isDebug = logger.isDebugEnabled();

    /** The method descriptor. */
    private final MethodDescriptor methodDescriptor;

    /** The trace context. */
    private final TraceContext traceContext;

    /**
     * Instantiates a new invoke context interceptor.
     *
     * @param traceContext the trace context
     * @param descriptor the descriptor
     */
    public ProxyInvokeInterceptor(final TraceContext traceContext, final MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.methodDescriptor = descriptor;
        traceContext.cacheApi(PROXY_API_TAG);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor#before(java.lang.Object, java.lang.Object[])
     */
    @Override
    public void before(final Object target, final Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        try {
            final Method methodInvoked = (Method) args[1];
            logger.debug("XXXXXXXXXXXXXXXXXXXXXXXXX{}XXXXXXXXXXXXXXXXXXXXXXXXXXXXX", methodInvoked.toString());
            final StringBuilder methodNameBuilder = new StringBuilder();
            if (methodInvoked != null) {
                try {
                    final Class<?> declaringClass = methodInvoked.getDeclaringClass();
                    if (declaringClass != null) {
                        methodNameBuilder.append(declaringClass.getCanonicalName());
                        methodNameBuilder.append('.');
                    }
                    methodNameBuilder.append(methodInvoked.getName());
                } catch (final Exception exception) {
                    logger.error("An error occurred while fetching method details", exception);
                }
            }
            final String methodName = methodNameBuilder.toString();
            final InvokerLocator uri = getRemoteProxyUri(target);
            final String remoteAddress = getRemoteAddress(uri);
            final String endPoint = getEndPoint(uri);
            final Trace trace = createTrace(target, methodName, remoteAddress, endPoint);
            if (trace == null) {
                return;
            }

            if (!trace.canSampled()) {
                return;
            }

            final SpanEventRecorder recorder = trace.traceBlockBegin();
            TraceId nextId = trace.getTraceId().getNextTraceId();
            recorder.recordNextSpanId(nextId.getSpanId());
            recorder.recordServiceType(JbossConstants.JBOSS_METHOD);
            recorder.recordAttribute(AnnotationKey.ARGS1, methodName);
            /*if (remoteAddress != null) {
                recorder.recordDestinationId(remoteAddress);
            }*/
            if (endPoint != null) {
                recorder.recordEndPoint(endPoint);
            }
    
        } catch (final Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", th.getMessage(), th);
            }
        }
    }

    /**
     * Creates the trace.
     *
     * @param target the target
     * @param methodName the method name
     * @return the trace
     */
    private Trace createTrace(final Object target, final String methodName, final String remoteAddress, final String endPoint) {
        Trace trace = traceContext.currentTraceObject();
        if(trace == null) {
            trace = traceContext.newTraceObject();
            if (trace.canSampled()) {
                final SpanRecorder recorder = trace.getSpanRecorder();
                final String remoteAddressDetails = JbossUtility.fetchRemoteAddressDetails(remoteAddress);
                recordRootSpan(recorder, methodName, remoteAddressDetails, endPoint);
                if (isDebug) {
                    logger.debug("Trace sampling is true, Recording trace. methodInvoked:{}, remoteAddress:{}", methodName, remoteAddress);
                }
            } else {
                if (isDebug) {
                    logger.debug("Trace sampling is false, Skip recording trace. methodInvoked:{}, remoteAddress:{}", methodName, remoteAddress);
                }
            }    
        }
        return trace;
    }

    /**
     * Record root span.
     *
     * @param recorder the recorder
     * @param rpcName the rpc name
     * @param remoteAddress
     * @param endPoint
     */
    private void recordRootSpan(final SpanRecorder recorder, final String rpcName, final String remoteAddress, final String endPoint) {
        recorder.recordServiceType(JbossConstants.JBOSS);
        recorder.recordRpcName(rpcName);
        recorder.recordEndPoint(endPoint);
        recorder.recordRemoteAddress(remoteAddress);
        recorder.recordApi(PROXY_API_TAG);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor#after(java.lang.Object, java.lang.Object[],
     * java.lang.Object, java.lang.Throwable)
     */
    @Override
    public void after(final Object target, final Object[] args, final Object result, final Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        if (!trace.canSampled()) {
            traceContext.removeTraceObject();
            trace.close();
            return;
        }
        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(methodDescriptor);
            recorder.recordException(throwable);
        } catch (final Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", th.getMessage(), th);
            }
        } finally {
            trace.traceBlockEnd();
            if (trace.isRootStack()) {
                trace.close();
                traceContext.removeTraceObject();
            }
        }
    }

    private InvokerLocator getRemoteProxyUri(Object target) {
        if (target instanceof JbossRemoteProxyUriGetter) {
            return ((JbossRemoteProxyUriGetter) target)._$PINPOINT$_getRemoteProxyUri();
        }
        return null;
    }

    private String getRemoteAddress(InvokerLocator uri) {
        if (uri != null) {
            return uri.getHost();
        }
        return null;
    }

    private String getEndPoint(InvokerLocator uri) {
        if (uri != null) {
            return uri.getHost() + ":" + uri.getPort();
        }
        return null;
    }
}
