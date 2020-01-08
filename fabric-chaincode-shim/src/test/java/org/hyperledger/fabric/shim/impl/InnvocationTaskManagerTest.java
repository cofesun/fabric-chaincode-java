/*
 * Copyright 2020 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.metrics.Metrics;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.chaincode.EmptyChaincode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class InnvocationTaskManagerTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    void setEnv() {
        environmentVariables.set("CORE_CHAINCODE_ID_NAME", "mycc");
        environmentVariables.set("CORE_PEER_ADDRESS", "localhost:7052");
        environmentVariables.set("CORE_PEER_TLS_ENABLED", "false");
        environmentVariables.set("CORE_PEER_TLS_ROOTCERT_FILE", "src/test/resources/ca.crt");
        environmentVariables.set("CORE_TLS_CLIENT_KEY_PATH", "src/test/resources/client.key.enc");
        environmentVariables.set("CORE_TLS_CLIENT_CERT_PATH", "src/test/resources/client.crt.enc");
    }

    @Test
    void getManager() throws IOException {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
    }

    @Test
    void getManagerChaincodeIDNull() throws IOException {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> {
                    final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, null);
                },
                "chaincodeId can't be null"
        );
    }

    @Test
    void getManagerChaincodeBaseNull() throws IOException {

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> {
                    final InnvocationTaskManager itm = InnvocationTaskManager.getManager(null, chaincodeId);
                },
                "chaincode is null"
        );
    }

    @Test
    void onChaincodeMessage() throws IOException {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> itm.onChaincodeMessage(null),
            "chaincodeMessage is null"
        );
    }

    @Test
    void setResponseConsumer() throws IOException {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
        itm.setResponseConsumer(null);
    }

    @Test
    void registerException() {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);

        Assertions.assertThrows(
                IllegalArgumentException.class, itm::register,
                "outgoingMessage is null"
        );

    }

    @Test
    void onChaincodeMessageREGISTER() {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
        final Consumer<ChaincodeShim.ChaincodeMessage> consumer = t -> {
            assertEquals(ChaincodeMessageFactory.newRegisterChaincodeMessage(chaincodeId), t);
        };

        itm.setResponseConsumer(consumer);
        final ChaincodeShim.ChaincodeMessage chaincodeMessage = ChaincodeMessageFactory.newRegisterChaincodeMessage(chaincodeId);
        itm.onChaincodeMessage(chaincodeMessage);
    }

    @Test
    void onChaincodeMessageINVOKE_CHAINCODE() {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final String chaincodeIdNumber = "chaincodeIdNumber12345";
        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName(chaincodeIdNumber).build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
        final Consumer<ChaincodeShim.ChaincodeMessage> consumer = t -> {
            assertEquals(ChaincodeMessageFactory.newRegisterChaincodeMessage(chaincodeId), t);
        };

        itm.setResponseConsumer(consumer);
        final ChaincodeShim.ChaincodeMessage chaincodeMessage = ChaincodeMessageFactory.newInvokeChaincodeMessage(chaincodeIdNumber, "txid", ByteString.copyFromUtf8(""));
        itm.onChaincodeMessage(chaincodeMessage);
    }

    @Test
    void onChaincodeMessagePutState() {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final String chaincodeIdNumber = "chaincodeIdNumber12345";
        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName(chaincodeIdNumber).build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
        final Consumer<ChaincodeShim.ChaincodeMessage> consumer = t -> {
            assertEquals(ChaincodeMessageFactory.newRegisterChaincodeMessage(chaincodeId), t);
        };

        itm.setResponseConsumer(consumer);
        final ChaincodeShim.ChaincodeMessage chaincodeMessage = ChaincodeMessageFactory.newPutStateEventMessage(chaincodeIdNumber, "txid", "collection", "key", ByteString.copyFromUtf8("value"));
        itm.onChaincodeMessage(chaincodeMessage);
    }

    @Test
    void shutdown() throws IOException {

        final ChaincodeBase chaincodeBase = new EmptyChaincode();
        chaincodeBase.processEnvironmentOptions();
        chaincodeBase.validateOptions();

        Properties props = chaincodeBase.getChaincodeConfig();
        Metrics.initialize(props);

        final ManagedChannelBuilder<?> managedChannelBuilder = chaincodeBase.newChannelBuilder();
        ChaincodeSupportClient chaincodeSupportClient = new ChaincodeSupportClient(managedChannelBuilder);

        final Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("chaincodeIdNumber12345").build();
        final InnvocationTaskManager itm = InnvocationTaskManager.getManager(chaincodeBase, chaincodeId);
        itm.shutdown();
    }
}