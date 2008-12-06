/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2008 Jonas Wolz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package yajhfc.tray;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;

import yajhfc.Launcher2;
import yajhfc.MainWin;
import yajhfc.RecvFormat;
import yajhfc.Utils;
import yajhfc.model.RecvYajJob;
import yajhfc.model.UnReadMyTableModel;
import yajhfc.model.UnreadItemEvent;
import yajhfc.model.UnreadItemListener;
import yajhfc.util.ExcDialogAbstractAction;

/**
 * The default YajHFC tray icon.
 * @author jonas
 *
 */
public class YajHFCTrayIcon implements UnreadItemListener, WindowListener {

    private ITrayIcon trayIcon = null;
    MainWin mainw;
    private Action showAction;
    private boolean connected = false;
    private boolean minimizeToTray = false;
    private UnReadMyTableModel recvModel;
    
    /**
     * Creates a new tray icon if it is available and configures the main window
     * to minimize to tray.
     * @param mainw
     * @param recvModel
     * @param actions the actions to add to the popup menu. Specify null to include a separator
     */
    public YajHFCTrayIcon(MainWin mainw, UnReadMyTableModel recvModel, Action... actions) {
        this.mainw = mainw;
        this.recvModel = recvModel;
        
        try {
            if (TrayFactory.trayIsAvailable()) {
                showAction = new ExcDialogAbstractAction() {
                    @Override
                    protected void actualActionPerformed(ActionEvent e) {
                        YajHFCTrayIcon.this.mainw.bringToFront();
                    }
                };
                showAction.putValue(Action.NAME, Utils._("Restore"));

                PopupMenu popup = new PopupMenu();
                popup.add(createMenuItemForAction(showAction));
                popup.addSeparator();
                for (Action act : actions) {
                    if (act == null) {
                        popup.addSeparator();
                    } else {
                        popup.add(createMenuItemForAction(act));
                    }
                }
                trayIcon = TrayFactory.getTrayManager().installTrayIcon(mainw.getIconImage(), Utils.AppShortName, popup, showAction);

                recvModel.addUnreadItemListener(this);
                mainw.addWindowListener(this);

                updateTooltip();
            }
        } catch (Exception ex) {
            Logger.getLogger(YajHFCTrayIcon.class.getName()).log(Level.SEVERE, "Error creating tray icon:", ex);
            trayIcon = null;
        }
    }
    
    private MenuItem createMenuItemForAction(Action a) {
        MenuItem rv = new MenuItem((String)a.getValue(Action.NAME));
        rv.setEnabled(a.isEnabled());
        rv.addActionListener(a);
        a.addPropertyChangeListener(new MenuItemActionPropChangeListener(a, rv));
        
        return rv;
    }
    
    public void setConnectedState(boolean connected) {
        this.connected = connected;
        updateTooltip();
    }   

    public void newItemsAvailable(UnreadItemEvent evt) {
        if (trayIcon != null && !evt.isOldDataNull()) {
            StringBuffer msg = new StringBuffer();
            new MessageFormat(Utils._("{0} fax(es) received ({1} unread fax(es)):")).format(new Object[] { evt.getItems().size(), recvModel.getNumberOfUnreadFaxes()}, msg, null);
            int senderIdx = recvModel.columns.getCompleteView().indexOf(RecvFormat.s);
            if (senderIdx >= 0) {
                for (RecvYajJob job : evt.getItems()) {
                    msg.append('\n');
                    msg.append(job.getStringData(senderIdx));
                }
            }

            trayIcon.displayMessage(Utils._("New fax received"), msg.toString(), ITrayIcon.MSGTYPE_INFO);
            updateTooltip();
        }
    }
    
    private void updateTooltip() {
        if (trayIcon != null) {
            int numUnread = recvModel.getNumberOfUnreadFaxes();
            String text;
            if (connected) {
                StringBuffer textBuf = new StringBuffer();
                String userName = (Launcher2.application.getClientManager() == null) ? Utils.getFaxOptions().user : Launcher2.application.getClientManager().getUser();
                textBuf.append(userName).append('@').append(Utils.getFaxOptions().host);
                textBuf.append(" - ");
                new MessageFormat(Utils._("{0} unread fax(es)")).format(new Object[] {numUnread}, textBuf, null);
                text = textBuf.toString();
            } else {
                text = Utils._("Disconnected");
            }           

            trayIcon.setToolTip(text);
        }
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }
    
    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }
    
    public void windowIconified(WindowEvent e) {
        if (minimizeToTray && trayIcon != null) {
            mainw.setVisible(false);
        }
    }
    
    public void windowClosed(WindowEvent e) {
        dispose();
    }

    public void windowClosing(WindowEvent e) {
        // stub 
    }

    public void windowDeactivated(WindowEvent e) {
        // stub 
    }

    public void windowDeiconified(WindowEvent e) {
        // stub 
    }

    public void windowOpened(WindowEvent e) {
        // stub 
    }
    
    public void windowActivated(WindowEvent e) {
        // stub 
    }

    public void readStateChanged() {
        updateTooltip();
    }
    
    public void dispose() {
        if (trayIcon != null) {
            TrayFactory.getTrayManager().removeTrayIcon(trayIcon);
            trayIcon = null;
            mainw.removeWindowListener(this);
            recvModel.removeUnreadItemListener(this);
        }
    }
    
    private static class MenuItemActionPropChangeListener implements PropertyChangeListener {
        private final Action action;
        private final WeakReference<MenuItem> itemRef;
        
        public void propertyChange(PropertyChangeEvent evt) {
            MenuItem menuItem = itemRef.get();
            if (menuItem == null) {
                action.removePropertyChangeListener(this);
            } else {
                String propName = evt.getPropertyName();
                if (Action.NAME.equals(propName)) {
                    menuItem.setLabel((String)action.getValue(Action.NAME));
                } else if ("enabled".equals(propName)) {
                    menuItem.setEnabled(action.isEnabled());
                }
            }
        }
        
        public MenuItemActionPropChangeListener(Action action, MenuItem menuItem) {
            this.action = action;
            itemRef = new WeakReference<MenuItem>(menuItem);
        }
    }
}