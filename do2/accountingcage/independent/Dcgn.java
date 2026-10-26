package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;

@RestController
public class Dcgn extends AccountingCageBase implements java.io.Serializable {

	private static final String INVALID_PAO = "VL:PAO should be AMC\n.";
	private static final String INVALID_Col5 = "VL:Col 5A,B,C should not be left blank\n.";
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	String message;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidPao(do2)){
			validity = false;
			buffer.append(INVALID_PAO);
		} 
		if (do2.getColumnA() == null || do2.getColumnB() != null || do2.getColumnC() == null){
			validity = false;
			buffer.append(INVALID_Col5);
		}
		if (validity == false){
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		}else {
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
	
	public boolean isValidPao(Do2 do2){
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (Utils.getPao().getPaoCode().equals("85")){
			return true;
		}
		return false;
	}
}
