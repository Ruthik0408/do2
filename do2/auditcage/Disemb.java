package itsdc.dolphin.do2.auditcage;

 

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;

 
@RestController
public class Disemb extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;
	static final String NOT_EMBODIED = "AU:Not On Embodied Strength. \n";
	static final String NOT_TA = "AU:PBOR Not On TA Strength. \n";
	private final StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);
		if (!(isTaPbor(do2))) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(NOT_TA);
		}
		
		if (do2.getColumnA() != null ) {
			if (do2.getColumnA().equals("FN") || do2.getColumnA().equals("AN")) {
				validity = true;
			}
		} 
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		newAuditCage.setDeputationFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		esdRepo.deleteFromEsdForDo2(refDo2);
		 
		return Do2Manager.SUCCESS;
	}

	public boolean isTaPbor(Do2 do2) {
		if (employee.getRegularTaFlag().equals("T")) {
			return true;
		}
		return false;
	}

	
}