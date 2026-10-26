package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */
@RestController
public class Recall extends AccountingCageBase implements java.io.Serializable {

	String leaveType;
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_LRA = "VL:Period does not match with number of days in TJRA and LRA Cols.\n";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n ";
	static public final String INVALID_TJRA ="VL:No of days in TJRA Column does not match with DA in Col.7B. \n";
	static public final String INVALID_DA_DURATION ="VL:DA Duration is not matching.\n";
	private StringBuffer buffer = new StringBuffer();
	private double do2Days, totalNra ,totalDaDays;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (leaveType != null && leaveType.equals("RL")
				&& !isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		}
		
		if (do2.getColumnB() !=null){
			if(!isValidDaDuration(do2)){
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(INVALID_DA_DURATION);
			}
			if(!isValidTjra(do2)){
				validity =false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(INVALID_TJRA);
			}
		}
		if (!validity) {
			if ((do2.getStatus() == null)
					|| (do2.getStatus() != null && !do2.getStatus().equals(
							Do2Manager.PENDING))) {
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
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		
		double daAmount=0.0;
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(Do2Manager.DAILY,
				 do2.getFromDate(), do2.getFromDate(), Do2Manager.CCOTH, getBasicPay(do2));
		for (Allowance allow : allowances){
			 daAmount  += allow.getRateAmount() * getTotalDaDays(do2);
		}
		if (daAmount !=0){
			Utils.addArrears(do2, daAmount, Do2Manager.DAILY, do2.getFromDate(),DateManager.getDate(do2.getFromDate(),(do2.getTjra()-1)) );
		}
	
		if (leaveType.equals("RL")) {
			List<Allowance> allowancesNra = Utils.getApplicableAllowanceRates(
					Do2Manager.NRA, do2.getFromDate(), do2.getFromDate());
			for (Allowance allow : allowancesNra) {
				totalNra += allow.getRateAmount() * do2.getLra() * -1;				
			}
			Utils.addArrears(do2, totalNra, Do2Manager.NRA, do2.getFromDate(),do2.getToDate());			
		}
		return Do2Manager.SUCCESS;
	
	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getTjra() == null)
			do2.setTjra(0);
		if (do2.getLra() == null)
			do2.setLra(0);
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		return (do2Days == (do2.getTjra()== null ? 0 : do2.getTjra() + do2.getLra()));
	}
	public boolean isValidDaDuration(Do2 do2){
		return true;
	}
	
	public boolean isValidTjra(Do2 do2){
		 if(do2.getTjra()==null) {
			 do2.setTjra(0);
		 }
		 return (Math.round(getTotalDaDays(do2)) == do2.getTjra());		    			   
	 }
	 public double getTotalDaDays(Do2 do2){
		 if (do2.getColumnB() != null){
			if(!do2.getColumnB().contains("/")){
				buffer.append("Column 7b is not as per Appendix J");
				return 0;
			}
			if(!do2.getColumnB().contains("-")){
				buffer.append("Column 7b is not as per Appendix J");
				return 0;
			}
		}
		 if (do2.getColumnB() != null) {
				String columnB = do2.getColumnB();
				double daHourDays = 0.0;
				int daDays = Integer.parseInt(columnB.substring(columnB.indexOf("-")+1,(columnB.indexOf("/"))));
				int daHours = Integer.parseInt(columnB.substring(columnB.indexOf("/")+1));
				
				
				if (daHours >= 6 && daHours <12) {
					daHourDays = .7;
				}
				if (daHours >= 12){
					daDays = daDays +1;
				}
				
				totalDaDays = daDays+daHourDays;
		 }
			return totalDaDays;
	 }
	List<String> leaveList = Arrays.asList(new String[] { "AL", "ADVAL", "BAL",
			"ACCMAL", "EXTNAL", "EXTNCL", "PAL", "RL", "CL", "EXTNLV" });

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> do2List = Utils.getReferenceDo2List(do2);

		for (Do2 refDo2 : do2List) {
			if (leaveList.contains(refDo2.getDescription())) {
				leaveType = refDo2.getDescription();
				return true;
			}
		}
		return false;
	}

	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(
				Do2Manager.NRA, do2.getFromDate(), do2.getFromDate());
		return allowances.size() > 0;

	}
	public Integer getBasicPay(Do2 do2) {
		AuditCage auditCage =	AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		return auditCage.getBasicPay();
	}
}
