/******************************************************************************
* Title: CopyTools.java
* Author: Mike Schoonover
* Date: 9/26/13
*
* Purpose:
*
* This class handles copying of files, folders and the files therein, and
* folder trees (folders and all the folders and files in tree below).
*
* The code was derived from an example in the Oracle Java Tutorial -- see
* copyright notice below.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
* ----------------------------------------------------------------------------
*
* Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*   - Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*
*   - Redistributions in binary form must reproduce the above copyright
*     notice, this list of conditions and the following disclaimer in the
*     documentation and/or other materials provided with the distribution.
*
*   - Neither the name of Oracle nor the names of its
*     contributors may be used to endorse or promote products derived
*     from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
* IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/

package chart.mksystems.tools;

import java.io.IOException;
import java.nio.file.*;
import static java.nio.file.FileVisitResult.*;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class CopyTools
//
// This class provides methods for copying files and file trees.
//
// A file tree is a folder and all the folders and files within and all the
// files and folders within those folders and so forth.
//

public class CopyTools extends Object{


    static private JFrame mainFrame = null;

    static private String errorMessage = "";

//-----------------------------------------------------------------------------
// CopyTools::CopyTools (constructor)
//

public CopyTools()
{

}//end of CopyTools::CopyTools (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of CopyTools::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::okayToOverwrite
//
// Asks user if it is okay to overwrite an existing file.
//
// Return true if okay, false otherwise.
//

static boolean okayToOverwrite(Path file) {

    int n = JOptionPane.showConfirmDialog( mainFrame,
        "The folder or file already exists -- overwrite?",
        "Folder or File Already Exists", JOptionPane.OK_CANCEL_OPTION);

    if (n == JOptionPane.OK_OPTION) {
        return(true);
    }
    else{
        return(false);
    }

}//end of CopyTools::okayToOverwrite
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::copyFile
//
// Copy source file to target location. If {@code pPrompt} is true then
// prompt user to overwrite target if it exists. The {@code pPreserve}
// parameter determines if file attributes should be copied/preserved.
//
// Returns true if no error, false otherwise.
//

static public boolean copyFile(JFrame pMainFrame,
        Path pSource, Path pTarget, boolean pPrompt, boolean pPreserve) {

    mainFrame = pMainFrame;

    errorMessage = "";

    return(copyFileHelper(pSource, pTarget, pPrompt, pPreserve));

}//end of CopyTools::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::copyFileHelper
//
// Copy source file to target location. If pPrompt is true then prompt user to
// overwrite target if it exists. The pPreserve parameter determines if file
// attributes should be copied/preserved.
//
// Returns true if no error, false otherwise.
//

static public boolean copyFileHelper(
        Path pSource, Path pTarget, boolean pPrompt, boolean pPreserve) {

    CopyOption[] options = (pPreserve) ?
        new CopyOption[] { COPY_ATTRIBUTES, REPLACE_EXISTING } :
        new CopyOption[] { REPLACE_EXISTING };

    if (!pPrompt || Files.notExists(pTarget) || okayToOverwrite(pTarget)) {

        try {
            Files.copy(pSource, pTarget, options);
        }
        catch (IOException e) {

            errorMessage = e.getMessage() + "; File: " + pSource;

            return(false);
        }
    }

    return(true);

}//end of CopyTools::copyFileHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::copyTree
//
// Copies a file tree. A file tree is a folder and all the folders and files
// within and all the files and folders within those folders and so forth.
//
// Returns true if no error, false otherwise.
//

static public boolean copyTree(JFrame pMainFrame,
        Path pSource, Path pTarget, boolean pPrompt, boolean pPreserve) {

    mainFrame = pMainFrame;

    errorMessage = "";

    TreeCopier tc = new TreeCopier(pSource, pTarget, pPrompt, pPreserve);

    tc.doCopy();

    errorMessage = tc.getErrorMessage();

    return(tc.getSuccess());

}//end of CopyTools::copyTree
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyTools::getErrorMessage
//
// Returns errorMessage which contains the message explaining the last error
// encountered. Only the last error message is recorded.
//

static public String getErrorMessage()
{

    return(errorMessage);

}//end of CopyTools::getErrorMessage
//-----------------------------------------------------------------------------

}//end of class CopyTools
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TreeCopier
//
// This class copies a file tree. A file tree is a folder and all the folders
// and files within and all the files and folders within those folders and
// so forth.
//

class TreeCopier implements FileVisitor<Path> {

    private final Path source;
    private Path target;
    private final boolean prompt;
    private final boolean preserve;

    private boolean success;
    private String errorMessage = "";

//-----------------------------------------------------------------------------
// TreeCopier::TreeCopier (constructor)
//

TreeCopier(Path pSource, Path pTarget, boolean pPrompt, boolean pPreserve) {

    source = pSource;
    target = pTarget;
    prompt = pPrompt;
    preserve = pPreserve;

}//end of TreeCopier::TreeCopier (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::doCopy
//
// Peforms the copy operation.
//
// Sets success true if no errors , false otherwise.
//
// If an error(s) was encountered, errorMessage will be set to a message
// describing the last error encountered. Only the last message is preserved.
//

public void doCopy()
{

    success = true;

    // check if target is a directory
    boolean isDir = Files.isDirectory(target);

    //if destination is a directory, then the filename is extracted from the
    //source path and appended to the target path to form the complete target
    //folder path

    target = (isDir) ? target.resolve(source.getFileName()) : target;

    // follow links when copying files
    EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

    try{
        Files.walkFileTree(source, opts, Integer.MAX_VALUE, this);
    }
    catch(IOException e){
        success = false;
        errorMessage = e.getMessage() + "; Error copying: " + source;
    }

}//end of TreeCopier::doCopy
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::createFolder
//
// Creates folder pFolder.
//
// Sets success false on error.
//
// If an error(s) was encountered, errorMessage will be set to a message
// describing the last error encountered. Only the last message is preserved.
//

public void createFolder(Path pFolder)
{

    try{

        Files.createDirectory(pFolder);

    }
    catch(FileAlreadyExistsException e){
        //okay if already exists -- existing folder will be used
    }
    catch(Exception e){
        success = false;
        errorMessage = e.getMessage() + "; Unable to create: " + pFolder;
    }

}//end of TreeCopier::createFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::getSuccess
//
// Returns success which will be true if no errors encountered during last
// operation or false otherwise.
//

public boolean getSuccess()
{

    return(success);

}//end of TreeCopier::getSuccess
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::getErrorMessage
//
// Returns errorMessage which contains the message explaining the last error
// encountered. Only the last error message is recorded.
//

public String getErrorMessage()
{

    return(errorMessage);

}//end of TreeCopier::getErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::preVisitDirectory
//

@Override
public FileVisitResult preVisitDirectory(Path pDir, BasicFileAttributes pAttrs)
{

    // before visiting entries in a directory we copy the directory
    // (okay if directory already exists).
    CopyOption[] options = (preserve) ?
        new CopyOption[] { COPY_ATTRIBUTES } : new CopyOption[0];

    Path newdir = target.resolve(source.relativize(pDir));
    try {
        Files.copy(pDir, newdir, options);
    } catch (FileAlreadyExistsException e) {
        // ignore
    } catch (IOException e) {
        success = false;
        errorMessage = e.getMessage() + "; Unable to create: " + newdir;
        return SKIP_SUBTREE;
    }
    return CONTINUE;

}//end of TreeCopier::preVisitDirectory
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::visitFile
//

@Override
public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs)
{

    CopyTools.copyFileHelper(pFile, target.resolve(source.relativize(pFile)),
                                                            prompt, preserve);
    return CONTINUE;

}//end of TreeCopier::visitFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::postVisitDirectory
//

@Override
public FileVisitResult postVisitDirectory(Path pDir, IOException pE)
{

    // fix up modification time of directory when done
    if (pE == null && preserve) {
        Path newdir = target.resolve(source.relativize(pDir));
        try {
            FileTime time = Files.getLastModifiedTime(pDir);
            Files.setLastModifiedTime(newdir, time);
        } catch (IOException e) {
            success = false;
            errorMessage = e.getMessage()
                            + "; Unable to copy all attributes to: " + newdir;
        }
    }
    return CONTINUE;

}//end of TreeCopier::postVisitDirectory
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TreeCopier::visitFileFailed
//

@Override
public FileVisitResult visitFileFailed(Path pFile, IOException pE)
{

    if (pE instanceof FileSystemLoopException) {
        success = false;
        errorMessage = "Cycle detected: " + pFile;
    } else {
        success = false;
        errorMessage = pE.getMessage() + "; Unable to copy: " + pFile;
    }
    return CONTINUE;

}//end of TreeCopier::visitFileFailed
//-----------------------------------------------------------------------------


}//end of class TreeCopier
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


/*
    static void usage() {
        System.err.println("java Copy [-ip] source... target");
        System.err.println("java Copy -r [-ip] source-dir... target");
        System.exit(-1);
    }



    public static void main(String[] args) throws IOException {
        boolean recursive = false;
        boolean prompt = false;
        boolean preserve = false;

        // process options
        int argi = 0;
        while (argi < args.length) {
            String arg = args[argi];
            if (!arg.startsWith("-")) { break; }
            if (arg.length() < 2) { usage(); }
            for (int i=1; i<arg.length(); i++) {
                char c = arg.charAt(i);
                switch (c) {
                    case 'r' : recursive = true; break;
                    case 'i' : prompt = true; break;
                    case 'p' : preserve = true; break;
                    default : usage();
                }
            }
            argi++;
        }

        // remaining arguments are the source files(s) and the target location
        int remaining = args.length - argi;
        if (remaining < 2) { usage(); }
        Path[] source = new Path[remaining-1];
        int i=0;
        while (remaining > 1) {
            source[i++] = Paths.get(args[argi++]);
            remaining--;
        }
        Path target = Paths.get(args[argi]);


*
*/
