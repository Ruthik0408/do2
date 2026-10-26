package itsdc.dolphin.do2.accountingcage.independent;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author N. RAMACHANDRAN    &&     K. RAMADEVI
 * 
 */

//TODO  -	AMOUNT CALCULATED AS ARREARS IN THIS DESCRIPTION TO BE REMITTED TO FAMILY IF MARRIED 
//		  	ELSE ON EXISTENCE OF PROOF OF MAINTAINING HIS PARENTS 			THRU FAMO/MO/DD
			

@RestController
public class Subst extends AccountingCageBase implements java.io.Serializable {

	private Employee employee;
	static public final String NOT_A_REQUISITE_REFDO2 = "AU:Incorrect REFERENCE DO2 MUST BE OF MILRI or CVLRI \n";
	static public final String NOT_MARRIED = "AU:PBOR not married \n";
	boolean validity = true;
	
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getMarried() == false){
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(NOT_MARRIED);
		}
		if (!isValidRefDo2(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(NOT_A_REQUISITE_REFDO2);
		} 
		if (validity != false) {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		/*
		 * A letter may be generated to respective RECORDS OFFICE to include this amount in next due of FAMO.
		 */	
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2);
		if ((refDo2.getDescription().equals(Do2Manager.MILRI) || refDo2.getDescription().equals(Do2Manager.CVLRI)) && 
				(refDo2.getFromDate().equals(do2.getFromDate()) && refDo2.getToDate().equals(do2.getToDate()))) {
			return true;
		}			
		return false;
	}
	
}
