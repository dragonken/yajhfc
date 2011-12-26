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
package yajhfc.file.textextract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yajhfc.Utils;
import yajhfc.file.FileConverter.ConversionException;
import yajhfc.file.FormattedFile;

/**
 * @author jonas
 *
 */
public class FaxnumberExtractor {

    private static final Logger log = Logger.getLogger(FaxnumberExtractor.class.getName());
    
    protected final Pattern faxnumberPattern;
    protected final HylaToTextConverter converter;
       
    public FaxnumberExtractor(HylaToTextConverter converter,
            Pattern faxnumberPattern) {
        super();
        this.converter = converter;
        this.faxnumberPattern = faxnumberPattern;
    }

    public FaxnumberExtractor(HylaToTextConverter converter) {
        this(converter,
                Pattern.compile("@@\\s*recipient\\s*:?\\s*(.*?)@@", Pattern.CASE_INSENSITIVE));
    }

    public FaxnumberExtractor() {
        this(HylaToTextConverter.findDefault());
    }
    
    public void extractFromMultipleFileNames(Collection<String> input, List<String> listToAddTo) throws IOException, ConversionException {
        if (input.size() == 0)
            return;
        
        List<FormattedFile> formattedInput = new ArrayList<FormattedFile>(input.size());
        for (String f : input) {
            formattedInput.add(new FormattedFile(f));
        }
        extractFromMultipleFiles(formattedInput, listToAddTo);
    }

    public void extractFromMultipleFiles(List<FormattedFile> input, List<String> listToAddTo) throws IOException, ConversionException {
        if (input.size() == 0)
            return;
        
        CharSequence[] texts = converter.convertFilesToText(input);        
        
        for (CharSequence text : texts) {
            getMatchesInText(text, 1, listToAddTo);
        }
        
    }
    
    public void getMatchesInText(CharSequence text, int captureGroup, List<String> listToAddTo) throws IOException {
        if (Utils.debugMode) {
            log.finest("input text is:\n" + text);
        }
        System.out.println(text);
        Matcher m = faxnumberPattern.matcher(text);
        
        while (m.find()) {
            if (Utils.debugMode) {
                log.fine("Found match: " + m);
            }
            listToAddTo.add(m.group(captureGroup));
        }
        if (Utils.debugMode) {
            log.fine("No more matches");
        }
    }
}