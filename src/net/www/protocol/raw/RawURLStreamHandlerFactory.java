/******************************************************************************
* Title: RawURLStreamHandlerFactory.java
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

//-----------------------------------------------------------------------------

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

//-----------------------------------------------------------------------------
// class RawURLStreamHandlerFactory
//
// This class is used during the creation of network streams which can handle
// the improperly formatted XML/HTML received from some network components.
// The basic classes provided with Java refuse to process the data if it does
// not have all the proper headers and other formatting.
//
// See the net.www.protocol.raw package for more information.
//

public class RawURLStreamHandlerFactory implements URLStreamHandlerFactory {

//-----------------------------------------------------------------------------
// RawURLStreamHandlerFactory::createURLStreamHandler
//

@Override
public URLStreamHandler createURLStreamHandler(String protocol) {

    if ( protocol.equalsIgnoreCase("raw") ) {
        return new net.www.protocol.raw.Handler();
    }
    else {
        return null;
    }

}// end of RawURLStreamHandlerFactory::createURLStreamHandler
//-----------------------------------------------------------------------------

}//end of class RawURLStreamHandlerFactory
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
