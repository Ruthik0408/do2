package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;

import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 


@RestController
//This is effective from 01042013 
public class Rumcig extends AuditCageBase implements Do2AuditCageProcessor {


	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

//  static public final String INVALID_DATE = "VL:Admissible from 01042013 only \n";
	static public final String INVALID_DATE = "VL:Admissible from 01072017 only \n";
	static public final String RATE_NOT_MATCHING = "AU:Rate Not Matching with ColumnB. \n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String INVALID_COLUMNB_VALUE = "VL:Incorrect Column_7B Value. \n";

	
	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA().equals("CEASED")){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;			
		}
		
		List<String> validColumnBList = Arrays.asList(new String[] {"FIELD1", "FIELD2", "FIELD3", "FIELD4", "PEACE"});
		if (do2.getColumnB()==null || (do2.getColumnB()!=null && !validColumnBList.contains(do2.getColumnB()))) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMNB_VALUE);
			do2Repo.save(do2);
			return false;			
		}
		
		Date allowanceStartDate =null;
		/*try {
			allowanceStartDate = DateManager.getDate("2013-04-01");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		try {
			allowanceStartDate = DateManager.getDate("2017-07-01");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(do2.getFromDate().before(allowanceStartDate)){
			do2.setReason(INVALID_DATE);
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			
			return false;
		}
		//Rank rank  = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		Rank rank  = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
			boolean validity = true;
			
		/*if(newDo2.getId()==do2.getId()){
			 if(!isRateMatching(do2)){
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(RATE_NOT_MATCHING);
				validity =false;
			
			}
		}*/
		
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
		double rate = 0.0;
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
    
		if(do2.getColumnA().equals("GRANTED")){
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
				do2.getColumnB(),do2.getFromDate(),do2.getFromDate());
		 
		if (alwList.size() > 0) {
			 rate = alwList.get(0).getRateAmount();
		}
		
		  //  double payableRate = (rate * 10)/12; Allowance rate table updated after rationalisation
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), rate);
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

	public boolean isRateMatching(Do2 do2) {
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
				do2.getColumnB(),do2.getFromDate(),do2.getFromDate());
		  if(alwList.size()>0){
			  
			  if(alwList.get(0).getRateAmount()==do2.getRate()){
				  return true;
			  }
		  }
		 return false;
	}
}