public class TransactionInput {
    public String transactionOutputId; //transaction Id
    public TransactionOutput UTXO; // contains the unspent transaction output

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
