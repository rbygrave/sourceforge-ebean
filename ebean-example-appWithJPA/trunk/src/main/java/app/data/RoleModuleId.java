package app.data;

import javax.persistence.Embeddable;

/**
 * Id for Role Module
 */
@Embeddable
public class RoleModuleId {


    Integer moduleId;

    Integer roleId;

    /**
     * Default constructor.
     */
    public RoleModuleId(){
    }

    /**
     * Construct with fields.
     */
    public RoleModuleId(Integer moduleId, Integer roleId){
        this.moduleId = moduleId;
        this.roleId = roleId;
    }

    /**
     * Return module id.
     */    
    public Integer getModuleId() {
  	    return moduleId;
    }

    /**
     * Set module id.
     */    
    public void setModuleId(Integer moduleId) {
  	    this.moduleId = moduleId;
    }

    /**
     * Return role id.
     */    
    public Integer getRoleId() {
  	    return roleId;
    }

    /**
     * Set role id.
     */    
    public void setRoleId(Integer roleId) {
  	    this.roleId = roleId;
    }

    public String toString() {
        return "moduleId="+moduleId+","+"roleId="+roleId;
    }

    /**
     * equals by field.
     */
    public boolean equals(Object o) {
        if (o instanceof RoleModuleId == false) {
	        return false;
        }
        if (o == this) {
	        return true;
        }
        return o.hashCode() == hashCode();
    }

    /**
     * store the hashcode so that it doesn't change if
     * first calculated when fields are null.
     */
    private Integer hashCodeValue;

	/**
	 * Hashcode based on non null fields.
	 */
	public int hashCode() {
	
		if (hashCodeValue != null) {
			return hashCodeValue.intValue();
		}
		// expecting all fields to be null or not null
		if (moduleId != null && roleId != null) {

			int hc = getClass().hashCode();
			hc = 31 * (hc + moduleId.hashCode());
			hc = 31 * (hc + roleId.hashCode());

			hashCodeValue = new Integer(hc);
			return hc;
		}
		// going to be using instance equality from now on
		int hc = super.hashCode();
		hashCodeValue = new Integer(hc);
		return hc;
	}

}
