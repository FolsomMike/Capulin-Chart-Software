/******************************************************************************
* Title: RawInputStream.java
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
* These associated classes were derived from examples at:
*   http://docstore.mik.ua/orelly/java/exp/ch09_06.htm
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
// class RawInputStream
//

abstract class RawInputStream extends InputStream {

    InputStream in;
    OutputStream talkBack;
    abstract public void set( InputStream in, OutputStream talkBack );
    public RawURLConnection owner;

//-----------------------------------------------------------------------------
// RawInputStream::close
//

@Override
public void close()
{




}//end of RawInputStream::close
//-----------------------------------------------------------------------------

}//end of class RawInputStream
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
