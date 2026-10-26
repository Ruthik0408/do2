package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */

@RestController
public class Wound extends AccountingCageBase implements java.io.Serializable {
	
	private static final String INVALID_COLUMNA = "VL:Operation name should be mentioned in Col 5A.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	StringBuffer buffer = new StringBuffer();
	String message;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
		// As per the discussion on 15.04.14 with ASDC, ND visiting team, column_5a can be blank
		/*if (do2.getColumnA() != null) {
			return true;
		}
		return false;*/
		return true;
	}
	
}
