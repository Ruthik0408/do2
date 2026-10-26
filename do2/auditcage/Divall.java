package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Divall extends AuditCageBase implements Do2AuditCageProcessor {
  	
	static public final String NOT_ADMISSIBLE_RECRUIT = "AU:Not Admissible to Recruit. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table. \n";
	static public final String INVALID_MODULE = "VL:To be claimed through contingent bill before 01.06.2011. \n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
	 	StringBuffer buffer = new StringBuffer();
	  			
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			
			do2Repo.save(do2);

	 		return true;
		}
	
		if (!isValidModule(do2)) {
	 		buffer.append(INVALID_MODULE);
			validity = false;
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

	public boolean isRecruit(Do2 do2) {
		//Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		 return (rank!=null && !rank.getRankCode().equals(Do2Manager.RECRUIT));
	}

	public boolean isRateAvailable(Do2 do2) {
		return (Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate()).size()>0); 
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	 	return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
				
		if (do2.getColumnA().equals("CEASED")) {
			List<String> stopAllowanceList = Arrays.asList("DIVALL");
			AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		}
	  
		if (do2.getColumnA().equals("GRANTED")) {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());
			if (allowances.size()>0) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), allowances.get(0).getRateAmount());
			}
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
		}
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidModule(Do2 do2) {
		
		Date cutOffDate = new Date();
		try {
			cutOffDate = DateManager.getDate("2011-06-01");
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	return (!do2.getFromDate().before(cutOffDate));
	}
}
