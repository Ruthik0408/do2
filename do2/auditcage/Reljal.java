package itsdc.dolphin.do2.auditcage;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */
@RestController
public class Reljal extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	Employee employee;
	Do2 refDo2;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2. \n ";
	static public String INVALID_TJRA = "VL:No of Months not mentioned in TJRA column.\n";
	static public String INVALID_LRA = "VL:No of Days in LRA column should be between 0 to 30 days.\n";

	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (do2.getDescription().equals("RELJAL")) {
			if (!isValidLra(do2)) {
				validity = false;
				buffer.append(INVALID_LRA);
			}
		}
		
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);	
		}
		if(validity){
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
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
		List<Do2> do2List = Utils.getReferenceDo2List(do2);
		for (Do2 reljalDo2 : do2List){
			if (reljalDo2.getDescription().equals("CVLRI")){
				refDo2 = reljalDo2;
			}
		}
		
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
		for(EmployeeServiceDetail esd : esdList){
			esd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
			esdRepo.save(esd);
		//	Utils.getEntityManager().persist(esd);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getLra() >= 0 && do2.getLra() <= 30) {
			return true;
		} 
		return false;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if(refDo2 != null && (refDo2.getDescription().equals(Do2Manager.CVLRI))){
			return true;
		}
		return false;
	}
}