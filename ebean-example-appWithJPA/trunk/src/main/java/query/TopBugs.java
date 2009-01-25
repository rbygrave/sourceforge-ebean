package query;

import java.util.ArrayList;
import java.util.List;

import app.data.Bug;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

public class TopBugs {

	public static void main(String[] args) {

		ArrayList<String> statusCodes = new ArrayList<String>();
		statusCodes.add("ACTIVE");
		statusCodes.add("NEW");

		Query<Bug> query = Ebean.createQuery(Bug.class);
		query.join("status", null);
		query.join("priority", null);
		query.join("userLogged", "name, email");
		query.join("userAssigned", "name, email");
		query.join("attachments", null);
		
		// active, new bugs...
		query.where().in("status.code", statusCodes);
		query.where().eq("type.code", "BUG");

		query.setOrderBy("priority.sortOrder ASC");
		query.setMaxRows(10);

		List<Bug> bugs = query.findList();
		System.out.println(""+bugs);
	}

	/**
<sql summary='[app.data.Bug, priority, status, userAssigned, userLogged] +many[attachments]'>
SELECT b.id, b.body, b.cretime, b.duplicate_of, b.fixed_version, b.reported_version, b.resolution, b.title, b.updtime
        , pp.code, pp.sort_order, pp.title, b.product_id
        , ss.code, ss.title, b.type_code
        , uu.id, uu.email, uu.name
        , cu.id, cu.email, cu.name
        , da.id, da.cretime, da.file_name, da.file_path, da.file_size, da.abstract, da.updtime, da.bug_id 
FROM b_bug b
JOIN b_bug_priority pp ON b.priority_code = pp.code 
JOIN b_bug_status ss ON b.status_code = ss.code 
JOIN b_bug_type tt ON b.type_code = tt.code 
LEFT OUTER JOIN s_user uu ON b.user_assigned_id = uu.id 
LEFT OUTER JOIN s_user cu ON b.user_logged_id = cu.id 
LEFT OUTER JOIN b_bug_attachment da ON b.id = da.bug_id  
WHERE ss.code IN (?,?) AND tt.code = ? 
ORDER BY pp.sort_order ASC
</sql>
	
	*/
}
