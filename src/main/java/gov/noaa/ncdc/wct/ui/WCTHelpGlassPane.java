package gov.noaa.ncdc.wct.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXHyperlink;

public class WCTHelpGlassPane extends JPanel {

	private WCTViewer viewer;
	
	private JXHyperlink closeLink = new JXHyperlink();
	
	public WCTHelpGlassPane(WCTViewer viewer) {
		super();
		
		this.viewer = viewer;
		createPane();
		setOpaque(false);
		setVisible(true);
		
		repaint();
	}
	
	
	private void createPane() {
		
		closeLink.setText("[ Close ]");
		closeLink.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				viewer.setGlassPane(null);
			}
			
		});
		
		this.add(new JLabel("THis button does this stuff ...."));
		
		this.add(closeLink);
		
		validate();
	}
	

	public void paintComponent(Graphics g) {
		g.setColor(Color.CYAN);
		
		g.drawString("TEST TEST TEST", 50, 50);
		
		System.out.println("paintComponent is called");
	}
}
