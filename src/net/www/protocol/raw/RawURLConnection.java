/******************************************************************************
* Title: RawURLConnection.java
* Author: Mike Schoonover
* Date: 8/5/12
*
* Purpose:
*
* This class is part of a package designed to handle the XML data from devices
* which don't output proper HTTP headers. It also assumes that the content type
* is "text/html" and returns that type regardless of what is or is not specified
* in the content itself.
*
* Note that the XMLNoHeaderInputStream class commonly used in conjuction with
* this class actually handles any type of text data, not just xml.
*
* Before creating a URL object to access a host, execute the following:
*
*    //set this so our custom version of HTTP stream can be used
*    URL.setURLStreamHandlerFactory(new RawURLStreamHandlerFactory());
*
* This allows the URL object to instantiate the specialized data handlers in
* this package when it encounters prefixes in the URL such as "raw:".
*
* NOTE:
*  By removing any requirements for HTTP headers, this class is actually no
*  better than opening a normal socket connection with the host and sending a
*  GET request. It is only useful in that it mimics the Java approach of using
*  the URL class to retrieve HTTP content using a traditional URL address.
*
*  As such, the URL class uses a URL such as:
*
*   raw:XMLNoHeader//169.254.250.222/state.xml?relayState=1
*
*  I strips off the raw:XMLNoHeader prefix for use in selecting a content
*  handler, then strips off the IP address to open a socket connection with
*  the host, then sends to the host the string:
*
*  GET /state.xml?relayState=1 HTTP/1.1
*
*  This is no different than opening a TCP/IP socket to that IP address and
*  sending the URL's suffix as a string.
*
* Examples of hosts which provide malformed HTTP content are devices from
* "Control by Web" (www.ControlByWeb.com) (a division of Xytronix Research &
* Design, Inc.)
*
* These devices send an XML page which does not have HTTP headers.  Later
* versions of Java will not accept the response data in the basic URLConnection
* class.
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
import java.net.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class RawURLConnection
//

public class RawURLConnection extends URLConnection {

    RawInputStream cis;
    static int defaultPort = 80;
    Socket s;

//-----------------------------------------------------------------------------
// RawURLConnection::RawURLConnection
//

RawURLConnection ( URL url, String pRawType ) throws IOException {

    super( url );

    try {
        String name = "net.www.protocol.raw." + pRawType
                           + "InputStream";
        cis = (RawInputStream)Class.forName(name).newInstance();
    }
    catch ( ClassNotFoundException | 
            InstantiationException | IllegalAccessException e ) {
        System.err.println(getClass().getName() + " - Error: 58");
    }

}//end of RawURLConnection::RawURLConnection
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RawURLConnection::connect
//

@Override
synchronized public void connect() throws IOException {

    int port;

    if ( cis == null ) {
        throw new IOException("Raw Class Not Found");
    }

    if ( (port = url.getPort()) == -1 ) {
        port = defaultPort;
    }

    //after creating the URLConnection with:
    //   URLConnection urlConn = url.openConnection();
    //the user can set the connection time out with:
    //   urlConn.setConnectTimeout(1000);
    //to apply that timeout, the short method of socket creation:
    //    Socket s = new Socket(url.getHost(), port);
    //cannot be used. Rather, the SocketAddress/connect combination is used
    //as seen below.
    //(note that timeout of 0 actually defaults to about 22 seconds, not
    //infinity as described in the document for URLConnection class)

    SocketAddress sockaddr = new InetSocketAddress(url.getHost(), port);
    s = new Socket();
    s.connect(sockaddr, getConnectTimeout());

    //after creating the URLConnection with:
    //   URLConnection urlConn = url.openConnection();
    //the user can set the read time out with:
    //   urlConn.setReadTimeout(1000);
    //this prevents the connection from freezing until the buffer is filled
    //with data
    //the value is applied to the socket as seen below

    s.setSoTimeout(getReadTimeout());

    //send the filename in plaintext
    OutputStream server = s.getOutputStream();

    PrintStream out = new PrintStream( server );

    out.print( "GET " + url.getFile() + " HTTP/1.1\r\n" );
    out.print("Host: " + url.getHost() + "\r\n");
    out.print("\r\n");

    //initialize the RawInputStream
    cis.set( s.getInputStream(), server );

    connected = true;

}//end of RawURLConnection::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RawURLConnection::disconnect
//
// Closes the socket.
//

synchronized public void disconnect() throws IOException
{

    connected = false;

    s.close();

}//end of RawURLConnection::disconnect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RawURLConnection::getInputStream
//

 @Override
 synchronized public InputStream getInputStream() throws IOException
{

    if (!connected) {connect();}

    return (cis);

}//end of RawURLConnection::getInputStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RawURLConnection::getContentType
//
// This gets called by the URL class during a getContent call. Normally, the
// guessContentTypeFromName method would be called here, but that requires
// proper headers in the response data.
//
// Since this class is designed to work with content which does not have
// proper headers, the contentType is always assumed to be "text/html". It is
// up to the main program to determine further type distinctions, such as
// whether or not the content contains XML code.
//

@Override
public String getContentType() {

    // the following call is a typical response, but fails if the content does
    // not have proper headers so it is not used
    //      return guessContentTypeFromName(url.getFile());

    // assume all content is "text/html"
    return("text/html");

}//end of RawURLConnection::getContentType
//-----------------------------------------------------------------------------

}//end of class RawURLConnection
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
