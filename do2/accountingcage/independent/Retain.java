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
public class Retain extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	private StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COLUMNA = "VL:Col.5A should contain Retention Service in years/age or CEASED.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if(!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);
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
				
		do2Repo.save(do2);
		message = buffer.toString();
		do2.setReason(message);

		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && do2.getColumnB().equals("GRANTED")){
			return true;
		}
		if (do2.getColumnA() != null && do2.getColumnA().equals("CEASED") && 
				do2.getColumnB() == null){
			return true;
		}
		return false;
	}
}
