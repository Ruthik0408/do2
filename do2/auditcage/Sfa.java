package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
 
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

 
@RestController
public class Sfa extends AuditCageBase implements Do2AuditCageProcessor {
	
	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();
	List<Allowance> alwList = null;
	
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
 	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table. \n";
 	static public final String SERVICE_DETAIL_MISSING = "AU:Employee service details are missing. \n";
 
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
		return true;
		}
			 
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		if (!isServiceDetailAvailable(rank)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_MISSING);
			validity = false;
		}
		
		if (!isValidRate(do2, rank)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}

		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
				
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	 
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), do2.getRate());
				AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			} else {
				double sfaAmount=0.0;				
				if (do2.getRate() == 2000 || do2.getRate() == 2400 || do2.getRate() == 3600)  {
					sfaAmount=6000;
				} else if ( do2.getRate() == 3240 || do2.getRate() == 3880 || do2.getRate() == 5820) {
					sfaAmount=9700;
				} else {
					sfaAmount=do2.getRate();
				}
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), sfaAmount);
				AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);				
			}			
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	// *************************************************************************
	
	public boolean isServiceDetailAvailable(Rank rank) {
		if (rank == null)
			return false;
		else
			return true;
	}
	
	public boolean isValidRate(Do2 do2, Rank rank) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), rank);
 		 
		for (Allowance allw : alwList) {
			if (allw.getRateAmount()==do2.getRate()) {
				return true;
			} else {
				if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) &&
						do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc())) {
					if (do2.getRate() == 2000 || do2.getRate() == 2400 || do2.getRate() == 3240 || do2.getRate() == 3600
							|| do2.getRate() == 3880 || do2.getRate() == 5820) {
						return true;
					}
				} else if (!do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc())) {
					if (do2.getRate() == 6000 || do2.getRate() == 9700) {
						return true;
					}
				}
			}
				
		}
		
		return false;
	}
}