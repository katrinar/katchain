import java.security.*;
import java.util.ArrayList;

public class Transaction {
    public String transactionId; //this is also the hash of the transaction
    public PublicKey sender; //sender address/public key
    public PublicKey recipient; //recipient address/public key
    public float value;
    public byte[] signature;

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; //count of transactions generated

    //Constructor
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    //Calculates the transaction hash, used as transaction Id
    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        Float.toString(value) +
                        sequence
        );
    }

    //Signs the data
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        signature = StringUtil.applyECDSA(privateKey, data);
    }

    //Verifies the data signed is valid
    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    //Returns true if new transaction created
    public boolean processTransaction() {
        if (verifySignature() == false) {
            System.out.println("Transaction Signature failed to verify");
            return false;
        }

        //get transaction inputs
        for (TransactionInput i : inputs) {
            i.UTXO = KatChain.UTXOs.get(i.transactionOutputId);
        }

        //check if transaction is valid
        if (getInputsValue() < KatChain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            return false;
        }

        //generate transaction outputs
        float leftover = getInputsValue() - value;
        transactionId = calculateHash();

        //send value to recipient
        outputs.add(new TransactionOutput( this.recipient, value, transactionId));

        //send the left over back to sender
        outputs.add(new TransactionOutput( this.sender, leftover, transactionId));

        //add outputs to unspent list
        for (TransactionOutput o : outputs) {
            KatChain.UTXOs.put(o.id, o);
        }

        //remove transaction inputs from UTXO lists as spent
        for (TransactionInput i: inputs) {
            if (i.UTXO == null) continue;
            KatChain.UTXOs.remove(i.UTXO.id);
        }
        return true;
    }

    //Returns sum of inputs (UTXOs)
    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            total += i.UTXO.value;
        }
        return total;
    }

    //Returns sum of outputs
    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput o: outputs) {
            total += o.value;
        }
        return total;
    }
}
