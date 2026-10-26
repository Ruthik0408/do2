package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.Utils;
 

//Forfeiture of Seniority / service for promotion
@RestController
public class Forsen extends AccountingCageBase implements java.io.Serializable {

	Employee employee;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);
		if(do2.getAcceptedQe()==null){
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		}else{
			if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_BARRING+"");
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		return Do2Manager.SUCCESS;
	}
}
