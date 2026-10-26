package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.InvalidDateRangeException;
import itsdc.dolphin.utils.Utils;

// Daily Allowance for Local Journey to temporary place of duty 8 kms away  from HQrs.
@RestController
public class Locjrn extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static public final String INVALID_RATE = "VL:Invalid Rate. This should be 15,35,50. \n "; 
	static public final String INVALID_COL7A = "VL:Departure time not mentioned in ColA or incorrect. \n ";
	static public final String INVALID_COL7B = "VL:Arrival time not mentioned in ColB or incorrect. \n ";
	static public final String INVALID_COL7C = "VL:Transport within city amount to be shown in Col-7C \n ";
	public double totalDaily = 0.0, foodAmount = 0.0, transportAmount = 0.0;
	public double totFoodAmt = 0.0, totTransAmt= 0.0;
	
	
    private boolean todatesame = false;
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
		if (!isValidRate(do2)) {
			validity = false;
			buffer.append(INVALID_RATE);
		} 
		if (!isValid7a(do2)) {
			validity = false;
			buffer.append(INVALID_COL7A);
		}
		if (!isValid7b(do2)) {
			validity = false;
			buffer.append(INVALID_COL7B);
		}
		if (!isValid7c(do2)) {
			validity = false;
			buffer.append(INVALID_COL7C);
		}
		

		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe() == null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}

		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		double do2Days = 0.0;
		if (isValid(do2)) {
			if (todatesame){
				if (do2.getToDate().after(do2.getFromDate())){
					do2Days = DateManager.getNoOfDays(DateManager.getNextDate(do2.getFromDate()), do2.getToDate());
				}
			} else {
				do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			}

			List<Allowance> allowances = Utils.getApplicableAllowanceRates(
					Do2Manager.RDAILY, getGradePay(do2), do2.getFromDate(), do2.getToDate());
			
			
			
			if (allowances.size() > 0) {
											
				if(allowances.get(0).getRatePercentage() > 0 ) {
					
						foodAmount = ((do2.getRate()/100) * allowances.get(0).getRatePercentage());
					
				}
				
				
				if(allowances.get(0).getRateAmount() > 0 ) {
					if (Integer.parseInt(do2.getColumnC()) < allowances.get(0).getRateAmount()) {
						transportAmount = Integer.parseInt(do2.getColumnC());
					} else {
						transportAmount = allowances.get(0).getRateAmount();
					}
				}
				
				totFoodAmt  = (int) Math.round(do2Days * foodAmount);
				totTransAmt =  (int) Math.round(do2Days * transportAmount);
				
				totalDaily = totFoodAmt+totTransAmt;
	
            Utils.addArrears(do2, totalDaily,Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());
		}
	}
		return Do2Manager.SUCCESS;
	}

	
	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA() != null){
			try {
				Integer.parseInt(do2.getColumnA().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public boolean isValid7b(Do2 do2) {
		if (do2.getColumnB() != null){
			try {
				Integer.parseInt(do2.getColumnB().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isValid7c(Do2 do2) {
		if (do2.getColumnC() != null){
			try {
				Integer.parseInt(do2.getColumnC().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	
	private boolean isValidRate(Do2 do2) {
	/*	if (do2.getRate()==null || do2.getRate()==0) {
			return false;
		} else if (do2.getRate() != 15.0 || do2.getRate() != 30.0 || do2.getRate() != 50.0) {				
			return false;
		}
		return true;*/
		//14.07.2026
		Double rate = do2.getRate();
		if (rate == null) {
			return false;
		}
		return (rate.equals(15.0) || rate.equals(35.0) || rate.equals(50.0));		
	}
	public int getGradePay(Do2 do2) {
		int gradePay = 0;
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(emp.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT ac from AuditCage ac WHERE ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND " +
				" ac.absentFlag=false AND ac.recordStatus!='P' AND ac.fromDate<=:fromDate ORDER BY ac.fromDate DESC")
				.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.setParameter("fromDate", do2.getFromDate()).getResultList();*/

		if (acList.size() > 0) {
			if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				gradePay = acList.get(0).getOldGradePay();
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
				gradePay = acList.get(0).getGradePay();
			}
		}
		return gradePay;
	}

	
}
