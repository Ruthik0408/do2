package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Sccia extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	
	Rank rank;
	
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	static public String COLUMN_7B_BLANK = "AU:Column 7b should not be left blank after 1.5.1999.\n";
	static public String AFTER_EXPIRY_DATE = "AU:Claim After Expiry Date Of The Allowance - 22022019.\n";
	
	
	public List<Allowance> alwList;
	private StringBuffer buffer = new StringBuffer();
	
	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {	
			if (!do2.getDo2Date().before(DateManager.getRiskAndHaucaPublicationStartDate())
					&& !do2.getFromDate().before(DateManager.getRiskAndHaucaAllowanceStartDate())) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(AFTER_EXPIRY_DATE);
				do2Repo.save(do2);
				return false;
			}
		}
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.GRANTED)){
			if ((do2.getFromDate() != null && do2.getFromDate().after(DateManager.getSixthCpcDate())) && do2.getColumnB() == null){
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(COLUMN_7B_BLANK);
			}
		}
		if(!isValidServiceDetail(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_UNAVAILABLE);
		}
		
		if(!isValidAllowanceRate(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
		}
		if(validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
			return validity;
	}

	public boolean isValidServiceDetail(Do2 do2) {
		return rank != null;
	}
	
	public boolean isValidAllowanceRate(Do2 do2) {
		  if (do2.getColumnB() == null ){
			  alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						 DateManager.getScciaOldDate(),DateManager.getScciaOldDate(), rank);
		  } else {
			  alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						do2.getColumnB(), do2.getFromDate(), do2.getFromDate(), rank);	  
		  }
		
		return (alwList.size() > 0);
	}


	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		  
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
		    rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		    if(do2.getColumnB() ==null){
			  alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						 DateManager.getScciaOldDate(),DateManager.getScciaOldDate(), rank);
		    }else{
			  alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						do2.getColumnB(), do2.getFromDate(), do2.getFromDate(), rank);	  
		    }
			
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			if(alwList.size() > 0) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.SCCIA, alwList.get(0).getRateAmount());
				AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			}
		} else  {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}