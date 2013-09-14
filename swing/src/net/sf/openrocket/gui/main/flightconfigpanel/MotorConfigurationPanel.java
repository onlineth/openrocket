package net.sf.openrocket.gui.main.flightconfigpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import net.sf.openrocket.gui.dialogs.flightconfiguration.IgnitionSelectionDialog;
import net.sf.openrocket.gui.dialogs.motor.MotorChooserDialog;
import net.sf.openrocket.gui.util.GUIUtil;
import net.sf.openrocket.motor.Motor;
import net.sf.openrocket.rocketcomponent.IgnitionConfiguration;
import net.sf.openrocket.rocketcomponent.MotorConfiguration;
import net.sf.openrocket.rocketcomponent.MotorMount;
import net.sf.openrocket.rocketcomponent.Rocket;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.unit.UnitGroup;
import net.sf.openrocket.util.Chars;
import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.Pair;

public class MotorConfigurationPanel extends FlightConfigurablePanel<MotorMount> {
	
	private static final String NONE = trans.get("edtmotorconfdlg.tbl.None");
	
	private final JButton selectMotorButton, removeMotorButton, selectIgnitionButton, resetIgnitionButton;
	
	protected final JTable configurationTable;
	protected final MotorConfigurationTableModel configurationTableModel;

	MotorConfigurationPanel(final FlightConfigurationPanel flightConfigurationPanel, Rocket rocket) {
		super(flightConfigurationPanel,rocket);
		
		//// Motor selection table.
		configurationTableModel = new MotorConfigurationTableModel(rocket);
		configurationTable = new JTable(configurationTableModel);
		configurationTable.setCellSelectionEnabled(true);
		configurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		configurationTable.setDefaultRenderer(Object.class, new MotorTableCellRenderer());
		
		configurationTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				updateButtonState();
				int selectedColumn = configurationTable.getSelectedColumn();
				if (e.getClickCount() == 2) {
					if (selectedColumn > 0) {
						selectMotor();
					}
				}
			}
		});
		
		configurationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if ( e.getValueIsAdjusting() ) {
					return;
				}
				int firstrow = e.getFirstIndex();
				int lastrow = e.getLastIndex();
				ListSelectionModel model = (ListSelectionModel) e.getSource();
				for( int row = firstrow; row <= lastrow; row ++) {
					if ( model.isSelectedIndex(row) ) {
						String id = (String) configurationTableModel.getValueAt(row, 0);
						flightConfigurationPanel.setCurrentConfiguration(id);
						return;
					}
				}
			}
			
		});
		
		JScrollPane scroll = new JScrollPane(configurationTable);
		this.add(scroll, "grow, wrap");
		
		//// Select motor
		selectMotorButton = new JButton(trans.get("MotorConfigurationPanel.btn.selectMotor"));
		selectMotorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectMotor();
			}
		});
		this.add(selectMotorButton, "split, sizegroup button");
		
		//// Remove motor button
		removeMotorButton = new JButton(trans.get("MotorConfigurationPanel.btn.removeMotor"));
		removeMotorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeMotor();
			}
		});
		this.add(removeMotorButton, "sizegroup button");
		
		//// Select Ignition button
		selectIgnitionButton = new JButton(trans.get("MotorConfigurationPanel.btn.selectIgnition"));
		selectIgnitionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectIgnition();
			}
		});
		this.add(selectIgnitionButton, "sizegroup button");
		
		//// Reset Ignition button
		resetIgnitionButton = new JButton(trans.get("MotorConfigurationPanel.btn.resetIgnition"));
		resetIgnitionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetIgnition();
			}
		});
		this.add(resetIgnitionButton, "sizegroup button, wrap");
		
		updateButtonState();
		
	}
	
	@Override
	protected JTable getTable() {
		return configurationTable;
	}

	public void fireTableDataChanged() {
		int selected = configurationTable.getSelectedRow();
		configurationTableModel.fireTableDataChanged();
		if (selected >= 0) {
			selected = Math.min(selected, configurationTable.getRowCount() - 1);
			configurationTable.getSelectionModel().setSelectionInterval(selected, selected);
		}
		updateButtonState();
	}
	
	private void updateButtonState() {
		String currentID = rocket.getDefaultConfiguration().getFlightConfigurationID();
		MotorMount currentMount = getSelectedComponent();
		selectMotorButton.setEnabled(currentMount != null && currentID != null);
		removeMotorButton.setEnabled(currentMount != null && currentID != null);
		selectIgnitionButton.setEnabled(currentMount != null && currentID != null);
		resetIgnitionButton.setEnabled(currentMount != null && currentID != null);
	}
	
	
	private void selectMotor() {
		String id = rocket.getDefaultConfiguration().getFlightConfigurationID();
		MotorMount mount = getSelectedComponent();
		if (id == null || mount == null)
			return;
		
		MotorConfiguration config = mount.getMotorConfiguration().get(id);
		
		MotorChooserDialog dialog = new MotorChooserDialog(
				config.getMotor(),
				config.getEjectionDelay(),
				mount.getMotorMountDiameter(),
				SwingUtilities.getWindowAncestor(flightConfigurationPanel));
		dialog.setVisible(true);
		Motor m = dialog.getSelectedMotor();
		double d = dialog.getSelectedDelay();
		
		if (m != null) {
			config = new MotorConfiguration();
			config.setMotor(m);
			config.setEjectionDelay(d);
			mount.getMotorConfiguration().set(id, config);
		}
		
		fireTableDataChanged();
	}
	
	private void removeMotor() {
		String id = rocket.getDefaultConfiguration().getFlightConfigurationID();
		MotorMount mount = getSelectedComponent();
		if (id == null || mount == null)
			return;
		
		mount.getMotorConfiguration().resetDefault(id);
		
		fireTableDataChanged();
	}
	
	private void selectIgnition() {
		String currentID = rocket.getDefaultConfiguration().getFlightConfigurationID();
		MotorMount currentMount = getSelectedComponent();
		if (currentID == null || currentMount == null)
			return;
		
		IgnitionSelectionDialog dialog = new IgnitionSelectionDialog(
				SwingUtilities.getWindowAncestor(this.flightConfigurationPanel),
				rocket,
				currentMount);
		dialog.setVisible(true);
		
		fireTableDataChanged();
	}
	
	
	private void resetIgnition() {
		String currentID = rocket.getDefaultConfiguration().getFlightConfigurationID();
		MotorMount currentMount = getSelectedComponent();
		if (currentID == null || currentMount == null)
			return;
		
		currentMount.getIgnitionConfiguration().resetDefault(currentID);
		
		fireTableDataChanged();
	}
	
	
	private class MotorTableCellRenderer extends DefaultTableCellRenderer {
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!(c instanceof JLabel)) {
				return c;
			}
			JLabel label = (JLabel) c;
			
			switch (column) {
			case 0: {
				label.setText(descriptor.format(rocket, (String) value));
				return label;
			}
			default: {
				Pair<String, MotorMount> v = (Pair<String, MotorMount>) value;
				String id = v.getU();
				MotorMount mount = v.getV();
				MotorConfiguration motorConfig = mount.getMotorConfiguration().get(id);
				String motorString = getMotorSpecification(mount, motorConfig);
				String ignitionString = getIgnitionEventString(id, mount);
				label.setText(motorString + " " + ignitionString);
				return label;
			}
			}
		}
		
		private String getMotorSpecification(MotorMount mount, MotorConfiguration motorConfig) {
			Motor motor = motorConfig.getMotor();
			
			if (motor == null)
				return NONE;
			
			String str = motor.getDesignation(motorConfig.getEjectionDelay());
			int count = getMountMultiplicity(mount);
			if (count > 1) {
				str = "" + count + Chars.TIMES + " " + str;
			}
			return str;
		}
		
		private int getMountMultiplicity(MotorMount mount) {
			RocketComponent c = (RocketComponent) mount;
			return c.toAbsolute(Coordinate.NUL).length;
		}
		
		
		
		private String getIgnitionEventString(String id, MotorMount mount) {
			IgnitionConfiguration ignitionConfig = mount.getIgnitionConfiguration().get(id);
			IgnitionConfiguration.IgnitionEvent ignitionEvent = ignitionConfig.getIgnitionEvent();
			
			Double ignitionDelay = ignitionConfig.getIgnitionDelay();
			boolean isDefault = mount.getIgnitionConfiguration().isDefault(id);
			
			String str = trans.get("MotorMount.IgnitionEvent.short." + ignitionEvent.name());
			if (ignitionDelay > 0.001) {
				str = str + " + " + UnitGroup.UNITS_SHORT_TIME.toStringUnit(ignitionDelay);
			}
			if (isDefault) {
				String def = trans.get("MotorConfigurationTableModel.table.ignition.default");
				str = def.replace("{0}", str);
			}
			return str;
		}
		
	}
	
}
