package itsdc.dolphin.do2.accountingcage.independent;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Pbnctr extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_PUBLICATION = "VL:Description is not valid in old Appendix-J.\n";
	private static final String INCORRECT_REF_DO2 = "AU:Reference DOII is not a MSTJCO do2. \n";
	private static final String INVALID_PERIOD = "VL:Probation period is not one year .\n";
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			validity = false;
			buffer.append(INVALID_PUBLICATION);	
		}
		
		if (!(isValidRefDo2(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(INCORRECT_REF_DO2);
			validity = false;
		}
		
		if (!(isValidProbationPeriod(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(INVALID_PERIOD);
			validity = false;
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		}else{
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2Repo.save(do2);
		do2.setReason(buffer.toString());
	 	return validity;
	}


	@Override
	public String process(Do2 do2) {		
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		List <Do2> refDo2List = null;
		refDo2List = Utils.getReferenceDo2List(do2);
		for(Do2 refDo2 : refDo2List){
			if ((refDo2.getDescription().equals("MSTJCO"))) {
				return true;
			} 
		}
		
		return false;
	}
	
	public boolean isValidProbationPeriod(Do2 do2) {
		double probationPeriodMonths = DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate());
		probationPeriodMonths = new BigDecimal(probationPeriodMonths).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		if (probationPeriodMonths == 12.0)
			return true;
		else
			return false;
	}
	
	
	
		
		
}
