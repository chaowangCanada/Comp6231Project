package util;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import Config.PublicParamters;

public class Operation {

	public static void copyDatabase(int source, int target) throws IOException{
		
		String sourcePath = PublicParamters.PROJECT_ROOT_PATH+"LVL"+Integer.toString(source)+".tmp";
		String targerPath = PublicParamters.PROJECT_ROOT_PATH+"LVL"+Integer.toString(target)+".tmp";

		Path FROM = Paths.get(sourcePath);
		Path TO = Paths.get(targerPath);
		CopyOption[] options = new CopyOption[]{
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES
		};
		
		Files.copy(FROM, TO, options);
		
		sourcePath = PublicParamters.PROJECT_ROOT_PATH+"MTL"+Integer.toString(source)+".tmp";
		targerPath = PublicParamters.PROJECT_ROOT_PATH+"MTL"+Integer.toString(target)+".tmp";
		FROM = Paths.get(sourcePath);
		TO = Paths.get(targerPath);
		Files.copy(FROM, TO, options);
		
		sourcePath = PublicParamters.PROJECT_ROOT_PATH+"DDO"+Integer.toString(source)+".tmp";
		targerPath = PublicParamters.PROJECT_ROOT_PATH+"DDO"+Integer.toString(target)+".tmp";
		FROM = Paths.get(sourcePath);
		TO = Paths.get(targerPath);
		Files.copy(FROM, TO, options);

			
	}
}
