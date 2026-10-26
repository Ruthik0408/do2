package itsdc.dolphin.do2.auditcage;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;
 
@RestController
public class Cvlri extends AuditCageBase implements
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
		if (do2.getDescription().equals("CVLRI")) {
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
		/*
		 * List<Do2> acquitDo2List = Utils.getEntityManager().
		 * createQuery("SELECT d from Do2 d where d.employee=:emp and d.fromDate=:frdt and d.description='ACQUIT' and d.status='V' "
		 * ) .setParameter("emp", newAuditCage.getEmployee()).setParameter("frdt",
		 * newAuditCage.getFromDate()).getResultList();
		 * 
		 * if (acquitDo2List==null || (acquitDo2List!=null && acquitDo2List.size()==0))
		 */
			newAuditCage.setAbsentFlag(true);
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_NQS);
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
		if(refDo2 != null && (refDo2.getDescription().equals(Do2Manager.ARREST) ||
				refDo2.getDescription().equals(Do2Manager.CONFIN) || refDo2.getDescription().equals(Do2Manager.CLOSAR))){
			return true;
		}
		return false;
	}
}