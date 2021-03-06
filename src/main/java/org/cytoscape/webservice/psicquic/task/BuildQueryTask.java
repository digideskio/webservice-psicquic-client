package org.cytoscape.webservice.psicquic.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient;
import org.cytoscape.webservice.psicquic.PSICQUICRestClient.SearchMode;
import org.cytoscape.webservice.psicquic.RegistryManager;
import org.cytoscape.webservice.psicquic.mapper.CyNetworkBuilder;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

/*
 * #%L
 * Cytoscape PSIQUIC Web Service Impl (webservice-psicquic-client-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2017 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 * Create query based on the selected node
 */
public class BuildQueryTask extends AbstractTask {
	
	@ProvidesTitle
	public String getTitle() {
		return "Extend Network by PSICQUIC Web Services";
	}
	
	@Tunable(description = "Select Query Column:")
	public ListSingleSelection<String> columnList;

	private PSICQUICRestClient client;
	private final RegistryManager manager;

	private final CyTable table;
	private final View<CyNode> nodeView;

	private final CyNetworkBuilder builder;
	private final CyNetworkView netView;

	private final Map<String, CyColumn> colName2column;
	
	private final CyServiceRegistrar serviceRegistrar;

	BuildQueryTask(
			final CyNetworkView netView,
			final View<CyNode> nodeView,
			final PSICQUICRestClient client,
			final RegistryManager manager,
			final CyNetworkBuilder builder,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.table = netView.getModel().getDefaultNodeTable();
		this.nodeView = nodeView;
		this.manager = manager;
		this.client = client;
		this.builder = builder;
		this.netView = netView;
		this.serviceRegistrar = serviceRegistrar;

		colName2column = new HashMap<>();
		final Collection<CyColumn> columns = table.getColumns();
		final CyRow row = table.getRow(nodeView.getModel().getSUID());

		String defaultSelection = null;
		boolean alreadySet = false;
		
		for (CyColumn col : columns) {
			final Object val = row.get(col.getName(), col.getType());
			
			if (val != null && col.getType() == String.class) {
				final String labelString = col.getName() + " (" + val.toString() + ")";
				colName2column.put(labelString, col);
				
				if (col.getName().equals(CyNetwork.NAME) && !alreadySet )
					defaultSelection = labelString;
				else if( col.getName().equals("identifier") )
					defaultSelection = labelString;
			}
		}
		
		columnList = new ListSingleSelection<>(new ArrayList<>(colName2column.keySet()));

		if (defaultSelection != null)
			columnList.setSelectedValue(defaultSelection);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		final String selectedStr = columnList.getSelectedValue();
		final CyColumn selected = colName2column.get(selectedStr);
		final Object value = table.getRow(nodeView.getModel().getSUID()).get(selected.getName(), selected.getType());

		if (value == null)
			throw new NullPointerException("Selected column value is null: " + selected.getName());

		final String query = value.toString();
		SearchRecordsTask searchTask = new SearchRecordsTask(client, SearchMode.INTERACTOR);
		final Map<String, String> activeSource = manager.getActiveServices();
		searchTask.setQuery(query);
		searchTask.setTargets(activeSource.values());

		final ProcessSearchResultTask expandTask = new ProcessSearchResultTask(query, client, searchTask, netView,
				nodeView, manager, builder, serviceRegistrar);

		insertTasksAfterCurrentTask(searchTask, expandTask);
	}
}
