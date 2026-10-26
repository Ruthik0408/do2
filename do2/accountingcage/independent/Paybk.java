package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.Date;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

/**
 * Authority :
 * 
 * Author : T Vanaja & A J Noronha
 */

// PAY BOOK
@RestController
public class Paybk extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	
	public static String INVALID_COLUMNA = "VL:Col 7A should contain the last Sl.No of Pay book.\n";
	public static String INVALID_COLUMNB = "VL:COL 7B should contain the date of payment of last PB Sl.No.\n";
	public static String INVALID_DATE = "VL:Col 7B date should be prior to From Date.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!(isValidColumnA(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_COLUMNA);
			validity = false;
		}

		if (!(isValidColumnB(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_COLUMNB);
			validity = false;
		}

		if (!(isValidFromdt(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_DATE);
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
		
		return Do2Manager.SUCCESS;
	}

	// **************************************************************
	public boolean isValidColumnA(Do2 do2) {
		if (do2.getColumnA() != null) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isValidColumnB(Do2 do2) {
		if (do2.getColumnB() != null && do2.getColumnB().length() == 8) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isValidFromdt(Do2 do2) {
		if(do2.getColumnB() ==null)
			return false;
		Date lastSnoDt = DateManager.getDateFromDdMmYyyy(do2.getColumnB());
		if (lastSnoDt.before(do2.getFromDate()))
			return true;
		else
			return false;
	}
}
