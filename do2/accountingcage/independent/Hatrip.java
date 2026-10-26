package itsdc.dolphin.do2.accountingcage.independent;
import  itsdc.dolphin.do2.AccountingCageBase;
import  itsdc.dolphin.do2.Allowance;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import  itsdc.dolphin.do2.Do2Manager;
import  itsdc.dolphin.utils.DateManager;
import  itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Hatrip extends AccountingCageBase implements java.io.Serializable {

	 
	EntityManager entityManager;
	String subMessage = "";
	Employee employee;
	List<Allowance> alwList;
	static public final String MUTUALLY_EXCLUSIVE_DESCRIPTIONS = "AU:Dates overlap with mutually exclusive descriptions. \n";
	static public String IS_A_RECRUIT = "AU:Individual is a Recruit.\n ";
	static public String INVALID_DAYS = "VL:Invalid number of days.\n ";
	static public String RATE_NOT_AVAILABLE = "AU:Rate not available in allowance rate table.\n ";

	private double hatripDays = 0.0;
	private StringBuffer buffer = new StringBuffer();
	Rank rank;
	String message;

		@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (isMutex(do2)) {
			validity = false;
			buffer.append(MUTUALLY_EXCLUSIVE_DESCRIPTIONS);
		}
		if (isRecruit(do2)) {
			validity = false;
			subMessage += IS_A_RECRUIT;
		}
		if (!isValidDays(do2)){
			validity = false;
			subMessage += INVALID_DAYS;
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					 do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
			message = buffer.toString();
			do2.setReason(message);
			
			do2Repo.save(do2);
		
		 	return validity;
	}
    
	@Override
	public String process(Do2 do2) {
		double amount = 0.0;
		if (!isValid(do2))
			return Do2Manager.FAILURE;		
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
				do2.getFromDate(), do2.getFromDate(), rank);
		amount=alwList.get(0).getRateAmount();
		GregorianCalendar cal2 = new GregorianCalendar(1999, Calendar.MAY, 01);
		Date toDate = new Date();
		toDate.setTime(cal2.getTimeInMillis());
		// allowance rate gives amount for <= 10 days. For more than 10 days
		if (!(do2.getToDate().before(toDate) && hatripDays > 10)) {
			if (amount == 72){
				amount = 86;
			} else if (amount == 98) {
			    amount = 117;
			} else if (amount == 128) {
			    amount = 153;
			}
		}
		if (alwList.size() > 0) {
			Utils.addArrears(do2, Math.round(amount));			
		}
		return Do2Manager.SUCCESS;
	}
	public boolean isRecruit(Do2 do2) {
		if (employee == null) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		}
		if (employee.getInitialMusteringDate() != null
				&& employee.getInitialMusteringDate().after(do2.getFromDate())) {
			
			return true;
		}
		return false;
	}
	
	public boolean isMutex(Do2 do2) {
		List<String> nonAllowanceList = Arrays.asList(new String[] { "OSL",
				"AWL", "HOSPTL", "POSIN", "POSOUT", "ADVAL", "BAL", "AL", "EXTNLV",
				"TYOJ", "TYRJ", "ACCMAL", "CL", "DAILY", "LDAILY", "EXTNAL",
				"EXTNCL", "EXTNSL", "PAL", "RL", "SL" , "RTYRJ", "RTYOJ", "RDAILY"});
		
		if (Utils.isMutexAllowance(do2.getArmyNo(), nonAllowanceList,
						do2.getFromDate(), do2.getToDate(),do2)) {
			return true;
		}
		return false;
	}

	public boolean isValidDays(Do2 do2) {
		double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		GregorianCalendar cal2 = new GregorianCalendar(1999, Calendar.MAY, 01);
		Date toDate = new Date();
		toDate.setTime(cal2.getTimeInMillis());
		
		if ((do2.getToDate().before(toDate) && hatripDays <= 10) 
				|| (!(do2.getToDate().before(toDate)) && hatripDays <= 12)) {
			return true;
			/*if (do2Days <= 10){
				hatripDays = 10;
				return true;
			} else if(do2Days >10 && do2Days <=12){
				hatripDays = 12;
				return true;
			}*/
		} 
		return false;
	}
	
}
