package unsw.blackout;
import java.util.ArrayList;
import java.util.List;

import unsw.utils.Angle;

public class Apparatus {
    private String apparatusId;
    private String type;
    private Angle position;
    private double height;
    private List<File> files;
    private List<File> outbox;

    public Apparatus(String apparatusId, String type, double height, Angle position) {
        this.apparatusId = apparatusId;
        this.type = type;
        this.height = height;
        this.position = position;
        this.files = new ArrayList<>();
        this.outbox = new ArrayList<>();
    }

    /**
     * Given a distance, check whether that distance is within the apparatus' max range
     * @param distance
     */
    public boolean inMaxRange(double distance) {
        if (type.equals("StandardSatellite")) {
            return distance <= 150000;
        }
        if (type.equals("ShrinkingSatellite")) {
            return distance <= 200000;
        }
        if (type.equals("RelaySatellite")) {
            return distance <= 300000;
        }
        if (type.equals("HandheldDevice")) {
            return distance <= 50000;
        }
        if (type.equals("LaptopDevice")) {
            return distance <= 100000;
        }
        if (type.equals("DesktopDevice")) {
            return distance <= 200000;
        }
        return false;
    }

    /**
     * add @param newFile to the storage
     */
    public void addFile(File newFile) {
        files.add(newFile);
    }

    /**
     * @param existingFileName
     */
    public void removeFile(String existingFileName) {
        files.remove(getFileFromName(existingFileName));
    }

    /**
     * @param fileName
     * @return boolean of whether the file exists in the apparatus' inbox
     */
    public boolean isFileInInbox(String fileName) {
        for (File file : files) {
            if (file.getFilename().equals(fileName)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Getter for the complete files (100% transferred) in the apparatus
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Getter for the files currently SENDING FROM the apparatus
     */
    public List<File> getOutbox() {
        return outbox;
    }

    /**
     * 
     * given
     * @param newFile
     */
    public void addFileToOutbox(File newFile) {
        outbox.add(newFile);
    }

    /**
     * removes all files with no data from an outbox
     */
    public void cleanUpOutbox() {
        List<File> removing = new ArrayList<File>();
        for (File file : getOutbox()) {
            if (file.getData().length() == 0) {
                removing.add(file);
            }
        }
        outbox.removeAll(removing);
    }


    /**
     * given name and contents of  a file, add it to the current apparatus
     * @param filename
     * @param contents
     * @param size
     */

    public void addFile(String filename, String contents, int size) {
        File file = new File(filename, contents, size);
        files.add(file);
    }


    /**
     * given
     * @param name
     * @return file object corresponding to the name
     * if DNE, then return null
     */
    public File getFileFromName(String name) {
        for (File f : files) {
            if (f.getFilename().equals(name)) {
                return f;
            }
        }
        return null;
    }

    public String getId() {
        return apparatusId;
    }

    public String getType() {
        return type;
    }

    public Angle getPosition() {
        return position;
    }

    public double getHeight() {
        return height;
    }

    /**
     * Setter for position
     * @param position
     */
    public void setPosition(Angle position) {
        this.position = position;
    }

}
