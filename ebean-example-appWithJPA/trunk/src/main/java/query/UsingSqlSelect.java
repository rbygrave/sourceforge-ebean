package query;

import java.util.List;

import app.data.Bug;
import app.data.BugReport;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

/**
 * Some examples of using BugReport which is a bean based on Sql and SqlSelect.
 */
public class UsingSqlSelect {

	public static void main(String[] args) {

		showsSqlSelectOnNormalEntityBean();

		showsDefaultQuery();

		showsNamedQuery();

		showsExtendAndQueryParameters();
	}

	/**
	 * Bug is a normal entity bean, with a simple SqlSelect named query. The
	 * goal is for the raw sql query to be used just like a Ebean generated
	 * query.
	 */
	private static void showsSqlSelectOnNormalEntityBean() {

		Query<Bug> query = Ebean.createQuery(Bug.class, "simple");
		query.where().eq("status.code", "ACTIVE");

		List<Bug> list = query.findList();

		System.out.println(list);
		
		for (Bug bug : list) {
			String body = bug.getBody();
			System.out.println(body);
		}

	}

	private static void showsDefaultQuery() {

		Query<BugReport> query = Ebean.createQuery(BugReport.class);
		query.where().eq("status.code", "ACTIVE");
		query.having().gt("count", 0);

		List<BugReport> list = query.findList();

		System.out.println(list);

	}

	/**
	 * A named query other than the default.
	 */
	private static void showsNamedQuery() {

		Query<BugReport> queryAssigned = Ebean.createQuery(BugReport.class, "withAssigned");
		queryAssigned.where().eq("assigned.id", 1);

		List<BugReport> assigned = queryAssigned.findList();
		System.out.println("" + assigned);

		for (BugReport bugReport : assigned) {
			// this will invoke lazy loading of the user name
			String name = bugReport.getAssigned().getName();
			// this will invoke lazy loading of the status title
			String status = bugReport.getStatus().getTitle();
			int count = bugReport.getCount();

			System.out.println(" assigned: " + count + " to " + name + " as " + status);
		}
	}

	/**
	 * A named query that extends another query. Also has where and having
	 * parameters.
	 */
	private static void showsExtendAndQueryParameters() {

		Query<BugReport> queryNoneAssigned = Ebean.createQuery(BugReport.class,
			"assignedStatusCount");
		queryNoneAssigned.setParameter("status", "NEW");
		queryNoneAssigned.setParameter("count", 0);
		queryNoneAssigned.where().isNotNull("assigned.id");

		List<BugReport> newBugsAssigned = queryNoneAssigned.findList();

		System.out.println("");
		System.out.println("");
		System.out.println("");

		for (BugReport bugReport : newBugsAssigned) {
			Integer userId = bugReport.getAssigned().getId();
			int count = bugReport.getCount();
			String status = bugReport.getStatus().getCode();

			System.out.println(" new bugs assigned: " + count + " to " + userId + " as " + status);
		}
	}

}
