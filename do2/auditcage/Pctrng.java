package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */

@RestController
public class Pctrng extends AuditCageBase implements
		Do2AuditCageProcessor, java.io.Serializable {
	 
	Employee employee;
	static public String INVALID_COL7B = "AU:Appointment in DOII and appointment held by the PBOR not matching.\n";
	private StringBuffer buffer = new StringBuffer();
	 
	@Override
	public boolean isValid(Do2 do2) {
		Date revised_appxj_impl_date=DateManager.getRevisedAppendixJImplementationDate();	
		boolean validity = true;
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			if (do2.getColumnA() != null){
				validity = false;
				buffer.append("VL: Column-7A should be left blank");
			} 
		} else {
			if (!(do2.getColumnA().equals("TORS") || do2.getColumnA().equals("SORS"))){
				validity = false;
				buffer.append("VL: Column-7A should be TORS/SORS");
	        }
		}		
        
		if (!isValidAppointment(do2)) {
			validity = false;
			buffer.append(INVALID_COL7B);
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());			
		}
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		List<String> alwToStop = Arrays.asList(new String[] { "EDA"});
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.removeAllowances(Do2Manager.NON_PERSONAL_ALLOWANCES, newAuditCage);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		return Do2Manager.SUCCESS;
	}
	
	private List<String> APPOINTMENT_NAME;

	private List<String> getAppointName() {
		if (APPOINTMENT_NAME != null)
			return APPOINTMENT_NAME;
		APPOINTMENT_NAME = Arrays.asList(new String[] { "LNK", "LHAV", "BQMH", "BHM", "RQMH", "RHM",
	     "CQMH", "CHM", "TSLNK", "TSALD", "ALD", "LDFR", "SDM", "RDM", "SQMD", "RQMD", "BDM", "QMNK", "MQMD"});
		return APPOINTMENT_NAME;
	}
	
	public boolean isValidAppointment(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnB() == null)
			return true;
		if (do2.getColumnB() != null && getAppointName().contains(do2.getColumnB())
		    && do2.getColumnB().equals(employee.getAppointmentRate().getAppointmentName())){
			return true;
		} 
		return false;
	}
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}