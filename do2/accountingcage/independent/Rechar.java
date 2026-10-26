package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;
import java.util.List;

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
public class Rechar extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNB = "VL:Original assessment should be mentioned in Col 7B.\n";
	private static final String INVALID_COLUMNC = "VL:Revised assessment should be mentioned in Col. 7C.\n";
	final static List<String> COL_7LIST = Arrays.asList(new String[] { "EXEMPLARY", "VERY GOOD", "GOOD", "FAIR", "INDIFFERENT", "BAD",
	     "VERY BAD","NO ASSESSMENT"});
	private static final String INVALID_REF    = "VL:Reference DO2 No. is required.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	StringBuffer buffer = new StringBuffer();
	String message;
 
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (do2.getRefDo2No() == null){
		//	do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_REF);
		}
		
		if (do2.getColumnB() == null || (do2.getColumnB() != null && !COL_7LIST.contains(do2.getColumnB()))) {
		//	do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_COLUMNB +NOT_IN_APPENDIX_J_FORMAT);
		}
		if (do2.getColumnC() == null || (do2.getColumnC() != null && !COL_7LIST.contains(do2.getColumnC()))) {
		//	do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_COLUMNC +NOT_IN_APPENDIX_J_FORMAT);
		}
		if (validity == false) {
		//	do2.setStatus("MR");
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
	
}

