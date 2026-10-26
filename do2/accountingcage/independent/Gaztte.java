package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/**
 * Authority :
 * 
 * Author : A J Noronha & T Vanaja
 */

// GAZETTE NOTIFICATION FOR PROMOTION - JCO
@RestController
public class Gaztte extends AccountingCageBase implements java.io.Serializable {

	static final String NOT_A_JCO_NO = "VL:Not A JC Number.\n";
	static final String NO_PROMOT_DO2 = "AU:No Promotion DoII/Reference DO2 is incorrect. \n";

	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();
	 
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (!(isJcno(employee))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_A_JCO_NO);
			validity = false;
		}
		if (!(isJco(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_PROMOT_DO2);
			validity = false;
		}
		if (validity) {
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
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setGazetteFlag(true);
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {// not here
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		employee.setGazetteFlag(false);
		return Do2Manager.SUCCESS;
	}

	// ****************************************************************************************
	public boolean isJcno(Employee employee) {
		return (employee.getArmyNo().startsWith("JC")|| employee.getArmyNo().startsWith("TJ")); 
	}

	public boolean isJco(Do2 do2) {
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return Integer.parseInt(rank.getRankCode()) >= Do2Manager.NBSUB_INT;
	}
}
