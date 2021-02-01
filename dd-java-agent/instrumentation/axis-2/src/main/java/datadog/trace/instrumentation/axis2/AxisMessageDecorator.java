package datadog.trace.instrumentation.axis2;

import static datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes.SOAP;
import static datadog.trace.bootstrap.instrumentation.api.Tags.HTTP_METHOD;
import static datadog.trace.bootstrap.instrumentation.api.Tags.HTTP_STATUS;
import static datadog.trace.bootstrap.instrumentation.api.Tags.HTTP_URL;
import static datadog.trace.bootstrap.instrumentation.api.Tags.PEER_HOST_IPV4;
import static datadog.trace.bootstrap.instrumentation.api.Tags.PEER_HOST_IPV6;
import static datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator._200;
import static datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator._500;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;

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

  public boolean shouldTrace(final MessageContext message) {
    return true;
  }

  public boolean sameTrace(final AgentSpan span, final MessageContext message) {
    return true;
  }

  public void onMessage(final AgentSpan span, final MessageContext message) {
    span.setResourceName(message.getSoapAction());

    Object httpMethod = message.getProperty(HTTPConstants.HTTP_METHOD);
    if (httpMethod instanceof String) {
      span.setTag(HTTP_METHOD, (String) httpMethod);
      String address = message.getTo().getAddress();
      Object servicePrefix = message.getProperty("SERVICE_PREFIX");
      if (!address.startsWith("http") && servicePrefix instanceof String) {
        address = servicePrefix + address;
      }
      span.setTag(HTTP_URL, address);
    }

    Object remoteAddr = message.getProperty(MessageContext.REMOTE_ADDR);
    if (remoteAddr instanceof String) {
      String peerHostIp = (String) remoteAddr;
      if (peerHostIp.indexOf(':') > 0) {
        span.setTag(PEER_HOST_IPV6, peerHostIp);
      } else {
        span.setTag(PEER_HOST_IPV4, peerHostIp);
      }
    }
  }

  public void onError(final AgentSpan span, final MessageContext message, final Throwable error) {
    if (null != error) {
      super.onError(span, error);
    } else if (message.isProcessingFault()) {
      span.setError(true);
    }

    if (null != span.getTag(HTTPConstants.HTTP_METHOD)) {
      Object statusCode = message.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE);
      if (statusCode instanceof Integer) {
        span.setTag(HTTP_STATUS, statusCode);
      } else if (span.isError()) {
        span.setTag(HTTP_STATUS, _500);
      } else {
        span.setTag(HTTP_STATUS, _200);
      }
    }
  }
}
