package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
/**
 * 
 * @author IT & SDC , Secunderabad
 * 
 */

@RestController

public class Ssa extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	
	static public String RATE_NOT_AVAILABLE = "AU:Rate Percentage not available in the Allowance Rate table.\n ";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";	

	private StringBuffer buffer = new StringBuffer();
	List<String> stopAllowanceList =  Arrays.asList("DEPA");
	private double ssaRate;
	Rank rank;
	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(),do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
		if (!isRankAvailable(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			validity = false;
			buffer.append(SERVICE_DETAIL_UNAVAILABLE);
			
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		}

		if(validity){
			do2.setStatus(Do2Manager.VALID);	
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	/*@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		//rank = Utils.getRank(do2.getArmyNo(),do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals("CEASED")) {
			AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.SSA }), newAuditCage);
		} else if (do2.getColumnA().equals("GRANTED")) {
			int basicPay = (int) Math.round(newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay());
			int gradePay = (int) Math.round(newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay());
			int msPay = (int) Math.round(newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay());
			int xGroupPay = (int) Math.round(newAuditCage.getXGroupPay() == null ? 0 : newAuditCage.getXGroupPay());
			int oldBasicPay = (int) Math.round(newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay());
		    int oldDp = (int) Math.round(oldBasicPay * 0.5);
			List<Allowance> allowances  = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(), do2.getFromDate());
			if(allowances.size() >0){
				if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
		 			ssaRate = Math.round((oldBasicPay + oldDp) * allowances.get(0).getRatePercentage());
		 		} else{
		 			ssaRate = Math.round((basicPay + gradePay + msPay + xGroupPay) * allowances.get(0).getRatePercentage());
		 		} 
			}
			 AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.SSA, ssaRate);
	 
		}
		return Do2Manager.SUCCESS;
	}*/
	
	// above method commented and modified as stated below due to sevencpc
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		//rank = Utils.getRank(do2.getArmyNo(),do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals("CEASED")) {
			AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.SSA }), newAuditCage);
		} else if (do2.getColumnA().equals("GRANTED")) {
			int basicPay = (int) Math.round(newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay());
			int da = (int) Math.round(newAuditCage.getDa() == null ? 0 : newAuditCage.getDa());
			int gradePay = (int) Math.round(newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay());
			int msPay = (int) Math.round(newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay());
			int xGroupPay = (int) Math.round(newAuditCage.getXGroupPay() == null ? 0 : newAuditCage.getXGroupPay());
			int oldBasicPay = (int) Math.round(newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay());
		    int oldDp = (int) Math.round(oldBasicPay * 0.5);
		    int oldGradePay = (int) Math.round(newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay());
			List<Allowance> allowances  = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(), do2.getFromDate());
			/*if(allowances.size() >0){
				if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
		 			ssaRate = Math.round((oldBasicPay + oldDp) * allowances.get(0).getRatePercentage());
		 		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
		 			ssaRate = Math.round((basicPay + gradePay + msPay + xGroupPay) * allowances.get(0).getRatePercentage());
		 		} else if (newAuditCage.getFromDate().after(DateManager.getSeventhCpcDate())){
		 			ssaRate = Math.round((basicPay + da) * allowances.get(0).getRatePercentage());
		 		} else {
		 			msPay=Utils.get6cpcMsPay(newAuditCage);
		 			xGroupPay=Utils.get6cpcXGroupPay(newAuditCage);
		 			ssaRate = Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay) * allowances.get(0).getRatePercentage());
		 		}
			}*/
			
			if(allowances.size() >0){
				if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
		 			ssaRate = Math.round((oldBasicPay + oldDp) * allowances.get(0).getRatePercentage());
		 		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
		 			ssaRate = Math.round((basicPay + gradePay + msPay + xGroupPay) * allowances.get(0).getRatePercentage());
		 		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
		 			msPay=Utils.get6cpcMsPay(newAuditCage);
		 			xGroupPay=Utils.get6cpcXGroupPay(newAuditCage);
		 			ssaRate = Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay) * allowances.get(0).getRatePercentage());
		 		} else {
		 			if (!Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
		 				ssaRate = Math.round((basicPay) * allowances.get(0).getRatePercentage());	
		 			} else {
		 				ssaRate = Math.round((basicPay) * 0.20);	
		 			}
		 			
		 			AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		 		}
			}
			 AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.SSA, ssaRate);
	 
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
	
	public boolean isRankAvailable(Do2 do2) {
		return rank !=null;
		
	}
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = new ArrayList<Allowance>();
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(),
				do2.getFromDate());
		return allowances.size() >0;
	}
	
}