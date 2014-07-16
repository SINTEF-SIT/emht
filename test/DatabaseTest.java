import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import models.Patient;

import org.junit.Test;


public class DatabaseTest extends BaseModelTest{


    @Test
    public void patientTests() {
    	
    	final String testAddr = "rua dokks"; 
    	
    	Patient fullPatient = new Patient();
    	fullPatient.address = "rua abc";
    	fullPatient.age = 12;
    	fullPatient.name = "Braulio";
    	fullPatient.personalNumber = "1234567";
    	
    	// test create
    	Patient.create(fullPatient);
    	// test getOrCreate to get an unique
    	Patient fullPatientCpy = Patient.getOrCreate(fullPatient);
    	compareAllPatientFieldsExceptId(fullPatient,fullPatientCpy);
    	List<Patient> l = Patient.patientFromAddress(fullPatient.address);
    	assertTrue(l.size() == 1);
    	
    	// test getOrCreate to create a new one
    	Patient fullPatient2 = new Patient();
    	fullPatient2.address = testAddr;
    	fullPatient2.age = 13;
    	fullPatient2.name = "Bruno";
    	fullPatient2.personalNumber = "222222";
    	Patient fullPatient2Cpy = Patient.getOrCreate(fullPatient2);
    	compareAllPatientFieldsExceptId(fullPatient2Cpy,fullPatient2);
    	
    	// test getOrCreate to create a new one with no personal number
    	Patient emptyPat = new Patient();
    	emptyPat.address = testAddr;
    	emptyPat.age = 0;
    	emptyPat.name = "Rui";
    	emptyPat.personalNumber = "";
    	Patient emptyPatcpy = Patient.getOrCreate(emptyPat);
    	compareAllPatientFieldsExceptId(emptyPatcpy,emptyPat);
    	
    	// test getOrCreate to create a second patient with no personal number
    	Patient emptyPat2 = new Patient();
    	emptyPat2.address = testAddr;
    	emptyPat2.age = 0;
    	emptyPat2.name = "Carvalho";
    	emptyPat2.personalNumber = "";
    	Patient emptyPat2cpy = Patient.getOrCreate(emptyPat2);
    	compareAllPatientFieldsExceptId(emptyPat2cpy,emptyPat2);
    	
    	l = Patient.patientFromAddress("rua rua");
    	assertTrue(l.size() == 0);
    	
    	l = Patient.patientFromAddress(testAddr);
    	assertTrue(l.size() == 3);
    	
    }
    
    public void compareAllPatientFieldsExceptId(Patient p1, Patient p2){
    	assertThat(p1.address).isEqualTo(p2.address);
    	assertThat(p1.age).isEqualTo(p2.age);
    	assertThat(p1.name).isEqualTo(p2.name);
    	assertThat(p1.personalNumber).isEqualTo(p2.personalNumber);
    }

	
}
