
/**
 * NOAA's National Climatic Data Center
 * NOAA/NESDIS/NCDC
 * 151 Patton Ave, Asheville, NC  28801
 * 
 * THIS SOFTWARE AND ITS DOCUMENTATION ARE CONSIDERED TO BE IN THE 
 * PUBLIC DOMAIN AND THUS ARE AVAILABLE FOR UNRESTRICTED PUBLIC USE.  
 * THEY ARE FURNISHED "AS IS." THE AUTHORS, THE UNITED STATES GOVERNMENT, ITS
 * INSTRUMENTALITIES, OFFICERS, EMPLOYEES, AND AGENTS MAKE NO WARRANTY,
 * EXPRESS OR IMPLIED, AS TO THE USEFULNESS OF THE SOFTWARE AND
 * DOCUMENTATION FOR ANY PURPOSE. THEY ASSUME NO RESPONSIBILITY (1)
 * FOR THE USE OF THE SOFTWARE AND DOCUMENTATION; OR (2) TO PROVIDE
 * TECHNICAL SUPPORT TO USERS.
 */

package gov.noaa.ncdc.wct.ui;


import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import com.jidesoft.swing.Searchable;
import com.jidesoft.swing.SearchableBar;
import com.jidesoft.swing.SearchableUtils;

public class WCTTextPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = -7134768157747603740L;

	public static enum SearchBarProperties { NO_SEARCH_BAR, SEARCH_BAR_FULL, SEARCH_BAR_COMPACT }; 

	private String textData;

    private JButton printButton, closeButton, saveButton, copyButton;
    private JTextArea textArea;
    private JScrollPane scrollPane;

    private SearchBarProperties searchBarProps = SearchBarProperties.SEARCH_BAR_FULL;
    
    private boolean showActionButtons = true;
    
    public WCTTextPanel(List<String> textData) {
        this(parseList(textData), SearchBarProperties.SEARCH_BAR_FULL, true);
    }
    public WCTTextPanel(String textData, boolean showActionButton) {
        this(textData, SearchBarProperties.SEARCH_BAR_FULL, showActionButton);
    }
    
    public WCTTextPanel(String textData, SearchBarProperties sbp) {
    	this(textData, SearchBarProperties.SEARCH_BAR_FULL, true);
    }
    public WCTTextPanel(String textData, SearchBarProperties sbp, boolean showActionButtons) {
        this.textData = textData;
        this.searchBarProps = sbp;
        this.showActionButtons = showActionButtons;
        createGUI();
    }

    
    public static String parseList(List<String> list) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<list.size(); i++) {
            sb.append(list.get(i));
            sb.append("\n");         
        }
        return sb.toString();
    }

    /**
     * Set the supplemental data text in the text area
     * @param textData
     */
    public void setText(String textData) {
        
        if (textData == null) {
            textArea.setText("NO SUPPLEMENTAL DATA AVAILABLE");
            return;
        }

        textArea.setText(textData);
        textArea.setCaretPosition(0);
        
//        pack();
        this.setSize(new Dimension(500, 500));
        validate();
        repaint();

    }


    /**
     * Set the supplemental data text in the text area
     * @param textData
     */
    public void setTextArray(String[] textData) {
        if (textData == null) {
            textArea.setText("NO SUPPLEMENTAL DATA AVAILABLE");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int n=0; n<textData.length; n++) {
            sb.append(textData[n]+"\n");
        }
        
        this.textData = sb.toString();
        
        textArea.setText(sb.toString());       
        textArea.setCaretPosition(0);

//        pack();
        this.setSize(new Dimension(500, 500));
        validate();
        repaint();

    }
    

    

    private void createGUI() {

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setCaretPosition(0);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        textArea.setText(textData);


        this.setLayout(new BorderLayout());

        if (showActionButtons) {
        	JPanel buttonPanel = new JPanel();
        	copyButton = new JButton("Copy");
        	printButton = new JButton("Print");
        	saveButton = new JButton("Save");
        	closeButton = new JButton("Close");
        	copyButton.addActionListener(this);
        	printButton.addActionListener(this);
        	saveButton.addActionListener(this);
        	closeButton.addActionListener(this);
        	buttonPanel.add(copyButton);
        	buttonPanel.add(printButton);
        	buttonPanel.add(saveButton);
        	buttonPanel.add(closeButton);
            this.add(buttonPanel, "South");
        }

//        getContentPane().add(new JScrollPane(getTextArea()), "Center");
        this.add(createSearchableTextArea(textArea), "Center");


        getTextArea().setSelectionStart(0);
        getTextArea().setSelectionEnd(0);

    }
    
    
    private JPanel createSearchableTextArea(final JTextArea textArea) {
        final JPanel panel = new JPanel(new BorderLayout());
        scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        if (searchBarProps != SearchBarProperties.NO_SEARCH_BAR) {
        
        	Searchable searchable = SearchableUtils.installSearchable(textArea);
        	searchable.setRepeats(true);
        	SearchableBar textAreaSearchableBar = SearchableBar.install(searchable, 
        			KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), new SearchableBar.Installer() {
        		public void openSearchBar(SearchableBar searchableBar) {
        			String selectedText = textArea.getSelectedText();
        			if (selectedText != null && selectedText.length() > 0) {
        				searchableBar.setSearchingText(selectedText);
        			}
        			panel.add(searchableBar, BorderLayout.AFTER_LAST_LINE);
        			panel.invalidate();
        			panel.revalidate();
        		}

        		public void closeSearchBar(SearchableBar searchableBar) {
        			panel.remove(searchableBar);
        			panel.invalidate();
        			panel.revalidate();
        		}
        	});
        	if (searchBarProps == SearchBarProperties.SEARCH_BAR_COMPACT) {
        		textAreaSearchableBar.setCompact(true);
        	}
        	
        	textAreaSearchableBar.getInstaller().openSearchBar(textAreaSearchableBar);
        
        }
        return panel;
    }

    public void addCloseButtonActionListener(ActionListener l) {
    	closeButton.addActionListener(l);
    }
    
    
    

    // Implementation of ActionListener interface.
    public void actionPerformed(ActionEvent event) {

        Object source = event.getSource();

        if (source == closeButton) {
        	
        }
        else if (source == copyButton) {            
        	
        	String str = textArea.getSelectedText();
        	if (str == null || str.length() <= 0) {
        		str = textArea.getText();
        	}
        	
            TextUtils.getInstance().copyToClipboard(str);
            
        }
        else if (source == printButton) {

            try {

            	TextUtils.getInstance().print(textArea);
            	
            } catch (PrinterException e) {
    			e.printStackTrace();
                JOptionPane.showMessageDialog(this, e);
			}


        }
        else if (source == saveButton) {

            try {

                TextUtils.getInstance().save(this, textData, "txt", "Text File (.txt)");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, e);
            }
        }

    }



    public JTextArea getTextArea() {
        return textArea;
    }
    
    public JScrollPane getScrollPane() {
    	return scrollPane;
    }



}



