package itsdc.dolphin.do2.accountingcage.independent;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Biomet extends AccountingCageBase implements java.io.Serializable {

	Usr usr;
	public StringBuffer buffer = new StringBuffer();
	Employee emp; 
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_COLA  = "VL:Relationship not mentioned in Col.5A.\n";
	static public String INVALID_COLB  = "VL:Name not mentioned in Col.5B.\n";
	static public String INVALID_COLC  = "VL:UID No. not mentioned in Col.5C.\n";
	static public String INVALID_COLA_COLB = "VL: Name not matching with details available in family details. \n";
	static public String INCORRECT_SELF_UID = "VL: Incorrect UID Number Mentioned For SELF. \n";
	static public String INCORRECT_UID = "VL: Incorrect UID Number Mentioned For Relation. \n";
	static public String DUPLICATE = "DU: Valid DOII Already Exists For Same Relation And UID. \n";
	
	final static public List<String> col5AList = Arrays.asList("SELF", "SPOUSE", "SON", "DAUGHTER");

	public Biomet() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (emp == null) {
			emp = Utils.getCurrentEmployee(do2.getArmyNo());
		}		

		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)
				|| (do2.getColumnA()!=null && !col5AList.contains(do2.getColumnA().toUpperCase()))) {
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0)) {
			validity = false;
			buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);		
		}
		
		if (do2.getColumnC()==null || (do2.getColumnC()!=null && (do2.getColumnC().length()!=12 || !Utils.isNumeric(do2.getColumnC())))) {
			validity = false;
			buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);		
		} else {
			String uidNo = do2.getColumnC().trim();	 
			Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
			List<Employee>employeeList = empRepo.findByCurrentRecordAndUidNoAndQe(emp.getCurrentRecord(),uidNo, Utils.getCurrentQuarter());
					
			/*		Utils.getEntityManager().createQuery("select emp from Employee emp " +
					" where emp.currentRecord=true and emp.uidNo=:uidNo and emp.qe=:qe" )
					.setParameter("uidNo", uidNo)
					.setParameter("qe", Utils.getCurrentQuarter()).getResultList();*/
					
			if (employeeList.size()>0) {
				if (!do2.getArmyNo().equals(employeeList.get(0).getArmyNo())) {
					buffer.append("Already Aadhaar Number "   +uidNo +" exists for the Army Number " +employeeList.get(0).getArmyNo());
					validity=false;
				}
			}
			
			List<FamilyDetail> fdList = fdRepo.findByUidNoAndStatus(do2.getColumnC().trim());
			if (fdList!=null && fdList.size()>0) {
				System.out.println("... armyno " +do2.getArmyNo()+"  fdlist size " +fdList.size());
				if (do2.getEmployee().getId()==fdList.get(0).getEmployee().getId() && 
						(do2.getColumnA()!=null && fdList.get(0).getRelationship()!=null
							&& ((do2.getColumnA().toLowerCase().equals(fdList.get(0).getRelationship().toLowerCase()))
								|| (do2.getColumnA().toLowerCase().equals("wife") && fdList.get(0).getRelationship().toLowerCase().equals("spouse"))
								|| (do2.getColumnA().toLowerCase().equals("spouse") && fdList.get(0).getRelationship().toLowerCase().equals("wife"))))) {
					System.out.println("... armyno 11111111111111" +do2.getArmyNo()+"  fdlist size " +fdList.size());
					;
				} else {
					buffer.append("Already Aadhaar Number "   +uidNo +" exists for the Relation Of ArmyNumber "+fdList.get(0).getEmployee().getArmyNo());
					validity=false;
				}
			}
		}
		/*
		
		else {
			Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
			if (do2.getColumnA()!=null && emp!=null) {
				if (do2.getColumnA().trim().toUpperCase().equals("SELF")) {
					if (emp.getUidNo()!=null && !emp.getUidNo().equals(do2.getColumnC().trim())) {
						validity = false;
						buffer.append(INCORRECT_SELF_UID);						
					}  
				} else {
					if (do2.getColumnB()!=null) {
						List<FamilyDetail> fdList = fdRepo.findByEmpRelationAndNomination(emp.getId(), do2.getColumnA().toUpperCase(), do2.getColumnB().toUpperCase());
						if (fdList!=null && fdList.size()>0) {
							boolean uidMatched=false;
							for (FamilyDetail fd : fdList) {
								if (fd.getRecordStatus()!=null && fd.getRecordStatus().equals("V")) {
									if (fd.getUidNo()!=null && fd.getUidNo().equals(do2.getColumnC().trim())) {
										uidMatched=true;
										break;
									}
								}
							}
							validity=uidMatched;
							buffer.append(INCORRECT_UID);	
						} else {
							validity = false;
							buffer.append(INCORRECT_UID);	
						}
					}
					
				}						
			}
		}*/ // commented on 22.12.2023
	 
		if (isDuplicate(do2)) {
			validity = false;
			buffer.append(DUPLICATE);	
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
		
	public boolean isDuplicate(Do2 do2) {
		List<Do2> do2List=do2Repo.findAllByEmployeeIdAndDescriptionAndColumnAAndColumnC(do2.getEmployee().getId(), do2.getId(), do2.getDescription(), 
															do2.getColumnA().toLowerCase(), do2.getColumnC());		
		return (do2List!=null && do2List.size()>0);
	}
	
	@Override
	public String process(Do2 do2) {		 
		return Do2Manager.SUCCESS;
	}
}		
	