/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.jboss.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.plugin.jboss.JbossConstants;
import com.navercorp.pinpoint.plugin.jboss.interceptor.getter.JbossRemoteProxyUriGetter;

import java.lang.reflect.Method;
import org.jboss.remoting.InvokerLocator;

/**
 * The Class EJBMethodInterceptor.
 *
 * @author <a href="mailto:guillermoadrianmolina@hotmail.com">Guillermo Adrián Molina</a>
 * @author guillermoadrianmolina
 */
public class EJBMethodInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {
    public EJBMethodInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        final Method method = (Method) args[1];
        logger.debug("XXXXXXXXXXXXXXXXXXXXXXXXX{}XXXXXXXXXXXXXXXXXXXXXXXXXXXXX", method.getName());

        recorder.recordServiceType(JbossConstants.JBOSS_METHOD);
        recorder.recordApi(methodDescriptor);

        final String methodName = method.getName();
        if (method != null && StringUtils.hasLength(methodName)) {
            logger.debug("YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY{}", methodName);
            recorder.recordAttribute(AnnotationKey.ARGS1, methodName);
        }

        final String remoteAddress = getRemoteAddress(target);
        if (remoteAddress != null) {
            recorder.recordDestinationId(remoteAddress);
        }
    }

    @Override
    public void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordException(throwable);

        if (result instanceof AsyncContextAccessor) {
            if (AsyncContextAccessorUtils.getAsyncContext(result) == null) {
                // Avoid duplicate async context
                final AsyncContext asyncContext = recorder.recordNextAsyncContext();
                ((AsyncContextAccessor) result)._$PINPOINT$_setAsyncContext(asyncContext);
            }
        }
        final Method method = (Method) args[1];
        final String methodName = method.getName();
        logger.debug("ZZZZZZZZZZZZZZZZZ{}ZZZZZZZZZZZZZZZZZ{}ZZZZZZZZZZZZZZZZZZZZZ", methodName, methodDescriptor);
    }


    private String getRemoteAddress(Object target) {
        if (target instanceof JbossRemoteProxyUriGetter) {
            final InvokerLocator uri = ((JbossRemoteProxyUriGetter) target)._$PINPOINT$_getRemoteProxyUri();
            if (uri != null) {
                return uri.getHost();
            }
        }
        return null;
    }
}
