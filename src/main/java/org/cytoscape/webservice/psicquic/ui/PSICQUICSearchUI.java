package org.cytoscape.webservice.psicquic.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.task.create.CreateNetworkViewTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.PSIMI25VisualStyleBuilder;
import org.cytoscape.webservice.psicquic.RegistryManager;
import org.cytoscape.webservice.psicquic.task.SearchRecoredsTask;
import org.cytoscape.webservice.psicquic.ui.SelectorBuilder.Species;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

public class PSICQUICSearchUI extends JPanel {

	private static final long serialVersionUID = 3163269742016489767L;

	private static final String MIQL_REFERENCE_PAGE_URL = "http://code.google.com/p/psicquic/wiki/MiqlReference";

	private static final Dimension PANEL_SIZE = new Dimension(680, 500);
	
	// Color Scheme
	private static final Color MIQL_COLOR = new Color(0x7f, 0xff, 0xd4);
	private static final Color ID_LIST_COLOR = new Color(0xff, 0xa5, 0x00);
	private static final Font STRONG_FONT = new Font("SansSerif", Font.BOLD, 14);

	// Fixed messages
	private static final String MIQL_MODE = "Search by Query Language (MIQL)";
	private static final String INTERACTOR_ID_LIST = "Search by ID (gene/protein/compound ID)";
	private static final String BY_SPECIES = "Search by Species";

	private static final String MIQL_QUERY_AREA_MESSAGE_STRING = "Please enter search query (MIQL) here.  "
			+ "Currently the result table shows number of all binary interactions available in the database.  "
			+ "\nIf you need help, please click Syntax Help button below.";
	private static final String INTERACTOR_LIST_AREA_MESSAGE_STRING = "Please enter list of genes/proteins/compounds, separated by space.  "
			+ "Currently the result table shows number of all binary interactions available in the database.";

	private final RegistryManager regManager;
	private final PSICQUICRestClient client;
	private final TaskManager<?, ?> taskManager;
	private final CyNetworkManager networkManager;
	private final CreateNetworkViewTaskFactory createViewTaskFactory;

	private JEditorPane queryArea;
	private SourceStatusPanel statesPanel;
	private JScrollPane queryScrollPane;

	private JPanel searchPanel;
	private JLabel modeLabel;
	private JButton searchButton;
	private JButton refreshButton;

	private JPanel speciesPanel;
	
	private JComboBox searchModeSelector;
	private JComboBox speciesSelector;

	private SearchMode mode = SearchMode.MIQL;
	private String searchAreaTitle = MIQL_MODE;

	private boolean firstClick = true;

	private final PSIMI25VisualStyleBuilder vsBuilder;
	private final VisualMappingManager vmm;
	private final PSIMITagManager tagManager;

	public PSICQUICSearchUI(final CyNetworkManager networkManager, final RegistryManager regManager,
			final PSICQUICRestClient client, final TaskManager<?, ?> tmManager,
			final CreateNetworkViewTaskFactory createViewTaskFactory,
			final PSIMI25VisualStyleBuilder vsBuilder, final VisualMappingManager vmm, final PSIMITagManager tagManager) {
		this.regManager = regManager;
		this.client = client;
		this.taskManager = tmManager;
		this.networkManager = networkManager;
		this.createViewTaskFactory = createViewTaskFactory;
		this.vmm = vmm;
		this.vsBuilder = vsBuilder;
		this.tagManager = tagManager;

		init();
	}

	private void init() {
		// Background (Base Panel) settings
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBackground(Color.white);
		this.setBorder(new EmptyBorder(10, 10, 10, 10));

		createDBlistPanel();
		createQueryPanel();
		createQueryModePanel();
		createSpeciesPanel();
		
		queryModeChanged();
		
		this.setSize(PANEL_SIZE);
		this.setPreferredSize(PANEL_SIZE);
	}

	private final void createDBlistPanel() {
		// Source Status - list of remote databases
		this.statesPanel = new SourceStatusPanel("", client, regManager, networkManager, null, taskManager, mode,
				createViewTaskFactory, vsBuilder, vmm, tagManager);
		statesPanel.enableComponents(false);
		this.add(statesPanel);
	}

	private final void createQueryPanel() {
		// Query text area
		queryScrollPane = new JScrollPane();
		queryScrollPane.setBackground(Color.white);
		queryArea = new JEditorPane();

		final TitledBorder border = new TitledBorder(searchAreaTitle);
		border.setTitleColor(MIQL_COLOR);
		border.setTitleFont(STRONG_FONT);
		queryScrollPane.setBorder(border);
		queryScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		queryScrollPane.setPreferredSize(new Dimension(500, 150));
		queryScrollPane.setViewportView(queryArea);
		this.add(queryScrollPane);
		
		queryArea.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(firstClick) {
					queryArea.setText("");
					firstClick = false;
					searchButton.setEnabled(true);
				}
			}
		});
	}

	private final void createQueryModePanel() {
		// Query type selector - Gene ID, MIQL, or species
		modeLabel = new JLabel("Search Mode:");
		this.searchModeSelector = new JComboBox();
		this.searchModeSelector.setPreferredSize(new Dimension(200, 30));
		this.searchModeSelector.addItem(BY_SPECIES);
		this.searchModeSelector.addItem(INTERACTOR_ID_LIST);
		this.searchModeSelector.addItem(MIQL_MODE);
		this.searchModeSelector.setSelectedItem(INTERACTOR_ID_LIST);

		this.searchModeSelector.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				queryModeChanged();
			}
		});

		searchPanel = new JPanel();
		searchPanel.setBackground(Color.white);
		searchPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));

		searchButton = new JButton("Search");
		searchButton.setPreferredSize(new java.awt.Dimension(90, 28));
		searchButton.setFont(new Font("SansSerif", Font.BOLD, 12));
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search();
				statesPanel.enableComponents(true);
			}
		});
		searchButton.setEnabled(false);

		refreshButton = new JButton("Refresh");
		refreshButton.setPreferredSize(new java.awt.Dimension(90, 28));
		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				refreshButtonActionPerformed();
			}
		});

		searchPanel.add(modeLabel);
		searchPanel.add(searchModeSelector);
		searchPanel.add(searchButton);
		searchPanel.add(refreshButton);

		this.add(searchPanel);
	}

	private final void createSpeciesPanel() {
		speciesPanel = new JPanel();
		speciesPanel.setBackground(Color.white);
		final JLabel speciesLabel = new JLabel("Select Species:");

		final SelectorBuilder speciesBuilder = new SelectorBuilder();
		speciesSelector = speciesBuilder.getComboBox();
		speciesPanel.setLayout(new BoxLayout(speciesPanel, BoxLayout.X_AXIS));
		speciesPanel.add(speciesLabel);
		speciesPanel.add(speciesSelector);
		
		speciesSelector.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				searchButton.setEnabled(true);
			}
			
		});
	}
	
	
	private void refreshButtonActionPerformed() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				regManager.refresh();
				queryModeChanged();
			}
		});
	}


	private void search() {
		final SearchRecoredsTask searchTask = new SearchRecoredsTask(client, mode);
		final Map<String, String> activeSource = regManager.getActiveServices();
		String query = this.queryArea.getText();
		
		
		// Query by species
		if(mode == SearchMode.SPECIES)
			query = buildSpeciesQuery();
		
		
		statesPanel.setQuery(query);
		
		searchTask.setQuery(query);
		searchTask.setTargets(activeSource.values());

		taskManager.execute(new TaskIterator(searchTask, new SetTableTask(searchTask)));
	}
	
	private final String buildSpeciesQuery() {
		mode = SearchMode.SPECIES;
		final Object selectedItem = this.speciesSelector.getSelectedItem();
		final Species species = (Species) selectedItem;
		
		if (species == Species.ALL) {
			return "*";
		} else {
			return "species:\"" + species.toString() + "\"";
		}
	}

	private final class SetTableTask extends AbstractTask {

		final SearchRecoredsTask searchTask;

		public SetTableTask(final SearchRecoredsTask searchTask) {
			this.searchTask = searchTask;
		}

		@Override
		public void run(TaskMonitor taskMonitor) throws Exception {
			final Map<String, Long> result = searchTask.getResult();
			
			String query;
			// Query by species
			if(mode == SearchMode.SPECIES)
				query = buildSpeciesQuery();
			else {
				query = queryArea.getText();
			}
						
			statesPanel = new SourceStatusPanel(query, client, regManager, networkManager, result,
					taskManager, mode, createViewTaskFactory, vsBuilder, vmm, tagManager);
			statesPanel.sort();
			updateGUILayout();
			statesPanel.enableComponents(true);
		}
	}

	private final void updateGUILayout() {	
		removeAll();
		
		add(statesPanel);
		if(mode == SearchMode.SPECIES)
			add(speciesPanel);
		else
			add(queryScrollPane);
		
		add(searchPanel);
		
		if (getRootPane() != null) {
			Window parentWindow = ((Window) getRootPane().getParent());
			parentWindow.pack();
			repaint();
			parentWindow.toFront();
		}
	}

	private final void queryModeChanged() {
		final Object selectedObject = this.searchModeSelector.getSelectedItem();
		if (selectedObject == null)
			return;
		
		final String modeString = selectedObject.toString();
		final Color borderColor;
		final String query;
		if (modeString.equals(MIQL_MODE)) {
			mode = SearchMode.MIQL;
			searchAreaTitle = MIQL_MODE;
			// speciesSelector.setEnabled(true);
			// helpButton.setEnabled(true);
			queryArea.setText(MIQL_QUERY_AREA_MESSAGE_STRING);
			borderColor = MIQL_COLOR;
			query = queryArea.getText();
		} else if (modeString.equals(INTERACTOR_ID_LIST)) {
			mode = SearchMode.INTERACTOR;
			searchAreaTitle = INTERACTOR_ID_LIST;
			// speciesSelector.setEnabled(false);
			// helpButton.setEnabled(false);
			queryArea.setText(INTERACTOR_LIST_AREA_MESSAGE_STRING);
			borderColor = ID_LIST_COLOR;
			query = queryArea.getText();
		} else {
			mode = SearchMode.SPECIES;
			searchAreaTitle = BY_SPECIES;
			borderColor = MIQL_COLOR;
			query = buildSpeciesQuery();
		}

		firstClick = true;

		final TitledBorder border = new TitledBorder(searchAreaTitle);
		border.setTitleColor(borderColor);
		border.setTitleFont(STRONG_FONT);
		queryScrollPane.setBorder(border);
		
		
		statesPanel = new SourceStatusPanel(query, client, regManager, networkManager, null,
				taskManager, mode, createViewTaskFactory, vsBuilder, vmm, tagManager);
		statesPanel.sort();
		
		updateGUILayout();
		statesPanel.enableComponents(false);
		searchButton.setEnabled(false);
	}
}