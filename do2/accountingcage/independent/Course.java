package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

@RestController
public class Course extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNB = "VL:Trg Est/Centre/Est/Unit Name should be mentioned in Col 5B.\n";
	private static final String INVALID_COLUMNC = "VL:Grading should be mentioned in Col. 5C.\n";
	private static final String INVALID_TODT    = "VL:To date to be mentioned.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public List<String>gradeList=Arrays.asList("AI", "AXI", "QI", "BI", "AX", 
			 "BX", "A", "B", "B+", "C", "BY", "CY", "CZ"); //(ADDED after AppxJ amendment 150 dt.15.04.2026

	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;		
		if (do2.getDescription().equals("COURSE") && do2.getToDate() == null){ 
			validity = false;
			buffer.append(INVALID_TODT);
		}
		if (!isValidA(do2)) {
			validity = false;
			if (do2.getDescription().equals("COURSE")){
				buffer.append("VL:Name of Course should be mentioned in Col 5A.\n");	
			} else {
				buffer.append("VL:Name of cadre should be mentioned in Col 5A.\n");
			}
		}
		if (!isValidB(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNB + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (!isValidC(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNC + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (validity == false) {
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if(do2.getAcceptedQe()==null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}		
		do2Repo.save(do2);		
	 	return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidA(Do2 do2) {
		if (do2.getColumnA() != null) {
			return true;
		}
		return false;
	}
	
	public boolean isValidB(Do2 do2) {
		if (do2.getColumnB() != null) {
			return true;
		}
		return false;
	}
	
	public boolean isValidC(Do2 do2) {		
		if (do2.getColumnC() == null)
			return false;
		if (do2.getColumnC()!=null && gradeList.contains(do2.getColumnC())){
			return true;
		} 
		return false;
	}
}

