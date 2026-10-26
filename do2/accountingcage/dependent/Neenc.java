package itsdc.dolphin.do2.accountingcage.dependent;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Neenc extends AccountingCageBase implements java.io.Serializable {
	
	Employee employee;
	double encashDue = 0, encashDrawn=0, encashDiff=0;
	public StringBuffer buffer = new StringBuffer();
	static public String INVALID_TJRA = "VL:Invalid Encash Days.\n";
	static public String DUPLICATE_DO2 = "DU:Dulicate Do2.\n";
	static public String PAYCODE_UNAVAILABLE = "FSCASH paycode is not available in PayCode Table";
	

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!isValidTjra(do2)){
			validity = false;
			buffer.append(INVALID_TJRA);	
		}
		
		if (!isFsCashPayCodeAvailable(do2)) {
			validity = false;
			buffer.append(PAYCODE_UNAVAILABLE);
		}
			
		
		if (isDuplicateDo2(do2)) {
			validity = false;
			buffer.append(DUPLICATE_DO2);
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		}else{
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2Repo.save(do2);
		do2.setReason(buffer.toString());
	 	return validity;
}
	
	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			 commitEncashDo2(do2);
			 return Do2Manager.SUCCESS;
		}
		return Do2Manager.FAILURE;
		
	}
	
	public String commitEncashDo2(Do2 do2) {
		int totalPay =0;
		AuditCage baseAuditCage = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = baseAuditCage.getBasicPay();
		int gradePay = baseAuditCage.getGradePay() ==null ? 0 :baseAuditCage.getGradePay();
		int da     = getFullMonthDa(do2,baseAuditCage);
		int xPay  = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
		int msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
		int classPay = baseAuditCage.getClassPay() == null ?0 : baseAuditCage.getClassPay();
		int dp       = baseAuditCage.getDp() == null ?0 : baseAuditCage.getDp();
		int oldGradePay = baseAuditCage.getOldGradePay() ==null ? 0 :baseAuditCage.getOldGradePay();
		int oldBasicPay = baseAuditCage.getOldBasicPay() ==null ? 0 :baseAuditCage.getOldBasicPay();
		
		/*if(!do2.getFromDate().after(DateManager.getSixthCpcDate())){
			totalPay = basicPay+dp+da+classPay;
		} else 	if(!do2.getFromDate().before(DateManager.getSixthCpcDate()) && !do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay;
		} else 	if(!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay+msPay;
		} */
		
		
		/*if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			totalPay = basicPay+dp+da+classPay;
		} else if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			totalPay = basicPay+gradePay+da+xPay;
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			totalPay = basicPay+gradePay+da+xPay;
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			totalPay = oldBasicPay+oldGradePay+da+1400;
		}
		*/
		
		
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			totalPay = basicPay+dp+da+classPay;
		} else if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			totalPay = basicPay+gradePay+da+xPay;
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			totalPay = basicPay+gradePay+msPay+da+xPay;			
		} else {
			totalPay = basicPay+da;
		}
		
		
		encashDue = (double)(Math.round((totalPay * do2.getTjra()) / 30));	
		encashDrawn=getEncashDrawn(do2);
		encashDiff = encashDue-encashDrawn;
		
		if (encashDiff != 0 && encashDiff != -1) {
			persistEncashDiff(do2, encashDiff,encashDue,encashDrawn);
			return Do2Manager.SUCCESS;
		} else {
			return "";
		}
	}
   
/*public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {				
		
		employee = Utils.getCurrentEmployee(dueAuditCage.getDo2().getArmyNo());

		if (employee.getRegularTaFlag() == Do2Manager.TERRITORIAL_ARMY
				&& employee.getTaAppointmentCategory() == Do2Manager.STOP_DA_TA_CATEGORY) {
			return 0;
		}
		if (employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(
						Do2Manager.RECRUIT)) {
			return 0;
		}
		if (employee.getDeputationFlag() != null
				&& employee.getDeputationFlag() == true
				&& employee.getRecordStatus().equals(
						Do2Manager.DEPU_IMTRAT_BHUTAN)) {
			return 0;
		}

		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;

		int variableComponents = 0;
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage
				.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage
				.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage
				.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage
				.getMsPay();
		
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					&& dp != 0) {
				daRate = allowance.getRateAmount();
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(
							DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else {
				variableComponents = gradePay + msPay + xGroupPay;
			}
			da += (dueAuditCage.getBasicPay() + variableComponents) * daRate;
		}
		return ((int) Math.round(da));
	}	*/

/*public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {
	
	employee = Utils.getCurrentEmployee(do2.getArmyNo());

	if (employee.getRegularTaFlag() == Do2Manager.TERRITORIAL_ARMY
			&& employee.getTaAppointmentCategory() == Do2Manager.STOP_DA_TA_CATEGORY) {
		return 0;
	}
	//*************************** to be reviewed
	if ((employee.getSubstantiveRank() != null
			&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
		return 0;
	}
	
	
	List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
			dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

	double da = 0.0;
	int variableComponents = 0;
	int basicPay = dueAuditCage.getBasicPay() == null ? 0 : dueAuditCage.getBasicPay();
	int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage.getXGroupPay();
	int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage.getGradePay();
	int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
	int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage.getClassPay();
	int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
	int oldGradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage.getOldGradePay();
	double daRate = 0.0;
	for (Allowance allowance : allowanceList) {
		daRate = allowance.getRatePercentage();
		if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0) {
			daRate = allowance.getRateAmount();
		} else if (!dueAuditCage.getFromDate().before (DateManager.getSeventhCpcDate()) && dueAuditCage.getGradePay()!=0) {
			basicPay = dueAuditCage.getOldBasicPay();
			msPay = Utils.get6cpcMsPay(dueAuditCage);
			if (dueAuditCage.getXGroupPay()!=0)
				xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);				
			variableComponents = oldGradePay + msPay + xGroupPay;
		}
		if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
			variableComponents = classPay;
		} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
				|| (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0)) {
			variableComponents = dp;
		} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			variableComponents = gradePay + xGroupPay;
		} else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
			variableComponents = gradePay + msPay + xGroupPay;
		}  else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			basicPay = dueAuditCage.getOldBasicPay();
		 	msPay = Utils.get6cpcMsPay(dueAuditCage);
			if (dueAuditCage.getXGroupPay()!=0)
				xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);
			daRate = 1.25;	
			variableComponents = oldGradePay + msPay + xGroupPay;
		} else {
			variableComponents =  msPay + xGroupPay;
		}
	System.out.println("nnnnnnnnnnnnnnnnnnnnnnnnn   variable components + date  " + variableComponents 
			+"  "+dueAuditCage.getFromDate()+"  "+basicPay +"  " +daRate);
		 da += (basicPay + variableComponents) * daRate;
 	}
	return ((int) Math.round(da));
}
*/
	public double getEncashDrawn(Do2 do2) {
		
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Arrears> arrList= arrRepo.findByEmployeeIdAndPayCodeId(emp.getId(),269);
				
	/*			Utils.getEntityManager().createQuery("SELECT a from Arrears a where a.employee=:emp AND a.payCode=269 ")
				.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.getResultList();*/
			if (arrList.size() > 0) {
				//System.out.println("ArrList.size : " +arrList.size());
				for (Arrears arr : arrList ) {
					encashDrawn += arr.getAmount();
				}
			} 
			return encashDrawn;
		}
		/*List<Do2> encashList = Utils.getEntityManager().createQuery("SELECT d from Do2 d where d.employee=:emp AND d.description=:desc " +
				"AND d.columnA=:colA AND d.status=:stat")
						.setParameter("desc", "ENCASH")
						.setParameter("colA", "DISCH")
						.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
						.setParameter("stat", Do2Manager.VALID)
						.getResultList();
		
		if (encashList.size() > 0) {
		List<Arrears> arrList= Utils.getEntityManager().createQuery("SELECT a from Arrears a where a.do2=:do order by a.do2 ")
				.setParameter("do", encashList.get(0)).getResultList();
			if (arrList.size() > 0) {
				//System.out.println("ArrList.size : " +arrList.size());
				for (Arrears arr : arrList ) {
					encashDrawn += arr.getAmount();
				}
			} 
		}
		return encashDrawn;
	}*/
	
	public void persistEncashDiff(Do2 do2, Double encashDiff, Double encashDue, Double encashDrawn) {
		String do2Description = null;
		if (do2.getDescription().equals("NEENC")) {
			do2Description = "FSCASH";
		}	
		Arrears arrears = new Arrears();
		arrears.setDo2(do2);
		arrears.setFromDate(do2.getFromDate());
		arrears.setToDate(do2.getToDate());
		arrears.setAmount(encashDiff);
		arrears.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		arrears.setPayCode(Utils.getPayCode(do2Description));
		arrears.setDue(encashDue);
		arrears.setDrawn(encashDrawn);
		arrears.setQe(Utils.getCurrentQuarter());
		arrears.setRoundOffAmount(false);
		arrears.setRegularPay(false);
		arrears.setPaid(false);
		arrears.setArrestPayment(false);
		arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		if (!do2.getDescription().equals(Do2Manager.CANCEL)) {
			arrears.setDo2ReverseArrears(do2);
		}
		arrRepo.save(arrears);
	//	Utils.getEntityManager().persist(arrears);
	}
	
	public boolean isValidA(Do2 do2){
		/*if (do2.getColumnA() != null && do2.getColumnA()=="DADIFF"){
			return true;
		}*/
		// commented above lines and modified as given below in 12/2018 to implement differnce of encash on accoutn of 7cpc pay.
		// below lines are to be commented in 01/2019 and above two lines are to be uncommented
		if (do2.getColumnA() != null && do2.getColumnA()=="7CPC"){
			return true;
		}
		
		return false;
	}
	
	public boolean isValidTjra(Do2 do2){
		if (do2.getTjra() != null && do2.getTjra() > 0 && do2.getTjra()<=300){
			return true;
		}
		return false;
	}
	
	public boolean isDuplicateDo2(Do2 do2) {
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> doList =do2Repo.findByColAFromDateEqualDo2IdNotEqual(emp.getId(), "NEENC", do2.getFromDate(), "V", do2.getColumnA(), do2.getId());
		

	/*			
				Utils.getEntityManager().createQuery("SELECT d from Do2 d WHERE d.employee=:emp AND d.description=:desc AND " +
				" d.fromDate=:frdt AND d.status=:stat  AND d.columnA=:colA AND d.id!=:did ")
				.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.setParameter("desc", "NEENC")
				.setParameter("frdt", do2.getFromDate())
				.setParameter("stat", "V")
				.setParameter("colA", do2.getColumnA())
				.setParameter("did", do2.getId())
				.getResultList();*/
		if (doList.size() > 0) {
			return true;
		}else {
			return false;
		}
	}
	
	public boolean isFsCashPayCodeAvailable(Do2 do2) {
		PayCode pc = pcRepo.findByPayCode("FSCASH");
				
		//		Utils.getEntityManager().createQuery("select pc from PayCode pc where pc.payCode='FSCASH'")
			//	.getResultList();
		if (pc!=null) {
			return true;
		} else {
			return false;
		}
	}
	
	

public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY)
				&& employee.getTaAppointmentCategory().equals(Do2Manager.STOP_DA_TA_CATEGORY)) {
			return 0;
		}
		//*************************** to be reviewed
		if ((employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
			return 0;
		}
		
		
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;
		int variableComponents = 0;
		int basicPay = dueAuditCage.getBasicPay() == null ? 0 : dueAuditCage.getBasicPay();
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
		int oldGradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage.getOldGradePay();
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0) {
				daRate = allowance.getRateAmount();
			} else if (!dueAuditCage.getFromDate().before (DateManager.getSeventhCpcDate()) && dueAuditCage.getGradePay()!=0) {
				basicPay = dueAuditCage.getOldBasicPay();
				msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);				
				variableComponents = oldGradePay + msPay + xGroupPay;
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
				variableComponents = gradePay + msPay + xGroupPay;
			}  else {
				
				// 	Below line is commented on 06-10-2021 as the mspay x group pay is not considered for encashment so DA on mspay and x group 
				//	pay also not taken in to account as the elements of mspay, xpay itself not considred for encashemnt 
				
				// variableComponents =  msPay + xGroupPay;
				
			}
			try {
				if (!do2.getFromDate().before(DateManager.getDate("2020-01-01")) &&
						do2.getFromDate().before(DateManager.getDate("2021-07-01"))) {
					daRate = do2.getRate();
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	/*	System.out.println("nnnnnnnnnnnnnnnnnnnnnnnnn   variable components + date  " + variableComponents 
				+"  "+dueAuditCage.getFromDate()+"  "+basicPay +"  " +daRate);*/
			 da += (basicPay + variableComponents) * daRate;
	 	}
		return ((int) Math.round(da));
	}
	
	
}
