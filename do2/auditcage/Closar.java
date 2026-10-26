package itsdc.dolphin.do2.auditcage;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.Utils;
 

 
@RestController
public class Closar extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	Rank rank;

	static public String INVALID_RANK = "AU:Rank of the PBOR is not SEPOY.\n ";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		
		if (!isSepoy(do2)) {
			validity = false;
			buffer.append(INVALID_RANK);
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
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}

	public boolean isSepoy(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank != null && rank.getRankName().equals("SEP")) {
			return true;
		}
		return false;
	}
}