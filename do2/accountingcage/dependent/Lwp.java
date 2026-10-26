package itsdc.dolphin.do2.accountingcage.dependent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * 
 * @author A. R. Reddy
 * 
 */
@RestController
public class Lwp extends AccountingCageBase implements java.io.Serializable {
	double basicPay = 0;
	double gradePay = 0;
	double da = 0;
	double dp = 0;
	double msPay = 0;
	double xGroupPay = 0;
	double classPay = 0;
	Employee employee;
	public StringBuffer buffer = new StringBuffer();

	static public String LRA_DAYS = "VL:Days in LRA Col. is not mentioned.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(LRA_DAYS);
		}

		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		}else{
			do2.setStatus(Do2Manager.VALID);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
		if (isValid(do2)) {
			 commitLwpDo2(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String commitLwpDo2(Do2 do2) {
		double recoveryAmount = 0;
		int lwpDays = 0;
		
		if (do2.getLra() != null) 			
			lwpDays += do2.getLra();
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> ac = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(employee.getId(), do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE "
					+ " ac.employee=:employee AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false AND " 
					+ " ac.recordStatus ='V' AND ac.fromDate<=:frDt ORDER BY ac.fromDate desc")
				.setParameter("employee", employee).setParameter("frDt", do2.getFromDate()).getResultList();*/
		if (ac.size() > 0) {
			basicPay = ac.get(0).getBasicPay() == null ? 0 : ac.get(0).getBasicPay();
			gradePay = ac.get(0).getGradePay() == null ? 0 : ac.get(0).getGradePay();
			da = ac.get(0).getDa() == null ? 0 : ac.get(0).getDa();
			dp = ac.get(0).getDp() == null ? 0 : ac.get(0).getDp();
			msPay = ac.get(0).getMsPay() == null ? 0 : ac.get(0).getMsPay();
			classPay = ac.get(0).getClassPay() == null ? 0 : ac.get(0).getClassPay();
			xGroupPay = ac.get(0).getXGroupPay() == null ? 0 : ac.get(0).getXGroupPay();
			
		}
		recoveryAmount = (double) Math.round((((basicPay + gradePay + da + dp + msPay + classPay + xGroupPay) * 
				lwpDays) / -30));
		Utils.addArrears(do2, recoveryAmount);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidLra(Do2 do2) {
		boolean validity = false;
		if (do2.getLra() != null){
			return true;
		}else
			return false;
	}

}