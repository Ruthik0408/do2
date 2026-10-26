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
 



// TB CASES
@RestController
public class Lep extends AccountingCageBase implements java.io.Serializable {
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
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	//	employee.setFsDueDate(getNextYearDate(do2.getFromDate()));
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		
		employee.setFsDueDate(null);
		return Do2Manager.SUCCESS;
	}

	public Date getNextYearDate(Date currentDo2Date) {
		Date nextYearDate = DateManager.getToday();
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDo2Date);
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
		nextYearDate.setTime(cal.getTimeInMillis());
		return nextYearDate;
	}
}
