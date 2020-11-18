package com.hedera.hashgraph.sdk.examples;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.contract.*;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;

public final class CreateSimpleContract {

    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final Ed25519PrivateKey OPERATOR_KEY = Ed25519PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = Dotenv.load().get("CONFIG_FILE");

    private CreateSimpleContract() { }

    public static void main(String[] args) throws HederaStatusException, IOException {
        Client client;

        if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch (FileNotFoundException e) {
                client = Client.forTestnet();
            }
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        ClassLoader cl = CreateSimpleContract.class.getClassLoader();

        Gson gson = new Gson();

        JsonObject jsonObject;

        try (InputStream jsonStream = cl.getResourceAsStream("hello_world.json")) {
            if (jsonStream == null) {
                throw new RuntimeException("failed to get hello_world.json");
            }

            jsonObject = gson.fromJson(new InputStreamReader(jsonStream), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
            .getAsString();

        // create the contract's bytecode file
        TransactionId fileTxId = new FileCreateTransaction()
            // Use the same key as the operator to "own" this file
            .addKey(OPERATOR_KEY.publicKey)
            .setContents(byteCodeHex.getBytes())
            .setMaxTransactionFee(2000000000)
            .execute(client);

        TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
        FileId newFileId = fileReceipt.getFileId();

        System.out.println("contract bytecode file: " + newFileId);

        // create the contract itself
        TransactionId contractTxId = new ContractCreateTransaction()
            .setGas(217000)
            .setBytecodeFileId(newFileId)
            // set an admin key so we can delete the contract later
            .setAdminKey(OPERATOR_KEY.publicKey)
            .setMaxTransactionFee(2000000000)
            .execute(client);

        TransactionReceipt contractReceipt = contractTxId.getReceipt(client);

        System.out.println(contractReceipt.toProto());

        ContractId newContractId = contractReceipt.getContractId();

        System.out.println("new contract ID: " + newContractId);

        ContractFunctionResult contractCallResult = new ContractCallQuery()
            .setGas(30000)
            .setContractId(newContractId)
            .setFunction("greet")
            .execute(client);

        if (contractCallResult.errorMessage != null) {
            System.out.println("error calling contract: " + contractCallResult.errorMessage);
            return;
        }

        String message = contractCallResult.getString(0);
        System.out.println("contract message: " + message);

        // now delete the contract
        TransactionId contractDeleteTxnId = new ContractDeleteTransaction()
            .setContractId(newContractId)
            .execute(client);

        TransactionReceipt contractDeleteResult = contractDeleteTxnId.getReceipt(client);

        if (contractDeleteResult.status != Status.Success) {
            System.out.println("error deleting contract: " + contractDeleteResult.status);
            return;
        }
        System.out.println("Contract successfully deleted");
    }
}