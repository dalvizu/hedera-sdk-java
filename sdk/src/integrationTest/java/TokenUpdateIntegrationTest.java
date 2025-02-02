import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TokenUpdateTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenUpdateIntegrationTest {
    @Test
    @DisplayName("Can update token")
    void canUpdateToken() throws Exception {
        var testEnv = new IntegrationTestEnv(1).useThrowawayAccount();

        var response = new TokenCreateTransaction()
            .setTokenName("ffff")
            .setTokenSymbol("F")
            .setDecimals(3)
            .setInitialSupply(1000000)
            .setTreasuryAccountId(testEnv.operatorId)
            .setAdminKey(testEnv.operatorKey)
            .setFreezeKey(testEnv.operatorKey)
            .setWipeKey(testEnv.operatorKey)
            .setKycKey(testEnv.operatorKey)
            .setSupplyKey(testEnv.operatorKey)
            .setFreezeDefault(false)
            .execute(testEnv.client);

        var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

        @Var var info = new TokenInfoQuery()
            .setTokenId(tokenId)
            .execute(testEnv.client);

        assertEquals(tokenId, info.tokenId);
        assertEquals("ffff", info.name);
        assertEquals("F", info.symbol);
        assertEquals(3, info.decimals);
        assertEquals(testEnv.operatorId, info.treasuryAccountId);
        assertNotNull(info.adminKey);
        assertNotNull(info.freezeKey);
        assertNotNull(info.wipeKey);
        assertNotNull(info.kycKey);
        assertNotNull(info.supplyKey);
        assertEquals(testEnv.operatorKey.toString(), info.adminKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.freezeKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.wipeKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.kycKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.supplyKey.toString());
        assertNotNull(info.defaultFreezeStatus);
        assertFalse(info.defaultFreezeStatus);
        assertNotNull(info.defaultKycStatus);
        assertFalse(info.defaultKycStatus);

        new TokenUpdateTransaction()
            .setTokenId(tokenId)
            .setTokenName("aaaa")
            .setTokenSymbol("A")
            .execute(testEnv.client)
            .getReceipt(testEnv.client);

        info = new TokenInfoQuery()
            .setTokenId(tokenId)
            .execute(testEnv.client);

        assertEquals(tokenId, info.tokenId);
        assertEquals("aaaa", info.name);
        assertEquals("A", info.symbol);
        assertEquals(3, info.decimals);
        assertEquals(testEnv.operatorId, info.treasuryAccountId);
        assertNotNull(info.adminKey);
        assertNotNull(info.freezeKey);
        assertNotNull(info.wipeKey);
        assertNotNull(info.kycKey);
        assertNotNull(info.supplyKey);
        assertEquals(testEnv.operatorKey.toString(), info.adminKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.freezeKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.wipeKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.kycKey.toString());
        assertEquals(testEnv.operatorKey.toString(), info.supplyKey.toString());
        assertNotNull(info.defaultFreezeStatus);
        assertFalse(info.defaultFreezeStatus);
        assertNotNull(info.defaultKycStatus);
        assertFalse(info.defaultKycStatus);

        testEnv.close(tokenId);
    }

    @Test
    @DisplayName("Cannot update immutable token")
    void cannotUpdateImmutableToken() throws Exception {
        var testEnv = new IntegrationTestEnv(1).useThrowawayAccount(new Hbar(10));

        var response = new TokenCreateTransaction()
            .setTokenName("ffff")
            .setTokenSymbol("F")
            .setTreasuryAccountId(testEnv.operatorId)
            .setFreezeDefault(false)
            .execute(testEnv.client);

        var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

        var error = assertThrows(ReceiptStatusException.class, () -> {
            new TokenUpdateTransaction()
                .setTokenId(tokenId)
                .setTokenName("aaaa")
                .setTokenSymbol("A")
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        });

        assertTrue(error.getMessage().contains(Status.TOKEN_IS_IMMUTABLE.toString()));

        // we lose this IntegrationTestEnv throwaway account
        testEnv.client.close();
    }
}
