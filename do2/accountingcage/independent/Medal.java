package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */
@RestController
public class Medal extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNA = "VL:Awarded/Ceased/Restored should be mentioned in Col-5A.\n.";
	private static final String INVALID_COLUMNB = "VL:Name of the Medal should be mentioned in Col-5B.\n.";

	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNA);
		}
		if (!isValidB(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNB);
		}
		if (do2.getColumnA() != null
				&& (do2.getColumnA().equals("CEASED") || 
				    do2.getColumnA().equals("RESTORED"))) {
			if (do2.getRefDo2No() == null) {
				validity = false;
				buffer.append("VL: Ref Do2 to be mentioned \n");
			}
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
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
		message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	

	public boolean isValidA(Do2 do2) {
		if (do2.getColumnA() != null
				&& (do2.getColumnA().equals("AWARDED") || do2.getColumnA().equals("CEASED") || 
				    do2.getColumnA().equals("RESTORED"))) {
			return true;
		}
		return false;
	}

	public boolean isValidB(Do2 do2) {
		if (do2.getColumnB() != null){
			return true;
		}
		return false;
	}
}
