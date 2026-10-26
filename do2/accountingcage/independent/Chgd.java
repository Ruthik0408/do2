package itsdc.dolphin.do2.accountingcage.independent;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
/*
 * @author N RAMACHANDRAN && K RAMADEVI   DATE: 03.08.2009
 */
@RestController
public class Chgd extends AccountingCageBase implements java.io.Serializable {
	private static final String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	StringBuffer buffer = new StringBuffer();
	String message;
    Usr usr;	
 	 
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
	 
		if (do2.getColumnA() == null){
			validity=false;
		} else {
			List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndNameOfMember(do2.getEmployee().getId(),do2.getColumnA().trim());
		 
			if (fdlist.size()== 0)
				validity=false;				
		}
		if (do2.getColumnB() == null){
			validity=false;
		} else {
			if (do2.getColumnB().equals("SON") || do2.getColumnB().equals("DAUGHTER")){
				;
			} else
				validity=false;
		}
		if (do2.getColumnC() == null){
			validity=false;
		}
			
		if (validity==false) {
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(NOT_IN_APPENDIX_J_FORMAT);	
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if (do2.getAcceptedQe()==null){
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
		List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndNameOfMember(do2.getEmployee().getId(),do2.getColumnA().trim());
		 
		if (fdlist.size() > 0){
		   FamilyDetail famdetail = new FamilyDetail();
		   famdetail.setAdopted(false);
		   famdetail.setDateOfBirth(fdlist.get(0).getDateOfBirth());
		   famdetail.setDo2(do2);
		   famdetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		   famdetail.setFromDate(do2.getFromDate());
		   if (do2.getColumnB().trim().equals("SON")){
			   famdetail.setGender("F");
			   famdetail.setRelationship("DAUGHTER");
		   } else {
			   famdetail.setGender("M");	
			   famdetail.setRelationship("SON");
		   }
		   famdetail.setRecordStatus("V");
		   famdetail.setNameOfMember(do2.getColumnC().trim());
		   famdetail.setCreatedAt(DateManager.getCurrentTimestamp());
		   famdetail.setUsr(usr);
		   fdRepo.save(famdetail);
		    
		   fdlist.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
		   fdlist.get(0).setStatusOfMember("GENDER CHANGED");
		   fdRepo.save(fdlist.get(0));
		  
		}
		return Do2Manager.SUCCESS;
	}
}		
	