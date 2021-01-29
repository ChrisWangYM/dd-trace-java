package datadog.trace.instrumentation.axis2;

import static datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes.SOAP;
import static datadog.trace.bootstrap.instrumentation.api.Tags.HTTP_METHOD;
import static datadog.trace.bootstrap.instrumentation.api.Tags.HTTP_STATUS;
import static datadog.trace.bootstrap.instrumentation.api.Tags.PEER_HOST_IPV4;
import static datadog.trace.bootstrap.instrumentation.api.Tags.PEER_HOST_IPV6;
import static java.lang.Boolean.TRUE;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;

@Slf4j
public class AxisMessageDecorator extends BaseDecorator {
  public static final CharSequence AXIS2 = UTF8BytesString.createConstant("axis2");
  public static final CharSequence AXIS2_MESSAGE = UTF8BytesString.createConstant("axis2.message");
  public static final AxisMessageDecorator DECORATE = new AxisMessageDecorator();

  private AxisMessageDecorator() {}

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"axis2"};
  }

  @Override
  protected CharSequence spanType() {
    return SOAP;
  }

  @Override
  protected CharSequence component() {
    return AXIS2;
  }

  public void onMessage(final AgentSpan span, final MessageContext context) {
    String resourceName = context.getTo().getAddress();

    Object httpMethod = context.getProperty(Constants.Configuration.HTTP_METHOD);
    if (httpMethod instanceof String) {
      span.setTag(HTTP_METHOD, (String) httpMethod);
      resourceName = httpMethod + " " + resourceName;
    }

    span.setResourceName(resourceName);

    Object remoteAddr = context.getProperty(MessageContext.REMOTE_ADDR);
    if (remoteAddr instanceof String) {
      String peerHostIp = (String) remoteAddr;
      if (peerHostIp.indexOf(':') > 0) {
        span.setTag(PEER_HOST_IPV6, peerHostIp);
      } else {
        span.setTag(PEER_HOST_IPV4, peerHostIp);
      }
    }

    if (context.isProcessingFault()) {
      span.setError(true);
    }
  }

  @Override
  public AgentSpan beforeFinish(final AgentSpan span) {
    if (null != span.getTag(HTTP_METHOD)) {
      if (TRUE.equals(span.isError())) {
        span.setTag(HTTP_STATUS, 500);
      } else {
        span.setTag(HTTP_STATUS, 200);
      }
    }
    return super.beforeFinish(span);
  }
}
