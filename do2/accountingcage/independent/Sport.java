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
public class Sport extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNA = "VL:Col.5A should be AWARD/MEDAL/REP\n.";
	private static final String INVALID_COLUMNB = "VL:Game/Event should be mentioned in Col.5B\n.";
	private static final String INVALID_COLUMNC = "VL:Name of the level should be mentioned in Col.5C\n.";
	static public List<String> col5CList = Arrays.asList("ASIAN","BRIGADE","BATTALION","COMMAND",
            "DIV","INTERNATIONAL","NATIONAL","OLYMPIC","SERVICE","SFA GAMES", "ARJUNA AWARD", "MAC GREGO MEDAL",
            "CWG", "NATONAL RIFLE ASSOCIATION", "STATE", "SERVICES");
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	StringBuffer buffer = new StringBuffer();
	String message;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidColA(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValidColB(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNB + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValidColC(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNC);
		}
		if (validity==false) {
			// do2.setStatus("MR");
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
	
	public boolean isValidColA(Do2 do2) {
		if (do2.getColumnA() != null && (do2.getColumnA().equals("MEDAL") 
				|| do2.getColumnA().equals("AWARD") || do2.getColumnA().equals("REP"))) {
			return true;
		}
		return false;
	}
	
	public boolean isValidColB(Do2 do2) {
		if (do2.getColumnB() != null && (do2.getColumnB().equals("SHOOTING") 
				|| do2.getColumnB().equals("ATHLETICS") || do2.getColumnB().equals("VOLLEYBALL") || do2.getColumnB().equals("FOOTBALL"))) {
			return true;
		}
		return false;
	}
	public boolean isValidColC(Do2 do2) {
		if (do2.getColumnC() != null && 
			   (col5CList.contains(do2.getColumnC().trim()))) {
			return true;
		}
		return false;
	}
}
