package dev.shelfie.ref.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** Runs a plain grpc-java server next to Tomcat. Reflection is on so grpcurl works without the .proto. */
@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private final ShelfieGrpcService service;
    private final int port;
    private Server server;

    public GrpcServerLifecycle(ShelfieGrpcService service, @Value("${shelfie.grpc.port:9090}") int port) {
        this.service = service;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            server = NettyServerBuilder.forPort(port)
                    .addService(service)
                    .addService(ProtoReflectionServiceV1.newInstance())
                    .build()
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
