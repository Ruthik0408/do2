package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */
@RestController
public class Revoke extends AuditCageBase implements
	 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Do2 refDo2;
	
	static public String ON_LEAVE = "AU:Individual is on leave on the date.\n ";
	static public String INVALID_REFDO2 = "AU:Incorrect Ref Do2.\n ";
	private StringBuffer buffer = new StringBuffer();
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (isValidLeave(do2)) {
			validity = false;
			buffer.append(ON_LEAVE);
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
		newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if(refDo2 != null && (refDo2.getDescription().equals(Do2Manager.INVOKE))){
			return true;
		}
		return false;
	}

	public boolean isValidLeave(Do2 do2) {
		List<String> nonAllowanceList = Arrays.asList(new String[] { "AL",
				"ADVAL", "BAL", "ACCMAL", "EXTNAL", "EXTNCL", "PAL", "SL",
				"EXTNSL", "RL", "EXTNLV","PATLVE" });
		if (Utils.isMutexAllowance(do2.getArmyNo(), nonAllowanceList, do2
				.getFromDate(), do2.getFromDate(),do2)) {
			return true;
		} 
		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}
}