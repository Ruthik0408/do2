package itsdc.dolphin.do2.auditcage;

 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

 
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * Author : IT & SDC, Secunderabad
 */

@RestController
public class Adinc extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	private static final long serialVersionUID = 1L;
	Employee employee = null;
	List<PayScale> payList = null;
	boolean incrValidity = true;
	String status = "";
	StringBuffer buffer = new StringBuffer();
	Do2 do2;
	Rank rank;
	boolean validity = true;
	String message = "";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Invalid Rate of Increment.\n";
	static public final String REF_DOII_NOT_FOUND= "AU:Reference DOII not found.\n";
	static public final String NOT_ELIGIBLE_FOR_ADINC= "AU:PBOR not completed enough service to get Stagnation Increment.\n";
	static public final String PAY_NOT_MATCH= "AU:PBOR's pay has not reached maximum of the pay scale.\n";	
	
	List<Allowance> allowances = new ArrayList<Allowance>();
	int incrRate = 0;
	String soldierGroup;
	
	 
	
	 



	@Override
	public boolean isValid(Do2 do2) {
		employee = do2.getEmployee();
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (do2.getFromDate().after(DateManager.getSixthCpcDate())){
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("VL:Not Admissible after 1.1.2006");
			do2Repo.save(do2);
			return false;
		}
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		if (!isRateAvailable(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}		
		
		if (!isValidFromDate(do2)) {
			buffer.append(NOT_ELIGIBLE_FOR_ADINC);
			validity = false;
		}
		
		if (do2.getColumnA().equals("SECOND") || (do2.getColumnA().equals("THIRD"))) {
			if (!isValidRefDo2(do2)){
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(REF_DOII_NOT_FOUND);
				validity = false;
			}
		}

		if (!validity) {
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
	do2Repo.save(do2);
		return validity;
			
	}

		
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
			return null;
	}
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage,baseAuditCage); 
		double adIncRate = 0;
		
		if (do2.getColumnA().equals("FIRST")) {
			adIncRate = do2.getRate();
		}
		
		if (do2.getColumnA().equals("SECOND")) {
			adIncRate = do2.getRate()*2;
		}
			
		if (do2.getColumnA().equals("THIRD")) {
			adIncRate = do2.getRate()*3;
		}
		
		AuditCageHelper.updateAuditCageAllowance(newAuditCage,do2.getDescription(),adIncRate);
 	
		return Do2Manager.SUCCESS;
	}
	
	
	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return ((rank != null) && (rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0)); 
	}	

	public boolean isRateAvailable(Do2 do2) {
		return (do2.getRate() == getIncrRate(do2));
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = null;
		refDo2 = Utils.getReferenceDo2(do2);
		return (refDo2!=null);
	}
	
	public boolean isValidPay(Do2 do2) {
		
		String soldierGroup;
		employee = do2.getEmployee();
		
		soldierGroup = employee.getGroupCategory().getSoldierGroup();
		
		List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(), soldierGroup, do2.getFromDate());
				 
		if (Utils.getAuditCage(do2).getBasicPay().equals(payList.get(0).getScaleEnd0())) {
			return true;
		} else
			return false;
	}
	public boolean isValidFromDate(Do2 do2) {
		employee = do2.getEmployee();
		int stagPeriod = 0;		
		List<Do2>incrDo2List = do2Repo.findByDescStatus(employee.getId(),Do2Manager.VALID,"INCR");
			 
		 
		if (incrDo2List.size() > 0 ) {
			 Do2 incrDo2 = incrDo2List.get(incrDo2List.size());
			 Calendar cal = new GregorianCalendar();
			 cal.setTime(incrDo2.getFromDate());
			 int year = cal.YEAR;
			 int month = cal.MONTH;
			 int day = cal.DATE;
			 try {
				 incrDo2.setFromDate(DateManager.getDate(year, month, day));
			 } catch (Exception e) {
				 Utils.handleException(e);
			 }
			 stagPeriod = (int) DateManager.getNoOfMonths(do2.getFromDate(), incrDo2.getFromDate());
		}
	 	if ((do2.getColumnA().equals("FIRST"))  && (stagPeriod == 24) ||
	 		 do2.getColumnA().equals("SECOND")  && (stagPeriod == 48) ||
	 		 do2.getColumnA().equals("THIRD")   && (stagPeriod == 72)){
	 		return true;
	 	} else {
	 		return false;
	 	}
	}

	public int getIncrRate(Do2 do2) {
		String soldierGroup;
		int incrRate = 0;
		employee = do2.getEmployee();
		
		
		soldierGroup = employee.getGroupCategory().getSoldierGroup();
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(), soldierGroup, do2.getFromDate());
		 
		
		if (payList.size() == 1 )	{
				incrRate = payList.get(0).getIncr0();
		}
		return incrRate;
}
	
}
