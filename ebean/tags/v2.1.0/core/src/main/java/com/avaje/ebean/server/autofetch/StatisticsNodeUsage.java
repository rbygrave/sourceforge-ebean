package com.avaje.ebean.server.autofetch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.meta.MetaAutoFetchStatistic.NodeUsageStats;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.el.ElPropertyValue;
import com.avaje.ebean.server.querydefn.OrmQueryDetail;

/**
 * Collects usages statistics for a given node in the object graph.
 */
public class StatisticsNodeUsage implements Serializable {

	private static final long serialVersionUID = -1663951463963779545L;

	private static final Logger logger = Logger.getLogger(StatisticsNodeUsage.class.getName());

	private final String monitor = new String();
	
	private final String path;
	
	private int loadCount;
	
	private int usedCount;
	
	private Set<String> aggregateUsed = new LinkedHashSet<String>();

	private Set<String> unusedBeanIndex = new LinkedHashSet<String>();

	public StatisticsNodeUsage(String path) {
		this.path = path;
	}
	
	public NodeUsageStats createPublicMeta() {
		synchronized(monitor){
			String[] usedProps = aggregateUsed.toArray(new String[aggregateUsed.size()]);
			return new NodeUsageStats(path, loadCount, usedCount, usedProps);
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
			if (desc != null){
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
				detail.addFetchJoin(path, propList);
			}
		}
	}
	
	public void publish(NodeUsageCollector profile) {
		
		synchronized(monitor){
			
			HashSet<String> used = profile.getUsed();
			
			loadCount++;
			if (!used.isEmpty()){
				usedCount++;
				aggregateUsed.addAll(used);
			} else {
				// perhaps use this later... object graph use is probably
				// top heavy meaning the first x beans are traversed and then
				// then after that they are unused... 
				String beanIndex = profile.getNode().getBeanIndex();
				unusedBeanIndex.add(beanIndex);
			}
		}
	}
	
	public String toString() {
		return "path["+path+"] load["+loadCount+"] used["+usedCount+"] props"+aggregateUsed;
	}
}