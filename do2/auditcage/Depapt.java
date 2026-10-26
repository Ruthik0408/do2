package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
 


@RestController
public class Depapt extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
    Do2 refDo2;
    AppointmentRate appointmentRate;
    AuditCage baseAuditCage;

	private StringBuffer buffer = new StringBuffer();
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_COLUMN7A= "AU:ED Appointment in Col.7A and appointment held not tallied.\n";
		
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			
		}
		do2.setStatus(Do2Manager.VALID);
		do2.setReason(message);
		do2Repo.save(do2);
	return validity;
	}
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
			AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
			AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
			AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.APTPAY }), newAuditCage);
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			employee.setAppointmentRate(null);
			employee.setAppointmentDate(null);
		return Do2Manager.SUCCESS;
	}
	
	/*public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null && (refDo2.getDescription().equals(Do2Manager.AWL)
							|| refDo2.getDescription().equals(Do2Manager.OSL)
							|| refDo2.getDescription().equals(Do2Manager.REP) 
							|| refDo2.getDescription().equals(Do2Manager.SEVREP))) {
			return true;
		} 
		return  false;
	}*/
		
	private List<String> APPOINTMENT_NAME;

	private List<String> getAppointName() {
		if (APPOINTMENT_NAME != null)
			return APPOINTMENT_NAME;
		APPOINTMENT_NAME = Arrays.asList(new String[] { "LNK", "LHAV", "BQMH", "BHM", "RQMH", "RHM",
	     "CQMH", "CHM", "TSLNK", "TSALD", "ALD", "LDFR", "SDM", "RDM", "SQMD", "RQMD", "BDM", "QMNK", "MQMD"});
		return APPOINTMENT_NAME;
	}
	/*public boolean isValidAppointment(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (getAppointName().contains(do2.getColumnA())
			    && do2.getColumnA().equals(employee.getAppointmentRate().getAppointmentName())){
			return true;
		} 
		return false;	
	}*/
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		AppointmentRate appt =apptRateRepo.findByAppointmentName(refDo2.getColumnA()); 
				
		 
			employee.setAppointmentRate(appt);
		 
		 
		List<Do2> do2List =do2Repo.findByDescColAFromDateLessThanEqual(currentDo2.getEmployee().getId(),Do2Manager.VALID,"APPT",currentDo2.getColumnA(),currentDo2.getFromDate());
		 
		if (do2List.size() >0){	
			employee.setAppointmentDate(do2List.get(0).getFromDate());
		}
		return Do2Manager.SUCCESS;
	}
}