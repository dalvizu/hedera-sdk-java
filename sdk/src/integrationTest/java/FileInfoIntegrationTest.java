import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.FileInfoQuery;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.MaxQueryPaymentExceededException;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileInfoIntegrationTest {
    @Test
    @DisplayName("Can query file info")
    void canQueryFileInfo() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        var response = new FileCreateTransaction()
            .setKeys(testEnv.operatorKey)
            .setContents("[e2e::FileCreateTransaction]")
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

        var info = new FileInfoQuery()
            .setFileId(fileId)
            .execute(testEnv.client);

        assertEquals(fileId, info.fileId);
        assertEquals(28, info.size);
        assertFalse(info.isDeleted);
        assertNotNull(info.keys);
        assertNull(info.keys.getThreshold());
        assertEquals(KeyList.of(testEnv.operatorKey), info.keys);

        new FileDeleteTransaction()
            .setFileId(fileId)
            .execute(testEnv.client)
            .getReceipt(testEnv.client);

        testEnv.close();
    }

    @Test
    @DisplayName("Can query file info with no admin key or contents")
    void canQueryFileInfoWithNoAdminKeyOrContents() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        var response = new FileCreateTransaction()
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

        var info = new FileInfoQuery()
            .setFileId(fileId)
            .execute(testEnv.client);

        assertEquals(fileId, info.fileId);
        assertEquals(0, info.size);
        assertFalse(info.isDeleted);
        assertNull(info.keys);

        testEnv.close();
    }

    @Test
    @DisplayName("Can get cost, even with a big max")
    @SuppressWarnings("UnusedVariable")
    void getCostBigMaxQueryFileInfo() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        var response = new FileCreateTransaction()
            .setKeys(testEnv.operatorKey)
            .setContents("[e2e::FileCreateTransaction]")
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

        var infoQuery = new FileInfoQuery()
            .setFileId(fileId)
            .setMaxQueryPayment(new Hbar(1000));

        var cost = infoQuery.getCost(testEnv.client);

        var info = infoQuery.setQueryPayment(cost).execute(testEnv.client);

        new FileDeleteTransaction()
            .setFileId(fileId)
            .execute(testEnv.client)
            .getReceipt(testEnv.client);

        testEnv.close();
    }

    @Test
    @DisplayName("Error, max is smaller than set payment.")
    void getCostSmallMaxQueryFileInfo() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        var response = new FileCreateTransaction()
            .setKeys(testEnv.operatorKey)
            .setContents("[e2e::FileCreateTransaction]")
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

        var infoQuery = new FileInfoQuery()
            .setFileId(fileId)
            .setMaxQueryPayment(Hbar.fromTinybars(1));

        assertThrows(MaxQueryPaymentExceededException.class, () -> {
            infoQuery.execute(testEnv.client);
        });

        new FileDeleteTransaction()
            .setFileId(fileId)
            .execute(testEnv.client)
            .getReceipt(testEnv.client);

        testEnv.close();
    }

    @Test
    @DisplayName("Insufficient tx fee error.")
    void getCostInsufficientTxFeeQueryFileInfo() throws Exception {
        var testEnv = new IntegrationTestEnv(1);

        var response = new FileCreateTransaction()
            .setKeys(testEnv.operatorKey)
            .setContents("[e2e::FileCreateTransaction]")
            .execute(testEnv.client);

        var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

        var infoQuery = new FileInfoQuery()
            .setFileId(fileId)
            .setMaxQueryPayment(Hbar.fromTinybars(1));

        var error = assertThrows(PrecheckStatusException.class, () -> {
            infoQuery.setQueryPayment(Hbar.fromTinybars(1)).execute(testEnv.client);
        });

        assertEquals(error.status.toString(), "INSUFFICIENT_TX_FEE");

        new FileDeleteTransaction()
            .setFileId(fileId)
            .execute(testEnv.client)
            .getReceipt(testEnv.client);

        testEnv.close();
    }
}
