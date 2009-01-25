package app.data;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;

@Entity
@Sql(
	select={
		@SqlSelect(
			columnMapping="x.status_code as status, count(*) as count",
			query="select x.status_code, count(*) from b_bug x group by x.status_code"
		),
		@SqlSelect(name="withAssigned", 
			query="select user_assigned_id as assigned, status_code as status, count(*) as count "
				+"from b_bug "
				+"group by user_assigned_id, status_code"
		),
		@SqlSelect(name="assignedStatusCount", 
			extend="withAssigned",
			where="status = :status",
			having="count > :count"
		)
	}
)
public class BugReport implements Serializable {

	private static final long serialVersionUID = 1063938774721972882L;

	@ManyToOne
	BugStatus status;
	
	int count;
	
	@ManyToOne
	User assigned;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public User getAssigned() {
		return assigned;
	}

	public void setAssigned(User assigned) {
		this.assigned = assigned;
	}

	public BugStatus getStatus() {
		return status;
	}

	public void setStatus(BugStatus status) {
		this.status = status;
	}
	
	 
}
