package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.web.bind.annotation.RestController;
 

/**
 * Authority :
 * 
 * Author : A J Noronha & T Vanaja
 */

// TB CASES
@RestController
public class Tb extends AccountingCageBase implements java.io.Serializable {
	Employee employee;

	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		if(do2.getAcceptedQe()==null){
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		}else{
			if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		do2Repo.save(do2);
		return true;
	}

	@Override
	public String process(Do2 do2) {

		return Do2Manager.SUCCESS;
	}


}
