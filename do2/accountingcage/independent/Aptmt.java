package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;
import java.util.List;

 
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.Utils;
 
 
@RestController
public class Aptmt extends AccountingCageBase implements java.io.Serializable {

	 
	public StringBuffer buffer = new StringBuffer();
    Do2 refDo2;
    static public List<String>apptList=Arrays.asList("ADC TO PRESIDENT", "ADC TO COAS", "ADC TO VCOAS", "ADC TO DCOAS", 
    		"ADC TO CISC", "ADC TO ARMY CDR", "ADC TO CORPS CDR", "INSTR CAT A", "INSTR CAT B", "INSTR FOREIGN",
    		"PA TO COAS", "PA TO VCOAS", "PA TO DCOAS", "PA TO CISC", "PA TO ARMY CDR", "PA TO CORPS CDR", "PA TO PSO",
    		"PA TO DG");
    static public final String INVALID_COLUMNB = "VL:Name of Apptmt to be mentioned in Col.5B.\n";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA().equals("CEASED")){
			if (do2.getRefDo2No() != null){
				refDo2 = Utils.getReferenceDo2(do2);
				if (refDo2 != null && refDo2.getDescription().equals("APTMT")) {
					validity = true;
				} else {
					validity=false;
				}				
			} else {
				validity = false;
				buffer.append("VL: Reference Part II order to be given");
			}
			
		}
		
		if (do2.getColumnA().equals("GRANTED")) {
			if (!isValidB(do2)){
				validity = false;
				buffer.append(INVALID_COLUMNB);	
			}
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidB(Do2 do2){
		if (do2.getColumnB() != null && apptList.contains(do2.getColumnB())){
			return true;
		}
		return false;
	}
}