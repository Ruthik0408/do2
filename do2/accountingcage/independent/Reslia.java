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
public class Reslia extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNA = "VL: 02/42, 03/48, 05/51 should be mentioned in Col.5A.\n.";
	
	StringBuffer buffer = new StringBuffer();
	String message;
	Do2 refDo2;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getDescription().equals("CESRES")){
			if (do2.getColumnA() != null || do2.getColumnB() != null || do2.getColumnC() != null){
				validity = false; 
				buffer.append("VL: Cols.5A,B,C to be left blank");
			}
		}
		if (do2.getDescription().equals("RESLIA") && !do2.getDo2Type().equals("1")){
			validity = false; 
			buffer.append("VL: To be published by Record Office \n");
		}
		if (do2.getDescription().equals("RESLIA")  && !isValidA(do2)) {
			validity = false; 
			buffer.append(INVALID_COLUMNA);
		}
		if (!isValidRefDo2(do2)){
			validity = false; 
			buffer.append("Ref Do2 is incorrect");
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
				&& (do2.getColumnA().equals("02/42")
						|| do2.getColumnA().equals("03/48") || do2
						.getColumnA().equals("05/51"))) {
			return true;
		}
		return false;
	}
	public boolean isValidRefDo2(Do2 do2) {
		if (do2.getRefDo2No() != null){
			refDo2 = Utils.getReferenceDo2(do2);
			if (refDo2 != null) {
				if (do2.getDescription().equals("RESLIA") && refDo2.getDescription().equals("DISCH"))
					return true;
				if (do2.getDescription().equals("CESRES") && refDo2.getDescription().equals("RESLIA"))
					return true;
			}
		}
		return false;
	}

}
