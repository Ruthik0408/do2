package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */
@RestController
public class Miledn extends AccountingCageBase implements java.io.Serializable {
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public final String INVALID_COLUMNA = "VL:Name of Exam to be mentioned in Col.5A.\n";
	static public final String INVALID_COLUMNB = "VL:Name of Trade to be mentioned in Col.5B.\n";
	static public final String INVALID_COLUMNC = "VL:Grading to be mentioned in Col.5C.\n";
	static public List<String>examList=Arrays.asList("RT","BFC1","BFC2","PFC1","PFC2","ACE1","ACE2","ACE3","AEC1","AEC2","AEC3",
			                                         "MR1","MR2","MR3","TTT1","TTT2","TTT3","TTT4","OTT1","OTT2","OTT3","OTT4");
	/*static public List<String>examList=Arrays.asList("RT","BFC-I","BFC-II","BFC-III","ACE-I","ACE-II","ACE-III","AEC-I","AEC-II","AEC-III",
                                                     "MR-I","MR-II","MR-III","TTT-I","TTT-II","TTT-III","TTT-IV","OTT-I","OTT-II","OTT-III",
                                                     "OTT-IV","PFC-I","PFC-II");*/
	static public List<String>gradeList=Arrays.asList("A+", "A", "B+", "B", "C", "D", "Q", "E"); // Q & E added on 14.05.2026 (AppxJ amendment 149 dt.13.04.2026
	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);	
		}
		
		if (!isValidB(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNB + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (!isValidC(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNC + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (validity == false) {
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
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
	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && examList.contains(do2.getColumnA())){
			return true;
		}
		return false;
	}
	public boolean isValidB(Do2 do2){
		if (do2.getColumnA() != null) {
			if (do2.getColumnA().startsWith("TTT") || do2.getColumnA().startsWith("OTT")) {
				if (do2.getColumnB()!=null && do2.getColumnB().length()>1)
					return true;
			} else {
				if (do2.getColumnB() == null)
					return true;
			}
		}
		return false;
	}
	public boolean isValidC(Do2 do2){
		if (do2.getColumnC() == null)
			return true;
		if (do2.getColumnC() != null && gradeList.contains(do2.getColumnC())){
			return true;
		} 
		return false;
	}
}
