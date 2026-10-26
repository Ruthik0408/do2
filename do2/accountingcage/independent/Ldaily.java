package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.InvalidDateRangeException;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Ldaily extends AccountingCageBase implements java.io.Serializable {


	 
	String message;

	static public final String INVALID_COLUMN7A = "VL:Departure time not mentioned in Col. 7A.\n ";
	static public final String INVALID_COLUMN7B = "VL:Arrival time not mentioned in Col. 7B.\n ";
	static public final String INVALID_TIME = "VL:Invalid Departure/Arrival time in Column.7A/7B.\n ";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate/City/Basic/GradePay/PayRange not matched/available.\n ";
	static public final String INVALID_PERIOD = "AU:Period is more than 15 days / less than 6 Hrs / overlapping with leave.\n";

	private double totalLdaily, ldailyAmount, arrivalHrs, departureHrs, arrdepHrs, do2Days, totLraDays, allowRateamount, differenceHrs, departureTime, arrivalTime = 0.0;
	private Integer basicPay;
	private double dayMinutes = 24 * 60;
	private StringBuffer buffer = new StringBuffer();


	
	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			getLraDays(do2);
		    double daRate = pickCorrectRate(do2);
		 	ldailyAmount = totLraDays * daRate;
			totalLdaily = Math.round(ldailyAmount/2);
			Utils.addArrears(do2, totalLdaily);
		}
		    return Do2Manager.SUCCESS;
	}
	

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		String columnA = do2.getColumnA();
		String columnB = do2.getColumnB();
		do2.setLra(0);
		
		if(columnA !=null) {
			int i = do2.getColumnA().indexOf("H");
			if (i > 0){
				do2.setColumnA(columnA.substring(0,columnA.lastIndexOf("H"))); 
			} else {
				i = do2.getColumnA().indexOf("h");
				if (i > 0)
					do2.setColumnA(columnA.substring(0,columnA.lastIndexOf("h"))); 			
			}
		}
		if(columnB !=null) {
			int i = do2.getColumnB().indexOf("H");
			if (i > 0){
				do2.setColumnB(columnB.substring(0,columnB.lastIndexOf("H"))); 
			} else {
				i = do2.getColumnB().indexOf("h");
				if (i > 0)
					do2.setColumnB(columnB.substring(0,columnB.lastIndexOf("h"))); 			
			}
		}
		if (!isValid7a(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMN7A);
		}
		if (!isValid7b(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMN7B);
		}
		if (!isValidTime(do2)) {
			validity = false;
			buffer.append(INVALID_TIME);
		}
		if (!isValidLdaily(do2)) {
			validity = false;
			buffer.append(INVALID_PERIOD);
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		} 
		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			} 
		} else {
			do2.setStatus(Do2Manager.VALID);

			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
		return validity;
	}

	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA() != null) {
			return true;
		}
		return false;
	}

	public boolean isValid7b(Do2 do2) {
		if (do2.getColumnB() != null) {
			return true;
		}
		return false;
	}

	public boolean isValidLdaily(Do2 do2) {
		double ldailyDays = 0.0;
		List<String> nonAllowanceList = Arrays.asList(new String[] { "OSL",
				"AWL", "HOSPTL", "POSIN", "POSOUT", "ADVAL", "BAL", "AL", "EXTNLV",
				"TYOJ", "TYRJ", "ACCMAL", "CL", "DAILY", "EXTNAL", "EXTNCL",
				"EXTNSL", "PAL", "RL", "SL", "RTYOJ", "RTYRJ", "RDAILY" });
				
		ldailyDays = DateManager.getNoOfDays(do2.getFromDate(), do2
					.getToDate());
		
		if (!Utils.isMutexAllowance(do2.getArmyNo(), nonAllowanceList, do2
				.getFromDate(), do2.getToDate(),do2)) {
			return true;
		}
		if (do2.getFromDate().equals(do2.getToDate()) && differenceHrs > 6) {
			return true;
		}
		if (ldailyDays > 1 && ldailyDays <= 15) {
			return true;
		}
		return false;
	}
	

	
	public boolean isValidTime(Do2 do2){
	
		if(!DateManager.isValidTime(do2.getColumnA()) || !DateManager.isValidTime(do2.getColumnB()))
			return false;
		try {
			double arrivalTime = (DateManager.getMinuteCount(do2.getColumnA())) / 60.0;
			double departureTime = (DateManager.getMinuteCount(do2.getColumnB())) / 60.0;
			if (do2.getFromDate().equals(do2.getToDate()) && (arrivalTime < departureTime && do2.getColumnA() != null && do2.getColumnB() != null)) {
				return true;
			}
			if (!do2.getFromDate().equals(do2.getToDate())) {
				return true;
			}
		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);
		}
		
		return false;
	}
	
	public boolean isRateAvailable(Do2 do2) {		
		String city = "CCOTH";
		
		if(!Utils.isValidAllowanceRate("DAILY",do2.getFromDate(),do2.getFromDate(),city,(do2.getRate())))
			return false;
		
		return true;
	}
	
	public double pickCorrectRate(Do2 do2) {
		double daRate = 0.0;
		String city = "CCOTH";		
		List<Allowance> allowances = Utils.getApplicableAllowanceRates("DAILY", do2.getFromDate(), do2.getToDate(),city, getBasicPay(do2));
	
		if (allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0) {
			daRate = allowances.get(0).getRateAmount();
			return daRate;
		}
		return daRate;
	}

	public Integer getBasicPay(Do2 do2) {
		
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
			List<AuditCage> acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(emp.getId(),do2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT ac from AuditCage ac " +
					"WHERE ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false " +
					" AND ac.recordStatus!='P' AND ac.fromDate<=:fromDate ORDER BY ac.fromDate DESC")
					.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo())).setParameter("fromDate", do2.getFromDate()).getResultList();*/
	
			if (acList.size() > 0) {
				if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()))
					basicPay = acList.get(0).getOldBasicPay() + acList.get(0).getClassPay();
				else
					basicPay = acList.get(0).getBasicPay();
			}
			return basicPay;
	}

	public double getArrivalTime(Do2 do2) {
		
		try {
			   String columnA= do2.getColumnA();
			   String columnB = do2.getColumnB();
			   if(columnA !=null && columnA.equals("2400")){
				    columnA = "2359";
			   }
				   
			   if(columnB !=null && columnB.equals("2400")){
				    columnB = "2359";
			   }
				if (do2.getFromDate().equals(do2.getToDate())) {
					arrivalHrs = (DateManager.getMinuteCount(columnA)) / 60.0;
				} else {
					arrivalHrs = (dayMinutes - DateManager.getMinuteCount(columnA)) / 60.0;
				}
				departureHrs = DateManager.getMinuteCount(columnB) / 60.0;

		} catch (InvalidDateRangeException e) {
				Utils.handleException(e);
		}
    
		if (do2.getFromDate().equals(do2.getToDate())) {
				arrdepHrs = departureHrs - arrivalHrs;				
				if (arrdepHrs <= 6) {
					totLraDays = 0;					
				} else if (arrdepHrs > 6 && arrdepHrs <= 12) {
					totLraDays = 0.70;
				} else if (arrdepHrs > 12 && arrdepHrs < 24) {
					totLraDays = 1;
				}
		} else {
				if (arrivalHrs <= 6) {
					totLraDays = 0;					
				} else if (arrivalHrs > 6 && arrivalHrs <= 12) {
					totLraDays = 0.70;					
				} else if (arrivalHrs > 12 && arrivalHrs < 24) {
					totLraDays = 1;
				}
		}
			return totLraDays;
	}

	public double getDepartureTime(Do2 do2) {
                
		if (!do2.getFromDate().equals(do2.getToDate())) {
			 
			    if (departureHrs > 6 && departureHrs <= 12) {
					totLraDays = totLraDays + 0.70;					
				} else if (departureHrs > 12 && departureHrs < 24) {
					++totLraDays;
				}
			}
			return totLraDays;
	}
		
	public double getLraDays(Do2 do2) {	
		    double ldailyDays = 0.0;
	    	getArrivalTime(do2);
	    	getDepartureTime(do2);	
	    	do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			if (do2.getFromDate().equals(do2.getToDate())) 
				ldailyDays = do2Days - 1;				
	        else 
				ldailyDays = do2Days - 2;	
			totLraDays = totLraDays + ldailyDays;
		return totLraDays;
	}
}
