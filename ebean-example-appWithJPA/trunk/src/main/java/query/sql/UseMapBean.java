package query.sql;

import java.util.List;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.MapBean;
import com.avaje.ebean.SqlQuery;

public class UseMapBean {

	public static void main(String[] args) {
		
		String sql = "select * from s_user";
		
		SqlQuery sqlQuery = Ebean.createSqlQuery();
		sqlQuery.setQuery(sql);
		List<MapBean> list = sqlQuery.findList();
		
		for (MapBean mapBean : list) {
			System.out.println(mapBean);
		}
		
		System.out.println("done");
	}
}
