package itsdc.dolphin.do2.accountingcage.independent;
 
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Divatd extends AccountingCageBase implements java.io.Serializable {
 
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in allowance rate table.\n ";
	static public final String INVALID_LRA_COLUMN = "VL:Invalid time (minutes) given in NRA days.\n";
	static public final String INVALID_COLUMN_A   = "VL:Valid value for depth in Fathoms to be given in column 7A.\n";
	static public final String INVALID_MODULE     = "VL:To be claimed through contingent bill before 01.06.2011. \n";
	static public final String INVALID_COLUMN_C   = "VL:Column 7C should contain INITIAL.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public final List<String> VALID_COLUMN_A= Arrays.asList("0-20", "20-30", "30-40", "40-50","50-60", "60-75", "75-100");
	@Override
	public String process(Do2 do2) {
		String description = "DMALLC";
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(description, do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
		for (Allowance allow : allowances) {
			Utils.addArrears(do2, Math.round((allow.getRateAmount()*.2)*do2.getLra()));
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public boolean isValid(Do2 do2) {
		StringBuffer buffer = new StringBuffer();	 
		boolean validity = true;
 		
		if (!isValidModule(do2)) {
	 		buffer.append(INVALID_MODULE);
			validity = false;
		}
 		
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
	 	} 

		if (do2.getLra()!=null && do2.getLra()<1 && do2.getLra()>60) {
			validity = false;
			buffer.append(INVALID_LRA_COLUMN);
		}
		
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && !VALID_COLUMN_A.contains(do2.getColumnA()))) {
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COLUMN_A + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (do2.getColumnC()!=null && !do2.getColumnC().equals("INITIAL")){
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity=false;
			buffer.append(INVALID_COLUMN_C + NOT_IN_APPENDIX_J_FORMAT);
		}
		
		if (!validity) {
			if (do2.getStatus()==null || (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
		 	}
			do2.setReason(buffer.toString());
		} else {
			 do2.setStatus(Do2Manager.VALID);
			 do2.setStatus(null);
			 if(do2.getAcceptedQe()==null){
					do2.setAcceptedQe(Utils.getCurrentQuarter());	
				}else{
					if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
						do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		} 
		
 		do2Repo.save(do2);
	
		return validity;
	} 
	
	public boolean isRateAvailable(Do2 do2) {
		String description = "DMALLC";
		return (Utils.getApplicableAllowanceRates(description, do2.getColumnA(), do2.getFromDate(), do2.getFromDate()).size()>0);
	}
 
	public boolean isValidModule(Do2 do2) {
	
		Date cutOffDate = new Date();
		try {
			cutOffDate = DateManager.getDate("2011-06-01");
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	return (!do2.getFromDate().before(cutOffDate));
	}

}
