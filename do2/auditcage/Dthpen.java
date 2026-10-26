package itsdc.dolphin.do2.auditcage;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;

@RestController
public class Dthpen extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	Do2 refDo2;
	Employee employee;
    
	static public String INVALID_REFDO2 = "AU:Incorrect reference Do2.\n ";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
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
		newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setFsDueDate(do2.getFromDate());
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		employee.setFsDueDate(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2.getDescription().equals(Do2Manager.ARREST)
					|| refDo2.getDescription().equals(Do2Manager.CONFIN) 
					|| refDo2.getDescription().equals(Do2Manager.CVLRI)){
			return true;
		} 
		return false;
	}
}