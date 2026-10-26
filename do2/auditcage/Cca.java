package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

 

@RestController
public class Cca extends AuditCageBase implements Do2AuditCageProcessor {
		 
	static public final String NOT_ADMISSIBLE_RECRUIT = "AU:Not Admissible to Recruit. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table. \n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();		

		if (do2.getColumnA().trim().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			
			do2Repo.save(do2);
	 		return true;
		}

		if (!isRecruit(do2)) {
		 	buffer.append(NOT_ADMISSIBLE_RECRUIT);
			validity = false;
		}

		if (!isRateAvailable(do2)) {
		 	buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}

		if (!validity) {
			do2.setStatus(Do2Manager.PENDING);
		} else {
		 	do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
 		return validity;
	}
//method replaced on 15/04/2012
	public boolean isRecruit(Do2 do2) {
		Employee emp =Utils.getCurrentEmployee(do2.getArmyNo());
		if(emp.getSubstantiveRank()!=null){
			int rankCode = Integer.parseInt(emp.getSubstantiveRank().getRankCode());
			if(rankCode >10)
				return true;
		}
		return false;
	}

	public boolean isRateAvailable(Do2 do2) {
	 	return (Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate()).size()>0);
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	 	return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
		int dearnessPay = newAuditCage.getDp() == null ? 0 : newAuditCage.getDp();
		int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay();
 		
		if (!do2.getFromDate().after(DateManager.getSixthCpcDate())) {
		 	if(do2.getFromDate().before(DateManager.getDPDate())){
				;
			}else{
				basicPay = basicPay + dearnessPay;
			}
		} else  {
			basicPay = (int) (oldBasicPay+(oldBasicPay*0.5));
		}
	 
 		if (do2.getColumnA().equals("GRANTED")) {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), basicPay);
			if (allowances.size()>0) {
 				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), allowances.get(0).getRateAmount());
			}
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
		}

		if (do2.getColumnA().equals("CEASED")) {
			List<String> stopAllowanceList = Arrays.asList("CCA1", "CCA", "CCB1", "CCB2");
			AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		}
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}
