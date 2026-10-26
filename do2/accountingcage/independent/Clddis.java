package itsdc.dolphin.do2.accountingcage.independent;
 import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Clddis extends AccountingCageBase implements java.io.Serializable {
	
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	
	static public String INVALID_MARITAL_STATUS = "AU:Invalid as PBOR is not married as per Employee Table.\n";
	static public String INVALID_COLA= "VL:Name not mentioned in Col.5A/not available in Family detail.\n";
	static public String INVALID_COLB= "VL:Relation to be mentioned in Col 5B.\n";
	static public String INVALID_COLC= "VL:MENTAL/PHYSICAL to be mentioned in Col 5C.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());

		if (!isMarried(do2)) {
			validity = false;
			buffer.append(INVALID_MARITAL_STATUS);
		}

		if(!isValidColA(do2)) {
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (do2.getColumnB().equals("SON") || do2.getColumnB().equals("DAUGHTER")
				|| do2.getColumnB().equals("BROTHER") || do2.getColumnB().equals("SISTER")){
			;
		} else {
			validity = false;
			buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (do2.getColumnC().equals("MENTAL") || do2.getColumnC().equals("PHYSICAL")){
			;
		} else {
			validity = false;
			buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);
		}

		if(validity==false) {
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
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
		 
		if(do2.getColumnA() != null) {
			List<FamilyDetail> fdList =fdRepo.findByEmployeeIdAndRelationshipAndNameOfMember(do2.getEmployee().getId(),do2.getColumnB(), do2.getColumnA());
			 
       		if (fdList.size() > 0) {
       			if (do2.getColumnC().equals("MENTAL")){
       				fdList.get(0).setStatusOfMember("HCAP-MEN");
       			} else {
       				fdList.get(0).setStatusOfMember("HCAP-PHY");
       			}
       			
       			fdRepo.save(fdList.get(0));
       		}
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isMarried(Do2 do2) {
		return (emp.getMarried());
	}

	public boolean isValidColA(Do2 do2) {
		if(do2.getColumnA() != null) {
			List<FamilyDetail> fdList = fdRepo.findByEmployeeIdAndRelationshipAndNameOfMember(do2.getEmployee().getId(),do2.getColumnB(), do2.getColumnA());
			 
       		if (fdList.size() > 0) {
			  return true;
			}
		}
		return false;
	}
}