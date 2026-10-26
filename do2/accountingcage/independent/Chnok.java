package itsdc.dolphin.do2.accountingcage.independent;
 

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Chnok extends AccountingCageBase implements java.io.Serializable {
	
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_COLA= "VL:Name not mentioned in Col.5A.\n";
	static public String INVALID_COLB= "VL:Relationship not mentioned in Col.5B.\n";
	static public String INVALID_COLC= "VL:Details of nomination not mentioned in Col.5C.\n";
	static public String INVALID_PERCENTAGE= "VL:Details of percentage not mentioned in Col.5C.\n";
	static public List<String> col5CList = Arrays.asList("NOK","FP","SFP","ESTATE","AFPPF","DCRG","AGIS");
	static public List<String> col5BList = Arrays.asList("FATHER","MOTHER","SPOUSE","SON","GUARDIAN", "STEP BROTHER", "STEP SISTER",
			                                             "DAUGHTER","BROTHER","SISTER","GRAND SON","GRAND DAUGHTER");
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (emp == null) {
			emp = Utils.getCurrentEmployee(do2.getArmyNo());
		}
		
		if (emp != null){
			if (do2.getDescription().trim().equals("NOK") && !do2.getFromDate().equals(emp.getEnrolmentDate())) {
				List<Do2> marryDo2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(emp.getId(), "MARRY", "V");
				if (marryDo2List!=null && marryDo2List.size()>0) {
					if (do2.getDescription().equals("NOK") && !do2.getFromDate().equals(marryDo2List.get(0).getFromDate())) {
						validity = false;
						buffer.append("VL: From date should be date of Enrolment or Marriage.\n");
					}
				} else {
					validity = false;
					buffer.append("VL: From date should be date of Enrolment or Marriage.\n");
				}
			}
		}

		if (!isValidColA(do2)) {
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (!isValidColB(do2)) {
			validity = false;
			buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (!do2.getDescription().equals("WILL") && !do2.getDescription().equals("AGINOM")){
			if (!isValidColC(do2)) {
				validity = false;
				buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);
			}
		}
		
		if (do2.getDescription().equals("AGINOM")){
			if (!isValidPercentage(do2)) {
				validity = false;
				buffer.append(INVALID_PERCENTAGE + NOT_IN_APPENDIX_J_FORMAT);
			}
		}
		
		if (validity==false) {
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
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
	 
		List<FamilyDetail> fdlist;
	 	if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID)) {
	 		if (do2.getDescription().equals("WILL")){
	 			return Do2Manager.SUCCESS;
	 		}
	 		if (do2.getDescription().equals("CHNOK") && do2.getColumnC().trim().equals("AFPPF")){
				fdlist =fdRepo.findByEmployeeIdAndAfppfNomination(do2.getEmployee().getId(),true);
				 
				if (fdlist.size() > 0){
					for (FamilyDetail famdet : fdlist){
						if (famdet.getNameOfMember().equals(do2.getColumnA().trim())){
							;
						} else if (famdet.getToDate() == null){
							famdet.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
							fdRepo.save(famdet);
						}
					}
				}
			}
			fdlist =fdRepo.findByEmployeeIdAndRelationship(do2.getEmployee().getId(),do2.getColumnB());
			 
			if (fdlist.size() > 0){
				for (FamilyDetail famdet : fdlist){
					if (famdet.getNameOfMember().equals(do2.getColumnA().trim()) && do2.getColumnC().trim().equals("AFPPF")){
						famdet.setFromDate(do2.getFromDate());
						famdet.setAfppfNomination(true);
						fdRepo.save(famdet);
					}
				}
			}		
	 	}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColA(Do2 do2) {
		return (do2.getColumnA() != null);
	}

	public boolean isValidColB(Do2 do2) {
		return (do2.getColumnB() != null && col5BList.contains(do2.getColumnB().trim()));
	}
	
	public boolean isValidColC(Do2 do2) {		
		return (do2.getColumnC() != null && col5CList.contains(do2.getColumnC().trim()));
	}
	public boolean isValidPercentage( Do2 do2){
	//	return (do2.getColumnC() == null);
		return (do2.getColumnC()!=null && do2.getColumnC().length()>0 && do2.getColumnC().length()<4 && Utils.isNumeric(do2.getColumnC().trim()));
	}
	
}