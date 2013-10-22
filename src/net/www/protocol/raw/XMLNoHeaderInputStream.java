/******************************************************************************
* Title: XMLNoHeaderInputStream.java
* Author: Mike Schoonover
* Date: 8/5/12
*
* Purpose:
*
* This class is part of a package designed to handle data from devices which
* don't output proper HTTP headers.
*
* Note that this class actually handles any type of text data, not just XML.
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

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class XMLNoHeaderInputStream
//

class XMLNoHeaderInputStream extends RawInputStream {

//-----------------------------------------------------------------------------
// XMLNoHeaderInputStream::set
//

@Override
public void set( InputStream in, OutputStream talkBack ) {

    //the following was in the original example
    //this.in = new example.io.rot13InputStream( in );

    //we don't need a filtering, so just use the input stream as is
    this.in = in;

}//end of XMLNoHeaderInputStream::set
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// XMLNoHeaderInputStream::read
//

@Override
public int read() throws IOException {

    if ( in == null ) {
        throw new IOException("No Stream");
    }

    return in.read();

}//end of XMLNoHeaderInputStream::read
//-----------------------------------------------------------------------------

}//end of class XMLNoHeaderInputStream
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
