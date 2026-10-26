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
import itsdc.dolphin.utils.Utils;
 
 
@RestController
public class Chname extends AccountingCageBase implements java.io.Serializable {
	
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	FamilyDetail fdRecord,fd;
	static public String INVALID_MARITAL_STATUS = "AU:Invalid as PBOR is not married as per Employee table.\n";
	static public String INVALID_COLA= "VL:Changed Name of child not mentioned in Col.5A.\n";
	static public String FD_RECORD_NOT_FOUND= "AU:Original Family Detail Record Not Found.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
 
		if(!isMarried(do2)) {
			validity = false;
			buffer.append(INVALID_MARITAL_STATUS);
		}

		if(!isValidColA(do2)) {
		//	do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}

		if(!isRecordFound(do2)) {
			validity = false;
			buffer.append(FD_RECORD_NOT_FOUND);
		}

		if(validity==false) {
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
		 
		FamilyDetail fd = new FamilyDetail();
		if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID)) {
			do2Repo.save(do2);
			
			fd.setFromDate(do2.getFromDate());
		    fd.setNameOfMember(do2.getColumnA());
		    fd.setDateOfBirth(fdRecord.getDateOfBirth());
		    fd.setGender(fdRecord.getGender());
		    fd.setRelationship(fdRecord.getRelationship());
		   	fd.setEmployee(emp);
			fd.setDo2(do2);
			fdRepo.save(fd);
			return Do2Manager.SUCCESS;
		}
		return Do2Manager.FAILURE;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		if(emp == null) {
			emp = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		}
		if(isRecordAvailable(refDo2)) {
			restoreFamilyDetail(currentDo2, refDo2);
			return Do2Manager.SUCCESS;
		}
		return Do2Manager.FAILURE;
	}

	protected void restoreFamilyDetail(Do2 currentDo2, Do2 refDo2) {
		List<FamilyDetail> fdList =fdRepo.findByDo2Id(refDo2.getId()); 
				
			 
		if(fdList.size() > 0) {
			for(FamilyDetail fd:fdList) {
				fdRepo.delete(fd);
			}
			 
		}
		
	}

	public boolean isRecordAvailable(Do2 do2) {
		if(do2.getColumnA() != null) {
			List<FamilyDetail> fdList =fdRepo.findByEmployeeIdAndRelationship(do2.getEmployee().getId(),"WIFE"); 
					
		 
			fd = fdList.get(0);
       		return (fdList.size() > 0);
		}	
		return false;
	}

	
	public boolean isMarried(Do2 do2) {
		return (emp.getMarried());
	}

	public boolean isValidColA(Do2 do2) {
		return (do2.getColumnA() != null);
	}

	public boolean isRecordFound(Do2 do2) {
		if(do2.getColumnA() != null) {
			List<String> relList=Arrays.asList(new String [] {"SON","DAUGHTER","BROTHER","SISTER"});
			List<FamilyDetail> fdList = fdRepo.findByEmpRelationListAndNameOfMember(do2.getEmployee().getId(), relList,do2.getColumnA());
			
			 
       		if (fdList.size() > 0) {
       			fdRecord = fdList.get(0);
				return (fdList.get(0).getRelationship().equals ("SON") ||
				  (fdList.get(0).getRelationship().equals ("DAUGHTER")) ||
				  (fdList.get(0).getRelationship().equals ("BROTHER")) ||
				  (fdList.get(0).getRelationship().equals ("SISTER")));
			}
		}	
		return false;
	}
}