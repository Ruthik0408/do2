package itsdc.dolphin.do2.accountingcage.independent;

import java.util.List;

import itsdc.dolphin.do2.AccountingCageBase;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */

//  Renamed on 03.09.2020 for release changed appendixJ version in Pan.java 
@RestController
public class PanOld extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	private StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COLUMNA = "VL:Col.5A should contain Valid Pan number\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private final String DUPLICATE_DO2 = "DU:Duplicate do2 fed for the PBOR. \n";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (isDuplicateDo2(do2)) {
     	 	buffer.append(DUPLICATE_DO2);
	    	validity = false;	 	    	
	    	return validity;
	   	}
		
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA + NOT_IN_APPENDIX_J_FORMAT);
		}
			
		if (validity==false) {
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
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
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());					
		emp.setPanNumber(do2.getColumnA().trim());
		empRepo.save(emp);
	//	Utils.getEntityManager().persist(emp);
	//	Utils.getEntityManager().flush();
		return Do2Manager.SUCCESS;
	}
		
	public boolean isValidA(Do2 do2){		
		if (do2.getColumnA() == null) {
			buffer.append("Column 5A is blank. \n");
			return false;
		}	
		boolean validity = true;
		String panNumber = do2.getColumnA().trim();
		
		/*List<Employee>employeeList = Utils.getEntityManager().createQuery("select emp from Employee emp " +
				" where emp.currentRecord=true  and emp.armyNo=:ano and  emp.qe=:qe" )
				.setParameter("ano", do2.getArmyNo())
				.setParameter("qe", Utils.getCurrentQuarter()).getResultList();
		
		
		if(employeeList.size() > 0) {
			if (employeeList.get(0).getPanNumber()!=null && employeeList.get(0).getPanNumber().equals(panNumber)) {
				buffer.append("Already Pan Number "   +employeeList.get(0).getPanNumber() +" exists for the Army Number " +employeeList.get(0).getArmyNo());
				validity=false;
			}
		}*/
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Employee>employeeList1 = empRepo.findByCurrentRecordAndPanNumberAndQe(emp.getCurrentRecord(), panNumber, Utils.getCurrentQuarter());
				
			/*	Utils.getEntityManager().createQuery("select emp from Employee emp " +
				" where emp.currentRecord=true and emp.panNumber=:panNumber and emp.qe=:qe" )
				.setParameter("panNumber", panNumber)
				.setParameter("qe", Utils.getCurrentQuarter()).getResultList();*/
				
				if(employeeList1.size()>0) {
					if (!do2.getArmyNo().equals(employeeList1.get(0).getArmyNo())) {
						buffer.append("Already Pan Number "   +panNumber +" exists for the Army Number " +employeeList1.get(0).getArmyNo());
						validity=false;
					}
				}
			
		if(panNumber.length()!=10) {
			buffer.append("PAN NUMBER LENGTH SHOULD BE EQUAL TO 10.\n");
			validity=false;
		}
		/*if(panNumber.length()==10 && !isAlphaFirstLetter(panNumber.substring(0,1))){
			buffer.append(" First Character should contain  'A', 'B', 'C', 'D', 'F'");
			validity=false;
		}*/


		if(panNumber.length()==10 && panNumber.charAt(3)!='P') {
			buffer.append("Fourth character of the Pannumber should be equal to 'P'.\n");
			validity=false;
		}
		
	/*	// Fourth letter 'H' is also allowed as per request of PAO(DSC) wef Dolphin Sys Cir 277
		if (panNumber!=null && panNumber.length()==10) {
			if (panNumber.charAt(3)!='P' || panNumber.charAt(3)!='H') {
				buffer.append("Fourth character of the Pannumber should be 'P' or 'H' ! \n");
				validity=false;
			}			
	    } // commented after complaints of more rejections */
		
		
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
		
	Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 	List<Do2> do2List = do2Repo.findByFromDateEqualAndDo2IdNotEqualAndMultipleStatusAndDescription(employee.getId(), do2.getId(),
	 																											do2.getFromDate(), "PAN");
	 			
	 	/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
					+ " AND d.id!=:currentDo2Id AND d.fromDate=:fromDate AND (d.status='V' or d.status='P') AND (d.description='PAN') " ) 
					.setParameter("employee", employee) 
					.setParameter("currentDo2Id", do2.getId()) 
					.setParameter("fromDate", do2.getFromDate())
					.getResultList();*/
		
		 return do2List.size()>0;
	}
}
