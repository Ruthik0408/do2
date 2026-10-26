package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Acclve extends AccountingCageBase implements java.io.Serializable {
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private static final String INVALID_ENCASHMENT_DAYS = "VL:Invalid number of days for leave encashment.\n";
	private static final String INVALID_ACCUMULATION = "VL:Invalid number of days for total accummulation of leave.\n";
	private static final String INVALID_YEAR = "VL:Invalid leave encashment year.\n";
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	private static final int MAX_NO_DAYS = 300;
	StringBuffer buffer = new StringBuffer();
	String message;
	Employee employee;
	Do2 refDo2;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_ENCASHMENT_DAYS + NOT_IN_APPENDIX_J_FORMAT);	
		}
	//	-- validation of column-b removed as we are checking max days as 300. They are accumulated more than 300 also rejection is incorrect.
	/*	if (!isValidB(do2)){
			validity = false;
			buffer.append(INVALID_ACCUMULATION + NOT_IN_APPENDIX_J_FORMAT);	
		}*/
		if (!isValidC(do2)){
			validity = false;
			buffer.append(INVALID_YEAR + NOT_IN_APPENDIX_J_FORMAT);	
		}
// Suspended for a period of one year vide ITSDC/SYS/Mis dated 09.03.2016
		/*if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}*/
		if (validity == false) {
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
	
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && Integer.valueOf(do2.getColumnA()) <= 30){
			return true;
		}
		return false;
	}
	
	public boolean isValidB(Do2 do2){
		if (do2.getColumnB() != null && Integer.valueOf(do2.getColumnB()) <= MAX_NO_DAYS){
			return true;
		}
		return false;
	}
	
	public boolean isValidC(Do2 do2){
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(employee.getEnrolmentDate());
		int enrolYear = cal.get(Calendar.YEAR);
		int currentYear = DateManager.getCurrentYear();
		if (!Utils.getPao().getPaoCode().equals("94")){
			if (employee.getArmyNo().startsWith("JC")){
				if (do2.getColumnC() != null && enrolYear < Integer.valueOf(do2.getColumnC()) &&  
						Integer.valueOf(do2.getColumnC()) < currentYear){
					return true;
				}
			} else {
				if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() == null){
					buffer.append("AU: PBOR is not a sepoy. \n");
					return false;
				} else {
					Date sepdate = null;
					if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() != null)
					   sepdate=employee.getInitialMusteringDate();
					else if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() == null)
						   sepdate=employee.getAttestationDate();
					else if (employee.getAttestationDate().before(employee.getInitialMusteringDate()))
						sepdate=employee.getAttestationDate();
					else if (employee.getAttestationDate().after(employee.getInitialMusteringDate()))
						sepdate=employee.getInitialMusteringDate();
					else
						sepdate=employee.getAttestationDate();
					if (sepdate != null){
						cal = new GregorianCalendar();
						cal.setTime(sepdate);
						int sepYear = cal.get(Calendar.YEAR);
						if (do2.getColumnC() != null && sepYear <= Integer.valueOf(do2.getColumnC()) &&  
								Integer.valueOf(do2.getColumnC()) < currentYear){
							return true;
						}
					}
				}
			}
		} else {
			if (do2.getColumnC() != null && enrolYear < Integer.valueOf(do2.getColumnC()) &&  
					Integer.valueOf(do2.getColumnC()) <= currentYear){
				return true;
			}
		}
		return false;
	}
	public boolean isValidRefDo2(Do2 do2) {
		if (do2.getColumnA() != null && do2.getColumnB() != null && 
			Integer.valueOf(do2.getColumnA()) == Integer.valueOf(do2.getColumnB())){
			if (do2.getRefDo2No() == null)
				return true;
		}
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null) {
			return true;
		} 
		return false;
	}
}
