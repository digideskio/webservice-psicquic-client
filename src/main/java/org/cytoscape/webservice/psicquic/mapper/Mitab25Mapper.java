package org.cytoscape.webservice.psicquic.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableEntry;

/**
 * Map minimal set of information from MITAB25.
 * 
 */
public class Mitab25Mapper {

	// Separator for multiple entries.
	private static final String SEPARATOR = "\\|";
	private static final String ATTR_PREFIX = "PSI-MI-25.";

	private static final int COLUMN_COUNT = 15;

	// Reg.Ex for parsing entry
	private final static Pattern miPttr = Pattern.compile("MI:\\d{4}");
	private final static Pattern miNamePttr = Pattern.compile("\\(.+\\)");

	private static final String TAB = "\t";

	// Attr Names
	private static final String DATABASE_UNIQUE_ID = ATTR_PREFIX + "database-unique ID";
	private static final String DETECTION_METHOD = ATTR_PREFIX + "interaction detection method";
	private static final String INTERACTION_TYPE = ATTR_PREFIX + "interaction type";
	private static final String SOURCE_DB = ATTR_PREFIX + "source database";
	private static final String INTERACTION_ID = ATTR_PREFIX + "Interaction ID";
	private static final String EDGE_SCORE = ATTR_PREFIX + "confidence score";

	// Stable IDs which maybe used for mapping later
	private static final String UNIPROT = "uniprotkb";
	private static final String ENTREZ_GENE = "entrezgene/locuslink";
	private static final String ENTREZ_GENE_SYN = "entrez gene/locuslink";

	private static final String CHEBI = "chebi";

	private static final String INTERACTOR_TYPE = ATTR_PREFIX + "interactor type";
	private static final String COMPOUND = "compound";

	private Matcher matcher;

	
	
	private final CyNetwork network;
	private final Map<String, CyNode> nodeMap;
	
	public Mitab25Mapper(final CyNetwork network) {
		this.network = network;
		this.nodeMap = new HashMap<String, CyNode>();
		
		// Create Columns
		network.getDefaultNodeTable().createColumn(INTERACTOR_TYPE, String.class, true);
		network.getDefaultNodeTable().createColumn(DATABASE_UNIQUE_ID, String.class, true);
		network.getDefaultEdgeTable().createColumn(INTERACTION_ID, String.class, true);
	}

	public void parse(final String line) {
		
		String[] sourceID;
		String[] targetID;

		String[] detectionMethods;
		CyNode source;
		CyNode target;
		CyEdge e;

		String[] sourceDB;
		String[] interactionID;
		String[] interactionType;

		String[] edgeScore;

		
		final String[] entry = line.split(TAB);

		// Validate entry list.
		if (entry == null || entry.length < COLUMN_COUNT)
			return;

		sourceID = entry[0].split(SEPARATOR);
		final String sourceIDHalf = sourceID[0].split(":")[1];
		targetID = entry[1].split(SEPARATOR);
		final String targetIDHalf = targetID[0].split(":")[1];

		// Create source and target node if necessary.
		source = nodeMap.get(sourceID[0]);
		if(source == null) {
			source = network.addNode();
			source.getCyRow().set(CyTableEntry.NAME, sourceIDHalf);
			source.getCyRow().set(DATABASE_UNIQUE_ID, sourceID[0]);
			nodeMap.put(sourceID[0], source);
		}
		target = nodeMap.get(targetID[0]);
		if (target == null) {
			target = network.addNode();
			target.getCyRow().set(CyTableEntry.NAME, targetIDHalf);
			target.getCyRow().set(DATABASE_UNIQUE_ID, targetID[0]);
			nodeMap.put(targetID[0], target);
		}
		
		// Set type if not protein
		if (sourceID[0].contains(CHEBI))
			source.getCyRow().set(INTERACTOR_TYPE, COMPOUND);
		if (targetID[0].contains(CHEBI))
			target.getCyRow().set(INTERACTOR_TYPE, COMPOUND);

//		// Aliases
//		setAliases(nodeAttr, source.getIdentifier(), entry[0].split(SEPARATOR));
//		setAliases(nodeAttr, target.getIdentifier(), entry[1].split(SEPARATOR));
//		setAliases(nodeAttr, source.getIdentifier(), entry[2].split(SEPARATOR));
//		setAliases(nodeAttr, target.getIdentifier(), entry[3].split(SEPARATOR));
//		setAliases(nodeAttr, source.getIdentifier(), entry[4].split(SEPARATOR));
//		setAliases(nodeAttr, target.getIdentifier(), entry[5].split(SEPARATOR));
//
//		// Tax ID (pick first one only)
//		setTaxID(nodeAttr, source.getIdentifier(), entry[9].split(SEPARATOR)[0]);
//		setTaxID(nodeAttr, target.getIdentifier(), entry[10].split(SEPARATOR)[0]);

//		sourceDB = entry[12].split(SEPARATOR);
		interactionID = entry[13].split(SEPARATOR);
		edgeScore = entry[14].split(SEPARATOR);

		detectionMethods = entry[6].split(SEPARATOR);
		interactionType = entry[11].split(SEPARATOR);
		e = network.addEdge(source, target, true);
		e.getCyRow().set(
				CyTableEntry.NAME,
				source.getCyRow().get(CyTableEntry.NAME, String.class) + " (" + interactionID[0] + ") "
						+ target.getCyRow().get(CyTableEntry.NAME, String.class));
		e.getCyRow().set(CyEdge.INTERACTION, interactionID[0]);

//		setEdgeListAttribute(edgeAttr, e.getIdentifier(), interactionType, INTERACTION_TYPE);
//		setEdgeListAttribute(edgeAttr, e.getIdentifier(), detectionMethods, DETECTION_METHOD);
//		setEdgeListAttribute(edgeAttr, e.getIdentifier(), sourceDB, SOURCE_DB);
//
//		// Map scores
//		setEdgeScoreListAttribute(edgeAttr, e.getIdentifier(), edgeScore, EDGE_SCORE);
//
		e.getCyRow().set(INTERACTION_ID, interactionID[0]);
//
//		setPublication(edgeAttr, e.getIdentifier(), entry[8].split(SEPARATOR), entry[7].split(SEPARATOR));

	}


//	private void setTaxID(String id, String value) {
//		String[] buf = value.split(":", 2);
//		String attrName;
//		String taxonName;
//		if (buf != null && buf.length == 2) {
//			attrName = ATTR_PREFIX + buf[0];
//
//			matcher = miNamePttr.matcher(buf[1]);
//			if (matcher.find()) {
//				taxonName = matcher.group();
//				attr.setAttribute(id, attrName, buf[1].split("\\(")[0]);
//				attr.setAttribute(id, attrName + ".name", taxonName.substring(1, taxonName.length() - 1));
//			} else {
//				attr.setAttribute(id, attrName, buf[1]);
//			}
//		}
//	}
//
//	private void setPublication(CyAttributes attr, String id, String[] pubID, String[] authors) {
//		String key = null;
//		String[] temp;
//
//		for (String val : pubID) {
//			temp = val.split(":", 2);
//			if (temp == null || temp.length < 2)
//				continue;
//
//			key = ATTR_PREFIX + temp[0];
//			listAttrMapper(attr, key, id, temp[1]);
//		}
//
//		for (String val : authors) {
//			key = ATTR_PREFIX + "author";
//			listAttrMapper(attr, key, id, val);
//		}
//	}
//
//	private void setAliases(final String id, final String[] entry) {
//		String key = null;
//		String[] temp;
//		String value;
//
//		for (String val : entry) {
//			temp = val.split(":", 2);
//			if (temp == null || temp.length < 2)
//				continue;
//
//			key = ATTR_PREFIX + temp[0];
//			value = temp[1].replaceAll("\\(.+\\)", "");
//			listAttrMapper(attr, key, id, value);
//		}
//	}
//
//	private void setEdgeListAttribute(CyAttributes attr, String id, String[] entry, String key) {
//
//		String value;
//		String name;
//
//		for (String val : entry) {
//			value = trimPSITerm(val);
//			name = trimPSIName(val);
//
//			listAttrMapper(attr, key, id, value);
//			listAttrMapper(attr, key + ".name", id, name);
//		}
//	}
//
//	// Special case for edge scores
//	private void setEdgeScoreListAttribute(CyAttributes attr, String id, String[] entry, String key) {
//
//		String scoreString;
//		String scoreType;
//
//		for (String val : entry) {
//			final String[] parts = val.split(":");
//			if (parts == null || parts.length != 2)
//				continue;
//
//			scoreString = parts[1];
//			scoreType = parts[0];
//
//			try {
//				final Double score = Double.parseDouble(scoreString);
//				edgeAttr.setAttribute(id, key + "." + scoreType, score);
//			} catch (Exception e) {
//				if (scoreString != null && scoreString.trim().equals("") == false)
//					edgeAttr.setAttribute(id, key + "." + scoreType, scoreString);
//
//				continue;
//			}
//		}
//	}
//

//
//	private String trimPSITerm(String original) {
//		String miID = null;
//
//		matcher = miPttr.matcher(original);
//
//		if (matcher.find()) {
//			miID = matcher.group();
//		} else {
//			miID = "-";
//		}
//
//		return miID;
//	}
//
//	private String trimPSIName(String original) {
//		String miName = null;
//
//		matcher = miNamePttr.matcher(original);
//
//		if (matcher.find()) {
//			miName = matcher.group();
//			miName = miName.substring(1, miName.length() - 1);
//		} else {
//			miName = "-";
//		}
//
//		return miName;
//	}

}
