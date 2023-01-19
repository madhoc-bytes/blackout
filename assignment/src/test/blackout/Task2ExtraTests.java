package blackout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import unsw.blackout.BlackoutController;
import unsw.blackout.FileTransferException;
import unsw.response.models.FileInfoResponse;
import unsw.response.models.EntityInfoResponse;
import unsw.utils.Angle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

import java.util.Arrays;

import static blackout.TestHelpers.assertListAreEqualIgnoringOrder;

@TestInstance(value = Lifecycle.PER_CLASS)
public class Task2ExtraTests {
    @Test
    public void testRelayMovement() {
        // Task 2
        // Example from the specification
        BlackoutController controller = new BlackoutController();

        // Creates 1 satellite and 2 devices
        // Gets a device to send a file to a satellites and gets another device to download it.
        // StandardSatellites are slow and transfer 1 byte per minute.
        controller.createSatellite("Satellite1", "RelaySatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(180));

        // moves in positive direction
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(180), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        controller.simulate();
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(181.23), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        controller.simulate();
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(182.46), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        controller.simulate();
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(183.69), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        
        // edge case
        controller.simulate(5);
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(189.82), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        controller.simulate(1);
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(191.05), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        
        // goes back down
        controller.simulate(1);
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(189.82), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        controller.simulate(5);
        assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(183.69), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
    }

    @Test
    public void testQuantumBehaviour() {
        // just some of them... you'll have to test the rest
        BlackoutController controller = new BlackoutController();

        // Creates 1 satellite and 2 devices
        controller.createSatellite("Satellite1", "ShrinkingSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
        controller.createDevice("DeviceA", "HandheldDevice", Angle.fromDegrees(320));
        controller.createDevice("DeviceB", "LaptopDevice", Angle.fromDegrees(315));

        // uploads at a rate of 15 per minute so we'll give it 21 bytes which when compressed is 14
        String msg = "hello quantum how are";
        controller.addFileToDevice("DeviceA", "FileAlpha", msg);
        assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "DeviceA", "Satellite1"));
        assertEquals(new FileInfoResponse("FileAlpha", "", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));

        // we still should have 6 bytes to send
        controller.simulate(1);
        assertEquals(new FileInfoResponse("FileAlpha", "hello quantum h", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));

        // now that we are done we should see shrinkage
        controller.simulate(1);
        assertEquals(new FileInfoResponse("FileAlpha", msg, 14, true), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));

        // sending file back down to other device it needs to send full 21 bytes, bandwidth out is 10 so it should take 3 ticks
        assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "Satellite1", "DeviceB"));
        assertEquals(new FileInfoResponse("FileAlpha", "", msg.length(), false), controller.getInfo("DeviceB").getFiles().get("FileAlpha"));

        // we still should have 11 bytes to send
        controller.simulate(1);
        assertEquals(new FileInfoResponse("FileAlpha", "hello quan", msg.length(), false), controller.getInfo("DeviceB").getFiles().get("FileAlpha"));

        // and still 1 more byte to send
        controller.simulate(1);
        assertEquals(new FileInfoResponse("FileAlpha", "hello quantum how ar", msg.length(), false), controller.getInfo("DeviceB").getFiles().get("FileAlpha"));

        // done! and file size should not be shrunk, we aren't on the shrinking satellite
        controller.simulate(1);
        assertEquals(new FileInfoResponse("FileAlpha", "hello quantum how are", msg.length(), true), controller.getInfo("DeviceB").getFiles().get("FileAlpha"));
    }


        // further tests
        @Test
        public void testFilesInProgress() {
            BlackoutController controller = new BlackoutController();
            controller.createSatellite("Satellite1", "StandardSatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
            controller.createDevice("DeviceC", "HandheldDevice", Angle.fromDegrees(320));
    
            String msg = "123456789";
            controller.addFileToDevice("DeviceC", "FileAlpha", msg);
            assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "DeviceC", "Satellite1"));
            assertEquals(new FileInfoResponse("FileAlpha", "", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate();
            assertEquals(new FileInfoResponse("FileAlpha", "1", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate();
            assertEquals(new FileInfoResponse("FileAlpha", "12", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate();
            assertEquals(new FileInfoResponse("FileAlpha", "123", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate();
            assertEquals(new FileInfoResponse("FileAlpha", "1234", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate(5);
            assertEquals(new FileInfoResponse("FileAlpha", msg, msg.length(), true), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
        }
    
        @Test
        public void testExceptionsNoBandwidth() {
            BlackoutController controller = new BlackoutController();
    
            controller.createDevice("DeviceA", "LaptopDevice", Angle.fromDegrees(320));
            controller.createSatellite("Satellite1", "StandardSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
            String msg1 = "https://www.youtube.com/c/madhoc1337";
            String msg2 = "yo";
            String msg3 = "hey";
            controller.addFileToDevice("DeviceA", "FileAlpha", msg2);
            controller.addFileToDevice("DeviceA", "FileBeta", msg1);
    
    
            // does not have enough bandwidth to receive
            assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "DeviceA", "Satellite1"));
            assertThrows(FileTransferException.VirtualFileNoBandwidthException.class, () -> controller.sendFile("FileBeta", "DeviceA", "Satellite1"));
            controller.simulate(msg2.length());
            assertEquals(new FileInfoResponse("FileAlpha", msg2, msg2.length(), true), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
    
            // not enough bandwidth to send
            controller.createDevice("DeviceB", "LaptopDevice", Angle.fromDegrees(330));
            controller.addFileToDevice("DeviceB", "FileCharlie", msg3);
            assertDoesNotThrow(() -> controller.sendFile("FileCharlie", "DeviceB", "Satellite1"));
            controller.simulate(msg3.length());
            assertEquals(new FileInfoResponse("FileCharlie", msg3, msg3.length(), true), controller.getInfo("Satellite1").getFiles().get("FileCharlie"));
            controller.createDevice("DeviceC", "LaptopDevice", Angle.fromDegrees(335));
    
            assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "Satellite1", "DeviceC"));
            assertThrows(FileTransferException.VirtualFileNoBandwidthException.class, () -> controller.sendFile("FileCharlie", "Satellite1", "DeviceC"));
        }
    
        @Test
        public void testExceptionStorageLimit() {
            BlackoutController controller = new BlackoutController();
    
            controller.createDevice("DeviceA", "LaptopDevice", Angle.fromDegrees(320));
            controller.createDevice("DeviceB", "LaptopDevice", Angle.fromDegrees(330));
            controller.createSatellite("Satellite1", "StandardSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
            String msg2 = "yooo";
            char[] data = new char[80];
            String msg3 = new String(data);
    
            controller.addFileToDevice("DeviceA", "FileAlpha", msg2);
            controller.addFileToDevice("DeviceB", "FileBeta", msg3);
            
            // too many bytes
            assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "DeviceA", "Satellite1"));
            controller.simulate(msg2.length());
            assertThrows(FileTransferException.VirtualFileNoStorageSpaceException.class, () -> controller.sendFile("FileBeta", "DeviceB", "Satellite1"));
        }
    
        @Test
        public void testExceptionFileLimit() {
            BlackoutController controller = new BlackoutController();
    
            controller.createDevice("DeviceA", "LaptopDevice", Angle.fromDegrees(320));
            controller.createDevice("DeviceB", "LaptopDevice", Angle.fromDegrees(330));
            controller.createSatellite("Satellite1", "StandardSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
            String msg1 = "a";
            String msg2 = "b";
            String msg3 = "c";
    
            controller.addFileToDevice("DeviceA", "File1", msg1);
            controller.addFileToDevice("DeviceA", "File2", msg2);
            controller.addFileToDevice("DeviceA", "File3", msg3);
            controller.addFileToDevice("DeviceA", "File4", "4");
            
            // too many file
            assertDoesNotThrow(() -> controller.sendFile("File1", "DeviceA", "Satellite1"));
            controller.simulate();
            assertDoesNotThrow(() -> controller.sendFile("File2", "DeviceA", "Satellite1"));
            controller.simulate();
            assertDoesNotThrow(() -> controller.sendFile("File3", "DeviceA", "Satellite1"));
            controller.simulate();
            assertThrows(FileTransferException.VirtualFileNoStorageSpaceException.class, () -> controller.sendFile("File4", "DeviceA", "Satellite1"));
        }
    
        @Test
        public void testMovementBeyondRevolution() {
            // Task 2
            // Example from the specification
            BlackoutController controller = new BlackoutController();
    
            // Creates 1 satellite and 2 devices
            // Gets a device to send a file to a satellites and gets another device to download it.
            // StandardSatellites are slow and transfer 1 byte per minute.
            controller.createSatellite("Satellite1", "StandardSatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(359));
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(359), 100 + RADIUS_OF_JUPITER, "StandardSatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(1.05), 100 + RADIUS_OF_JUPITER, "StandardSatellite"), controller.getInfo("Satellite1"));
        }
    
        @Test 
        public void testRelayMovementStartingOutsideRange() {
            BlackoutController controller = new BlackoutController();
    
            // greater than threshold (ie 345-360 || 0-140): positive movement towards lower boundary
            controller.createSatellite("Satellite1", "RelaySatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(130));
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(130), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(131.23), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(132.46), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(133.69), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));        
            controller.simulate(5);
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(139.82), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(141.05), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite1", Angle.fromDegrees(142.28), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite1"));
        
            // less than threshold (ie 190 - 345): negative movement towards upper boundary
            controller.createSatellite("Satellite2", "RelaySatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(200));
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(200), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(198.77), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(197.54), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));
            controller.simulate();
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(196.31), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));
            controller.simulate(5);
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(190.18), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));
            controller.simulate(5);
            assertEquals(new EntityInfoResponse("Satellite2", Angle.fromDegrees(184.04), 100 + RADIUS_OF_JUPITER, "RelaySatellite"), controller.getInfo("Satellite2"));        
        }
    
        @Test
        public void testFileMovesOutofRangeDuringTransfer() {
            BlackoutController controller = new BlackoutController();
    
            controller.createSatellite("Satellite1", "StandardSatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(320));
            controller.createDevice("DeviceB", "LaptopDevice", Angle.fromDegrees(310));
    
            String msg = "Hello World, this is a longer sentence." + 100 * 'a';
            controller.addFileToDevice("DeviceB", "FileAlpha", msg);
            assertDoesNotThrow(() -> controller.sendFile("FileAlpha", "DeviceB", "Satellite1"));
            assertEquals(new FileInfoResponse("FileAlpha", "", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            controller.simulate(3);
            assertEquals(new FileInfoResponse("FileAlpha", "Hel", msg.length(), false), controller.getInfo("Satellite1").getFiles().get("FileAlpha"));
            // move it out of range before file is finished tranferring
            controller.simulate(50);
            assertEquals(0, controller.getInfo("Satellite1").getFiles().size());
        }
}
