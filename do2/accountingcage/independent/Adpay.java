package itsdc.dolphin.do2.accountingcage.independent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:12-08-2009
 * 
 * IT IS ASSUMED THAT DOsII ARE PUBLISHED ONLY ON COMPLETION OF 24 SORTIES/PROPORTIONATE SORTIES FOR THE PERIOD
 * IN A ACCOUNTING YEAR  VIZ.1st OCTOBER TO 30th SEPTEMBER OF THE FOLLOWING YEAR 
 */

@RestController
public class Adpay extends AccountingCageBase implements java.io.Serializable {

	 
	String message;

	private boolean validity = true;
	private StringBuffer buffer = new StringBuffer();
	private GregorianCalendar fromDateCal = new GregorianCalendar();
	private GregorianCalendar toDateCal = new GregorianCalendar();
	private GregorianCalendar accountingYearCutOffDate = new GregorianCalendar();
	double do2RateAmount = 0;
	static public final String INVALID_FORMAT= "VL:To be published as ADALLC after 31.08.2008.\n";
	static public final String DATE_NOT_FIRST_OF_THE_MONTH = "VL:Date should be the first of the month.\n";
	static public final String DATE_NOT_LAST_OF_THE_MONTH = "VL:Date should be the last of the month.\n";
	static public final String INCORRECT_AMOUNT_IN_RATE_COLUMN = "VL:Amount in RATE Col. is incorrect.\n";
	static public final String INVALID_QUALIFYING_YEAR= "AU:Period exceeded qualifying year.\n";
	//static public final String REQUIRED_SORTIES_NOT_DONE = "AU:Less than 24 sorties in a year.\n";
	static public final String INVALID_SORTIES = "AU:Number of Sorites should be more than zero.\n";
	static public final String OVERLAP = "VL:Description is overlapping.\n";
	static public final String INVALID_DESCRIPTION = "VL: Abolished with effect from 01-07-2017\n";
	 
	// TO CHECK THE VALIDITY OF THE DOII
	@Override
	public boolean isValid(Do2 do2) {
	     
		validity=true;;
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			buffer.append(INVALID_DESCRIPTION);
			validity =  false;
			return validity;
		}
		
		
	    if (!isValidFormat(do2)) {
	    	buffer.append(INVALID_FORMAT);
	    	validity = false;
	    }
	     
		if (!isValidFromDate(do2)) {
			buffer.append(DATE_NOT_FIRST_OF_THE_MONTH);
			validity = false;
		}
	
		if (!isValidToDate(do2)) {
			buffer.append(DATE_NOT_LAST_OF_THE_MONTH);
			validity = false;
		}
		
		if(!isValidaccountingYearCutOffDate(do2)) {
			buffer.append(INVALID_QUALIFYING_YEAR);
			validity = false;
		}
	
		if (!isOverlapping(do2)) {
			buffer.append(OVERLAP);
			validity = false;
		}
		
		if (!isValidAmountInRateColumn(do2)) {
			buffer.append(INCORRECT_AMOUNT_IN_RATE_COLUMN);
			validity = false;
		}
			
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
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
	
	// PROCESS OF VALID DOII
	@Override
	public String process(Do2 do2) {
		
		if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID)) {
			Utils.addArrears(do2, (do2.getRate() * DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate())));
		}
		return Do2Manager.SUCCESS;
	}	
	//Method to check whether the DOII Description used is correct or not
	
	public boolean isValidFormat(Do2 do2) {
		if ((do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) && (do2.getToDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			if (do2.getDescription().equals(Do2Manager.ADPAY)) {
				return true;
			} else {
				return false;
			}

		} else if (do2.getDescription().equals("ADALLC")) {
			return true;
		} else {
			return false;
		}
		}
	
	
	// Method to check whether from date in current DO2 is a valid one i.e.first of the month
	public boolean isValidFromDate(Do2 do2) {
		  fromDateCal.setTime(do2.getFromDate());
		 
		if ((fromDateCal.get(Calendar.DATE) == 1)) {
			return true;
		} else {
			return false;
		}
 	}

	// Method to ensure that todate in current DO2 is the last date of the month
	public boolean isValidToDate(Do2 do2) {
 
		toDateCal.setTime(do2.getToDate());
		int days = DateManager.getNoOfDaysInMonth(toDateCal.get(Calendar.MONTH)+1,toDateCal.get(Calendar.YEAR));
		
		if (toDateCal.get(Calendar.DATE) == days) {
			return  true;
		} else {
			return  false;
		}
	}

	// Method to ensure that DO2 period is within the qualifying year
	public boolean isValidaccountingYearCutOffDate(Do2 do2) {
	 
	
		if ((fromDateCal.get(Calendar.MONTH) > 8) && (fromDateCal.get(Calendar.MONTH) < 12)) {
			accountingYearCutOffDate = new GregorianCalendar((fromDateCal.get(Calendar.YEAR)+1),Calendar.OCTOBER,1);
		} else {
			accountingYearCutOffDate = new GregorianCalendar((fromDateCal.get(Calendar.YEAR)),Calendar.OCTOBER,1);
		}
		
		if (toDateCal.getTime().before(accountingYearCutOffDate.getTime())) {
			return true;
		} else {
			return false;
		}
	}

	
	// Method to check overlapping of current DO2 with already adjusted DOs2 and other mutually exclusive items
	private boolean isOverlapping(Do2 do2) {
		boolean overlapValidity = false;
	 
		List<String> mutexNames = Arrays.asList(new String[] { "ADPAY", "ADALLC" });
		 
		try {
		overlapValidity = Utils.isMutexAllowance(do2.getArmyNo(), mutexNames, do2.getFromDate(), do2.getToDate(),do2);
		}
		catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (overlapValidity) {
			return false;
		} else {
			return true;
		}
		 
	}

	// Method to check the amount given in rate column of DO2 is a valid one
	private boolean isValidAmountInRateColumn(Do2 do2) {
 
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getToDate());
		
		if (allowanceList.size() > 0) {
		if(allowanceList.get(0).getRateAmount() == do2.getRate()) {
			return  true;
		} else {
			return  false;
		}
		
	}
		return  false;
	}
}
	
