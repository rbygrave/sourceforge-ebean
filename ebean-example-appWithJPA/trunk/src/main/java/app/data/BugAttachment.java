package app.data;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Bug Attachment entity bean.
 */
@Entity
@Table(name="b_bug_attachment")
public class BugAttachment {


    @Id
    Integer id;

    String fileName;

    String filePath;

    @Column(name="abstract")
    String summary;

    String fileSize;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    Bug bug;


    /**
     * Return id.
     */    
    public Integer getId() {
  	    return id;
    }

    /**
     * Set id.
     */    
    public void setId(Integer id) {
  	    this.id = id;
    }

    /**
     * Return file name.
     */    
    public String getFileName() {
  	    return fileName;
    }

    /**
     * Set file name.
     */    
    public void setFileName(String fileName) {
  	    this.fileName = fileName;
    }

    /**
     * Return file path.
     */    
    public String getFilePath() {
  	    return filePath;
    }

    /**
     * Set file path.
     */    
    public void setFilePath(String filePath) {
  	    this.filePath = filePath;
    }

    /**
     * Return summary.
     */    
    public String getSummary() {
  	    return summary;
    }

    /**
     * Set summary.
     */    
    public void setSummary(String summary) {
  	    this.summary = summary;
    }

    /**
     * Return file size.
     */    
    public String getFileSize() {
  	    return fileSize;
    }

    /**
     * Set file size.
     */    
    public void setFileSize(String fileSize) {
  	    this.fileSize = fileSize;
    }

    /**
     * Return cretime.
     */    
    public Timestamp getCretime() {
  	    return cretime;
    }

    /**
     * Set cretime.
     */    
    public void setCretime(Timestamp cretime) {
  	    this.cretime = cretime;
    }

    /**
     * Return updtime.
     */    
    public Timestamp getUpdtime() {
  	    return updtime;
    }

    /**
     * Set updtime.
     */    
    public void setUpdtime(Timestamp updtime) {
  	    this.updtime = updtime;
    }

    /**
     * Return bug.
     */    
    public Bug getBug() {
  	    return bug;
    }

    /**
     * Set bug.
     */    
    public void setBug(Bug bug) {
  	    this.bug = bug;
    }


}
