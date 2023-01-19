package unsw.blackout;

public class QuantumFile extends File {
    public QuantumFile(String fileName, String data, int size) {
        super(fileName, data, size);
    }

    @Override
    public boolean hasTransferCompleted() {
        return super.getData().length() >= super.getSize() * (3 / 2);
    }
}
