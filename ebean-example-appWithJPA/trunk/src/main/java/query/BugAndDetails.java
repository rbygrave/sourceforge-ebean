package query;

import java.util.List;

import app.data.Bug;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.control.ServerControl;
import com.avaje.ebean.meta.MetaQueryStatistic;

public class BugAndDetails {

	public static void main(String[] args) throws Exception {

		ServerControl serverControl = Ebean.getServer(null).getServerControl();
		serverControl.getLogControl().setDebugGeneratedSql(true);

		withAllProperties();

		System.out.println(" ");
		System.out.println(" GAP ");
		System.out.println(" ");

		withPartials();

		System.out.println(" ");
		System.out.println(" GAP ");
		System.out.println(" ");

		withPartialsAsQuery();
		
		
		Query<MetaQueryStatistic> query = Ebean.createQuery(MetaQueryStatistic.class);
		List<MetaQueryStatistic> list = query.findList();
		for (MetaQueryStatistic metaQueryStatistic : list) {
			System.out.println(metaQueryStatistic);
		}

	}

	

	/**
	 * With the "*" all the properties of the associated beans are fetched...
	 */
	public static void withAllProperties() {

		Query<Bug> query = Ebean.createQuery(Bug.class);
		query.setAutoFetch(false);
		query.join("details");
		query.join("details.user", "*");
		query.join("product", "*");

		// query.addWhere(Expr.eq("id", 300));
		// query.addWhere(Expr.ilike("product.name", "code%"));

		List<Bug> list = query.findList();
		System.out.println(list.size());
		System.out.println(list);
	}

	/**
	 * 
	 * <sql summary='[app.data.Bug, product] +many[details, details.user]'>
	 * SELECT b.id, b.body, b.cretime, b.duplicate_of, b.fixed_version,
	 * b.reported_version, b.resolution, b.title, b.updtime, b.priority_code ,
	 * ap.id, ap.cretime, ap.name, ap.sort_order, ap.updtime, b.status_code,
	 * b.type_code, b.user_assigned_id, b.user_logged_id , ed.id, ed.body,
	 * ed.cretime, ed.post_date, ed.title, ed.updtime, ed.bug_id , euu.id,
	 * euu.cookie_login, euu.cretime, euu.email, euu.error_count,
	 * euu.last_login, euu.name, euu.pwd, euu.reset_code, euu.reset_time,
	 * euu.salt, euu.status_code, euu.updtime FROM b_bug b JOIN b_product ap ON
	 * b.product_id = ap.id LEFT OUTER JOIN b_bug_detail ed ON b.id = ed.bug_id
	 * LEFT OUTER JOIN s_user euu ON ed.user_id = euu.id WHERE LOWER(b.id) <= ?
	 * AND LOWER(ap.name) LIKE ? </sql>
	 * 
	 */

	/**
	 * Only get the user name and email address... the user objects are
	 * "partially" populated.
	 */
	public static void withPartials() {

		Query<Bug> query = Ebean.createQuery(Bug.class);
		query.setAutoFetch(false);
		query.join("details", "*");
		query.join("details.user", "name, email");
		query.join("product", "name");

		query.where().eq("id", 300);
		query.where().ilike("product.name", "code%");

		// query.addWhere(Expr.eq("id", 300));
		// query.addWhere(Expr.ilike("product.name", "code%"));

		List<Bug> list = query.findList();
		System.out.println(list.size());
		System.out.println(list);

		/**
		 * <sql summary='[app.data.Bug, product] +many[details, details.user]'>
		 * SELECT b.id, b.body, b.cretime, b.duplicate_of, b.fixed_version,
		 * b.reported_version, b.resolution, b.title, b.updtime, b.priority_code ,
		 * ap.id, ap.name, b.status_code, b.type_code, b.user_assigned_id,
		 * b.user_logged_id , ed.id, ed.body, ed.cretime, ed.post_date,
		 * ed.title, ed.updtime, ed.bug_id , euu.id, euu.email, euu.name FROM
		 * b_bug b JOIN b_product ap ON b.product_id = ap.id LEFT OUTER JOIN
		 * b_bug_detail ed ON b.id = ed.bug_id LEFT OUTER JOIN s_user euu ON
		 * ed.user_id = euu.id WHERE LOWER(b.id) <= ? AND LOWER(ap.name) LIKE ?
		 * </sql>
		 */
	}

	public static void withPartialsAsQuery() {

		String oql = "find bug " + " join details " + " join details.user (name, email) "
				+ " join product (name) "
				+ " where id > :minId and lower(product.name) like :prodName ";

		// use a named query...
		// Query<Bug> query = Ebean.createQuery(Bug.class, "likeProduct.minId");

		// or build it yourself...
		Query<Bug> query = Ebean.createQuery(Bug.class);
		query.setAutoFetch(false);
		query.setQuery(oql);
		query.set("minId", 1);
		query.set("prodName", "code%");

		// query.setCaseSensitive(false);

		List<Bug> list = query.findList();
		System.out.println(list.size());
		System.out.println(list);

		/**
		 * 
		 * <sql summary='[app.data.Bug, product] +many[details, details.user]'>
		 * SELECT b.id, b.body, b.cretime, b.duplicate_of, b.fixed_version,
		 * b.reported_version, b.resolution, b.title, b.updtime, b.priority_code ,
		 * ap.id, ap.name, b.status_code, b.type_code, b.user_assigned_id,
		 * b.user_logged_id , ed.id, ed.body, ed.cretime, ed.post_date,
		 * ed.title, ed.updtime, ed.bug_id , euu.id, euu.email, euu.name FROM
		 * b_bug b JOIN b_product ap ON b.product_id = ap.id LEFT OUTER JOIN
		 * b_bug_detail ed ON b.id = ed.bug_id LEFT OUTER JOIN s_user euu ON
		 * ed.user_id = euu.id WHERE b.id > ? and lower(ap.name) like ? </sql>
		 * 
		 */
	}
}
