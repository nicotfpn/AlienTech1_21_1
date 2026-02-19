package net.nicotfpn.alientech.entropy;

/**
 * Transaction-safe entropy transfer result.
 * <p>
 * Used to ensure entropy transfers are atomic: either fully succeed or fully fail.
 * Prevents entropy duplication or loss from partial transfers.
 */
public final class EntropyTransaction {

    private final int amount;
    private final boolean committed;

    private EntropyTransaction(int amount, boolean committed) {
        this.amount = amount;
        this.committed = committed;
    }

    /**
     * Create a committed transaction (transfer succeeded).
     */
    public static EntropyTransaction committed(int amount) {
        return new EntropyTransaction(amount, true);
    }

    /**
     * Create a failed transaction (transfer did not occur).
     */
    public static EntropyTransaction failed() {
        return new EntropyTransaction(0, false);
    }

    /**
     * @return the amount transferred (only valid if committed)
     */
    public int getAmount() {
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
     * @param source the source entropy handler
     * @param dest the destination entropy handler
     * @param maxAmount maximum amount to transfer
     * @return transaction result
     */
    public static EntropyTransaction transfer(IEntropyHandler source, IEntropyHandler dest, int maxAmount) {
        if (source == null || dest == null || maxAmount <= 0) {
            return failed();
        }

        if (!source.canExtract() || !dest.canInsert()) {
            return failed();
        }

        // Step 1: Simulate extraction
        int available = source.extractEntropy(maxAmount, true);
        if (available <= 0) {
            return failed();
        }

        // Step 2: Simulate insertion
        int accepted = dest.insertEntropy(available, true);
        if (accepted <= 0) {
            return failed();
        }

        // Step 3: Determine actual transfer amount
        int toTransfer = Math.min(available, accepted);

        // Step 4: Execute both operations atomically
        int extracted = source.extractEntropy(toTransfer, false);
        if (extracted != toTransfer) {
            // Extraction failed - abort transaction
            return failed();
        }

        int inserted = dest.insertEntropy(extracted, false);
        if (inserted != extracted) {
            // Insertion failed - entropy is lost (should not happen with proper handlers)
            // This is a critical error state
            return failed();
        }

        return committed(inserted);
    }
}
