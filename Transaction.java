package noobchain;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {

    public String transactionId; //Contains a hash of transaction*
    public PublicKey sender; //Senders address/public key.
    public PublicKey reciepient; //Recipients address/public key.
    public float value; //Contains the amount we wish to send to the recipient.
    public byte[] signature; //This is to prevent anybody else from spending funds in our wallet.

    public ArrayList<noobchain.TransactionInput> inputs = new ArrayList<noobchain.TransactionInput>();
    public ArrayList<noobchain.TransactionOutput> outputs = new ArrayList<noobchain.TransactionOutput>();

    private static int sequence = 0; //A rough count of how many transactions have been generated

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<noobchain.TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //Gathers transaction inputs (Making sure they are unspent):
        for(noobchain.TransactionInput i : inputs) {
            i.UTXO = noobchain.NoobChain.UTXOs.get(i.transactionOutputId);
        }

        //Checks if transaction is valid:
        if(getInputsValue() < noobchain.NoobChain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + noobchain.NoobChain.minimumTransaction);
            return false;
        }

        //Generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calulateHash();
        outputs.add(new noobchain.TransactionOutput( this.reciepient, value,transactionId)); //send value to recipient
        outputs.add(new noobchain.TransactionOutput( this.sender, leftOver,transactionId)); //send the left over 'change' back to sender

        //Add outputs to Unspent list
        for(noobchain.TransactionOutput o : outputs) {
            noobchain.NoobChain.UTXOs.put(o.id , o);
        }

        //Remove transaction inputs from UTXO lists as spent:
        for(noobchain.TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it
            noobchain.NoobChain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(noobchain.TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal.
            total += i.UTXO.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = noobchain.StringUtil.getStringFromKey(sender) + noobchain.StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        signature = noobchain.StringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = noobchain.StringUtil.getStringFromKey(sender) + noobchain.StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        return noobchain.StringUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for(noobchain.TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

    private String calulateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return noobchain.StringUtil.applySha256(
                noobchain.StringUtil.getStringFromKey(sender) +
                        noobchain.StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }
}