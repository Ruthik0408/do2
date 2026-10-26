package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.do2.AccountingCageBase;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Indata extends AccountingCageBase implements java.io.Serializable {
	public StringBuffer buffer = new StringBuffer();
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (!emp.getEnrolmentDate().equals(do2.getFromDate())){
			validity = false;
			buffer.append("VL: From date should be date of Enrolment");
		}
		if (do2.getColumnA()==null){
			validity = false;
			buffer.append("VL: Column 5A should contain Medical Category");
		}
		if (do2.getColumnB()==null){
			validity = false;
			buffer.append("VL: Column 5B should contain Date of primary medical Exam");
		} else {
			if (!DateManager.getDateFromDdMmYyyy(do2.getColumnB().trim()).before(emp.getEnrolmentDate())) {
				validity = false;
				buffer.append("VL: Date of primary medical Exam should be before Enrolment Date");
			}
		}
		if (do2.getColumnC()==null){
			validity = false;
			buffer.append("VL: Column 5C should contain his earlier profession");
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
		return Do2Manager.SUCCESS;
	}
}