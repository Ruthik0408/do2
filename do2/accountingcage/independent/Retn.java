package itsdc.dolphin.do2.accountingcage.independent;
import java.math.BigDecimal;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * 
 * Author :T Vanaja & A J Noronha
 */

@RestController
public class Retn extends AccountingCageBase implements java.io.Serializable {

	Rank rank;
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	
	static final String INVALID_PERIOD = "VL:Retention period not in complete years.\n";
	static final String INVALID_RETN = "AU:Retention exceeded beyond maximum service specified for rank.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		/*if (!(isValidYears(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_PERIOD);
			validity = false;
		}
		if (!(isValidRetn(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(INVALID_RETN);
			validity = false;
		}
		do2.setReason(buffer.toString());
		*/
		do2.setStatus(Do2Manager.VALID);
		
		if (do2.getAcceptedQe()==null){
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		} else {
			if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		do2Repo.save(do2);
	return validity;
	}

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getDo2Date(),"ALLCE");
		int colorServ = Integer.parseInt(employee.getTermsColourService());
		double retentionMonths = DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate());
		
		if (retentionMonths == 24.0 ) {
			int newColorServ = colorServ + 2;
			employee.setTermsColourService(Integer.toString(newColorServ));
		} else if (retentionMonths == 12.0) {
			int newColorServ = colorServ + 1;
			employee.setTermsColourService(Integer.toString(newColorServ));
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidYears(Do2 do2) {
	
		double retentionMonths = DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate());
		retentionMonths = new BigDecimal(retentionMonths).setScale(1, BigDecimal.ROUND_HALF_UP)
		.doubleValue();
		if (retentionMonths == 24.0 || retentionMonths == 12.0)
			return true;
		else
			return false;
	}
	
	public boolean isValidRetn(Do2 do2) {
		boolean validity = false;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getDo2Date(),"ALLCE");
		int colorServ = Integer.parseInt(employee.getTermsColourService());
		int maxServ = Integer.parseInt(rank.getMaximumService());
		double retentionMonths = DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate());
		retentionMonths = new BigDecimal(retentionMonths).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		if (retentionMonths == 24.0 ) {
			int newColorServ = colorServ + 2;
			if (newColorServ <= maxServ) {
				validity = true;
			} else {
				validity = false;
			}
		} else if (retentionMonths == 12.0) {
			int newColorServ = colorServ + 1;
			if (newColorServ <= maxServ) {
				validity = true;
			} else {
				validity = false;
			}
		} else {
			validity = false;
		}
		return validity;
	}

}
