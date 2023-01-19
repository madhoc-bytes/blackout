package unsw.blackout;

import unsw.utils.Angle;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

public class Device extends Apparatus {

    public Device(String deviceId, String type, Angle position) {
        super(deviceId, type, RADIUS_OF_JUPITER, position);
    }  
    

}
