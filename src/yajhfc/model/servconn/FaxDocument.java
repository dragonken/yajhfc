/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2011 Jonas Wolz <info@yajhfc.de>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Linking YajHFC statically or dynamically with other modules is making 
 *  a combined work based on YajHFC. Thus, the terms and conditions of 
 *  the GNU General Public License cover the whole combination.
 *  In addition, as a special exception, the copyright holders of YajHFC 
 *  give you permission to combine YajHFC with modules that are loaded using
 *  the YajHFC plugin interface as long as such plugins do not attempt to
 *  change the application's name (for example they may not change the main window title bar 
 *  and may not replace or change the About dialog).
 *  You may copy and distribute such a system following the terms of the
 *  GNU GPL for YajHFC and the licenses of the other code concerned,
 *  provided that you include the source code of that other code when 
 *  and as the GNU GPL requires distribution of source code.
 *  
 *  Note that people who make modified versions of YajHFC are not obligated to grant 
 *  this special exception for their modified versions; it is their choice whether to do so.
 *  The GNU General Public License gives permission to release a modified 
 *  version without this exception; this exception also makes it possible 
 *  to release a modified version which carries forward this exception.
 */
package yajhfc.model.servconn;

import gnu.inet.ftp.ServerResponseException;

import java.io.IOException;
import java.io.OutputStream;

import yajhfc.file.FileFormat;
import yajhfc.file.FormattedFile;

public interface FaxDocument {
    /**
     * Returns the "path" to this document. 
     * This is used to give the user a clue which file is currently downloaded and does
     * not have to have something to do with the actual file source  
     * @return
     */
    public String getPath();
    
    /**
     * The format this document is in
     * @return
     */
    public FileFormat getType();
    
    /**
     * Returns a (possibly temporary) file with this document's content.
     * If this document resides in a path accessible over the "normal" file system, this method may return the original file.
     * Calling this method multiple times always returns the same file (i.e. it is only downloaded once)
     * @return
     */
    public FormattedFile getDocument() throws IOException,
    ServerResponseException;
    
    /**
     * Copies this document's content to the specified stream
     * @param target
     * @throws IOException
     */
    public void downloadToStream(OutputStream target) throws IOException,
    ServerResponseException;
    
    /**
     * Returns the path the HylaFAX server can use to directly access this document.
     * Returns null if this document cannot be accessed directly by the HylaFAX server.
     * @return
     */
    public String getHylafaxPath();
}
