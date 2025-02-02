package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.NftTransfer;

import javax.annotation.Nullable;
import java.util.Objects;

public class TokenNftTransfer implements Comparable<TokenNftTransfer> {
    public final AccountId sender;
    public final AccountId receiver;
    public final long serial;

    TokenNftTransfer(AccountId sender, AccountId receiver, long serial) {
        this.sender = sender;
        this.receiver = receiver;
        this.serial = serial;
    }

    static TokenNftTransfer fromProtobuf(NftTransfer nftTransfer) {
        return new TokenNftTransfer(
            AccountId.fromProtobuf(nftTransfer.getSenderAccountID()),
            AccountId.fromProtobuf(nftTransfer.getReceiverAccountID()),
            nftTransfer.getSerialNumber()
        );
    }

    public static TokenNftTransfer fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(NftTransfer.parseFrom(bytes).toBuilder().build());
    }

    NftTransfer toProtobuf() {
        return NftTransfer.newBuilder()
            .setSenderAccountID(sender.toProtobuf())
            .setReceiverAccountID(receiver.toProtobuf())
            .setSerialNumber(serial)
            .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("sender", sender)
            .add("receiver", receiver)
            .add("serial", serial)
            .toString();
    }

    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public int compareTo(TokenNftTransfer o) {
        int senderComparison = sender.compareTo(o.sender);
        if (senderComparison != 0) {
            return senderComparison;
        }
        int receiverComparison = receiver.compareTo(o.receiver);
        if (receiverComparison != 0) {
            return receiverComparison;
        }
        return Long.compare(serial, o.serial);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TokenNftTransfer)) {
            return false;
        }

        TokenNftTransfer otherTransfer = (TokenNftTransfer) o;
        return sender.equals(otherTransfer.sender) && receiver.equals(otherTransfer.receiver) && serial == otherTransfer.serial;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender.hashCode(), receiver.hashCode(), serial);
    }
}
