package gov.noaa.ncdc.wct.ui;

import gov.noaa.ncdc.common.RiverLayout;
import gov.noaa.ncdc.nexradiv.legend.CategoryLegendImageProducer;
import gov.noaa.ncdc.wct.WCTException;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class LegendEditor extends JDialog {

    private WCTViewer viewer;
    
    private static LegendEditor singleton = null;
    
        
    private boolean engageCustomLegendOverride = false;
    
    
    public static LegendEditor getInstance(final Frame parent, final WCTViewer viewer) {
    	if (singleton == null) {    		
    		singleton = new LegendEditor(parent, viewer);
    	}
    	return singleton;
    }
    
    
    private LegendEditor(Frame parent, WCTViewer viewer) {      
        super(parent, "Legend Editor", false);
        this.viewer = viewer;
        createGUI();
        pack();
    }
    
    
    
    private void createGUI() {
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new RiverLayout());
        
        final JTextField jtfTitle = new JTextField(50);
        jtfTitle.addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyTyped(KeyEvent evt) {
				try {
					CategoryLegendImageProducer legendProducer = viewer.getGridSatelliteLegendImageProducer();
					legendProducer.setLegendTitle(new String[] { jtfTitle.getText() + evt.getKeyChar()});
					viewer.getGridSatelliteLegend().setImage(legendProducer.createMediumLegendImage());
					viewer.getGridSatelliteLegend().repaint();
					viewer.getMapPane().repaint();
				} catch (WCTException e) {
					e.printStackTrace();
				}
			}			
		});
        
        
        final JCheckBox jcbEngage = new JCheckBox("Engage Legend Override");
        jcbEngage.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent evt) {
				engageCustomLegendOverride = jcbEngage.isSelected();
			}
		});
        
        mainPanel.add(jtfTitle, "hfill br");
        mainPanel.add(jcbEngage, "br");
        
        
        this.getContentPane().add(mainPanel);
        
    }


	public boolean isEngageCustomLegendOverride() {
		return engageCustomLegendOverride;
	}


	public void setEngageCustomLegendOverride(boolean engageCustomLegendOverride) {
		this.engageCustomLegendOverride = engageCustomLegendOverride;
	}
}
