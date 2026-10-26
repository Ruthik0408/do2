package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

@RestController
public class Civedn extends AccountingCageBase implements java.io.Serializable {
	
	static public final String INVALID_COLUMNA = "VL:Name of Exam to be mentioned in Col.5A.\n";
	static public final String INVALID_COLUMNB = "VL:Name of Board/University to be mentioned in Col.5B.\n";
	static public final String INVALID_COLUMNC = "VL:Roll No to be mentioned in Col.5C.\n";
	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);	
		}
		
		if (!isValidB(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNB);	
		}
		if (!isValidC(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNC);	
		}
		if (validity == false) {
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
		if (do2.getColumnA() != null){
			return true;
		}
		return false;
	}
	public boolean isValidB(Do2 do2){
		if (do2.getColumnB() != null){
			return true;
		}
		return false;
	}
	public boolean isValidC(Do2 do2){
		if (do2.getColumnC() != null){
			return true;
		}
		return false;
	}
}
