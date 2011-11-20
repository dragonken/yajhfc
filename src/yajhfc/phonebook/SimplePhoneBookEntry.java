package yajhfc.phonebook;

import java.util.Arrays;



/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2006 Jonas Wolz
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

public abstract class SimplePhoneBookEntry extends DefaultPhoneBookEntry {
    
    // Not using an EnumMap here to save some memory (we will have lots of Entries...)
    protected String[] data = new String[PBEntryField.FIELD_COUNT];
    
    private boolean dirty = false;
    /**
     * The cached result of the last toString call. Reset this to null
     * when you also would set dirty to true.
     */
    private String lastToString = null;
    
    @Override
    public String getField(PBEntryField field) {
        return data[field.ordinal()];
    }
    
    protected void setFieldUndirty(PBEntryField field, String value) {
        data[field.ordinal()] = value;
    }
    
    @Override
    public void setField(PBEntryField field, String value) {
        String oldVal = data[field.ordinal()];
        if (value != oldVal && (oldVal == null || !oldVal.equals(value))) {
            setFieldUndirty(field, value);
            dirty = true;
            lastToString = null;
        }
    }
    
    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            lastToString = null;
        }
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    @Override
    public void refreshToStringRule() {
        lastToString = null;
        super.refreshToStringRule();
    }
    
    @Override
    public String toString() {
        if (lastToString == null) {
            lastToString = super.toString();
        }
        return lastToString;
    }
    
    public SimplePhoneBookEntry() {
        Arrays.fill(data, "");
    }
}
