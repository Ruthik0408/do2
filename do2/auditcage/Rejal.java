package itsdc.dolphin.do2.auditcage;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:08-01-2010
 */

@RestController
public class Rejal extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	private boolean validity = true;
	StringBuffer buffer = new StringBuffer();
	Do2 refDo2;
	Do2 tptlDo2;
	Do2 leaveDo2;
	Employee employee;
	Rank rank;
	
	static public final String REF_DOII_NOT_FOUND = "AU:Reference DOII not found.\n";

	@Override
	public boolean isValid(Do2 do2) {
		
         buffer.setLength(0);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		/*if (!(isValidRefDo2(do2))) {
			buffer.append(REF_DOII_NOT_FOUND);
			validity = false;
		}*/
		if (!validity && do2.getStatus() !=null) {
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

		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findByAuditCageIdAndPayCodeId(newAuditCage.getId(),
																									"BHUTAN");
	/*			Utils
		.getEntityManager()
		.createQuery("SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage AND aca.payCode.payCode=:payCode")
		.setParameter("auditCage", newAuditCage).setParameter("payCode",
				"BHUTAN").getResultList();*/
		if(auditCageAllowanceList.size() >0){
			int bhutanRate = AuditCageHelper.getBhutanRate(newAuditCage.getDo2());
			
			AuditCageHelper.updateAuditCageAllowance(newAuditCage,"BHUTAN", bhutanRate);
		}
		/*employee = Utils.getCurrentEmployee(do2.getArmyNo());

		refDo2 = Utils.getReferenceDo2(do2);
		if (employee.getDeputationFlag().equals(true)
				&& (employee.getRecordStatus()
						.equals(Do2Manager.DEPU_IMTRAT_BHUTAN))) {

			Date fromDate = refDo2.getFromDate();
			AuditCage auditCage = AuditCageHelper.getPreviousDateAuditCage(
					fromDate, newAuditCage.getDo2());
			List<AuditCageAllowance> auditCageAllowanceList = new ArrayList<AuditCageAllowance>();
			if (auditCage!=null) {
				auditCageAllowanceList = Utils.getEntityManager().createQuery(
							"SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage AND aca.payCode.payCode=:payCode")
					.setParameter("auditCage", auditCage).setParameter("payCode", "BHUTAN").getResultList();
			}
			
		
			if (auditCageAllowanceList.size() > 0) {
			AuditCageHelper.updateAuditCageAllowance(newAuditCage,
						"BHUTAN", auditCageAllowanceList.get(0).getAmount());
			}

		}
*/
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for (Do2 refDo2 : refDo2List) {
			if(Do2Manager.getAllLeaveTypes().contains(refDo2.getDescription()))
			{
				return true;
			}
		}
		return false;
	}
	
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		return Do2Manager.SUCCESS;
	}
	public void revisedBhutanAllowance(AuditCage auditCage){
		
		
	}
}