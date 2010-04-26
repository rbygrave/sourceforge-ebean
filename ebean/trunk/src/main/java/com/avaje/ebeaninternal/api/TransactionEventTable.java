package com.avaje.ebeaninternal.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TransactionEventTable implements Serializable {

	private static final long serialVersionUID = 2236555729767483264L;
	
	private final Map<String, TableIUD> map = new HashMap<String, TableIUD>();
	
    public String toString() {
        return "TransactionEventTable " + map.values();
    }

	public void add(TransactionEventTable table){
		
		for (TableIUD iud : table.values()) {
			add(iud);
		}
	}

	public void add(String table, boolean insert, boolean update, boolean delete){

		table = table.toUpperCase();
		
		add(new TableIUD(table, insert, update, delete));
	}
	
	public void add(TableIUD newTableIUD){

		TableIUD existingTableIUD = map.put(newTableIUD.getTable(), newTableIUD);
		if (existingTableIUD != null){
			newTableIUD.add(existingTableIUD);
		}
	}
	
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public Collection<TableIUD> values() {
		return map.values();
	}
	
	public static class TableIUD implements Serializable {
		
		private static final long serialVersionUID = -1958317571064162089L;
		
		private String table;
		private boolean insert;
		private boolean update;
		private boolean delete;
		
		private TableIUD(String table, boolean insert, boolean update, boolean delete){
			this.table = table;
			this.insert = insert;
			this.update = update;
			this.delete = delete;
		}
		
		public String toString() {
		    return "TableIUD "+table+" i:"+insert+" u:"+update+" d:"+delete;
		}
		
		private void add(TableIUD other) {
			if (other.insert){
				insert = true;
			}
			if (other.update){
				update = true;
			}
			if (other.delete){
				delete = true;
			}
		}

		public String getTable() {
			return table;
		}

		public boolean isInsert() {
			return insert;
		}

		public boolean isUpdate() {
			return update;
		}

		public boolean isDelete() {
			return delete;
		}
		
		public boolean isUpdateOrDelete() {
			return update || delete;
		}
	}
}
