package itsdc.dolphin.do2.accountingcage.independent;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 

 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */
@RestController
public class Email extends AccountingCageBase implements java.io.Serializable {
	Usr usr;
	 
	String message;
	private StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COLUMNA = "VL:Col.5A should contain domain name\n";
	private static final String INVALID_COLUMNB = "VL:Col.5B should contain service provider \n";

/*	public Email() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}*/
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);
		}
		
		if (!isValidB(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNB);
		}
		if (do2.getFromDate().before(DateManager.getEmailRevisedDate())) {
			if (!isValidAB(do2)){
				validity = false;
				
			}
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
				
		do2Repo.save(do2);
		message = buffer.toString();
		do2.setReason(message);

		return validity;
	}

	@Override
	public String process(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getFromDate().after(DateManager.getEmailRevisedDate())) {
			if (do2.getColumnB().equals("SELF") || do2.getColumnB().equals("self")) {
				    emp.setEmailId(do2.getColumnA());
				    empRepo.save(emp);
				//	Utils.getEntityManager().persist(emp);
				//	Utils.getEntityManager().flush();
			}else if(do2.getColumnB()!=null && do2.getColumnC()!=null) {
				List<FamilyDetail> fdlist =fdRepo.findByEmpRelationAndNomination(emp.getId(), do2.getColumnB(), do2.getColumnC().trim());

		/*				Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
						" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
						.setParameter("emp", emp)
						.setParameter("nom", do2.getColumnC().trim())
					    .setParameter("fdrelat", do2.getColumnB()).getResultList();*/
						
						
				 if (fdlist.size() == 0) {
							FamilyDetail famdetail = new FamilyDetail();
							famdetail.setDo2(do2);
							famdetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
							famdetail.setFromDate(do2.getFromDate());
							if (do2.getColumnB().trim().equals("FATHER") || do2.getColumnB().trim().equals("BROTHER") ||
								   do2.getColumnB().trim().equals("SON")){
								   famdetail.setGender("M");
							   } else if (do2.getColumnB().trim().equals("MOTHER") || do2.getColumnB().trim().equals("SISTER") ||
								   do2.getColumnB().trim().equals("DAUGHTER") || do2.getColumnB().trim().equals("SPOUSE")){
								   famdetail.setGender("F");
							   }
							   famdetail.setRelationship(do2.getColumnB().trim());
							   famdetail.setRecordStatus("V");
							   famdetail.setNameOfMember(do2.getColumnC().trim());
							   famdetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
							   famdetail.setEmail(do2.getColumnA());
							   famdetail.setUsr(usr);	
							   fdRepo.save(famdetail);
						//	   Utils.getEntityManager().persist(famdetail);
						}
			}
			
			
		}else {
			String domain = do2.getColumnA().trim();
			String serviceProvider = do2.getColumnB().trim();
			String email = domain.concat("@").concat(serviceProvider);
			StringBuilder sb = new StringBuilder();
		    sb.append(email);
		    for(int i = 0; i<sb.length();i++){
		        char c = sb.charAt(i);
		        if(Character.isUpperCase(c)){
		            sb.setCharAt(i, Character.toLowerCase(c));
		        }
		    }
		  	emp.setEmailId(email);
		  	empRepo.save(emp);
		//	Utils.getEntityManager().persist(emp);
		//	Utils.getEntityManager().flush();
		}
		return Do2Manager.SUCCESS;
	}
		
	public boolean isValidA(Do2 do2){	
		
		boolean validity = true;
		if (do2.getColumnA() == null) {
			buffer.append("Column 5A is blank. \n");
			return false;
		}	
		
		return true;
	}
	
public boolean isValidB(Do2 do2){	
		
		boolean validity = true;
		if (do2.getColumnB() == null) {
			buffer.append("Column 5B is blank. \n");
			return false;
		}	
		
		return true;
	}

public boolean isValidAB(Do2 do2) {
	boolean validity = true;
	String domain = do2.getColumnA().trim();
	String serviceProvider = do2.getColumnB().trim();
	String email = domain.concat("@").concat(serviceProvider);
	Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
	
	List<Employee>employeeList = empRepo.findByCurrentRecordAndEmailIdAndQe(emp.getCurrentRecord(), emp.getEmailId(),Utils.getCurrentQuarter());
			
		/*	Utils.getEntityManager().createQuery("select emp from Employee emp " +
			" where emp.currentRecord=true and emp.emailId=:email and emp.qe=:qe" )
			.setParameter("email", email)
			.setParameter("qe", Utils.getCurrentQuarter()).getResultList();*/
			
			if(employeeList.size()>0) {
				if (!do2.getArmyNo().equals(employeeList.get(0).getArmyNo())) {
					buffer.append("Already Email Id "   +email +" exists for the Army Number " +employeeList.get(0).getArmyNo());
					validity=false;
				}
			}
			
			if (validity == false)
				return false;
			else
				return true;
		
}
	
	
		
}
