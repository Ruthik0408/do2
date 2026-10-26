package itsdc.dolphin.do2.accountingcage.independent;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.utils.Utils;

 
@RestController
public class Chsrnm extends AccountingCageBase implements java.io.Serializable {

//		@In(required=false)
		Usr usr;
		public StringBuffer buffer = new StringBuffer();
		Employee emp;

		static public String INVALID_COLA  = "VL:Relationship not mentioned in Col.5A.\n";
		static public String INVALID_COLB  = "VL:Name not mentioned in Col.5B.\n";
		static public String INVALID_COLC  = "VL:Name not mentioned in Col.5C.\n";
		static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
		
		 
		@Override
		public boolean isValid(Do2 do2) {
			boolean validity = true;
			
			if (emp == null) {
				emp = Utils.getCurrentEmployee(do2.getArmyNo());
			}		

			if (!isValidColA(do2)) {
				validity = false;
				buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
			}

			if (do2.getColumnB() == null) {
				validity = false;
				buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);		
			}
			
			if (do2.getColumnC() == null) {
				validity = false;
				buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);		
			}
			
			if (validity==false) {
				// do2.setStatus("MR");
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
		List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndRelationshipAndNameOfMember(do2.getEmployee().getId(),do2.getColumnA(), do2.getColumnB().trim());
	 
		if (fdlist.size() > 0){
			fdlist.get(0).setNameOfMember(do2.getColumnC().trim());
			fdRepo.save(fdlist.get(0));
			 
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColA(Do2 do2) {
		List<String> srnmList = Arrays.asList("SPOUSE", "SON","DAUGHTER");
		List<String> fmnmList = Arrays.asList("MOTHER","FATHER","BROTHER","SISTER", "SON", "DAUGHTER");
		boolean record_validity = true;
		if (do2.getColumnA() == null) {
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5A");
			return record_validity;
		} 
		if ((do2.getDescription().equals("CHNMWF") || do2.getDescription().equals("CHMADN")) && !do2.getColumnA().equals("SPOUSE")){
			record_validity = false;
			buffer.append("SPOUSE to be mentioned in Col.5A");
		} else if (do2.getDescription().equals("CHSRNM") && !srnmList.contains(do2.getColumnA())){
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5A");
		} else if (do2.getDescription().equals("CHFMNM") && !fmnmList.contains(do2.getColumnA())){
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5A");
		}
		return record_validity;
	}
}	

	