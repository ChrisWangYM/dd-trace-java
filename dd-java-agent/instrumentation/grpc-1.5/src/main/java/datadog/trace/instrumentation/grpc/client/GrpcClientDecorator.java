package datadog.trace.instrumentation.grpc.client;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.ClientDecorator;
import io.grpc.Status;

public class GrpcClientDecorator extends ClientDecorator {
  public static final CharSequence GRPC_CLIENT = UTF8BytesString.create("grpc.client");
  public static final CharSequence COMPONENT_NAME = UTF8BytesString.create("grpc-client");
  public static final CharSequence GRPC_MESSAGE = UTF8BytesString.create("grpc.message");
  public static final GrpcClientDecorator DECORATE = new GrpcClientDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"grpc", "grpc-client"};
  }

  @Override
  protected CharSequence component() {
    return COMPONENT_NAME;
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.RPC;
  }

  @Override
  protected String service() {
    return null;
  }

  public AgentSpan onClose(final AgentSpan span, final Status status) {

    span.setTag("status.code", status.getCode().name());
    span.setTag("status.description", status.getDescription());

    onError(span, status.getCause());
    if (!status.isOk()) {
      span.setError(true);
    }

    return span;
  }
}
