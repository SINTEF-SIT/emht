import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.avaje.ebean.Ebean;

import play.Application;
import play.GlobalSettings;
import play.test.FakeApplication;
import play.test.Helpers;

// solution for creating a fake application before the tests and 
// cleaning the db, credits to http://blog.matthieuguillermin.fr/2012/03/unit-testing-tricks-for-play-2-0-and-ebean/

public class BaseModelTest {
	
	
	public static class DummyGlobalSettings extends GlobalSettings {
	  	  @Override
	  	  public void onStart(Application app) {
	  	    System.out.println("Starting FakeApplication");
	  	  }
	}
	
	
	public static FakeApplication app;
	public static String createDdl = "";
	public static String dropDdl = "";
	
	  @BeforeClass
	  public static void startApp() {
	    app = Helpers.fakeApplication(Helpers.inMemoryDatabase(),new DummyGlobalSettings());
	    Helpers.start(app);
	    
	    // Reading the evolution file
	    try{
	    String evolutionContent = FileUtils.readFileToString(
	        app.getWrappedApplication().getFile("conf/evolutions/default/1.sql"));
		    // Splitting the String to get Create & Drop DDL
		    String[] splittedEvolutionContent = evolutionContent.split("# --- !Ups");
		    String[] upsDowns = splittedEvolutionContent[1].split("# --- !Downs");
		    createDdl = upsDowns[0];
		    dropDdl = upsDowns[1];
	    
	    }
	    catch(Exception e){
	    	
	    }
	 

	  }
	  
	  
	  @AfterClass
	  public static void stopApp() {
	    Helpers.stop(app);
	  }
	  
	  @Before
	  public void createCleanDb() {
	    Ebean.execute(Ebean.createCallableSql(dropDdl));
	    Ebean.execute(Ebean.createCallableSql(createDdl));
	  } 
}
