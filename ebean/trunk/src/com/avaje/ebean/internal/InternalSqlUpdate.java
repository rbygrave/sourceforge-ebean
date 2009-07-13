package com.avaje.ebean.internal;

import com.avaje.ebean.SqlUpdate;

public interface InternalSqlUpdate extends SqlUpdate {

	public BindParams getBindParams();
}
