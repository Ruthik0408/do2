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
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Osl extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";

	private StringBuffer buffer = new StringBuffer();
	Do2 refDo2;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
		
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	
		return validity; 
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		newAuditCage.setTransport(0);
		newAuditCage.setAbsentFlag(true);
		 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		esdRepo.deleteFromEsdForDo2(refDo2);
		
	/*	Utils.getEntityManager().createNativeQuery("DELETE FROM employee_service_detail where fk_do2 =:refDo2 ")
				.setParameter("refDo2",refDo2).executeUpdate();*/
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
        List<Do2> do2List = Utils.getReferenceDo2List(do2);       
    
        List<String> nonAllowanceList = Arrays.asList(new String[] { "AL",
				"ADVAL", "BAL", "ACCMAL", "CL", "EXTNAL", "EXTNCL", "PAL", "EXTNLV" });
		
        for (Do2 leaveDo2 : do2List){        
        	if (nonAllowanceList.contains(leaveDo2.getDescription())){
        		refDo2 = leaveDo2; 
        	}
        }
	
        if (refDo2 == null || do2.getFromDate().equals(DateManager.getNextDate(refDo2.getFromDate()))){
        	return false;
        }
	
		return true;

	}
}