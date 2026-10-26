package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC , Secunderabad
 * 
 */

@RestController
public class Haa extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;

	static public String RATE_PERCENTAGE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table.\n";

	private StringBuffer buffer = new StringBuffer();
	private double haaRate = 0.0;
 	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
	 		if (!isPercentageRateAvailable(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				do2.setReason(RATE_PERCENTAGE_NOT_AVAILABLE);
	 		}else{
	 			do2.setStatus(Do2Manager.VALID);
	 		}
		 
		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		if (!isValid(do2)) 
			return Do2Manager.FAILURE;
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.HAA }), newAuditCage);
		} else if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			AuditCage acList = Utils.getAuditCage(do2);
			int basicPay = (acList.getBasicPay() == null ? 0 : acList.getBasicPay());
			int dp = (acList.getDp() == null ? 0 : acList.getDp());
			int gradePay = (acList.getGradePay() == null ? 0 : acList.getGradePay());
			int msPay = (acList.getMsPay() == null ? 0 : acList.getMsPay());
			int xGroupPay = (acList.getXGroupPay() == null ? 0 : acList.getXGroupPay());
			int oldBasicPay = (int) (acList.getOldBasicPay() == null ? 0 : acList.getOldBasicPay());
			int oldDp = (int) Math.round(oldBasicPay * 0.5);
			
			// the following line added due to sevencpc
			int oldGradePay = (int) (acList.getOldGradePay() == null ? 0 : acList.getOldGradePay());
			
			List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),  do2
							.getFromDate(), do2.getFromDate());
			/*for (Allowance allow : allowances){					
				if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
						haaRate = ((basicPay + dp) * allow.getRatePercentage());
				} else if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					haaRate = ((oldBasicPay + oldDp) * allow.getRatePercentage());
				} else {
					haaRate = ((basicPay + gradePay + msPay + xGroupPay) * allow.getRatePercentage());
				}
			}*/
			
			// the above lines commented and amended as stated below due to sevencpc
			
			for (Allowance allow : allowances){					
				if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
						haaRate = ((basicPay + dp) * allow.getRatePercentage());
				} else if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					haaRate = ((oldBasicPay + oldDp) * allow.getRatePercentage());
				} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate())){
					haaRate = ((basicPay + gradePay + msPay + xGroupPay) * allow.getRatePercentage());
				} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
					msPay = Utils.get6cpcMsPay(baseAuditCage);
					haaRate = ((oldBasicPay + oldGradePay + msPay + xGroupPay) * allow.getRatePercentage());
				} else {
					haaRate = ((basicPay) * allow.getRatePercentage());
				}
			}
			
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.HAA,haaRate);
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
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
	public boolean isPercentageRateAvailable(Do2 do2) {
		List<Allowance> allowances = new ArrayList<Allowance>();
		allowances = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getFromDate());
		if (allowances.size() > 0){
			return true;
		}
		return false;
	} 
}