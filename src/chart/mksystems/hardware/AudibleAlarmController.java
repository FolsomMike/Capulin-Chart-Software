/******************************************************************************
* Title: AudibleAlarmController.java
* Author: Mike Schoonover
* Date: 10/22/13
*
* Purpose:
*
* This file contains the interface definition for AudibleAlarmController.  This
* interface provides functions required by all devices which handle an audible
* alarm.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// interface AudibleAlarmController
//

public interface AudibleAlarmController {

    public boolean isAudibleAlarmController();
    public void pulseAudibleAlarm();
    public void turnOnAudibleAlarm();
    public void turnOffAudibleAlarm();

}//end of interface AudibleAlarmController
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
