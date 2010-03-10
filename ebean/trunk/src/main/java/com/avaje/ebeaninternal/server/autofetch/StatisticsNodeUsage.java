package com.avaje.ebeaninternal.server.autofetch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.meta.MetaAutoFetchStatistic.NodeUsageStats;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssoc;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;

/**
 * Collects usages statistics for a given node in the object graph.
 */
public class StatisticsNodeUsage implements Serializable {

	private static final long serialVersionUID = -1663951463963779547L;

	private static final Logger logger = Logger.getLogger(StatisticsNodeUsage.class.getName());

	private final String monitor = new String();
	
	private final String path;
	
	private final boolean queryTuningAddVersion;
	
	private int profileCount;
	
	private int profileUsedCount;
	
	private boolean modified;
	
	private Set<String> aggregateUsed = new LinkedHashSet<String>();

	public StatisticsNodeUsage(String path, boolean queryTuningAddVersion) {
		this.path = path;
		this.queryTuningAddVersion = queryTuningAddVersion;
	}
	
	public NodeUsageStats createPublicMeta() {
		synchronized(monitor){
			String[] usedProps = aggregateUsed.toArray(new String[aggregateUsed.size()]);
			return new NodeUsageStats(path, profileCount, profileUsedCount, usedProps);
		}
	}
	
	
	public void buildTunedFetch(OrmQueryDetail detail, BeanDescriptor<?> rootDesc) {
		
		synchronized(monitor){
							
			BeanDescriptor<?> desc = rootDesc;
			if (path != null){
				ElPropertyValue elGetValue = rootDesc.getElGetValue(path);
				if (elGetValue == null){
					desc = null;
					logger.warning("Can't find join for path["+path+"] for "+rootDesc.getName());
					
				} else {
					BeanProperty beanProperty = elGetValue.getBeanProperty();
					if (beanProperty instanceof BeanPropertyAssoc<?>){
						desc = ((BeanPropertyAssoc<?>) beanProperty).getTargetDescriptor();
					}
				}
			}
			if ((modified || queryTuningAddVersion) && desc != null){
				BeanProperty[] versionProps = desc.propertiesVersion();
				if (versionProps.length > 0){
					aggregateUsed.add(versionProps[0].getName());
				}
			}
			
			String propList = aggregateUsed.toString();
			// chop of leading "[" and trailing "]" characters
			propList = propList.substring(1, propList.length()-1);
							
			if (path == null){
				detail.select(propList);
			} else {
				detail.addJoin(path, propList, null);
			}
		}
	}
	
	public void publish(NodeUsageCollector profile) {
		
		synchronized(monitor){
			
			HashSet<String> used = profile.getUsed();
			
			profileCount++;
			if (!used.isEmpty()){
				profileUsedCount++;
				aggregateUsed.addAll(used);
			}
			if (profile.isModified()){
				modified = true;
			}
		}
	}
	
	public String toString() {
		return "path["+path+"] profileCount["+profileCount+"] used["+profileUsedCount+"] props"+aggregateUsed;
	}
}