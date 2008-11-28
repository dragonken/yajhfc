package yajhfc;

import gnu.hylafax.HylaFAXClient;
import gnu.inet.ftp.ServerResponseException;

import java.awt.Window;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.util.ExceptionDialog;
import yajhfc.util.PasswordDialog;

public class HylaClientManager {
    protected boolean adminMode;
    protected HylaFAXClient client;
    protected FaxOptions myopts;
    protected String password;
    protected String adminPassword;
    protected String lastUser;
    protected int transactionCounter;
    protected List<HylaModem> modems = null;
    
    protected static final String modemListFormat = "$$$|%m|%n";
    protected static final String modemListPrefix = "$$$";    
    
    private static final Logger log = Logger.getLogger(HylaClientManager.class.getName());
    
    public HylaClientManager(FaxOptions myopts) {
        super();
        this.myopts = myopts;
        optionsChanged();
    }

    public void optionsChanged() {
        forceLogout();
        
        modems = null;
        
        if (!myopts.askPassword)
            password = myopts.pass;
        else
            password = null;
        
        if (!myopts.askAdminPassword)
            adminPassword = myopts.AdminPassword;
        else
            adminPassword = null;
    }
    
    public HylaFAXClient beginServerTransaction(Window owner) {
        transactionCounter++;
        
        if (Utils.debugMode) {
            log.fine("HylaClientManager -> beginServerTransaction: " + transactionCounter);
        }
        if (client != null  && myopts.useDisconnectedMode && transactionCounter == 1) {
            synchronized (client) {
                int time = 0;
                Thread.yield();
                try {
                    while (client != null && time < 500) {
                        Thread.sleep(50); // Wait that the other user frees the client (Quick and Dirty !!)
                        time += 50;
                    }
                } catch (InterruptedException e) {
                    // NOP
                }
                if (Utils.debugMode && time >= 500) {
                    log.info("In beginServerTransaction: TIMEOUT waiting for other client");
                }
            }
        }
        if (client == null) {
            if (transactionCounter != 1) {
                log.warning("Before forceLogin: transactionCounter = " + transactionCounter);
            }
            return forceLogin(owner);
        } else {
            return client;   
        }
    }
    
    public void endServerTransaction() {
        if (Utils.debugMode) {
            log.fine("HylaClientManager -> endServerTransaction: " + transactionCounter);
        }
        transactionCounter--;
        if (myopts.useDisconnectedMode && transactionCounter <= 0) {
            forceLogout();
        }
    }

    /**
     * Logs in. Returns null if an error occurred.
     */
    public HylaFAXClient forceLogin(Window owner) {
        if (Utils.debugMode) {
            log.fine("HylaClientManager -> forceLogin");
        }
        if (client == null)
        {
            client = new HylaFAXClient();
            synchronized (client) {
                //client.setDebug(Utils.debugMode);
                client.setSocketTimeout(myopts.socketTimeout);
                try {
                    client.open(myopts.host, myopts.port);
                    if (Utils.debugMode) {
                        log.info("Greeting was: " + client.getGreeting());
                    }
                    
                    while (client.user(myopts.user)) {                
                        if (password == null) {

                            String pwd = PasswordDialog.showPasswordDialogThreaded(owner, Utils._("User password"), MessageFormat.format(Utils._("Please enter the password for user \"{0}\"."), myopts.user));
                            if (pwd == null) { // User cancelled
                                client.quit();
                                //doErrorCleanup(); // TODO
                                return null;
                            } else
                                try {
                                    client.pass(pwd);
                                    password = pwd;
                                    //repeatAsk = false;
                                    break;
                                } catch (ServerResponseException e) {
                                    ExceptionDialog.showExceptionDialogThreaded(owner, Utils._("An error occured in response to the password:"), e);
                                    //repeatAsk = true;
                                }
                        } else {
                            client.pass(password);
                            break;
                        }
                    } 

                    if (adminMode) {
                        boolean authOK = false;
                        if (adminPassword == null) {
                            do {
                                String pwd = PasswordDialog.showPasswordDialogThreaded(owner, Utils._("Admin password"), MessageFormat.format(Utils._("Please enter the administrative password for user \"{0}\"."), myopts.user));
                                if (pwd == null) { // User cancelled
                                    break; //Continue in "normal" mode
                                } else
                                    try {
                                        client.admin(pwd);
                                        adminPassword = pwd;
                                        authOK = true;
                                    } catch (ServerResponseException e) {
                                        ExceptionDialog.showExceptionDialogThreaded(owner, Utils._("An error occured in response to the password:"), e);
                                        authOK = false;
                                    }
                            } while (!authOK);
                        } else {
                            client.admin(adminPassword);
                            authOK = true; // No error => authOK
                        }
                        ;
                        adminMode = authOK;
                    }

                    client.setPassive(myopts.pasv);
                    client.tzone(myopts.tzone.getTimezone());

                    client.rcvfmt(myopts.recvfmt.getFormatString());
                    return client;
                } catch (Exception e) {
                    ExceptionDialog.showExceptionDialogThreaded(owner, Utils._("An error occured connecting to the server:"), e);
                    return null;
                }
            }
        } else {
            return client;
        }
    }
    
    public void forceLogout() {
        if (Utils.debugMode) {
            log.fine("HylaClientManager -> forceLogout");
        }
        if (transactionCounter != 0) {
            log.warning("In forceLogout: transactionCounter = " + transactionCounter);
            transactionCounter = 0;
        }
        if (client != null) {
            try {
                client.quit();
            } catch (IOException e) {
                if (Utils.debugMode)
                    log.log(Level.INFO, "On client.quit():", e);
            } catch (ServerResponseException e) {
                if (Utils.debugMode)
                    log.log(Level.INFO, "On client.quit():", e);
            }
            client = null;
        }
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public void setAdminMode(boolean adminMode) {
        if (adminMode != this.adminMode) 
        {
            this.adminMode = adminMode;
            forceLogout();
        }
    }
    
    public List<HylaModem> getModems() {
        if (modems == null) {
            HylaFAXClient hyfc = beginServerTransaction(null);
            if (hyfc == null) {
                modems = HylaModem.defaultModems;
                return modems;
            }
            
            Vector<?> status;
            try {
                synchronized (hyfc) {
                    String oldModemFmt = hyfc.mdmfmt();

                    hyfc.mdmfmt(modemListFormat);
                    status = hyfc.getList("status");

                    hyfc.mdmfmt(oldModemFmt);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error fetching modem list: ", e);
                modems = HylaModem.defaultModems;
                return modems;
            } finally {
                endServerTransaction();
            }
            
            modems = new ArrayList<HylaModem>();
            modems.addAll(HylaModem.defaultModems);
            for (Object o : status) {
                String line = (String)o;
                if (line.startsWith(modemListPrefix)) { // Is a line describing a modem
                    String[] fields = Utils.fastSplit(line, '|');
                    if (fields.length < 2) {
                        log.log(Level.WARNING, "Invalid modem \"" + line + "\".");                            
                    } else {
                        modems.add(new HylaModem(fields[1], fields.length >= 3 ? fields[2] : ""));
                    }
                }
            }
        }
        return modems;
    }
}
