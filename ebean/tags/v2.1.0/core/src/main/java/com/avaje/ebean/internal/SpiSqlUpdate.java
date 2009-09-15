package com.avaje.ebean.internal;

import com.avaje.ebean.SqlUpdate;

public interface SpiSqlUpdate extends SqlUpdate {

	public BindParams getBindParams();
}
