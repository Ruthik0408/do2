package itsdc.dolphin.do2.accountingcage.independent;


import java.util.ArrayList;
import java.util.Arrays;
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
public class Siacct extends AccountingCageBase implements java.io.Serializable {

//		@In(required=false)
		Usr usr;
		public StringBuffer buffer = new StringBuffer();
		Employee emp;

		static public String INVALID_COLA  = "VL:Name not mentioned in Col.5A.\n";
		static public String INVALID_COLB  = "VL:Relationship not mentioned in Col.5B.\n";
		static public String INVALID_COLC  = "VL:Bank Acct No. not mentioned in Col.5C.\n";
		final static public List<String> col5BList = Arrays.asList("BROTHER","SISTER", "SPOUSE", "SON","DAUGHTER", 
													"FATHER", "MOTHER", "GUARDIAN", "STEP BROTHER", "STEP SISTER" );
		
		public Siacct() {
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

			if (do2.getDescription().equals("JTACCT") && do2.getColumnA() == null) {
				validity = false;
				buffer.append(INVALID_COLA);		
			}
			
			if (do2.getDescription().equals("JTACCT") && !isValidColB(do2)) {
				validity = false;
				buffer.append(INVALID_COLB);
			}
			
			if (do2.getColumnC() == null) {
				validity = false;
				buffer.append(INVALID_COLC);		
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
		
	@Override
	public String process(Do2 do2) {
		if (do2.getDescription().equals("JTACCT")){
			List<FamilyDetail> fdlist = fdRepo.findByEmpRelationAndNomination(emp.getId(),do2.getColumnB(),do2.getColumnA().trim());
					
	/*				Utils.getEntityManager().createQuery("Select fd from FamilyDetail fd where " +
			" fd.employee = :emp and fd.relationship=:fdrelat and fd.nameOfMember = :nom")
			.setParameter("emp", emp)
			.setParameter("nom", do2.getColumnA().trim())
		    .setParameter("fdrelat", do2.getColumnB()).getResultList();*/
			if (fdlist.size() > 0){
				return Do2Manager.SUCCESS;
			}
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColB(Do2 do2) {
		
		boolean record_validity = true;
		if (do2.getColumnB() == null) {
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5B");
			return record_validity;
		} 
		if (!col5BList.contains(do2.getColumnB())){
			record_validity = false;
			buffer.append("Relationship not mentioned in Col.5B");
		} 
		return record_validity;
	}
}		
	