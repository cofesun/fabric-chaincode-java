/*
 * Copyright 2020 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.shim;


import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * implementation grpc server with NettyGrpcServer.
 */
public final class NettyGrpcServer implements GrpcServer {

    private static Log logger = LogFactory.getLog(NettyGrpcServer.class);

    private final Server server;
    /**
     * init netty grpc server.
     *
     * @param chaincodeBase - chaincode implementation (invoke, init)
     * @param chaincodeServerProperties - setting for grpc server
     * @throws IOException
     */
    public NettyGrpcServer(final ChaincodeBase chaincodeBase, final ChaincodeServerProperties chaincodeServerProperties) throws IOException {
        if (chaincodeBase == null) {
            throw new IOException("chaincode must be specified");
        }
        if (chaincodeServerProperties == null) {
            throw new IOException("chaincodeServerProperties must be specified");
        }
        chaincodeServerProperties.validate();

        final NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(chaincodeServerProperties.getPortChaincodeServer())
                .addService(new ChatChaincodeWithPeer(chaincodeBase))
                .keepAliveTime(chaincodeServerProperties.getKeepAliveTimeMinutes(), TimeUnit.MINUTES)
                .keepAliveTimeout(chaincodeServerProperties.getKeepAliveTimeoutSeconds(), TimeUnit.SECONDS)
                .permitKeepAliveTime(chaincodeServerProperties.getPermitKeepAliveTimeMinutes(), TimeUnit.MINUTES)
                .permitKeepAliveWithoutCalls(chaincodeServerProperties.isPermitKeepAliveWithoutCalls())
                .maxConnectionAge(chaincodeServerProperties.getMaxConnectionAgeSeconds(), TimeUnit.SECONDS)
                .maxInboundMetadataSize(chaincodeServerProperties.getMaxInboundMetadataSize())
                .maxInboundMessageSize(chaincodeServerProperties.getMaxInboundMessageSize());

        if (chaincodeServerProperties.isTlsEnabled()) {
            final File keyCertChainFile = Paths.get(chaincodeServerProperties.getKeyCertChainFile()).toFile();
            final File keyFile = Paths.get(chaincodeServerProperties.getKeyFile()).toFile();

            if (chaincodeServerProperties.getKeyPassword() == null || chaincodeServerProperties.getKeyPassword().isEmpty()) {
                serverBuilder.sslContext(SslContextBuilder.forServer(keyCertChainFile, keyFile).build());
            } else {
                serverBuilder.sslContext(SslContextBuilder.forServer(keyCertChainFile, keyFile, chaincodeServerProperties.getKeyPassword()).build());
            }
        }

        logger.info("<<<<<<<<<<<<<chaincodeServerProperties>>>>>>>>>>>>:\n");
        logger.info("PortChaincodeServer:" + chaincodeServerProperties.getPortChaincodeServer());
        logger.info("MaxInboundMetadataSize:" + chaincodeServerProperties.getMaxInboundMetadataSize());
        logger.info("MaxInboundMessageSize:" + chaincodeServerProperties.getMaxInboundMessageSize());
        logger.info("MaxConnectionAgeSeconds:" + chaincodeServerProperties.getMaxConnectionAgeSeconds());
        logger.info("KeepAliveTimeoutSeconds:" + chaincodeServerProperties.getKeepAliveTimeoutSeconds());
        logger.info("PermitKeepAliveTimeMinutes:" + chaincodeServerProperties.getPermitKeepAliveTimeMinutes());
        logger.info("KeepAliveTimeMinutes:" + chaincodeServerProperties.getKeepAliveTimeMinutes());
        logger.info("PermitKeepAliveWithoutCalls:" + chaincodeServerProperties.getPermitKeepAliveWithoutCalls());
        logger.info("KeyPassword:" + chaincodeServerProperties.getKeyPassword());
        logger.info("KeyCertChainFile:" + chaincodeServerProperties.getKeyCertChainFile());
        logger.info("KeyFile:" + chaincodeServerProperties.getKeyFile());
        logger.info("isTlsEnabled:" + chaincodeServerProperties.isTlsEnabled());
        logger.info("\n");

        this.server = serverBuilder.build();
    }

    /**
     * start grpc server.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        logger.info("start grpc server");
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(() -> {
                            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                            System.err.println("*** shutting down gRPC server since JVM is shutting down");
                            NettyGrpcServer.this.stop();
                            System.err.println("*** server shut down");
                        }));
        server.start();
    }

    /**
     * Waits for the server to become terminated.
     *
     * @throws InterruptedException
     */
    public void blockUntilShutdown() throws InterruptedException {
        logger.info("Waits for the server to become terminated.");
        server.awaitTermination();
    }

    /**
     * shutdown now grpc server.
     */
    public void stop() {
        logger.info("shutdown now grpc server.");
        server.shutdownNow();
    }
}
