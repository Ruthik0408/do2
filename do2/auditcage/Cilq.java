package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Cilq extends AuditCageBase implements Do2AuditCageProcessor,
		java.io.Serializable {

	List<Allowance> alwList;
	static public String INVALID_AGE = "AU:PBOR less than 25 years of age.\n";
	static public String INVALID_RANK = "AU:Not admissible to Recruit / Hon.ranks.\n";
	static public String INVALID_CILQ = "AU:CILQ is not admissible for Unmarried Employees.\n";
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate not available / city & rate do not match.\n";
	static public String RATES_NOT_MATCHING = "AU:City and Rate do not match.\n";
	static public String CITY_CLASS_DIFFERS = "AU:Class of City differs from previous grant of CILQ.\n";
	StringBuffer buffer = new StringBuffer();
	Rank rank;
	Employee employee;
	
	@Override
	public boolean isValid(Do2 do2) {
	
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		if (!isValidAge(do2)) {
			validity = false;
			buffer.append(INVALID_AGE);
		}

		if (!isServiceDetailAvailable()) {
			validity = false;
			buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
		} else if (!isValidRank()) {
			validity = false;
			buffer.append(INVALID_RANK);
		}
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			if (do2.getColumnC().equals("CCX") || do2.getColumnC().equals("CCY") || do2.getColumnC().equals("CCZ")){
		         validity = false;
		         buffer.append("Class of city and from date mismatch");
			}
		}
		
		if (do2.getAcceptedQe() == null && do2.getReviewQe() == null) {
			if (!isRateAvailable(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			}
		}
		if (do2.getAcceptedQe() == null && do2.getReviewQe() == null) {
			if (!isClassOfCitySame(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(CITY_CLASS_DIFFERS);
			}
		}
		
		if (validity == false && do2.getStatus() != null
				&& !do2.getStatus().equals(Do2Manager.PENDING)) {
			do2.setStatus(Do2Manager.INVALID);
		}
		if (validity == false && do2.getStatus() == null) {
			do2.setStatus(Do2Manager.INVALID);
		}

		if (validity == true) {
			do2.setStatus(Do2Manager.VALID);
		}

		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
//		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
//      CILQ abolished with effect from 01-07-2017.
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {	
			
		//	rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");   // commented on 18.04.2022 and below condition added.
			
			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
			} else {
				// To fetch MACPS Rank after 01.09.2008.  As the getrank method return previous date rank for payfix option, getnextdate is used.
				rank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()), "PAYFIX");
			}
			
			
			String columnC = do2.getColumnC();
			if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				
			 if(do2.getColumnC() != null && (do2.getColumnC().equals("CCC")) || do2.getColumnC().equals("CCOTH")){
				 columnC = "CCZ";
			 }
			 if(do2.getColumnC() != null &&  do2.getColumnC().equals("CCA1")){
				 columnC = "CCX";
			 }
			 if(do2.getColumnC() != null && (do2.getColumnC().equals("CCA")) || do2.getColumnC().equals("CCB1") 
					 || do2.getColumnC().equals("CCB2")){
				 columnC = "CCY";
			 }
			}
			
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
					do2.getFromDate(), do2.getFromDate(), rank, columnC);
			
			if(do2.getColumnC().equals("CCOTH") || do2.getColumnC().equals("CCZ")){
				 final String [] alwUnderProcess = { Do2Manager.HRA,Do2Manager.HRALF,Do2Manager.FAA};
				 final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			}else{
				final String [] alwUnderProcess = { Do2Manager.HRA,Do2Manager.HRAFAM,Do2Manager.FAA, Do2Manager.HRALF, Do2Manager.FAMLF};
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			}
		
			if (alwList.size() > 0) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.CILQ, alwList.get(0).getRateAmount());
				
			}
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
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

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> do2List = Utils.getReferenceDo2List(do2);

		for (Do2 refDo2 : do2List) {
			if (refDo2.getDescription().equals(Do2Manager.CILQ)
					&& do2.getColumnA() != null
					&& do2.getColumnA().equals(Do2Manager.GRANTED)) {
				return true;
			} else if (refDo2.getDescription().equals(Do2Manager.CILQ)
					&& do2.getColumnB() != null
					&& do2.getColumnA().equals(Do2Manager.INITIAL)) {
				return true;
			}
		}

		return false;
	}

	public boolean isValidAge(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getBirthDate() == null) {
			return false;
		}
		double empAge = DateManager.getNoOfMonths(employee.getBirthDate(), do2
				.getFromDate());
		return ((empAge / 12) >= 25);

	}

	public boolean isServiceDetailAvailable() {
		return rank != null;	
	}
	public boolean isValidRank(){
		return (rank!= null && !rank.getRankCode().equals(Do2Manager.RECRUIT) 
				&& !rank.getRankCode().equals(Do2Manager.HONYCP) && !rank.getRankCode().equals(Do2Manager.HONYLT));
	}

	public boolean isMarried(Do2 do2) {
		return (employee.getMarried() != null && employee.getMarried() == true);

	}

	public boolean isRateAvailable(Do2 do2) {
		 
		Date fromDate = do2.getFromDate();
			
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			try {
				fromDate = DateManager.getDate("2008-08-31");
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		
		if (!do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			try {
				fromDate = DateManager.getDate("2010-12-31");
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		
		if (Utils.isValidAllowanceRate(do2.getDescription(), fromDate, do2.getFromDate(), do2.getColumnC(), do2.getRate())) {
			return true;
		}
		
		String city = do2.getColumnC();
		
		if (city.equals("CCZ")) {
			city="CCOTH";
			if (Utils.isValidAllowanceRate(do2.getDescription(), fromDate, do2.getFromDate(), city, do2.getRate())) { 
				return true;
			}  
			city="CCC";
			if (Utils.isValidAllowanceRate(do2.getDescription(), fromDate, do2.getFromDate(), city, do2.getRate())) { 
				return true;
			}			
		}
		
		if (city.equals("CCY")) {
			city="CCA";
			if (Utils.isValidAllowanceRate(do2.getDescription(), fromDate, do2.getFromDate(), city, do2.getRate())) { 
				return true;
			}
		}
		
		if (city.equals("CCX")) {
			city="CCA1";
			if (Utils.isValidAllowanceRate(do2.getDescription(), fromDate, do2.getFromDate(), city, do2.getRate())) { 
				return true;
			}
		}
		
		return false;
	}
	public boolean isClassOfCitySame(Do2 do2) {
		if (do2.getColumnA() != null && !do2.getColumnA().equals("GRANTED")){
			return true;
		}		
		if (do2.getColumnB() != null && do2.getColumnB().equals("INITIAL")){
			return true;
		}
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			return true;
		}
	
		List<Do2>do2List=do2Repo.findByUnitCodeDescColAFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),do2.getUnitCode(),do2.getFromDate(),do2.getDescription(),"GRANTED");
		 
	    if (do2List == null || do2List.size() == 0)
	       return true;
	  
        if (do2List.get(0).getColumnC().equals(do2.getColumnC())){
        	return true;
        } else {
        	return false;
        }	
	}

}