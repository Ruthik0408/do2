package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
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
public class Rl extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	Employee employee;

	static public String INVALID_TJRA = "VL:Tjra not mentioned or more than 4 days.\n";
	static public String INVALID_LRA = "VL:Lra days not mentioned or more than authorised days.\n";
	static public String INVALID_LEAVE_PERIOD = "VL:Period not matching with TJRA/LRA.\n";
	static public String NOT_RECRUIT_OR_NOT_COMPLETED_SIXMONTHS = "AU:PBOR is not a recruit OR not completed 6 months from date of enrolment.\n";
	static public String RATE_NOT_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n ";
	static public String SIX_MONTH_SERVICE = "AU:Six Month Service Not Validated. Can be relaxed by OC.\n ";
	private StringBuffer buffer = new StringBuffer();
	private double do2Days = 0.0, noofMonths = 0.0;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

	/*	if (!isValidTjra(do2)) {
			validity = false;
			buffer.append(INVALID_TJRA);
		}*/
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (!isTjraLra(do2)) {
			validity = false;
			buffer.append(INVALID_LEAVE_PERIOD);
		}
		if (!isRecruit(do2)) {
			validity = false;
			buffer.append(NOT_RECRUIT_OR_NOT_COMPLETED_SIXMONTHS);
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

	@Override
	public String process(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(
				Do2Manager.NRA, do2.getFromDate(), do2.getToDate());
		double totalNra = 0.0;
		for (Allowance allow : allowances) {
			totalNra += (allow.getRateAmount()
					* DateManager.getNoOfDays(allow.getFromDate(), allow.getToDate()));
			
		}
		Utils.addArrears(do2, Math.round(totalNra), Do2Manager.NRA, do2.getFromDate(), do2.getToDate());
		return Do2Manager.SUCCESS;
	}

	public boolean isValidTjra(Do2 do2) {
		if (do2.getTjra() != null && do2.getTjra() <= 4) {
			return true;
		}
		return false;
	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getLra() != null && do2.getLra() <= 30) {
			return true;
		}
		return false;
	}

	public boolean isTjraLra(Do2 do2) {
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2Days == do2.getTjra() + do2.getLra()) {
			return true;
		}
		return false;
	}

	public boolean isRecruit(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		noofMonths = DateManager.getNoOfMonths(employee.getEnrolmentDate(), do2
				.getFromDate());
		if (((employee.getInitialMusteringDate() == null) || (employee
				.getInitialMusteringDate() != null && employee
				.getInitialMusteringDate().after(do2.getFromDate())))) {
			if (noofMonths < 6) {
				buffer.append(SIX_MONTH_SERVICE);
			}
			return true;

		}
		return false;
	}
	private boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(Do2Manager.NRA,
				do2.getFromDate(), do2.getToDate());
		if (allowances.size() > 0) {
			return true;
		}
		return false;
	}
}