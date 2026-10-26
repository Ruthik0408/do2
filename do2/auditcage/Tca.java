package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Tca extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	Employee emp;
	Rank rank;
		
	
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Rate is not available in Allowance Rate table.\n";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	static public String IS_A_RECRUIT = "AU:Not admissible to Recruit.\n";
	
	 
	public List<Allowance> alwList;
	private StringBuffer buffer = new StringBuffer();
	
	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
        if(rank==null){
        	buffer.append(SERVICE_DETAIL_UNAVAILABLE);
        	do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
        	return false;
        }
        
        if(rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)){
          buffer.append(IS_A_RECRUIT);
          do2.setStatus(Do2Manager.PENDING);
          validity=false;
        }
		


		if(!isValidAllowanceRate(do2)){
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
			validity=false;
		}
		if(!validity){
			if(do2.getStatus() !=null && do2.getStatus() !=Do2Manager.PENDING){
				do2.setStatus(Do2Manager.INVALID);
			}
		}else{
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
		}
		
		do2.setReason(buffer.toString());
		
		
		return validity;
	}

	
	
	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
				do2.getFromDate(), do2.getFromDate(),rank);
		 
		return alwList.size()>0;
	}


	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	    AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
			rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
			emp = Utils.getCurrentEmployee(do2.getArmyNo());
		
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						do2.getFromDate(), do2.getFromDate(), rank);
			if(alwList != null)
			    AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.TCA, alwList.get(0).getRateAmount());
	 
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			final String[] alwUnderProcess = { Do2Manager.TCA };
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