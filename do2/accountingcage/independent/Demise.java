package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Demise extends AccountingCageBase implements java.io.Serializable {
	
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	FamilyDetail fd, fdRecord;
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_COLA= "VL:Name not mentioned in Col.5A.\n";
	static public String INVALID_COLB= "VL:Relationship is to be mentioned in Col.5B.\n";
	static public String INVALID_FD= "AU:Record in Family Details not found.\n";
	static public List<String> col5BList = Arrays.asList("FATHER","MOTHER","SPOUSE","SON","DAUGHTER","BROTHER","SISTER","SON-IN-LAW","BROTHER-IN-LAW","GRAND SON",
			"GUARDIAN", "STEP BROTHER", "STEP SISTER","GRAND DAUGHTER");
	public static final String INVALID_FROM_DATE= null;
	public static final Boolean INVALID_MARITAL_STATUS= null;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());

		if(!isValidColA(do2)) {
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}
		if(!isValidColB(do2)) {
			validity = false;
			buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isRecordAvailable(do2)) {
			validity = false;
			buffer.append(INVALID_FD);
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
		List<FamilyDetail> fdList =fdRepo.findByEmpIdAndNameAndRelation(do2.getEmployee().getId(),do2.getColumnA(), do2.getColumnB()); 
		 
		if (fdList.size() > 0){
			fdList.get(0).setStatusOfMember("DEMISE");
			fdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
			fdRepo.save(fdList.get(0));
			 
		}
		return Do2Manager.SUCCESS;
	}
	
	
	public boolean isRecordAvailable(Do2 do2) {
		List<FamilyDetail> fdList = fdRepo.findByEmpIdAndNameAndRelation(do2.getEmployee().getId(),do2.getColumnA().trim(), do2.getColumnB().trim()); 
		 System.out.println("............ fdlist size  ... " +fdList.size());
		return (fdList.size() > 0);
	}

	public boolean isValidColA(Do2 do2) {
		return (do2.getColumnA() != null);
	}

	public boolean isValidColB(Do2 do2) {
		return (do2.getColumnB() != null && col5BList.contains(do2.getColumnB()));
	}
	
	
}