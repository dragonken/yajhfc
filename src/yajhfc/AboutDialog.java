package yajhfc;
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

import static yajhfc.Utils._;
import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import yajhfc.util.CancelAction;
import yajhfc.util.ClipboardPopup;
import yajhfc.util.URIClickListener;

public class AboutDialog extends JDialog implements HyperlinkListener {
    private static final Logger log = Logger.getLogger(AboutDialog.class.getName());
    
    public enum Mode { ABOUT, READMES };
    
    private static final String[] readmeFiles = 
            { "README.txt", "doc/faq.html", "LICENSE", "COPYING" };
    /**
     * Determines if the english version of the readmeFiles above
     * should be displayed in addition to the localized ones.
     */
    private static final boolean[] addEnglishReadme = 
            { true, true, false, false };
    
    private JPanel jContentPane;
    
    private Box boxButtons;
    private JButton buttonOK;
    
    private JTabbedPane tabMain;
    private JPanel aboutPane;
    //private ArrayList<JScrollPane> scrollTxt;
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();
            try {
                URL u = e.getURL();
                String proto = u.getProtocol();
                if (proto.equals("file") || proto.equals("jar")) {
                    pane.setPage(u);
                } else {
                    //getToolkit().beep();
                    DesktopManager.getDefault().safeBrowse(u.toURI(), this);
                }
            } catch (Exception ex) {
                //t.printStackTrace();
                log.log(Level.WARNING, "Error handling hyperlink:", ex);
            }
        }
    }
    
    private JScrollPane addScrollTxt(String resName, boolean useLocalized) {
        
        URL txtURL;
        if (useLocalized && !Utils.getLocale().equals(Locale.ENGLISH))
            txtURL = Utils.getYajHFCLanguage().getLocalizedFile(resName, false);
        else
            txtURL = AboutDialog.class.getResource(resName);
        
        if (txtURL == null)
            return null;
        
        /*JTextArea text = new JTextArea();*/
        JEditorPane text = new JEditorPane();
        text.setEditable(false);
        text.setFont(new Font("DialogInput", java.awt.Font.PLAIN, 12));
        text.addMouseListener(ClipboardPopup.DEFAULT_POPUP);
        text.addHyperlinkListener(this);
        
        try {
            /*
            text.setText("");
            
            BufferedReader fIn = new BufferedReader(new InputStreamReader(txtURL.openStream(), Charset.forName("UTF-8")));
            while (true) {
                String line = fIn.readLine();
                if (line != null) {
                    text.append(line + "\n");
                } else
                    break;
            }
            fIn.close();*/
            
            text.putClientProperty("charset", "UTF-8");
            text.setPage(txtURL);
            //text.setCaretPosition(0);
        } catch (IOException e) {
            text.setText("Error loading text file " + resName + ".");
        }
        
        JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scroll;
        
        /* if (scrollLicense == null) {
            textLicense = new JTextArea();
            textLicense.setEditable(false);
            textLicense.setFont(new Font("DialogInput", java.awt.Font.PLAIN, 12));
            loadFile();
            
            scrollLicense = new JScrollPane(textLicense);          
        }
        return scrollLicense; */
    }
    
    private JScrollPane addSysPropTxt() {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFont(new Font("Monospaced", Font.PLAIN, 12));
        text.addMouseListener(ClipboardPopup.DEFAULT_POPUP);

        Properties sp = System.getProperties();
        String[] keys = new String[sp.size()];
        
        Enumeration<?> e = sp.propertyNames();
        int i = 0;
        while (e.hasMoreElements()) {
            keys[i++] = (String)e.nextElement();
        }
        
        Arrays.sort(keys);
        
        for (i = 0; i < keys.length; i++) {
            text.append(keys[i] + "=" + sp.getProperty(keys[i]) + "\n");
        }
        text.setCaretPosition(0);
        
        JScrollPane scroll = new JScrollPane(text);
        return scroll;
    }
    
    private JPanel getAboutPane() {
        if (aboutPane == null) {
            double border = 15;
            double[][] dLay = {
                    { border, TableLayout.PREFERRED, border, TableLayout.FILL, border },
                    { border, TableLayout.PREFERRED, border, TableLayout.PREFERRED, border, TableLayout.PREFERRED, TableLayout.PREFERRED, border, 0.25, 0.3, border }
            };
            
            aboutPane = new JPanel(new TableLayout(dLay));
            
            JLabel lblImg = new JLabel(new ImageIcon(AboutDialog.class.getResource("logo-large.png")));
            
            JLabel lblApp = new JLabel("<html>" + VersionInfo.AppName + "</html>");
            lblApp.setFont(new Font("Serif", Font.BOLD, 18));
            
            JLabel lblVer = new JLabel(MessageFormat.format(_("Version {0}"), VersionInfo.AppVersion));
            lblVer.setFont(new Font("Dialog", Font.ITALIC, 11));
            
            Box boxApp = new Box(BoxLayout.PAGE_AXIS);
            boxApp.add(lblApp);
            boxApp.add(Box.createVerticalStrut(4));
            boxApp.add(lblVer);
            
            JLabel lblCopyright = new JLabel(VersionInfo.AppCopyright);
            
            JLabel lblInfo = new JLabel("<html>" + _("YajHFC is a client for the HylaFAX fax server.") + "</html>");
            
            JPanel panelHomepage = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0), false);
            panelHomepage.add(new JLabel(Utils._("Homepage") + ": "));
            JLabel homepageLabel = new JLabel("<html><a href=\"http://www.yajhfc.de\">" + VersionInfo.HomepageURL + "</a></html>");
            homepageLabel.addMouseListener(new URIClickListener(VersionInfo.HomepageURL));
            homepageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            panelHomepage.add(homepageLabel);
            
            JPanel panelEMail = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0), false);
            panelEMail.add(new JLabel(Utils._("Author") + ": " + VersionInfo.AuthorName));
            JLabel emailLabel = new JLabel("<html>&lt;<a href=\"mailto:"+VersionInfo.AuthorEMail+"\">" + VersionInfo.AuthorEMail + "</a>&gt;</html>");
            emailLabel.addMouseListener(new URIClickListener("mailto:"+VersionInfo.AuthorEMail));
            emailLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            panelEMail.add(emailLabel);
            
            JLabel lblTranslator = null;
            
            String translator = _("$TRANSLATOR$");
            if (!translator.equals("$TRANSLATOR$")) {
                lblTranslator = new JLabel(MessageFormat.format(_("$LANGUAGE$ translation by {0}"), translator));
            }
            
            aboutPane.add(lblImg, "1, 1");
            aboutPane.add(boxApp, "3, 1, F, C");
            
            aboutPane.add(lblInfo, "1, 3, 3, 3, F, C");
            aboutPane.add(panelHomepage, "1, 5, 3, 5, F, C");
            aboutPane.add(panelEMail, "1, 6, 3, 6, F, C");
            if (lblTranslator != null)
                aboutPane.add(lblTranslator, "1, 8, 3, 8, F, C");
            aboutPane.add(lblCopyright, "1, 9, 3, 9, F, C");
        }
        return aboutPane;
    }
   
    private Box getBoxButtons() {
        if (boxButtons == null) {
            boxButtons = new Box(BoxLayout.LINE_AXIS);
            
            CancelAction actOK = new CancelAction(this, _("OK"));
            buttonOK = actOK.createCancelButton();
            
            boxButtons.add(Box.createHorizontalGlue());
            boxButtons.add(buttonOK);
            boxButtons.add(Box.createHorizontalGlue());
        }
        return boxButtons;
    }
    
    private JTabbedPane getTabMain() {
        if (tabMain == null) {
            tabMain = new JTabbedPane(JTabbedPane.BOTTOM);
            
            //tabMain.addTab(_("About"), getAboutPane());
            //tabMain.addTab(_("License"), getScrollLicense());
        }
        return tabMain;
    }
    
    private JPanel getjContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel(new BorderLayout());
            jContentPane.add(getBoxButtons(), BorderLayout.SOUTH);
            jContentPane.add(getTabMain(), BorderLayout.CENTER);
        }
        return jContentPane;
    }
    
    private void initialize() {
        //setResizable(false);
        setModal(true);
        setSize(640, 380);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setContentPane(getjContentPane());
        
        getRootPane().setDefaultButton(buttonOK);
        //setLocationByPlatform(true);
    }
    
    public void setMode(Mode mode) {
        getTabMain().removeAll();
        switch (mode) {
        case ABOUT:
            tabMain.addTab(_("About"), getAboutPane());
            tabMain.addTab(_("License"), addScrollTxt("/COPYING", false));
            tabMain.addTab(_("System properties"), addSysPropTxt());
            this.setTitle(MessageFormat.format(_("About {0}"), VersionInfo.AppShortName));
            break;
        case READMES:
            for (int i = 0; i < readmeFiles.length; i++) {
                JScrollPane tab = addScrollTxt("/" + readmeFiles[i], true);
                if (tab != null)
                    tabMain.addTab(readmeFiles[i], tab);
                
                if ((tab == null) || (addEnglishReadme[i] && (!Utils.getLocale().equals(Locale.ENGLISH)))) {
                    tab = addScrollTxt("/" + readmeFiles[i], false);
                    if (tab != null)
                        tabMain.addTab(MessageFormat.format("{0} ({1})", readmeFiles[i], Locale.ENGLISH.getDisplayLanguage(Utils.getLocale())), tab);
                }
            }
            this.setTitle(_("Documentation"));
            this.setSize(640, 480);
            break;
        default:
            log.log(Level.WARNING, "Invalid mode!");
        }
    }
    
    public AboutDialog(Frame owner) {
        super(owner);
        
        initialize();
        setLocationRelativeTo(owner);
    }
}
