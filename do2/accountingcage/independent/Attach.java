package itsdc.dolphin.do2.accountingcage.independent;

 import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

@RestController
public class Attach extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	Do2 refDo2;
	static public final String INVALID_UNIT_CODE = "VL:Unit Code is invalid.\n ";
	static public final String INVALID_REFDO2 = "AU:Reference Do2 is Invalid.\n ";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidColAB(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_UNIT_CODE);
		}		
		if (do2.getColumnA().equals("GRANTED") || do2.getColumnA().equals("CEASED")){
			;
		} else {
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append("Column 5A should contain either GRANTED/CEASED" + NOT_IN_APPENDIX_J_FORMAT);
		}
/*		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
*/		
		if (validity==false) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		}else {
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
	
	public boolean isValidColAB(Do2 do2) {
		if (do2.getColumnA() !=null && do2.getColumnA().equals(Do2Manager.CEASED)){
              if (do2.getColumnB() == null)
            	  return true;
		}
		if (do2.getColumnA() !=null && do2.getColumnA().equals(Do2Manager.GRANTED)) {
			if (do2.getColumnB() != null){			    
			    	return true;
			}
		}
		return false;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		if(do2.getColumnA() !=null && do2.getColumnA().equals("GRANTED")){
			return true;
		}
	/*	if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			refDo2 = Utils.getReferenceDo2(do2);
			if (refDo2 != null && refDo2.getDescription().equals(Do2Manager.ATTACH)) {
				 	return true;
			}
		} */
		return false;
	}
}