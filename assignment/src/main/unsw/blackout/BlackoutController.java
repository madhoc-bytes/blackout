package unsw.blackout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

import unsw.blackout.FileTransferException.VirtualFileAlreadyExistsException;
import unsw.blackout.FileTransferException.VirtualFileNoBandwidthException;
import unsw.blackout.FileTransferException.VirtualFileNoStorageSpaceException;
import unsw.blackout.FileTransferException.VirtualFileNotFoundException;

public class BlackoutController {
    private List<Apparatus> apparatuses = new ArrayList<Apparatus>();

    public void createDevice(String deviceId, String type, Angle position) {
        // device already exists
        if (getObjFromId(deviceId) != null) {
            return;
        }
        Device device = new Device(deviceId, type, position);
        apparatuses.add(device);
    }

    public void removeDevice(String deviceId) {
        apparatuses.remove(getObjFromId(deviceId));
    }

    public void createSatellite(String satelliteId, String type, double height, Angle position) {
        // satellite already exists
        if (getObjFromId(satelliteId) != null) {
            return;
        }
        
        Satellite satellite = new Satellite(satelliteId, type, height, position);
        apparatuses.add(satellite);
    
    }

    public void removeSatellite(String satelliteId) {
        apparatuses.remove(getObjFromId(satelliteId));
    }

    public List<String> listDeviceIds() {
        List<String> deviceIds = new ArrayList<String>();

        for (Apparatus apparatus : apparatuses) {
            if (isDevice(apparatus)) {
                deviceIds.add(apparatus.getId());
            }
        }     

        return deviceIds;
    }

    public List<String> listSatelliteIds() {
        List<String> satelliteIds = new ArrayList<String>();

        for (Apparatus apparatus : apparatuses) {
            if (isSatellite(apparatus)) {
                satelliteIds.add(apparatus.getId());
            }
        }     
           
        return satelliteIds;
    }

    public void addFileToDevice(String deviceId, String filename, String content) {
        Apparatus device = getObjFromId(deviceId);
        // file already exists
        if (device.getFileFromName(filename) != null) {
            return;
        }        
        device.addFile(filename, new String(content), content.length());        
    }

    public EntityInfoResponse getInfo(String id) {        
        Map<String, FileInfoResponse> fileInfo = new HashMap<>();
        Apparatus apparatus = getObjFromId(id);

        Angle position = apparatus.getPosition();
        double height = apparatus.getHeight();
        String type = apparatus.getType();

        
        for (File file : apparatus.getFiles()) {
            boolean transferStatus = file.hasTransferCompleted();
            if (apparatus.getType().equals("ShrinkingSatellite")) {
                QuantumFile qf = new QuantumFile(
                    file.getFilename(),
                    file.getData(), 
                    file.getSize()
                );
                transferStatus = qf.hasTransferCompleted();
            }
            fileInfo.put(
                file.getFilename(), 
                new FileInfoResponse(
                    file.getFilename(), 
                    file.getData(), 
                    file.getSize(), 
                    transferStatus
                )
            );
        }   

        EntityInfoResponse apparatusInfo = new EntityInfoResponse(id, position, height, type, fileInfo);
        return apparatusInfo;
    }

    public void simulate() {
        for (Apparatus apparatus : apparatuses) {
            if (isSatellite(apparatus)) {
                moveSatellite(apparatus);               
            }
        } 

        for (Apparatus apparatus : apparatuses) {
            if (isSatellite(apparatus)) {             
                sendFilesFromSatellite(apparatus);  
            } else {             
                sendFilesFromDevice(apparatus);  
            }
        }     

        transferCleanup();
    }

    /**
     * Simulate for the specified number of minutes.
     * You shouldn't need to modify this function.
     */
    public void simulate(int numberOfMinutes) {
        for (int i = 0; i < numberOfMinutes; i++) {
            simulate();
        }
    }

    public List<String> communicableEntitiesInRange(String id) { 
        List<String> communicableEntities = new ArrayList<String>();
        Apparatus targetApparatus = getObjFromId(id);
        
        if (targetApparatus == null) {
            return communicableEntities;
        }
        
        double distance = -1;
        boolean visible = false;

        // targetApparatus is Device, apparatus is Satellite
        if (isDevice(targetApparatus)) {
            // communication: device to satellite
            for (Apparatus apparatus : apparatuses) {
                if (isSatellite(apparatus)) {
                    distance = MathsHelper.getDistance(
                        apparatus.getHeight(), 
                        apparatus.getPosition(), 
                        targetApparatus.getPosition()
                    );
                    visible = MathsHelper.isVisible(
                        apparatus.getHeight(), 
                        apparatus.getPosition(), 
                        targetApparatus.getPosition()
                    );
                    if (
                        !apparatus.equals(targetApparatus)
                        && visible                        
                        && targetApparatus.inMaxRange(distance)
                        && isCompatible(targetApparatus, apparatus)
                    ) {
                        communicableEntities.add(apparatus.getId());
                    }                    
                }    
            }
        } else { // target is satellite, apparatus is either
            for (Apparatus apparatus : apparatuses) {                    
                distance = MathsHelper.getDistance(
                    targetApparatus.getHeight(), 
                    targetApparatus.getPosition(), 
                    apparatus.getHeight(), 
                    apparatus.getPosition()
                );

                // no idea why height of device in isVisible is radius_of_jupiter + 50
                // instead of just radius of jupiter. 
                if (isDevice(apparatus)) {
                    visible = MathsHelper.isVisible(
                        targetApparatus.getHeight(), 
                        targetApparatus.getPosition(), 
                        apparatus.getPosition()
                    );
                } else {
                    visible = MathsHelper.isVisible(
                        targetApparatus.getHeight(), 
                        targetApparatus.getPosition(), 
                        apparatus.getHeight(), 
                        apparatus.getPosition()
                    ); 
                }

                if (
                    !apparatus.equals(targetApparatus)
                    && visible                    
                    && targetApparatus.inMaxRange(distance)
                    && isCompatible(targetApparatus, apparatus)
                ) {
                    communicableEntities.add(apparatus.getId());
                }
            }    
        }

        return communicableEntities;
    }

    public void sendFile(String fileName, String fromId, String toId) throws FileTransferException {
        Apparatus fromApparatus = getObjFromId(fromId);
        Apparatus toApparatus = getObjFromId(toId);
        File targetFile = fromApparatus.getFileFromName(fileName);

        if (targetFile == null) {
            throw new VirtualFileNotFoundException(fileName);
        } 
        
        if (toApparatus.isFileInInbox(fileName)) {
            throw new VirtualFileAlreadyExistsException(fileName);
        }

        if (isSatellite(fromApparatus)) {
            Satellite fromSatellite = (Satellite) fromApparatus;
            if (!fromSatellite.hasBandwithToSend()) {
                throw new VirtualFileNoBandwidthException(fromApparatus.getId());
            }
        }
        
        if (isSatellite(toApparatus)) {
            Satellite toSatellite = (Satellite) toApparatus;
            if (toApparatus.getType().equals("RelaySatellite")) {
                return;
            }
            if (!toSatellite.hasBandwithToReceive()) {
                throw new VirtualFileNoBandwidthException(toApparatus.getId());
            }
            if (!toSatellite.canStoreFile(targetFile).equals("true")) {
                throw new VirtualFileNoStorageSpaceException(toSatellite.canStoreFile(targetFile));
            }
        }

        fromApparatus.addFileToOutbox(targetFile);
        toApparatus.addFile(targetFile.getFilename(), "", targetFile.getData().length());
    }
    
    // custom helper functions
    
    /**
     * given an 
     * @param id
     * @return apparatus object corresponding to the id.
     * if DNE, then return null
     */
    public Apparatus getObjFromId(String id) {
        for (Apparatus apparatus : apparatuses) {
            if (apparatus.getId().equals(id)) {
                return apparatus;
            }
        }
        return null;
    }

    public boolean isSatellite(Apparatus apparatus) {
        return apparatus.getClass().getSimpleName().equals("Satellite");
    }

    public boolean isDevice(Apparatus apparatus) {
        return apparatus.getClass().getSimpleName().equals("Device");
    }


    /**
     * given
     * @param sender
     * transfer files from satellite by allocating satellite's
     * upload bandwith equally amongst the files in its outbox.
     * updates the incomplete files being received in corresponding apparatuses.
     */
    public void sendFilesFromSatellite(Apparatus sender) {
        Satellite senderS = (Satellite) sender;
        List<String> entitiesInRange = communicableEntitiesInRange(sender.getId());
        List<File> newOutbox = new ArrayList<File>();
        List<File> removeOldOutbox = new ArrayList<File>();
        
        for (File sendingFile : sender.getOutbox()) {
            File targetFile = findIncompleteFile(sendingFile.getFilename());            
            Apparatus receiver = getReceiver(targetFile.getFilename());  

            if (entitiesInRange.contains(receiver.getId())) {
                int maxBandwith = senderS.getMaxSendBandwidth();
                if (isSatellite(receiver)) {
                    Satellite receiverS = (Satellite) receiver;
                    maxBandwith 
                        = Math.min(senderS.getMaxSendBandwidth(), receiverS.getMaxReceiveBandwidth());
                }

                int bytesPerFile 
                    = maxBandwith / senderS.getOutbox().size();
                int maxBytesPerFile 
                    = Math.min(bytesPerFile, sendingFile.getSize());
                
                String payload;
                

                if (sendingFile.getData().length() >= maxBytesPerFile) {
                    // payload: the bytes being transferred
                    payload = sendingFile.getData().substring(0, maxBytesPerFile);                
                    // delete the bytes already sent from the sendingFile in the outbox
                    newOutbox.add(new File(
                        sendingFile.getFilename(), 
                        sendingFile.getData().substring(maxBytesPerFile),
                        sendingFile.getSize()
                    ));   
                    removeOldOutbox.add(sendingFile);
                } else {
                    payload = sendingFile.getData();
                    newOutbox.add(new File(
                        sendingFile.getFilename(), 
                        "",
                        sendingFile.getSize()
                    ));  
                    removeOldOutbox.add(sendingFile);
                }             

                // append received bytes into the target file                
                targetFile.setData(targetFile.getData() + payload); 
            } else {
                removeOldOutbox.add(sendingFile);
                receiver.removeFile(sendingFile.getFilename());
            }           
        }
        sender.getOutbox().removeAll(removeOldOutbox);
        sender.getOutbox().addAll(newOutbox);
    }

    /**
     * given
     * @param apparatus
     * transfer files from device by allocating satellite's
     * upload bandwith equally amongst the files in its outbox.
     * updates the incomplete files being received in corresponding apparatuses.
     */
    public void sendFilesFromDevice(Apparatus sender) {
        List<String> entitiesInRange = communicableEntitiesInRange(sender.getId());
        List<File> newOutbox = new ArrayList<File>();
        List<File> removeOldOutbox = new ArrayList<File>();

        for (File sendingFile : sender.getOutbox()) { 
            File targetFile = findIncompleteFile(sendingFile.getFilename());
            Apparatus receiver = getReceiver(targetFile.getFilename());

            if (entitiesInRange.contains(receiver.getId())) {  
                Satellite receiverS = (Satellite) receiver;
                int maxBandwith 
                    = receiverS.getMaxReceiveBandwidth();                                
                int bytesPerFile 
                    = maxBandwith / sender.getOutbox().size();
                int maxBytesPerFile 
                    = Math.min(sendingFile.getSize(), bytesPerFile);            
                String payload;

                if (sendingFile.getData().length() >= maxBytesPerFile) {
                    // payload: the bytes being transferred
                    payload = sendingFile.getData().substring(0, maxBytesPerFile);                
                    // delete the bytes already sent from the sendingFile in the outbox
                    newOutbox.add(new File(
                        sendingFile.getFilename(), 
                        sendingFile.getData().substring(maxBytesPerFile),
                        sendingFile.getSize()
                    ));   
                    removeOldOutbox.add(sendingFile);
                } else {
                    payload = sendingFile.getData();
                    newOutbox.add(new File(
                        sendingFile.getFilename(), 
                        "",
                        sendingFile.getSize()
                    ));  
                    removeOldOutbox.add(sendingFile);
                }             
                // append received bytes into the target file

                targetFile.setData(targetFile.getData() + payload);  
            } else {
                removeOldOutbox.add(sendingFile);   
                receiver.removeFile(targetFile.getFilename());
            }
        }
        
        sender.getOutbox().removeAll(removeOldOutbox);
        sender.getOutbox().addAll(newOutbox);
    }


    /**
     * update the quantum files' file sizes
     * and remove all empty files from all outboxes
     */

    public void transferCleanup() {        
        for (Apparatus apparatus : apparatuses) {
            if (apparatus.getType().equals("ShrinkingSatellite")) {
                List<File> updatedFiles = new ArrayList<File>();
                List<File> removingFiles = new ArrayList<File>();
                for (File f : apparatus.getFiles()) {
                    if (f.getData().contains("quantum") && f.hasTransferCompleted()) {
                        updatedFiles.add(new QuantumFile(
                            f.getFilename(), f.getData(), f.getSize() * 2 / 3
                        ));
                        removingFiles.add(f);
                    }
                }

                apparatus.getFiles().removeAll(removingFiles);
                apparatus.getFiles().addAll(updatedFiles);

            }

            apparatus.cleanUpOutbox();
        }

    }

    /**
     * Given
     * @param fileName
     * @param sender
     * @return the receiver of the in-progress file.
     * if DNE, then return null.
     */
    public Apparatus getReceiver(String fileName) {
        for (Apparatus apparatus : apparatuses) {
            for (File f : apparatus.getFiles()) {
                if (f.getFilename().equals(fileName)) {
                    if (f.isQuantum()) {
                        QuantumFile qf = (QuantumFile) f;
                        if (!qf.hasTransferCompleted()) {
                            return apparatus;
                        }
                    }
                    if (!f.hasTransferCompleted()) {
                        return apparatus;
                    }
                }
                
            }
        }
        return null;
    }


    /**
     * Find
     * @param fileName
     * @return the corresponding file object 
     * in the inbox of any apparatus. If DNE, return null
     */

    public File findIncompleteFile(String fileName) {
        for (Apparatus apparatus : apparatuses) {
            for (File file : apparatus.getFiles()) {
                if (file.getFilename().equals(fileName)) {
                    if (file.isQuantum()) {
                        QuantumFile qf = (QuantumFile) file;
                        if (!qf.hasTransferCompleted()) {
                            return file;
                        }
                    }
                    if (!file.hasTransferCompleted()) {
                        return file;
                    }                    
                }
            }
        }
        return null;
    }


    /**
     * given
     * @param apparatus
     * change the satellite's position by add its angular velocity
     * to its current position
     */
    public void moveSatellite(Apparatus apparatus) {
        Satellite satellite = (Satellite) apparatus;
        Angle newPos;
        if (apparatus.getType().equals("RelaySatellite")) {
            satellite.updateRelayDirection();
        }

        if (satellite.getDirection() == 1) {
            newPos = apparatus.getPosition().add(
                satellite.getAngularVelocity()
            );    
        } else {
            newPos = apparatus.getPosition().subtract(
                satellite.getAngularVelocity()
            );
        } 
        if (newPos.compareTo(Angle.fromDegrees(360)) == 1) {
            newPos = newPos.subtract(Angle.fromDegrees(360));
        }

        apparatus.setPosition(newPos);  
    }

    /**
     * given 
     * @param apparatus1
     * @param apparatus2
     * @return a boolean on whether they're compatible
     */
    public boolean isCompatible(Apparatus apparatus1, Apparatus apparatus2) {
        if (
            (apparatus1.getType().equals("StandardSatellite")
            && apparatus2.getType().equals("DesktopDevice"))
            || (apparatus2.getType().equals("StandardSatellite")
            && apparatus1.getType().equals("DesktopDevice"))
        ) {
            return false;
        } 

        return true;
    }

    
}
