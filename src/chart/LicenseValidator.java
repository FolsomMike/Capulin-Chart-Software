/******************************************************************************
* Title: LicenseValidator.java
* Author: Mike Schoonover
* Date: 1/27/09
*
* Purpose:
*
* This class verifies that the software license has not expired.  If the
* expiration date stored in the license file has expired or the license file
* is missing, an info code will be displayed along with a cipher code, and the
* user will be requested to enter a response code.
*
* The response code should be derived from the cipher code, the formula for
* which should only be known to the software manufacturer.  This prevents the
* user from extending the license unilaterally.  The info message does not
* refer to the fact that the license has expired, but rather is a generic
* message.
*
* The license extension period is encrypted in with the response code.  The
* license file will be written with the new expiration date.  If no code is
* entered, the license file will be written with a bogus value which will
* fail the validation from thereon.  This prevents the user from setting the
* system clock back in time so that the old license is valid once again.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import javax.swing.*;
import java.io.*;
import java.util.Date;
import javax.swing.JOptionPane;
import java.awt.event.WindowEvent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class LicenseValidator
//

public class LicenseValidator{

String licenseFilename;
JFrame mainFrame;

//-----------------------------------------------------------------------------
// LicenseValidator::LicenseValidator (constructor)
//

public LicenseValidator(JFrame pMainFrame, String pLicenseFilename)
{

mainFrame = pMainFrame; licenseFilename = pLicenseFilename;

}//end of LicenseValidator::LicenseValidator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::validateLicense
//
// Checks to see if the license has expired and displays an info message
// if it has.  The user is allowed to enter a code to renew the license
// for another period of time.  The input window displays a random cipher
// code which the user must relay to the technician.  The technician uses
// the cipher code to create a renewal code.  The user should NOT close the
// window after relaying the cipher - the renewal code generated from that
// cipher must be entered into the window before it is closed - if it is closed,
// opening the window again will result in a new cipher which must be relayed
// to the technician for a matching renewal code.
//

public void validateLicense()
{

    getMACAddress(); //debug mks -- just displays for testing
    
    File license = new File(licenseFilename);

    //if the license file does not exist then, request renewal code
    // the program will be aborted if proper renewal code is not supplied
    if (!license.exists()) requestLicenseRenewal(true);

    //if the license file is invalid or the date has expired, request renewal code
    // the program will be aborted if proper renewal code is not supplied
    if (!validateLicenseFile()) requestLicenseRenewal(true);

}//end of LicenseValidator::validateLicense
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::requestLicenseRenewal
//
// Generates and displays a cipher which can be relayed to the technician
// so that a matching license renewal code can be generated and entered
// to renew the license.
//
// If pExitProgramOnFail is true, the program will close if a valid entry is
// not provided.
//
// See validateLicense for more info.
//

public void requestLicenseRenewal(boolean pExitProgramOnFail)
{

//create a random number which can be relayed to the technician - the
//technician must supply a matching answer value

//try random numbers until one above 1000 is generated to ensure 4 digit code
int cipher = 0;
while (cipher < 1000) cipher = (int)(Math.random() * 10000);

//NOTE: createRenewalCode function is called for testing only - the programmer
//       can set a breakpoint to catch the generated code and feed it back into
//       the License Renewal window.  The user cannot view or access the code.

createRenewalCode(cipher, 12);

String renewalCode = (String)JOptionPane.showInputDialog(mainFrame,
   "Call manufacturer for assistance with Code: "
   + cipher +
   "  Enter response code from technician into the box below and click OK.",
                    "Message L9000",  JOptionPane.INFORMATION_MESSAGE);

long renewalDate = -1;

if ((renewalCode != null) && (renewalCode.length() > 0))
    renewalDate = verifyRenewalCode(cipher, renewalCode);

if (renewalDate == -1){

    //if flag set, exit program on fail
    if (pExitProgramOnFail) mainFrame.dispatchEvent(
                       new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));

    return; //exit on failure no matter what - don't save license file
    }

//if user entered a valid code, save the new expiration date
saveLicenseFile(renewalDate, false);

}//end of LicenseValidator::requestLicenseRenewal
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// LicenseValidator::validateLicenseFile
//
// Loads the data from the license file, verifies its integrity, and checks
// to see if the date has expired.
//
// See saveLicenseFile for the expected format.
//
// Returns true if data is valid and license is not expired.
// Returns false otherwise.
//

private boolean validateLicenseFile()
{

FileInputStream fileInputStream = null;
InputStreamReader inputStreamReader = null;
BufferedReader in = null;

try{

    fileInputStream = new FileInputStream(licenseFilename);
    inputStreamReader = new InputStreamReader(fileInputStream);
    in = new BufferedReader(inputStreamReader);

    //read in the expiration date
    String expiryDateS = in.readLine();
    //read in the encoded version of the expiration date
    String encodedExpiryDateS = in.readLine();
    //fail if lines were not read
    if (expiryDateS == null || encodedExpiryDateS == null) return(false);

    long expiryDate, encodedExpiryDate;
    try{
        expiryDate = Long.valueOf(expiryDateS);
        encodedExpiryDate = Long.valueOf(encodedExpiryDateS);
        }
    catch(NumberFormatException e){
        return(false); //fail on error converting either value
        }

    //if the unencoded value does not match the encoded value, fail
    if (expiryDate != encodeDecode(encodedExpiryDate)) return(false);

    Date now = new Date(); //get the current date
    Date expire = new Date(expiryDate); //create the expiry date

    //if the current date is after the expiry date, write a bogus expiry
    //date which will fail on validation every time due to integrity and not
    //expiration date

    if (now.after(expire)){
        saveLicenseFile(expiryDate, true);
        return(false);
        }

    }
catch(IOException e){
    return(false); //invalid file if error reading
    }
finally{

    try{if (in != null) in.close();}
    catch(IOException e){}
    try{if (inputStreamReader != null) inputStreamReader.close();}
    catch(IOException e){}
    try{if (fileInputStream != null) fileInputStream.close();}
    catch(IOException e){}
    }

return(true); //file valid and unexpired

}//end of LicenseValidator::validateLicenseFile
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// LicenseValidator::saveLicenseFile
//
// Saves the renewal date as a text string to the license file, overwriting
// the existing file if there is one.
//
// The renewalDate is the date in milliseconds and is saved as a text string
// followed by the same string encoded using the encodeDecode function.  This
// is done both as verification and to prevent the user from easily creating
// a date string from scratch.
//
// If pBogus is true, then the data written is invalid on purpose and will
// fail the integrity test when checked.  This can be used after the
// expiration date has passed so that the user cannot adjust the system time
// backwards before the date.  After a bogus file is saved, the license file
// validation will fail every time, regardless of the date.
//

private void saveLicenseFile(long renewalDate, boolean pBogus)
{

FileOutputStream fileOutputStream = null;
OutputStreamWriter outputStreamWriter = null;
BufferedWriter out = null;

try{

    fileOutputStream = new FileOutputStream(licenseFilename);
    outputStreamWriter = new OutputStreamWriter(fileOutputStream);
    out = new BufferedWriter(outputStreamWriter);

    //if the number is to purposely invalid, add junk to the encoded value
    int bogusModifier = 0;
    if (pBogus) bogusModifier = 249872349;

    //save the renewal date followed by the same value encoded
    
    out.write("" + renewalDate); out.newLine();
    out.write("" + encodeDecode(renewalDate + bogusModifier)); out.newLine();

    }
catch(IOException e){}
finally{

    try{if (out != null) out.close();}
    catch(IOException e){}
    try{if (outputStreamWriter != null) outputStreamWriter.close();}
    catch(IOException e){}
    try{if (fileOutputStream != null) fileOutputStream.close();}
    catch(IOException e){}
    }

}//end of LicenseValidator::saveLicenseFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::verifyRenewalCode
//
// Verifies that the renewal code has a valid checksum and is the proper
// response to the cipher.  If valid, the number of days for which the
// license is to be renewed is extracted from the code.
//
// If valid, the expiration data is returned as a long int in milliseconds.
// If not valid, -1 is returned.
//

private long verifyRenewalCode(int pCipher, String pRenewalCode)
{

//remove all non-numeric characters from the string - this allows the user
//to enter the number with separator spaces, dashes, etc.

String stripped = "";
for (int i = 0; i < pRenewalCode.length(); i++)
    if (isNumeric(pRenewalCode.charAt(i)))stripped += pRenewalCode.charAt(i);

//convert to an integer ~ set to -1 if invalid to force failure
long renewalCode;
try{renewalCode = Long.valueOf(stripped);}
catch(NumberFormatException e){
    displayErrorMessage("Error L9001: Invalid entry.");
    return (-1);
    }

//verify that the last byte of the integer is the checksum of the first seven,
//i.e. adding all the bytes together should equal 0x100

int checksum = 0;
for (int i=0; i<8; i++)
    checksum += (renewalCode>>(i*8)) & 0xff;

checksum &= 0xff; //mask off upper bits

if(checksum != 0){
    displayErrorMessage("Error L9002: Invalid checksum.");
    return (-1);
    }

//snip off the checksum
renewalCode = renewalCode >> 8;

//decode the renewal code
renewalCode = encodeDecode(renewalCode);

//the number of days to renew is in the lower three decimal digits
//the remaining digits are the code which matches the cipher
//parse the renewal code into the days and match code

//get the renewal length in days
long renewalLength = renewalCode % 1000; //get three lower digits
//get the match code
renewalCode /= 1000; //strip off lower three decimal digits

if (renewalCode != pCipher){
    displayErrorMessage("Error L9003: Invalid code.");
    return (-1);
    }

//get current date/time
long nowTime = System.currentTimeMillis();
//convert the number of days for renewal to milliseconds
long renewalTime = renewalLength * 24 * 3600 * 1000;
//add renewal time to now time to get the expiration date
long expireTime = nowTime + renewalTime;

return(expireTime);

}//end of LicenseValidator::verifyRenewalCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::createRenewalCode
//
// NOTE: This function is for testing purposes only.  It should not be
// accessible to the user in the distributed program as it creates license
// renewal codes.
//
// Pass in the cipher for which the code is to be calculated via pCipher.
// Pass in the number of days for which the license is to be renewed via pDays.
//
// The renewal code will be returned as a string.
//

private String createRenewalCode(int pCipher, int pDays)
{

//concatenate the ciper with the days
long code = (pCipher * 1000) + pDays;

//encode the value
code = encodeDecode(code);

//shift value up to leave space for the checksum, lsb will now be zero
code = code << 8;

//calculate the checksum
//random cipher up to 9999 concatenated with the maximum number of days 999
//which is 9,999,999 decimal and 98967F hex gives 3 bytes, BUT the flipping
//by the encodeDecode function flips byte 2 into byte 3 giving result of 4 bytes
//a long is used to make sure all 4 bytes can be used without worrying about
//the sign

int checksum = 0;
for (int i=0; i<8; i++) checksum += (code>>(i*8)) & 0xff;

//concatenate with the checksum, adjusting with 0x100 so that the lsb of the
//sum total of the bytes will be 0x00
code += (0x100 - (checksum & 0xff));

String ts = "" + code; //convert to string
String codeString = "";

//add a space between every third digit to make it easier to read
int digitCount = 0;
for (int i = (ts.length() - 1); i >= 0; i--){
    if (digitCount++ == 3){codeString = " " + codeString; digitCount = 1;}
    codeString = ts.charAt(i) + codeString;
    }

return codeString;

}//end of LicenseValidator::createRenewalCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::encodeDecode
//
// Encodes and decodes pValue by flipping bytes 0 & 1 and 2 & 3, and so on.
// Works both as an encoder and decoder since the bytes are flipped.
//
// Returns the encoded/decoded value.
//

private long encodeDecode(long pValue)
{

//flip all the bytes - the matching code is scrambled in this fashion to
//obscure the manner in which it is derived from the cipher
long t0, t1, t2, t3, t4, t5, t6, t7, tt;
t0 = pValue & 0xff; //byte 0
t1 = (pValue>>8) & 0xff; //byte 1
t2 = (pValue>>16) & 0xff; //byte 2
t3 = (pValue>>24) & 0xff; //byte 3
t4 = (pValue>>32) & 0xff; //byte 4
t5 = (pValue>>40) & 0xff; //byte 5
t6 = (pValue>>48) & 0xff; //byte 6
t7 = (pValue>>56) & 0xff; //byte 7

tt = t0; t0 = t1; t1 = tt; //flip bytes 0 & 1
tt = t2; t2 = t3; t3 = tt; //flip bytes 2 & 3
tt = t4; t4 = t5; t5 = tt; //flip bytes 4 & 5
tt = t6; t6 = t7; t7 = tt; //flip bytes 6 & 7

//put the value back together with the flipped bytes
pValue = (t7<<56) + (t6<<48)
            + (t5<<40) + (t4<<32)
            + (t3<<24) + (t2<<16)
            + (t1<<8)  + t0;

//return the flipped value
return(pValue);

}//end of LicenseValidator::encodeDecode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::isNumeric
//
// Returns true if pChar is numeric, false if not.
//

boolean isNumeric(char pChar)
{

//check each possible number
if (pChar == '0') return true;
if (pChar == '1') return true;
if (pChar == '2') return true;
if (pChar == '3') return true;
if (pChar == '4') return true;
if (pChar == '5') return true;
if (pChar == '6') return true;
if (pChar == '7') return true;
if (pChar == '8') return true;
if (pChar == '9') return true;

return false; //not numeric

}//end of LicenseValidator::isNumeric
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

JOptionPane.showMessageDialog(mainFrame, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of LicenseValidator::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LicenseValidator::getMACAddress
//
// Retrieves the MAC address for the computer.
//
 
public void getMACAddress() {
    
    try {
        
        //get the IP address of the local host
        //(What if there are two network cards?)
        //(What if network connection switches between the two?)
        //(What if network is not connected?
        
        //(getHardwareAddress call seems to fail if network is not connected.)
        
        InetAddress address = InetAddress.getLocalHost();
        //InetAddress address = InetAddress.getByName("192.168.46.53");

        NetworkInterface ni = NetworkInterface.getByInetAddress(address);
        
        if (ni != null) {
            
            //get the MAC address -- returned in an array
            byte[] mac = ni.getHardwareAddress();
            
            if (mac != null) {

                 // extract each piece of mac address and convert it
                 // to hexa with the following format
                 // 08-00-27-DC-4A-9E.

                for (int i = 0; i < mac.length; i++) {
                    System.out.format("%02X%s",
                            mac[i], (i < mac.length - 1) ? "-" : "");
                }
            } else {
                System.out.println("Address doesn't exist or is not " +
                        "accessible.");
            }
        } else {
            System.out.println("Network Interface for the specified " +
                    "address is not found.");
        }
    } catch (UnknownHostException e) {
        System.out.println(e.getMessage());
    } catch (SocketException e) {
        System.out.println(e.getMessage());
    }

}//end of LicenseValidator::getMACAddress
//-----------------------------------------------------------------------------


}//end of class LicenseValidator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


