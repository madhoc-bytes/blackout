package unsw.blackout;

public class File {
    private String filename;
    private String data;
    private int size;


    public File(String filename, String data, int size) {
        this.filename = filename;
        this.data = data;
        this.size = size;
    }
    

    public String getFilename() {
        return filename;
    }

    public int getSize() {
        return size;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isQuantum() {
        return this.getClass().getSimpleName().equals("QuantumFile");
    }

    /**
     * @return boolean that checks whether the current file has completed transferring
     */
    public boolean hasTransferCompleted() {
        return data.length() == size;
    }

}
