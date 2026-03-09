package net.nicotfpn.alientech.entropy;

/**
 * Transaction-safe entropy transfer result.
 * <p>
 * Used to ensure entropy transfers are atomic: either fully succeed or fully
 * fail. Prevents entropy duplication or loss from partial transfers.
 * All amounts are {@code long} to match the upgraded {@link IEntropyHandler}
 * API.
 */
public final class EntropyTransaction {

    private final long amount;
    private final boolean committed;

    private EntropyTransaction(long amount, boolean committed) {
        this.amount = amount;
        this.committed = committed;
    }

    public static EntropyTransaction committed(long amount) {
        return new EntropyTransaction(amount, true);
    }

    public static EntropyTransaction failed() {
        return new EntropyTransaction(0L, false);
    }

    /**
     * @return the amount transferred (only valid if committed)
     */
    public long getAmount() {
        return amount;
    }

    /**
     * @return true if the transaction was committed (transfer occurred)
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Execute a transaction-safe entropy transfer from source to destination.
     * <p>
     * Algorithm:
     * 1. Simulate extraction from source
     * 2. Simulate insertion into destination
     * 3. If both succeed, execute both operations
     * 4. Return transaction result
     *
     * @param source    the source entropy handler
     * @param dest      the destination entropy handler
     * @param maxAmount maximum amount to transfer
     * @return transaction result
     */
    public static EntropyTransaction transfer(IEntropyHandler source, IEntropyHandler dest, long maxAmount) {
        if (source == null || dest == null || maxAmount <= 0L) {
            return failed();
        }

        if (!source.canExtract() || !dest.canInsert()) {
            return failed();
        }

        // Step 1: Simulate extraction
        long available = source.extractEntropy(maxAmount, true);
        if (available <= 0L) {
            return failed();
        }

        // Step 2: Simulate insertion
        long accepted = dest.insertEntropy(available, true);
        if (accepted <= 0L) {
            return failed();
        }

        // Step 3: Determine actual transfer amount
        long toTransfer = Math.min(available, accepted);

        // Step 4: Execute both operations atomically
        long extracted = source.extractEntropy(toTransfer, false);
        if (extracted <= 0L) {
            return failed();
        }

        long inserted = dest.insertEntropy(extracted, false);
        if (inserted != extracted) {
            long uninserted = extracted - inserted;
            if (uninserted > 0L) {
                source.insertEntropy(uninserted, false);
            }
            if (inserted <= 0L) {
                return failed();
            }
            return committed(inserted);
        }

        return committed(inserted);
    }
}
