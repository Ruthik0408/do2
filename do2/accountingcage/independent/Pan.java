package itsdc.dolphin.do2.accountingcage.independent;
 
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import itsdc.dolphin.do2.AccountingCageBase;
 
import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */
@RestController
public class Pan extends AccountingCageBase implements java.io.Serializable {

	 
	Usr usr;
	private StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COLUMNA = "VL:Col.5A should contain Valid Pan number\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private final String DUPLICATE_DO2 = "DU:Duplicate Do2 fed for the PBOR. \n";
	private static final String INVALID_COLUMNB = "VL:Col.5B should contain Valid Relationship - SELF / WIFE \n";
	private static final String MISSING_FAMILY_DETAIL = "VL:FAMILY DETAIL TABLE NOT UPDATED FOR RELATIONSHIP - WIFE \n";
	private static final String WIFE_PAN_ALREADY_UPDATED = "VL:Wife's PAN Already Updated In Family Detail Table \n";
	private static final String INDIVIDUAL_PAN_ALREADY_UPDATED = "VL: Individual's PAN Already Updated In Employee Table \n";
	static public String INVALID_COLA  = "VL:PAN No. not mentioned in Col.7A.\n";
	static public String INVALID_COLB  = "VL:Relationship not mentioned in Col.7B.\n";
	static public String INVALID_COLC  = "VL:Name not mentioned in Col.7C.\n";
	
	final static public List<String> col5BList = Arrays.asList("BROTHER","SISTER", "WIFE", "SON","DAUGHTER", "FATHER", "MOTHER", "SELF");
	Employee employee;
		
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
		if (isDuplicateDo2(do2)) {
     	 	do2.setReason(DUPLICATE_DO2);
     	 	do2.setStatus(Do2Manager.INVALID);
	    	validity = false;		    	
	    	return validity;
	   	}
 
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);
		}
 
		
		if (do2.getColumnB() == null) {
			validity = false;
			buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);		
		}
		
		if (do2.getColumnB()!=null && !do2.getColumnB().equals("SELF")) {
			if (do2.getColumnC() == null) {
				validity = false;
				buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);		
			}
		}
		
		if (do2.getColumnB()!=null && do2.getColumnB().equals("WIFE")) {
			List<FamilyDetail> fdList = fdRepo.findByEmpRelationAndDependant(do2.getEmployee().getId(), "WIFE");
					
			//		Utils.getEntityManager().createQuery("select fd from FamilyDetail fd where fd.employee=:emp and fd.relationship='WIFE' "
				//						+ " and fd.recordStatus='V' and fd.dependant=true ")
					//					.setParameter("emp", do2.getEmployee()).getResultList();
			
			if (fdList==null || (fdList!=null && fdList.size()==0)) {				
				validity = false;
				buffer.append(MISSING_FAMILY_DETAIL);
			}
			
			if (fdList!=null && fdList.size()>0 && fdList.get(0).getPanNo()!=null && fdList.get(0).getPanNo().length()==10) {
				validity = false;
				buffer.append(WIFE_PAN_ALREADY_UPDATED);
			}
		}

		if (do2.getColumnB()!=null && do2.getColumnB().equals("SELF")) {		 			
			if (employee!=null && employee.getPanNumber()!=null && employee.getPanNumber().length()==10  && employee.getPanNumber().equals(do2.getColumnA())) {
				validity = false;
				buffer.append(INDIVIDUAL_PAN_ALREADY_UPDATED);
			}
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
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnB()!=null && do2.getColumnB().equals("SELF")) {
			employee.setPanNumber(do2.getColumnA().trim());
			empRepo.save(employee);
		//	Utils.getEntityManager().persist(employee); 
		} else {
			List<FamilyDetail> fdlist = fdRepo.findByEmpRelationAndNomination(employee.getId(), do2.getColumnB(), do2.getColumnC().trim());
					
			/*		Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
					" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
					.setParameter("emp", employee)
					.setParameter("nom", do2.getColumnC().trim())
				    .setParameter("fdrelat", do2.getColumnB()).getResultList();*/
					
					if (fdlist!=null && fdlist.size()>0){
						fdlist.get(0).setPanNo(do2.getColumnA().trim());
						fdRepo.save(fdlist.get(0));
					//	Utils.getEntityManager().merge(fdlist.get(0));
					//	Utils.getEntityManager().flush();
						return Do2Manager.SUCCESS;
					} else if (fdlist.size() == 0) {
						FamilyDetail famdetail = new FamilyDetail();
						famdetail.setDo2(do2);
						famdetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
						famdetail.setFromDate(do2.getFromDate());
						if (do2.getColumnB().trim().equals("FATHER") || do2.getColumnB().trim().equals("BROTHER") ||
								do2.getColumnB().trim().equals("SON")){
							   famdetail.setGender("M");
						   } else if (do2.getColumnB().trim().equals("MOTHER") || do2.getColumnB().trim().equals("SISTER") ||
								   do2.getColumnB().trim().equals("DAUGHTER") || do2.getColumnB().trim().equals("WIFE")){
							   famdetail.setGender("F");
						   }
						   famdetail.setRelationship(do2.getColumnB().trim());
						   famdetail.setRecordStatus("V");
						   famdetail.setNameOfMember(do2.getColumnC().trim());
						   famdetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
						   famdetail.setPanNo(do2.getColumnA().trim());
						   famdetail.setUsr(usr);
						   fdRepo.save(famdetail);
						//   Utils.getEntityManager().persist(famdetail);
					}
				}
			
	//	Utils.getEntityManager().flush();
		return Do2Manager.SUCCESS;
	}
		
	public boolean isValidA(Do2 do2){		
		if (do2.getColumnA() == null) {
			buffer.append("Column 7A is blank. \n");
			return false;
		}	
		boolean validity = true;
		String panNumber = do2.getColumnA().trim();
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		if(emp.getPanNumber()!=null) {
		List<Employee>employeeList1 = empRepo.findByCurrentRecordAndPanNumberAndQe(emp.getCurrentRecord(),emp.getPanNumber(), Utils.getCurrentQuarter());
		
				
		/*		Utils.getEntityManager().createQuery("select emp from Employee emp " +
				" where emp.currentRecord=true and emp.panNumber=:panNumber and emp.qe=:qe" )
				.setParameter("panNumber", panNumber)
				.setParameter("qe", Utils.getCurrentQuarter()).getResultList();*/
				
		if(employeeList1.size()>0) {
			if (!do2.getArmyNo().equals(employeeList1.get(0).getArmyNo())) {
				buffer.append("Already Pan Number "   +panNumber +" exists for the Army Number " +employeeList1.get(0).getArmyNo());
				validity=false;
			}
		}
	}
		
		List<FamilyDetail> fdList =  fdRepo.findByPanNo(panNumber);
				
		//		Utils.getEntityManager().createQuery("select fd from FamilyDetail fd where fd.recordStatus='V' and fd.panNo!=null and fd.panNo=:pan ")
		//							.setParameter("pan", panNumber).getResultList();
		
		if (fdList!=null && fdList.size()>0) {
			if (!do2.getEmployee().equals(fdList.get(0).getEmployee())) {
				buffer.append("Already Pan Number "   +panNumber +" exists for dependant of the Army Number " +fdList.get(0).getEmployee().getArmyNo());
				validity=false;
			}
		}
		
		if(panNumber.length()!=10) {
			buffer.append("PAN NUMBER LENGTH SHOULD BE EQUAL TO 10.\n");
			validity=false;
		}
		
		if(panNumber.length()==10 && panNumber.charAt(3)!='P') {
			buffer.append("Fourth character of the Pannumber should be equal to 'P'.\n");
			validity=false;
		}
		
		if(panNumber.length()==10 && !isAlphaUpper(panNumber.substring(0,5))) {
			buffer.append("First five characters should be uppercasealphabets.\n");
			validity=false;
		} 
		if(panNumber.length()==10 && !isNumeric(panNumber.substring(5,9))) {
			buffer.append("6,7,8,9 Characters should be numerics.\n");
			validity=false;
		}
		if(panNumber.length()==10 &&  !isAlphaUpper(panNumber.substring(9))) {
			buffer.append("Last character should be uppercase alphabet.\n");
			validity=false;
		}
		
		if(panNumber.length()==10 && panNumber.charAt(9)=='O'){
			buffer.append("Last character should not be 'O'; ");
			validity=false;
		}
		
		if (validity == false)
			return false;
		else
			return true;
	}
	public boolean isAlphaUpper(final String s) {
		  final char[] chars = s.toCharArray();
		  for (int x = 0; x < chars.length; x++) {      
		    final char c = chars[x];
		     	if ((c >= 'A') && (c <= 'Z')) continue; // uppercase
		     	return false;
		  }  
		  return true;
	}
	public boolean isNumeric(final String s) {
		final char[] chars = s.toCharArray();
		for (int x = 0; x < chars.length; x++) {      
			final char c = chars[x];
	    	if ((c >= '0') && (c <= '9')) continue; // numeric
	    	return false;
		}  
		return true;
	}
	
	public boolean isAlphaFirstLetter(final String s) {
		  final char[] chars = s.toCharArray();
		  for (int x = 0; x < chars.length; x++) {      
		    final char c = chars[x];
		     if ((c >= 'A') && (c <= 'F')) continue; // uppercase
		      return false;
		  }  
		  return true;
	}
	
	public boolean isDuplicateDo2(Do2 do2) {
		/*
		 * List<Do2> do2List = Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee " +
		 * " AND d.id!=:currentDo2Id AND d.fromDate=:fromDate AND (d.status='V' or d.status='P') AND (d.description='PAN') "
		 * ) .setParameter("employee", employee) .setParameter("currentDo2Id",
		 * do2.getId()) .setParameter("fromDate", do2.getFromDate()) .getResultList();
		 */
		
		 List<Do2> do2List = do2Repo.findByEmployeeDo2IdNotEqualToStatusValidOrPendingAndColumnB(employee.getId(),
				 																	do2.getId(), "PAN", do2.getColumnB());
				 
				 
		/*		 Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee " +
				   " AND d.id!=:currentDo2Id AND (d.status='V' or d.status='P') AND d.description='PAN' "+
				   " AND d.columnB=:colB ") .setParameter("employee", employee) .setParameter("currentDo2Id", do2.getId())
				   .setParameter("colB", do2.getColumnB()) .getResultList();*/
		
		 return do2List.size()>0;
	}
}


