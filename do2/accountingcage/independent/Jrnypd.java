package itsdc.dolphin.do2.accountingcage.independent;

 
import itsdc.dolphin.models.Do2;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI   DATE: 16.09.2009
 */
@RestController
public class Jrnypd extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static final public String INCORRECT_REF_DOII = "AU:Incorrect Reference DOII - does not refer to POSIN occurrence. \n";
	private StringBuffer buffer = new StringBuffer();
	private boolean validity = true;

	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
	
	validity = true;
	
	if (!isValidRefDoII(do2)) {
		buffer.append(INCORRECT_REF_DOII);
		do2.setStatus(Do2Manager.INVALID);
		validity = false;
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

	private boolean isValidRefDoII(Do2 do2) {
	 	
	if ((Utils.getReferenceDo2(do2) != null ) && (Utils.getReferenceDo2(do2).getDescription().equals(Do2Manager.POSIN))) {
		return true;
	} 
	return false;
	}
	
}		