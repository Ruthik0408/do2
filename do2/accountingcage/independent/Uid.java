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
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Uid extends AccountingCageBase implements java.io.Serializable {

//		@In(required=false)
		Usr usr;
		public StringBuffer buffer = new StringBuffer();
		Employee emp;
		static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
		static public String INVALID_COLA  = "VL:Relationship not mentioned in Col.5A.\n";
		static public String INVALID_COLB  = "VL:Name not mentioned in Col.5B.\n";
		static public String INVALID_COLC  = "VL:UID No. not mentioned in Col.5C.\n";
		static public String INVALID_COLA_COLB = "VL : Either Name / RelationShip not matching with details availalbe in family details. \n";
		final static public List<String> col5AList = Arrays.asList("BROTHER","SISTER", "SPOUSE", "SON","DAUGHTER", "FATHER", "MOTHER", "SELF");
	
		public Uid() {
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
			} else {
				String uidNo = do2.getColumnC().trim();
				Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
				List<Employee>employeeList = empRepo.findByCurrentRecordAndUidNoAndQe(emp.getCurrentRecord(), uidNo, Utils.getCurrentQuarter());
						
				/*		Utils.getEntityManager().createQuery("select emp from Employee emp " +
						" where emp.currentRecord=true and emp.uidNo=:uidNo and emp.qe=:qe" )
						.setParameter("uidNo", uidNo)
						.setParameter("qe", Utils.getCurrentQuarter()).getResultList();*/
						
				if (employeeList!=null && employeeList.size()>0) {
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
						System.out.println("... armyno 222222222 " +do2.getArmyNo()+"  fdlist size " +fdList.size());
					}
				}
			}
			
			// the following method commented since the invalid do2s on account of UID is increasing due to 
			// non availability of details of family members in family_detail table and name mismatching 
			//  of the employee and family members. As of now as there is no check of family details data 
			// in adjusting any of the casulaties family details are added if not availablee in the family detail 
			// table based on the information availalbe in UID do2 to reduce the rejections on account of UID.
			// However Date of BIRth is not captured since no details of DOB in UID 
			 
			/*if (!isValidColAColB(do2)) {
				validity = false;
				buffer.append(INVALID_COLA_COLB);
			}*/
			

			

			
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
			
			String str = null;
			String strWithoutSpace = null;
			
			  str = do2.getColumnC();
			 strWithoutSpace = str.replaceAll("\\s", "");
				 
			if (do2.getColumnA().equals("SELF") || do2.getColumnA().equals("self")) {
				Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
				/*char[] strArray = str.toCharArray();
				 StringBuffer sb = new StringBuffer();
		        for (int i = 0; i < strArray.length; i++) {
		        	if( (strArray[i] != ' ') && (strArray[i] != '\t') {
		                sb.append(strArray[i]);
		            }
		        }*/
		        emp.setUidNo(strWithoutSpace);
			//	emp.setUidNo(do2.getColumnC().trim());
		        empRepo.save(emp);
		//		Utils.getEntityManager().persist(emp);
		//		Utils.getEntityManager().flush();
			} else {
				List<FamilyDetail> fdlist = fdRepo.findByEmpRelationAndNomination(emp.getId(), do2.getColumnA(), do2.getColumnB());
						
		/*				Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
				" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
				.setParameter("emp", emp)
				.setParameter("nom", do2.getColumnB().trim())
			    .setParameter("fdrelat", do2.getColumnA()).getResultList();*/
				
				if (fdlist.size() > 0){
					fdlist.get(0).setUidNo(strWithoutSpace);
					fdRepo.save(fdlist.get(0));
				//	Utils.getEntityManager().merge(fdlist.get(0));
			//		Utils.getEntityManager().flush();
					return Do2Manager.SUCCESS;
				} else if (fdlist.size() == 0) {
					FamilyDetail famdetail = new FamilyDetail();
					famdetail.setDo2(do2);
					famdetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
					famdetail.setFromDate(do2.getFromDate());
					if (do2.getColumnA().trim().equals("FATHER") || do2.getColumnA().trim().equals("BROTHER") ||
						   do2.getColumnA().trim().equals("SON")){
						   famdetail.setGender("M");
					   } else if (do2.getColumnA().trim().equals("MOTHER") || do2.getColumnA().trim().equals("SISTER") ||
						   do2.getColumnA().trim().equals("DAUGHTER") || do2.getColumnA().trim().equals("SPOUSE")){
						   famdetail.setGender("F");
					   }
					   famdetail.setRelationship(do2.getColumnA().trim());
					   famdetail.setRecordStatus("V");
					   famdetail.setNameOfMember(do2.getColumnB().trim());
					   famdetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					   famdetail.setUidNo(strWithoutSpace);
					   famdetail.setUsr(usr);	
					   fdRepo.save(famdetail);
					//   Utils.getEntityManager().persist(famdetail);
				}
			}
			
			return Do2Manager.SUCCESS;
		}
	
	public boolean isValidColA(Do2 do2) {
		
		boolean record_validity = true;
		if (do2.getColumnA() == null) {
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5A");
			return record_validity;
		} 
		if (!col5AList.contains(do2.getColumnA())){
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5A");
		} 
		return record_validity;
	}
	
	public boolean isValidColAColB(Do2 do2) {
		boolean record_validity = true;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA()!=null  && do2.getColumnB()!=null) {
			if (!(do2.getColumnA().equals("SELF") || do2.getColumnA().equals("self"))) {
				List<FamilyDetail> fdlist = fdRepo.findByEmpRelationAndNomination(emp.getId(), do2.getColumnA(), do2.getColumnB().trim());
						
		/*				Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
						" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
						.setParameter("emp", emp)
						.setParameter("nom", do2.getColumnB().trim())
					    .setParameter("fdrelat", do2.getColumnA()).getResultList();*/
						
						if (fdlist.size() == 0 ) {
							record_validity = false;
							buffer.append(" Eitehr Family details not Availalbe or Name/Releation Ship not matching  ");
						}
			}
			/*if (do2.getColumnA().equals("SELF")) {
				if (do2.getColumnB().equals(employee.getName())) {
					record_validity=true;
				} else record_validity = false;
			} else {
				List<FamilyDetail> fdlist = Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
				" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
				.setParameter("emp", emp)
				.setParameter("nom", do2.getColumnB().trim())
			    .setParameter("fdrelat", do2.getColumnA()).getResultList();
				
				if (fdlist.size() == 0 ) {
					record_validity = false;
					buffer.append(" Eitehr Family details not Availalbe or Name/Releation Ship not matching  ");
				}
				
			}*/
				/*if (fdlist.size() > 0){
				 if (!fdlist.get(0).getRelationship().equals(do2.getColumnA()) && !fdlist.get(0).getNameOfMember().equals(do2.getColumnB())) {
					 record_validity = false;
				 }
			} else  {
				record_validity = false;
				buffer.append("Family details not Availalbe. Please add the same /  Either Name / RelationShip not matching with details availalbe in family details ");
			}*/
		} 
		  return record_validity;
	}
	
}		
	