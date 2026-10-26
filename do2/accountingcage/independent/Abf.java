package itsdc.dolphin.do2.accountingcage.independent;

 

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
 
import javax.persistence.EntityManager;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.GregorianCalendar;

 
public class Abf extends AccountingCageBase implements java.io.Serializable {

	static final String RECRUIT_STATUS = "AU:Not Admissible To Recruits.\n";
	static public String INVALID_RATE = "VL:Rate column should not be blank / Rate Exceeds Rs.3000.00\n";
	static public String INVALID_FROMDT = "AU:ABF From Date not within AL/TD period.\n";
	static public String INVALID_WITH_PAL_BAL = "AU:ABF is already adjusted for PAL/BAL in the calendar year.\n";
	static public String ABF_ALREADY_ADJUSTED = "AU:ABF is already adjusted for to and fro within this leave/TD period.\n";
	
	String refDo2 = null;
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	 

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		refDo2 = null;

		if (!(isValidRank(do2))) {
			validity = false;
			buffer.append(RECRUIT_STATUS);
		}

		if (!(isValidRate(do2))) {
			validity = false;
			buffer.append(INVALID_RATE);
		}

		if (!(isValidFromDate(do2))) {
			validity = false;
			buffer.append(INVALID_FROMDT);
		}


		if (isAbfAdjTwiceWithinLeaveTdPeriod(do2)) {
			validity = false;
			buffer.append(ABF_ALREADY_ADJUSTED);
		}
		
		if (isAbfValidWithPalBal(do2)) {
			validity = false;
			buffer.append(INVALID_WITH_PAL_BAL);
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			if (do2.getId()==0 && refDo2 != null){
				do2.setRefDo2No(refDo2);
			}
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
	 
		do2Repo.save(do2);
		
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		Utils.addArrears(do2, Math.round(do2.getRate()));
		return Do2Manager.SUCCESS;
	}

	// ****************************************************************************************

	public boolean isValidRank(Do2 do2) {
      if (Utils.getPao().getPaoCode().equals("94"))
    	  return true;
      if (employee.getHavDate() != null && employee.getHavDate().equals(employee.getEnrolmentDate()) ||
    	  employee.getNbsubDate() != null && employee.getNbsubDate().equals(employee.getEnrolmentDate()))
    	  return true;
      if (employee.getAttestationDate() != null && do2.getFromDate().after(employee.getAttestationDate()))
    	  return true;
      if (employee.getInitialMusteringDate() != null && do2.getFromDate().after(employee.getInitialMusteringDate()))
    	  return true;
      if (employee.getRemusteringDate() != null && do2.getFromDate().after(employee.getRemusteringDate()))
    	  return true;
      return false;
	}

	private boolean isValidRate(Do2 do2) {
		if (do2.getRate()==null || do2.getRate()==0) {
			return false;
		} else if (do2.getRate()>3000) {				//	Check introduced on 26.09.2019.
			return false;
		}
		return true;
	}
	
	public boolean isValidFromDate(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date startDate = DateManager.getDate(do2.getFromDate(), -5);
		Date endDate = DateManager.getDate(do2.getFromDate(), +5);
		List<String> descList=Arrays.asList(new String[] {"TYOJ","TYRJ","LDAILY"});
		List<Do2> do2List = null;
		
		do2List = do2Repo.findByDescListFromDateEqualAndFromDateLessThan(do2.getEmployee().getId(), descList, do2.getFromDate(), DateManager.getSeventhCpcAllowanceDate());
				
	 
		if (do2List.size() > 0){
			refDo2 = Utils.getReferenceDo2No(do2List.get(0));
			return true;
		}	
		
		descList=Arrays.asList(new String[] {"RTYOJ","RTYRJ"});
		do2List =do2Repo.findByDescListFromDateEqualAndFromDateGreaterThanEqual(do2.getEmployee().getId(), descList, do2.getFromDate(),DateManager.getSeventhCpcAllowanceDate());
		 
		if (do2List.size() > 0){
			refDo2 = Utils.getReferenceDo2No(do2List.get(0));
			return true;
		}	
		
		descList=Arrays.asList(new String[] {"AL","PAL","BAL"});
		
		// added on 08.10.2024 - AOC irla
		startDate = DateManager.getDate(do2.getFromDate(), -3);
		endDate = DateManager.getDate(do2.getToDate(), +3);
		
		do2List =do2Repo.findByDescListAndFromDateBetween(do2.getEmployee().getId(), descList, startDate, endDate);
		
		if (do2List.size() > 0){
			refDo2 = Utils.getReferenceDo2No(do2List.get(0));
			return true;
		}
		 
		return false;
	}

	public boolean isAbfAdjTwiceWithinLeaveTdPeriod(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		Do2 referenceDo2 = null;
		boolean alreadyAdjusted = false;
		int times = 0;
		List<Do2> abfDo2List =do2Repo.findByEmpIdRefDo2NoDescStatus(do2.getEmployee().getId(), refDo2, "ABF",Do2Manager.VALID);
			 
		if (abfDo2List.size() > 0) {
			for (Do2 d : abfDo2List) {
				referenceDo2 = Utils.getReferenceDo2(d);
				if (referenceDo2 != null) {
					if (referenceDo2.getDescription().equals("TYOJ") ||
						referenceDo2.getDescription().equals("TYRJ")) {
							alreadyAdjusted = true;
							break;
					}
					if (referenceDo2.getDescription().equals("TYOJ") ||
						referenceDo2.getDescription().equals("TYRJ")) {
							alreadyAdjusted = true;
							break;
					}
					if (referenceDo2.getDescription().equals("AL") ||
						referenceDo2.getDescription().equals("PAL") || 
						referenceDo2.getDescription().equals("BAL")) {
						    ++times;
						    if (times == 2) {
						    	alreadyAdjusted = true;
						    	break;
						    }
					}
				}
			}			
		}			
		return alreadyAdjusted;
	}
	
	
	
/*
	public boolean isAbfValidWithPalBal(Do2 do2) {
		boolean validity = true;
		Do2 referenceDo2 = null;
		String description = null;
		String description1 = null;
		List<Do2> do2List =do2Repo.findByEmployeeIdAndRefDo2NoAndStatus(do2.getEmployee().getId(),refDo2, Do2Manager.VALID);
		 
		if (do2List.size() > 0){
			description = do2List.get(0).getDescription();
		}
		GregorianCalendar cal1 = new GregorianCalendar();
		GregorianCalendar cal2 = new GregorianCalendar();
		cal1.setTime(do2.getFromDate());
		int do2Year = cal1.get(Calendar.YEAR);

		//requires review as list size may be large
		List<Do2> abfDo2 =do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(),"ABF",Do2Manager.VALID); 
				
			 
		if (abfDo2.size() > 0) {
			
			for (Do2 abfdo : abfDo2) {

				cal2.setTime(abfdo.getFromDate());
				int abfYear = cal2.get(Calendar.YEAR);
				if (do2Year == abfYear) {
					final String[] abfValidWith = {"PAL", "BAL" };
					final List<String> leaveTd = Arrays.asList(abfValidWith);
					
					Do2 abfRefDo2 = Utils.getReferenceDo2(abfdo, leaveTd);
					if (abfRefDo2 != null) {
						if ((abfRefDo2.getDescription().equals("PAL")) ||
							(abfRefDo2.getDescription().equals("BAL"))) {
							  description1 = abfRefDo2.getDescription();
						}
					}
					if (description != null && description1 != null && !description.equals(description1)){
						validity = false;
						break;
					}
				}
			}
		}
		return validity;
	}*/
	
	
	public boolean isAbfValidWithPalBal(Do2 do2) {
		boolean validity = false;
		Do2 referenceDo2 = null;
		String description = null;
		String description1 = null;
		
		System.out.println ( " ref do2 " +refDo2);
		List<Do2> do2List =do2Repo.findByEmployeeIdAndRefDo2NoAndStatus(do2.getEmployee().getId(),refDo2, Do2Manager.VALID);
		 System.out.println ( " do2 list size abf " +do2List.size());
		if (do2List.size() > 0){
			description = do2List.get(0).getDescription();
			System.out.println ( " description " +description   +" id " +do2List.get(0).getId()  );
		}
		GregorianCalendar cal1 = new GregorianCalendar();
		GregorianCalendar cal2 = new GregorianCalendar();
		cal1.setTime(do2.getFromDate());
		int do2Year = cal1.get(Calendar.YEAR);

		//requires review as list size may be large
		List<Do2> abfDo2 =do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(),"ABF",Do2Manager.VALID); 
				
			 
		if (abfDo2.size() > 0) {
			
			for (Do2 abfdo : abfDo2) {
					System.out.println ( " abs do2 desc "+abfdo.getDescription()  +" abfdo2 fromdate " +abfdo.getFromDate()   +" id " +abfdo.getId()  + " ref do2 " +abfdo.getRefDo2No());
				cal2.setTime(abfdo.getFromDate());
				int abfYear = cal2.get(Calendar.YEAR);
				if (do2Year == abfYear) {
					System.out.println (  " im here ");
					System.out.println ( " xxxxabs do2 desc "+abfdo.getDescription()  +" abfdo2 fromdate " +abfdo.getFromDate()   +" id " +abfdo.getId()  + " ref do2 " +abfdo.getRefDo2No());

					final String[] abfValidWith = {"PAL", "BAL" };
					final List<String> leaveTd = Arrays.asList(abfValidWith);
					
					Do2 abfRefDo2 = Utils.getReferenceDo2(abfdo, leaveTd);
					if (abfRefDo2 != null) {
						if ((abfRefDo2.getDescription().equals("PAL")) ||
							(abfRefDo2.getDescription().equals("BAL"))) {
							  description1 = abfRefDo2.getDescription();
							  System.out.println ( " description 1 " +description1  + " description " +description  +" ref do2 " +abfRefDo2.getDo2No() + " from date " +abfRefDo2.getFromDate());
						}
					}
					if (description != null && description1 != null && description.equals(description1)){
						validity = true;
						break;
					}
				} else { 
					validity=false; 
				}
			}
		}
		return validity;
	}
}
