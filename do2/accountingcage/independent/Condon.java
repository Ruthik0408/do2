package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;

@RestController
public class Condon extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	public static String INVALID_COLUMNA = "VL:Column 7A should contain No. of days.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!(isValidColumnA(do2))) {
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);
			do2.setReason(buffer.toString());
			validity = false;
		}
		
		if (validity==true) {
			do2.setStatus(Do2Manager.VALID);
			do2.setStatus(null);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_LPC_SERVICE+"");
		return Do2Manager.SUCCESS;
	}

	// **************************************************************
	public boolean isValidColumnA(Do2 do2) {
		if (do2.getColumnA() != null && do2.getColumnA().length() != 0 && 
			Integer.parseInt(do2.getColumnA()) > 0) {
			return true;
		} else {
			return false;
		}
	}
}
