import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import Config.PublicParamters.Location;
import Database.Database;
import Record.Record;
import Record.StudentRecord;
import Record.TeacherRecord;
import Config.PublicParamters;
import java.io.File;

public class Test {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Database db = new Database(Location.DDO, 0, null);
		db.createSRecord("abc", "chao", "wang", "FRENCH", "ACTIVE", "2010");
		db.createSRecord("def", "ruiid", "yangtao", "FRENCH", "INACTIVE", "2011");

//		Record rc = new TeacherRecord("chao", "wang", "address","123", PublicParamters.Specialization.FRENCH, Location.DDO );
//		File fl = new File("DDO0.tmp");
//		FileOutputStream fos = new FileOutputStream(fl);
//		ObjectOutputStream oos = new ObjectOutputStream(fos);
//		oos.writeObject(rc);
//		oos.flush();
//		oos.close();
//		
//		FileInputStream fis = new FileInputStream("DDO0.tmp");
//	    ObjectInputStream ois = new ObjectInputStream(fis);
//	    Record tmp = (Record)ois.readObject();
		
//		======================================================
		db.writeWholeDB();
		db.restoreDB();
		for(LinkedList<Record> list : db.getRecordData().values())
			for(Record rc : list)
				System.out.println(((StudentRecord)rc).getStatus());
				
	}

}
