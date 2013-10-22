/******************************************************************************
* Title: Handler.java
* Author: Mike Schoonover
* Date: 8/5/12
*
* Purpose:
*
* This class is part of a package designed to handle data from devices which
* don't output proper HTTP headers.
*
* See comments at the top of new.www.protocol.raw.RawURLConnection.java for
* more details.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package net.www.protocol.raw;

import java.io.*;
import java.net.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Handler
//

public class Handler extends URLStreamHandler {

    String rawType;

//-----------------------------------------------------------------------------
// Handler::parseURL
//

@Override
protected void parseURL(URL u, String spec, int start, int end) {

    //the URL looks like:
    //"raw:XMLNoHeader//169.254.1.2/state.xml"
    //the part after the : is a special added selector for our purposes as it
    //allows different types of "raw" streams -- it must be removed before the
    //parent class can use the URL as it expects
    //below that part is extracted and stored in rawType
    //the rest of the URL is passed on to the parent

    //start already points to first character, find the following slash
    int slash = spec.indexOf('/');

    //extract and store the type
    rawType = spec.substring(start, slash);

    //start of the remainder of the URL
    start=slash;

/*
    try{
        u = new URL("http://169.254.1.2/state.xml");
    }
    catch(MalformedURLException me){
        int x = 1;
    }
*/

    //pass the "normal" parts of the URL on to the super class
    super.parseURL(u, spec, start, end);

}//end of Handler::parseURL
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Handler::openConnection
//

@Override
protected URLConnection openConnection(URL url) throws IOException
{

    return new RawURLConnection( url, rawType );

}//end of Handler::openConnection
//-----------------------------------------------------------------------------

}//end of class Handler
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
