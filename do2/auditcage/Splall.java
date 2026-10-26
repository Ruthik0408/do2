package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Splall extends AuditCageBase implements
 Do2AuditCageProcessor, java.io.Serializable{

	
		static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();
	
	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA().equals("CEASED")){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
			
		}

		
		Rank rank  = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
			boolean validity = true;
		
		if(validity){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
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
    
		if(do2.getColumnA().equals("GRANTED")){
			int basicPay = (int) Math.round(newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay());
				 
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(),do2.getFromDate());
		  
		    double rate = Math.round((basicPay) * alwList.get(0).getRatePercentage());
		   AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), rate);
		}else{
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

	
}