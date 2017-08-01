package Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import Config.PublicParamters.*;
/**
 * student record saved in hashmap in server
 * @author Chao
 * 
 */
public class StudentRecord extends Record{

	private ArrayList<Course> courseList;  //student can register more than 1 course
	private Status status;
	private String statusDate;

	// student only can add course through editRecord()
	public StudentRecord(String firstName, String lastName, Course course, Status stat, String date) {
		super(firstName, lastName);
		this.recordID = "SR"+Integer.toString(Record.baseID++);
        this.statusDate = date;
        this.courseList = new ArrayList<Course>();  //student can register more than 1 course
        this.courseList.add(course);
        this.status = stat;
	}
	
	public StudentRecord(String firstName, String lastName) {
		super(firstName, lastName);
		this.recordID = "SR"+Integer.toString(Record.baseID++);
	}

	public StudentRecord() {
		this("N/A", "N/A");
	}

	public String getRecordID(){
		return recordID;
	}
	
	public void setRecordID(String recordID) {
		this.recordID = recordID;
	}

	/**
	 * method overload for Course enum
	 * if course list already contains course, delete such course 
	 * otherwise add such course
	 * @param newCourse
	 */
	public void editCourse(Course newCourse) {
		// if course list already contains course, no need to add
		Iterator<Course> it = courseList.iterator();
		while(it.hasNext()){
			if(newCourse == it.next()){
				it.remove();
				return;
			}
		}
		courseList.add(newCourse);
		
	}
	
	/**
	 * method over load for string 
	 * if course list already contains course, delete such course 
	 * otherwise add such course
	 * @param newValue
	 */
	public void editCourse(String newValue) {
		// if course list already contains course, no need to add
		Iterator<Course> it = courseList.iterator();
		while(it.hasNext()){
			if(Course.valueOf(newValue) == it.next()){
				it.remove();
				return;
			}
		}
		courseList.add(Course.valueOf(newValue));
	}
	
	public String getCourse(){
		return Arrays.toString(courseList.toArray());
	}
	
	public Status getStatus() {
		return status;
	}

	/**
	 * if change satus, will update the status date at today as well
	 * @param status
	 */
	public void setStatus(Status status) {
		this.status = status;
		SimpleDateFormat dt = new SimpleDateFormat("yyyy/MM/dd"); 
		Date date = new Date();
		this.statusDate = dt.format(date);
	}
	

	public void setStatus(String newValue) {
		setStatus(Status.valueOf(newValue));
	}

	public String getStatusDate() {
		return statusDate;
	}

	public void setStatusDate(String statusDate) {
		this.statusDate = statusDate;
	}

	
	
	
}
