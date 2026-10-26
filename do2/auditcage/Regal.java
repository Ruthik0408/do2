package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

 

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Regal extends AuditCageBase implements Do2AuditCageProcessor,
		 java.io.Serializable {

	 
	Do2 stopAllDo2, restoreDo2;
	String description;
	static public String INVALID_REFDO2 = "AU:Incorrect reference DO2.\n ";
	static public String INVALID_LRA = "VL:LRA days not matching with Regularisation Period.\n";
	static public String REGAL_ONE_DAY = "VL:Regularisation for one day is not valid.\n";
	static public String REJOIN_ALREADY_CANCELLED = "AU:Rejoining DoII already cancelled. \n";
	static public String INVALID_COLUMNA = "AU:Col.7A should contain leave to which regularised\n";
	static public String INVALID_COLUMNB = "AU:Col.7B should contain year/nqs\n";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}	
		if (do2.getDo2Date().before(revised_appxj_impl_date)) {
			;
		} else if (!isValidColumnA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);
		}
		if (do2.getDo2Date().before(revised_appxj_impl_date)) {
			;
		} else if (!isValidColumnB(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNB);
		}		
		if (do2.getLra() == 1) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(REGAL_ONE_DAY);
			return false;
		}
		
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2Repo.save(do2);
		return validity;
	}

	public boolean isValidLra(Do2 do2) {
		
		double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		return (do2.getLra() == do2Days);
		
	}

	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 == null)
			return false;
		if (refDo2.getDescription().contains("AL") &&
			(!do2.getFromDate().before(refDo2.getFromDate()) &&
			 !do2.getFromDate().after(refDo2.getToDate())) &&
			 !do2.getToDate().after(refDo2.getToDate())){
             return true;        	 
		}
		
		if (!refDo2.getDescription().contains("AL")) {
			if (refDo2.getDescription().equals("AWL") || (refDo2.getDescription().equals("OSL"))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		if (newAuditCage.getDo2().getColumnA() != null && 
			newAuditCage.getDo2().getColumnA().equals("EOL")){
			newAuditCage.setAbsentFlag(true);
		} else {
			newAuditCage.setAbsentFlag(false);
		} 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {		
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColumnA(Do2 do2) {
		List<String> col7AList = Arrays.asList("AL","EOL");
		if (do2.getColumnA() == null){
			return false;
		} else if (col7AList.contains(do2.getColumnA())){
			return true;
		}
		return false;
	}
	public boolean isValidColumnB(Do2 do2) {
		if (do2.getColumnB() == null){
			return false;
		} else if (do2.getColumnB().contains("-") && do2.getColumnA().equals("AL")){
			return true;
		} else if (do2.getColumnB().contains("NQS") && do2.getColumnA().equals("EOL")){
			return true;
		}
		return false;
	}
}
