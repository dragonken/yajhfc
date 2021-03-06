/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2018 Jonas Wolz <info@yajhfc.de>
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
package yajhfc.phonebook.namelookup;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.FaxOptions;
import yajhfc.Utils;
import yajhfc.launch.Launcher2;
import yajhfc.model.FmtItem;
import yajhfc.phonebook.DistributionList;
import yajhfc.phonebook.PBEntryField;
import yajhfc.phonebook.PhoneBook;
import yajhfc.phonebook.PhoneBookEntry;
import yajhfc.phonebook.PhoneBookFactory;
import yajhfc.phonebook.convrules.PBEntryFieldContainer;

/**
 * @author jonas
 *
 */
public class PhoneNumberMap {
    static final Logger log = Logger.getLogger(PhoneNumberMap.class.getName());
    
    public static boolean disableLoad = false;

    // Map from Phone number to entry
    protected static Map<String,PBEntryFieldContainer> map ; 
    
    protected static List<PhoneNumberMapListener> listeners = new ArrayList<PhoneNumberMapListener>();
    
    protected static Semaphore loadLock = new Semaphore(1);
    
    public static void addPhoneNumberMapListener(PhoneNumberMapListener listener) {
        listeners.add(listener);
    }
    
    public static void removePhoneNumberMapListener(PhoneNumberMapListener listener) {
        listeners.remove(listener);
    }
    
    protected static void firePhoneNumberMapUpdated() {
        for (PhoneNumberMapListener listener : listeners) {
            listener.phoneNumberMapUpdated();
        }
    }
    
    public static boolean entriesAreLoaded() {
        return (map != null);
    }
    
    public static PBEntryFieldContainer getEntryForNumber(String number) {
        if (map == null)
            return null;
        
        final String canonicalizedNumber = PhoneNumberCanonicalizer.canonicalizeNumber(number);
        if (Utils.debugMode)
            log.finer("Getting entry for number: " + number + "; canonicalized: " + canonicalizedNumber);
        if (canonicalizedNumber.length() > 0) {
            return map.get(canonicalizedNumber);
        } else {
            return null;
        }
    }
    
    public static boolean containsPhonebookColumn(List<? extends FmtItem> list) {
        for (FmtItem fi : list) {
            if (fi.getVirtualColumnType().isPhonebook())
                return true;
        }
        return false;
    }
    
    public static void loadPhonebooks() {
        final FaxOptions fo = Utils.getFaxOptions();
        // Only load phone books if needed
        if (!disableLoad && (
            containsPhonebookColumn(fo.recvfmt) ||
            containsPhonebookColumn(fo.sentfmt) ||
            containsPhonebookColumn(fo.sendingfmt) ||
            containsPhonebookColumn(fo.archiveFmt)
           ))
        {
            loadPhonebooks(fo.phoneBooks);
        }
    }
    
    public static Runnable createRefreshRunner() {
        return new Runnable() {
            public void run() {
                PhoneNumberMap.loadPhonebooks();
            }
        };
    }
    
    public static void reloadPhoneBooksAsync() {
        Utils.poolExecutor.submit(createRefreshRunner());
    }
    
    public static void loadPhonebooks(List<String> phoneBooks) {
        if (!loadLock.tryAcquire()) {
            log.info("Another load is in progress, cancelling this one...");
            return;
        }
        try {
            Map<String,PBEntryFieldContainer> newMap = new ConcurrentHashMap<String,PBEntryFieldContainer>(phoneBooks.size() * 50);
            CountDownLatch latch = new CountDownLatch(phoneBooks.size());
            Dialog dummyDialog = new Dialog(Launcher2.application.getFrame(), "Dummy dialog");
            
            if (Utils.debugMode)
                log.fine("Loading phone books: " + phoneBooks);
            for (String desc : phoneBooks) {
                Utils.poolExecutor.submit(new PBRunnable(desc, latch, newMap, dummyDialog));
            }
            log.fine("Waiting for phone books to be loaded...");
            latch.await();
            map = newMap;
            log.fine("New number map available");
            firePhoneNumberMapUpdated();
            dummyDialog.dispose();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error refreshing phone books", e);
        } finally {
            loadLock.release();
        }
    }
    
    protected static class PBRunnable implements Runnable {
        protected final String descriptor;
        protected final CountDownLatch latch;
        protected final Map<String,PBEntryFieldContainer> myMap;
        protected final Dialog dialog;
        
        public void run() {
            try {
                if (Utils.debugMode)
                    log.fine("Opening phone book: " + descriptor);
                PhoneBook pb = PhoneBookFactory.instanceForDescriptor(descriptor, dialog);
                pb.open(descriptor);
                if (Utils.debugMode)
                    log.fine("Loading entries for phone book: " + descriptor);
                putEntries(pb.getEntries());
                pb.close();
                if (Utils.debugMode)
                    log.fine("Phone book loaded: " + descriptor);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error loading phone book " + descriptor, e);
            } finally {
                latch.countDown();
            }
        }
        
        private void putEntries(List<PhoneBookEntry> list) {
            for (PhoneBookEntry pbe : list) {
                if (pbe instanceof DistributionList) {
                    putEntries(((DistributionList) pbe).getEntries());
                } else {
                    putNumber(PBEntryField.FaxNumber, pbe);
                    putNumber(PBEntryField.VoiceNumber, pbe);
                }
            }
        }

        private void putNumber(PBEntryField field, PhoneBookEntry pbe) {
            final String number = pbe.getField(field);
            
            if (number != null && number.length() > 0) {
                final String canonicalizedNumber = PhoneNumberCanonicalizer.canonicalizeNumber(number);
                if (Utils.debugMode)
                    log.finest("Put: " + canonicalizedNumber + " -> " + pbe);
                
                if (canonicalizedNumber.length() > 0) {
                    PBEntryFieldContainer old = myMap.put(canonicalizedNumber, pbe.getReadOnlyClone());
                    if (old != null && old != pbe)
                        log.info("Phone number is not unique: " + canonicalizedNumber);
                }
            }
        }
        
        protected PBRunnable(String descriptor, CountDownLatch latch, Map<String,PBEntryFieldContainer> myMap, Dialog dialog) {
            super();
            this.descriptor = descriptor;
            this.latch = latch;
            this.myMap = myMap;
            this.dialog = dialog;
        }
    }
}
