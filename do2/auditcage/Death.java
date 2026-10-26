package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Death extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	
	Employee emp;
	boolean validity = true;
	private StringBuffer buffer = new StringBuffer();
	static final public String FS_DUE_DATE = "AU:Fs Due date already updated in Employee Table. Check the Correctness. \n";
	@Override
	public boolean isValid(Do2 do2) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
	//	if (newDo2.getId()== do2.getId() && emp.getFsDueDate() != null) {
		if (do2.getAcceptedQe() == null && emp.getFsDueDate() != null) {		
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(FS_DUE_DATE);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;

	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		
		emp.setFsStatus(scRepo.findByStatusCode("C"));
		emp.setFsDueDate(DateManager.getNextDate(do2.getFromDate()));
	empRepo.save(emp);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_DISCHARGE+"");
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}
	
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		emp = Utils.getCurrentEmployee(refDo2.getArmyNo());
		emp.setFsDueDate(null);
		emp.setFsStatus(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		return Do2Manager.SUCCESS;
	}

}