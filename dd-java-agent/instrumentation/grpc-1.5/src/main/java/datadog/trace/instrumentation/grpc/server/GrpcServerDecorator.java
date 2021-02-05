package datadog.trace.instrumentation.grpc.server;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.ServerDecorator;
import io.grpc.Status;

public class GrpcServerDecorator extends ServerDecorator {

  public static final CharSequence GRPC_SERVER = UTF8BytesString.create("grpc.server");
  public static final CharSequence COMPONENT_NAME = UTF8BytesString.create("grpc-server");
  public static final CharSequence GRPC_MESSAGE = UTF8BytesString.create("grpc.message");
  public static final GrpcServerDecorator DECORATE = new GrpcServerDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"grpc", "grpc-server"};
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.RPC;
  }

  @Override
  protected CharSequence component() {
    return COMPONENT_NAME;
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
