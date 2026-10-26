package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.do2.AccountingCageBase;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
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
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Marry extends AccountingCageBase implements java.io.Serializable {
//	@In(required=false)
	Usr usr;
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	FamilyDetail fd;
	
	static public String FROM_DATE_BEFORE_WIFE_DOB = "AU:From Date is before wife's Date of Birth.\n";
	static public String FROM_DATE_BEFORE_SELF_DOB = "AU:From Date is before self Date of Birth.\n";
	static public String FROM_DATE_AFTER_DO2_DATE = "VL:From Date is after Do2 Date.\n";
	static public String INVALID_COLA = "VL:Name of Spouse not mentioned in Col. 5A.\n";
	static public String INVALID_COLB = "VL:Invalid Relationship in Col. 5B.\n";
	static public String INVALID_COLC = "VL:Spouse Date of Birth not mentioned in Col. 5C.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public List<String> col5BList = Arrays.asList("DAUGHTER","SISTER","SPOUSE","ADOPTED DAUGHTER");

	public Marry() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}
	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;

		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getDescription().equals("ELOP") || do2.getDescription().equals("ANNULM")|| 
				do2.getDescription().equals("PLMARY")|| do2.getDescription().equals("DIVORC")){
			if (emp.getMarried()==false){
				validity = false;
				buffer.append("VL: Not married as per Employee");
			}
		}
		if (!isValidColA(do2)) {
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!do2.getDescription().equals("DIVORC")) {
			if(!isValidColB(do2)) {
				// do2.setStatus("MR");
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
				buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);
			}
		}
        if (do2.getDescription().equals("MARRY") || do2.getDescription().equals("REMARY")){
			if(!isValidColC(do2)) {
				// do2.setStatus("MR");
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
				buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);
			}
        }
		if(!isValidFromDate(do2)) {
			validity = false;
		}
		if (isAvailable(do2)) {
			validity = false;		
		}
		if (do2.getDescription().equals("ELOP") && isNotAvailable(do2)) {
			validity = false;
			buffer.append("VL:Details not available in Famdtl table.\n");
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
	//	do2.setQe(Utils.getCurrentQuarter());
		do2Repo.save(do2);
		return validity;
	}
	
	@Override
	public String process(Do2 do2) {
		 
		if (emp == null)
			emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getDescription().equals("ELOP") || do2.getDescription().equals("DIVORC") 
				|| do2.getDescription().equals("PLMARY") || do2.getDescription().equals("ANNULM")){
			List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(),"SPOUSE");
					
	/*				Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
			" fd.employee=:emp and fd.relationship='WIFE' and fd.recordStatus = 'V'")
			.setParameter("emp", emp).getResultList();*/
			if (fdlist.size() > 0){
				for (FamilyDetail fd : fdlist){
					if (do2.getDescription().equals("ELOP") && do2.getColumnA().trim().equals(fdlist.get(0).getNameOfMember())){
						fd.setStatusOfMember("ELOPED");
						fd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));	
						fdRepo.save(fd);
					//	em.persist(fd);
					} else if (do2.getDescription().equals("ANNULM")){
						fd.setStatusOfMember("Annument");
						fd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
						fdRepo.save(fd);
					//	em.persist(fd);
					} else if (do2.getDescription().equals("PLMARY")){
						fd.setStatusOfMember("PLURAL");
						fdRepo.save(fd);
					//	em.persist(fd);
					} else {
						fd.setStatusOfMember("DIVORCED");
						fd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
						fdRepo.save(fd);
						
					//	em.persist(fd);
					}
				}
			}
			if (do2.getDescription().equals("PLMARY")) {
				emp.setMarried(true);
			}
			emp.setMarried(false);
			empRepo.save(emp);
		//	em.persist(emp);
			return Do2Manager.SUCCESS;
		}
		emp.setMarried(true);
		empRepo.save(emp);
	//	em.persist(emp);
		fd = new FamilyDetail();
		fd.setFromDate(do2.getFromDate());
		if (!do2.getDescription().equals("ANNULM")){
		fd.setRelationship(do2.getColumnB());
		fd.setNameOfMember(do2.getColumnA());
		}
		fd.setEmployee(emp);
		fd.setGender("F");
		fd.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		fd.setDo2(do2);
		fd.setRecordStatus("V");
		fd.setUsr(usr);
		if ( do2.getDescription().equals("MARRY") || do2.getDescription().equals("REMDAS") || 
				do2.getDescription().equals("PLMARY") || do2.getDescription().equals("MARDAS")){
			try {
				fd.setDateOfBirth(DateManager.getDateFromDdMmYyyy(do2.getColumnC().trim()));
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		fdRepo.save(fd);
	//	em.persist(fd);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidColA(Do2 do2) {
		if (do2.getDescription().equals("DIVORC")) {
		   if (do2.getColumnA() != null && (do2.getColumnA().equals("MAINT") || do2.getColumnA().equals("WITHOUTMAINT")))
			   return true;
			} else if (do2.getDescription().equals("ANNULM")){
				if(do2.getColumnA() == null)
					return true;
			}else {	
			if (do2.getColumnA() != null)
				return true;
		}
		return false;
	}

	public boolean isValidColB(Do2 do2) {
		if (do2.getColumnB() != null && col5BList.contains(do2.getColumnB().trim()))
		   	return true;
		
		return false;		    
	}

	public boolean isValidColC(Do2 do2) {
		if (do2.getDescription().equals("DIVDAS")){
			 if (do2.getColumnC() != null && (do2.getColumnC().equals("MAINT") || do2.getColumnC().equals("WITHOUTMAINT")))
				   return true;
			}else {
				if (do2.getColumnC() != null)
					return true;
			}
		if (do2.getColumnC() != null && do2.getColumnC().length() == 8 ){
			int month = Integer.parseInt(do2.getColumnC().substring(2,4));
			if (month > 0 && month < 13) {
				Date wifedob = DateManager.getDateFromDdMmYyyy(do2.getColumnC().trim());
				if (do2.getFromDate().after(wifedob))
					return true;
			}
		} 
		return false;
	}
	public boolean isAvailable(Do2 do2) {
		List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), "SPOUSE");
				
	//			Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
	//			" fd.employee=:emp and fd.relationship='WIFE' and fd.recordStatus = 'V'")
	//			.setParameter("emp", emp).getResultList();
		if (fdlist.size() > 0){
			if (do2.getDescription().equals("MARRY")){
                buffer.append("VL:Details already available in Famdtl table.\n");
                return true;
			} else {
				for (FamilyDetail fd : fdlist){
					if (do2.getDescription().equals("REMARY")){
						if (fd.getNameOfMember().equals(do2.getColumnA().trim())){
							buffer.append("VL:Details already available in Famdtl table.\n");
							return true;
						} else if (fd.getToDate() == null || fd.getStatusOfMember() != null){
							buffer.append("VL:First wife alive/not divorced\n");
							return true;
						}
					} 					
				}				
			}
		}
		return false;
	}
	public boolean isNotAvailable(Do2 do2) {
		List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationStatus(emp.getId(), "SPOUSE");
				
			//	Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
		//		" fd.employee=:emp and fd.relationship='WIFE' and fd.recordStatus = 'V'")
			//	.setParameter("emp", emp).getResultList();
		if (fdlist.size() > 0){
			for (FamilyDetail fd : fdlist){
				 if (do2.getDescription().equals("ELOP")){
					if (fd.getNameOfMember().equals(do2.getColumnA().trim()))
						return false;
				 }		
			}
		}
		return true;
	}
	public boolean isValidFromDate(Do2 do2) {
		if (do2.getFromDate().after(emp.getBirthDate())) {
			return true;
		} else {
			buffer.append(FROM_DATE_BEFORE_SELF_DOB);
		}		
		if (do2.getFromDate().before(do2.getDo2Date())) {
			return true;
		} else {
			buffer.append(FROM_DATE_AFTER_DO2_DATE);
		}		
		return false;
	}
}