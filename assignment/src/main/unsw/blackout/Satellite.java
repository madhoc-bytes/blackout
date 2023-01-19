package unsw.blackout;

import unsw.utils.Angle;

public class Satellite extends Apparatus {
    private double height;
    private int direction;

    public Satellite(String satelliteId, String type, double height, Angle position) {
        super(satelliteId, type, height, position);
        this.height = height;
        this.direction = 1;
    }

    /**
     * @return the maximum bandwidth of upload of the satellite
     */
    public int getMaxSendBandwidth() {
        if (super.getType().equals("StandardSatellite")) {
            return 1;
        }
        
        if (super.getType().equals("ShrinkingSatellite")) {
            return 10;
        }

        return 0;
    }

     /**
     * @return the maximum bandwidth of upload of the satellite
     */
    public int getMaxReceiveBandwidth() {
        if (super.getType().equals("StandardSatellite")) {
            return 1;
        }
        
        if (super.getType().equals("ShrinkingSatellite")) {
            return 15;
        }

        return 0;
    }
    public boolean hasBandwithToSend() {
        if (super.getType().equals("StandardSatellite") && super.getOutbox().size() == 0) {
            return true;
        }

        if (super.getType().equals("ShrinkingSatellite") && (super.getOutbox().size() + 1 <= 10)) {
            return true;
        }

        return false;
    }

    public boolean hasBandwithToReceive() {
        int numberOfFilesReceiving = 0;
        for (File file : super.getFiles()) {
            if (!file.hasTransferCompleted()) {
                numberOfFilesReceiving++;
            }
        }
        if (super.getType().equals("StandardSatellite") && numberOfFilesReceiving == 0) {
            return true;
        }


        if (super.getType().equals("ShrinkingSatellite") && (numberOfFilesReceiving + 1 <= 15)) {
            return true;
        }

        return false;
    }
    

    /**
     * @return a string (error msg = false corresponding to msg, false = any other false) 
     * that identifies whether the 
     * @param newFile can be received by the current satellite
     */
    public String canStoreFile(File newFile) {
        int storedBytes = 0;

        for (File f : super.getFiles()) {
            storedBytes += f.getSize();
        }

        if (super.getType().equals("StandardSatellite")) {
            if (super.getFiles().size() >= 3) {
                return "Max Files Reached";
            } else if (storedBytes + newFile.getSize() > 80) {
                return "Max Storage Reached";
            } else {
                return "true";
            }
        }

        if (super.getType().equals("ShrinkingSatellite")) {            
            if (storedBytes + newFile.getSize() > 150) {
                return "Max Storage Reached";
            } else {
                return "true";
            }
        }

        return "false";
    }

    /**
     * updates the relays direction to keep in within bounds
     * OR set 1 direction depending if pos crosses the threshold (345-360, 0-140)
     * else set -1 direction
     */
    public void updateRelayDirection() {
        if ( // 190 < position < 345
            super.getPosition().compareTo(Angle.fromDegrees(190)) == 1
            && super.getPosition().compareTo(Angle.fromDegrees(345)) == -1
        ) {                 
            direction = -1;
        }

        if ( // 0 < position < 170 OR position == 345
            super.getPosition().compareTo(Angle.fromDegrees(0)) == 1
            && super.getPosition().compareTo(Angle.fromDegrees(140)) == -1
            || super.getPosition().compareTo(Angle.fromDegrees(345)) == 0
        ) {                 
            direction = 1;
        }
    }

    /**
     * @return the satellite's angular velocity
     * if satellite has invalid type, then null
     */
    public Angle getAngularVelocity() {
        if (super.getType().equals("StandardSatellite")) {
            return Angle.fromRadians(2500 / height);
        }
        if (super.getType().equals("ShrinkingSatellite")) {
            return Angle.fromRadians(1000 / height);
        }
        if (super.getType().equals("RelaySatellite")) {
            return Angle.fromRadians(1500 / height);
        }
        return null;
    }

    /**
     * 1 = positive, -1 = negative
     */
    public int getDirection() {
        return direction;
    }


    public double getHeight() {
        return height;
    }



}
