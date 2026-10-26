package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.do2.AccountingCageBase;

import java.sql.Timestamp; 
import java.util.Arrays;
import java.util.Date;
import java.util.List; 
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
/**
 * @author ITSDC, Sec'bad.
 */
@RestController
public class Famdtl extends AccountingCageBase implements java.io.Serializable {
 
	Usr usr;
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_COLA  = "VL:Name not mentioned in Col.5A.\n";
	static public String INVALID_COLC  = "VL:Date of Birth not mentioned in Col.5C.\n";
	static public String DUPLICATE_DO2 = "VL:Details already available in Famdtl table.\n";
	static public List<String> col5BList = Arrays.asList("FATHER","MOTHER","WIFE","SON","DAUGHTER","BROTHER","SISTER","GRAND SON",
			"GUARDIAN", "STEP BROTHER", "STEP SISTER","GRAND DAUGHTER");
	
	public Famdtl() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (emp==null) {
			emp=Utils.getCurrentEmployee(do2.getArmyNo());
		}		

		if (!isValidColA(do2)) {
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (!isValidColB(do2)) {
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(NOT_IN_APPENDIX_J_FORMAT);
		}
		if (do2.getDescription().equals("FAMDTL")) {
			if (isDuplicate(do2)) {
				validity = false;
				buffer.append(DUPLICATE_DO2);
			}
		}
		if (do2.getDescription().equals("ADOPT") || do2.getDescription().equals("DONADP") || do2.getDescription().equals("DEADPT")){			
			if (!isValidColC(do2)) {
			//	do2.setStatus("MR");
				do2.setStatus(Do2Manager.INVALID);
				validity = false;	
				buffer.append("VL:Date of birth not given in Col-5C/Date given in Col-5C not valid" + NOT_IN_APPENDIX_J_FORMAT);
			}
	
			if ((do2.getDescription().equals("DONADP") || do2.getDescription().equals("DEADPT")) && !isAvailable(do2)) {
				validity = false;
				buffer.append("VL:Not available in Family Details");
			}
		}
		
		if (validity==false) {
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
	 	if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID)) {	
	 	   if (do2.getDescription().equals("DONADP")){
	 			 List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), do2.getColumnB());
	 					 
	 		/*			 Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
	 			 	" fd.employee = :emp and fd.relationship=:fdrelat and fd.recordStatus = 'V'")
	 			 		.setParameter("emp", emp)
	 			 		.setParameter("fdrelat", do2.getColumnB()).getResultList();*/
	 			 	if (fdlist.size() > 0){
	 			 		for (FamilyDetail fd : fdlist){
	 			 			if (fd.getNameOfMember().equals(do2.getColumnA().trim())){
	 			 				fd.setStatusOfMember("DONATED");
	 			 				fd.setFromDate(do2.getFromDate());
	 			 				fd.setCreatedAt(new Timestamp(System.currentTimeMillis()));
	 			 			    fd.setUsr(usr);	
	 			 			    fdRepo.save(fd);
	 			 			//    Utils.getEntityManager().persist(fd);
	 			 			}
	 			 		}
	 			 	}
	 			 	return Do2Manager.SUCCESS;
	 	   }
	 	  if (do2.getDescription().equals("DEADPT")) {
	 			 List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), do2.getColumnB());
	 					 
	 		/*			 Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
	 			 	" fd.employee = :emp and fd.relationship=:fdrelat and fd.recordStatus = 'V'")
	 			 		.setParameter("emp", emp)
	 			 		.setParameter("fdrelat", do2.getColumnB()).getResultList();*/
	 			 	if (fdlist.size() > 0){
	 			 		for (FamilyDetail fd : fdlist){
	 			 			if (fd.getNameOfMember().equals(do2.getColumnA().trim())){
	 			 				fd.setStatusOfMember("DEADOPT");
	 			 				fd.setAdopted(false);
	 			 				fd.setFromDate(do2.getFromDate());
	 			 				fd.setCreatedAt(new Timestamp(System.currentTimeMillis()));
	 			 			    fd.setUsr(usr);	
	 			 			    fdRepo.save(fd);
	 			 			//    Utils.getEntityManager().persist(fd);
	 			 			}
	 			 		}
	 			 	}
	 			 	return Do2Manager.SUCCESS;
	 	   }
	 	   
		   FamilyDetail famdetail = new FamilyDetail();
		   famdetail.setAdopted(false);
		   if (do2.getDescription().equals("ADOPT")){
			   famdetail.setDateOfBirth(DateManager.getDateFromDdMmYyyy(do2.getColumnC().trim()));
			   famdetail.setAdopted(true);
		   } else {
			   famdetail.setDateOfBirth(do2.getFromDate());
		   }
		   famdetail.setDo2(do2);
		   famdetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		   famdetail.setFromDate(do2.getFromDate());
		   if (do2.getColumnB().trim().equals("FATHER") || do2.getColumnB().trim().equals("BROTHER") ||
			   do2.getColumnB().trim().equals("SON") || do2.getColumnB().trim().equals("GRAND SON")){
			   famdetail.setGender("M");
		   } else if (do2.getColumnB().trim().equals("MOTHER") || do2.getColumnB().trim().equals("SISTER") ||
			   do2.getColumnB().trim().equals("DAUGHTER") || do2.getColumnB().trim().equals("WIFE") || do2.getColumnB().trim().equals("GRAND DAUGHTER")){
			   famdetail.setGender("F");
		   }
		   famdetail.setRelationship(do2.getColumnB().trim());
		   famdetail.setRecordStatus("V");
		   famdetail.setNameOfMember(do2.getColumnA().trim());
		   famdetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		   famdetail.setUsr(usr);	
		   fdRepo.save(famdetail);
		//   Utils.getEntityManager().persist(famdetail);
		}
		
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColA(Do2 do2) {
		return (do2.getColumnA() != null && do2.getColumnA().trim().length()>2);
	}

	public boolean isValidColB(Do2 do2) {
		boolean record_validity = true;
		if (do2.getColumnB() == null) {
			record_validity = false;
			buffer.append("VL:Relationship not mentioned in Col.5B");
		} else {
			if (!col5BList.contains(do2.getColumnB().trim())){
				record_validity = false;
				buffer.append("VL:Relationship not mentioned in Col.5B");
			}
			// the below method commented as in employee table  married field is not updated since IIA details are
			// not captured earlier.  
			
			/*if (do2.getColumnB().trim().equals("SON") || do2.getColumnB().trim().equals("DAUGHTER")){
				if (emp.getMarried() != true){
					record_validity = false;
					buffer.append("VL:PBOR not married");
				}
			}*/
			if (do2.getDescription().equals("BIRTH") || do2.getDescription().equals("ADOPT") || do2.getDescription().equals("DONADP")){
				if (do2.getColumnB().trim().equals("FATHER") || do2.getColumnB().trim().equals("MOTHER")){
					record_validity = false;
					buffer.append("VL:Father or Mother cannot be published in Birth");
				}
			}
			if (do2.getDescription().equals("ADOPT") || do2.getDescription().equals("DONADP") || do2.getDescription().equals("DEADPT")){
				if (do2.getColumnB().trim().equals("SON") || do2.getColumnB().trim().equals("DAUGHTER")){
					;
				} else {
					record_validity = false;
					buffer.append("VL:SON or DAUGHTER allowed in ADOPT/DONADP/DEADPT");
				}
			}
		}
		return record_validity;
	}
	public boolean isValidColC(Do2 do2) {
		Date dob = null;
		if (do2.getColumnC() == null){
			return false;
		}
		if (do2.getColumnC().length() == 8) {
			int month = Integer.parseInt(do2.getColumnC().substring(2,4));
			if (month > 0 && month < 13) {
				dob = DateManager.getDateFromDdMmYyyy(do2.getColumnC().trim());
				if (do2.getFromDate().after(dob))
					return true;
			}
		}
		return false;
	}

	public boolean isDuplicate(Do2 do2) {
		List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), do2.getColumnB());
				
		/*		Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
				" fd.employee = :emp and fd.relationship=:fdrelat and fd.recordStatus = 'V'")
				.setParameter("emp", emp)
			    .setParameter("fdrelat", do2.getColumnB()).getResultList();*/
		if (fdlist.size() > 0){
			if (!do2.getColumnB().trim().equals("FATHER") && !do2.getColumnB().trim().equals("MOTHER")){
				for (FamilyDetail fd : fdlist){
					if (fd.getNameOfMember().equals(do2.getColumnA().trim()))
						return true;
				}
			} else {
              return true;
			}
		}
		return false;
	}
	
	public boolean isAvailable(Do2 do2) {
		List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), do2.getColumnB());
				
		/*		Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
				" fd.employee = :emp and fd.relationship=:fdrelat")
				.setParameter("emp", emp)
			    .setParameter("fdrelat", do2.getColumnB()).getResultList();*/
		if (fdlist.size() > 0) {
            for (FamilyDetail fd : fdlist){
            	if (fd.getNameOfMember().equals(do2.getColumnA().trim())) {
            		return true;
            	}
            }
		}
		return false;
	}
}