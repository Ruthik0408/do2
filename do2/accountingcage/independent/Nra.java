package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.do2.AccountingCageBase;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Nra extends AccountingCageBase implements java.io.Serializable {

	static public final String MUTUALLY_EXCLUSIVE_DESCRIPTIONS = "OL:Dates overlap with mutually exclusive descriptions. \n";
	static public String RATE_NOT_AVIALABLE = "AU:Rate not available in allowance rate table.\n ";
	static public String INVALID_LRA = "VL:Invalid Lra days.\n";
	static public String HONY_COMMISSIONED = "AU: Not admissible to HONY COMMISSIONED OFFICERS.\n";
	static public String NOT_HONY_COMMISSIONED = "AU: Admissible only to HONY COMMISSIONED OFFICERS.\n";
	static public String FROM_DATE_BEFORE_ALLOWANCE_BIRTH = "VL:From Date is before the date on which the allowance came into effect.\n ";
	
	 
	String message;
	Employee employee;
	private StringBuffer buffer = new StringBuffer();
	List mutexList;

	
	@Override
	public boolean isValid(Do2 do2) {
		employee=Utils.getCurrentEmployee(do2.getArmyNo());
		boolean validity = true;
		Date honyDate = null;
		if (do2.getDescription().equals("DRDSRA") && !isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (isMutex(do2)) {
			validity = false;
			buffer.append(MUTUALLY_EXCLUSIVE_DESCRIPTIONS);
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVIALABLE);
	 	} 
		
		if (!do2.getDescription().equals("RMAHCO")) {
			if (employee!=null && employee.getSubstantiveRank()!=null 
					&& (employee.getSubstantiveRank().getRankName().equals("HONYLT") 
					|| employee.getSubstantiveRank().getRankName().equals("HONYCAPT"))) {
				if (employee.getHonyCaptainDate() != null && employee.getHonyLieuDate() != null)
					honyDate = employee.getHonyLieuDate();
				else if (employee.getHonyCaptainDate() == null && employee.getHonyLieuDate() != null)
					honyDate = employee.getHonyLieuDate();
				else 
					honyDate = employee.getHonyCaptainDate();
				
				if (honyDate!=null && !honyDate.after(do2.getFromDate())) {
					validity = false;
					buffer.append(HONY_COMMISSIONED);
				}
			}
		}
		
		
		  if (do2.getDescription().equals("RMAHCO")) { 
			  if (employee!=null && employee.getSubstantiveRank()!=null 
					  &&  !(employee.getSubstantiveRank().getRankName().equals("HONYLT") ||
							  employee.getSubstantiveRank().getRankName().equals("HONYCAPT"))) { 
				  validity = false; 
				  buffer.append(NOT_HONY_COMMISSIONED); 
			  }
		  
			  if (employee.getHonyCaptainDate() != null && employee.getHonyLieuDate() != null) 
				  honyDate = employee.getHonyLieuDate(); 
			  else if (employee.getHonyCaptainDate() == null && employee.getHonyLieuDate() != null)
				  honyDate = employee.getHonyLieuDate(); 
			  else honyDate = employee.getHonyCaptainDate();
			  
			  if (honyDate!=null && do2.getFromDate().before(honyDate)) { 
				  validity = false;
				  buffer.append(NOT_HONY_COMMISSIONED); 
			  }
			  
			  if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				  validity = false; 
				  buffer.append(FROM_DATE_BEFORE_ALLOWANCE_BIRTH); 
			  } 
		}
		 
		
		
		if (!validity) {
			if (do2.getStatus()==null || (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
			
		message = buffer.toString();
		do2.setReason(message);
		
 		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			double amount=0;
			List<Allowance> allowances=null;
			if (do2.getDescription().equals("GSRA")){
				 allowances = Utils.getApplicableAllowanceRates("SRA", do2.getFromDate(), do2.getToDate());	
			} else if (do2.getDescription().equals("DRDSRA")){
				 allowances = Utils.getApplicableAllowanceRates("SRA", do2.getFromDate(), do2.getToDate());
			} else if (do2.getDescription().equals("RMAHCO")){
				 allowances = Utils.getApplicableAllowanceRates("NRA", do2.getFromDate(), do2.getToDate()); 
			} else { 
				 allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate());
			}
			
			if (allowances !=null && allowances.size() >0){
				for (Allowance allow : allowances) {
				    amount +=(allow.getRateAmount()
							* DateManager.getNoOfDays(allow.getFromDate(), allow.getToDate())); 					
				}	
			}
			
			if (do2.getDescription().equals("DRDSRA")){
				Utils.addArrears(do2, Math.round(amount), "SRA", do2.getFromDate(), do2.getToDate());
			} else if (do2.getDescription().equals("RMAHCO")){
				Utils.addArrears(do2, Math.round(amount), "RMAHCO", do2.getFromDate(), do2.getToDate());
			} else {
				Utils.addArrears(do2, Math.round(amount));	
			}
			
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isMutex(Do2 do2) {
		if (Utils.isMutexAllowance(do2.getArmyNo(), Do2Manager.getMutexNames(), do2.getFromDate(), do2.getToDate(), do2)) {
			return true;
		}
		return false;
	}

	public boolean isValidLra(Do2 do2) {
		double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2.getLra() == do2Days) {
			return true;
		}
		return false;
	}

	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances =null;
		if (do2.getDescription().equals("GSRA")){
			allowances = Utils.getApplicableAllowanceRates("SRA", do2.getFromDate(), do2.getToDate());	
		} else if (do2.getDescription().equals("DRDSRA")){
			allowances = Utils.getApplicableAllowanceRates("SRA", do2.getFromDate(), do2.getToDate());
		} else if (do2.getDescription().equals("RMAHCO")){
			allowances = Utils.getApplicableAllowanceRates("NRA", do2.getFromDate(), do2.getToDate());
		} else {
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate());
		}
		
		if (allowances !=null && allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0) {
			return true;
		}
		return false;
	}
}
