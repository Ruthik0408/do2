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
 * @author ITSDC, Secunderabad
 * 
 */
@RestController
public class Forpay extends AccountingCageBase implements java.io.Serializable {
	double basicPay = 0;
	double gradePay = 0;
	double da = 0;
	double dp = 0;
	double msPay = 0;
	double xGroupPay = 0;
	double classPay = 0;
	Employee employee;
	public StringBuffer buffer = new StringBuffer();

	static public String PUNISHMENT_INVALID = "VL:OC or CM should be mentioned.\n";
	static public String LRA_DAYS = "VL:Days in LRA Col. is not matching.\n";
	static public String ZERO_AUDIT_CAGE = "AU:PBOR is not getting Pay and Allowances.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!isValidPunishment(do2)) {
			validity = false;
			buffer.append(PUNISHMENT_INVALID);
		}

		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(LRA_DAYS);
		}

		if (!zeroAuditCage(do2)) {
			validity = false;
			buffer.append(ZERO_AUDIT_CAGE);
		}

		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe() == null){
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
		if (isValid(do2)) {
			 payrecovery(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String payrecovery(Do2 do2) {
		double recoveryamt = 0;
		int forpaydays = 0;
		if (do2.getTjra() == null) 
			forpaydays = 0;
		else
			forpaydays = do2.getTjra() * 30;
		if (do2.getLra() != null) 			
			forpaydays += do2.getLra();
		
		recoveryamt = (double) Math.round((basicPay + gradePay + da + dp + msPay + classPay + xGroupPay) * 
				DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) * -1);
		Utils.addArrears(do2, recoveryamt);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidPunishment(Do2 do2) {
		boolean validity = false;
		if (do2.getPunishment().equals("OC") || do2.getPunishment().equals("CM")) {
			return true;
		}
		return false;
	}

	public boolean isValidLra(Do2 do2) {
		int noOfDays = (int) DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		int lraDays = 0;
		int tjraDays = 0;
		int forpayDays = 0;
		if (do2.getLra() == null)
			lraDays = 0;
		else
			lraDays = do2.getLra();
		if (do2.getTjra() == null)
			tjraDays = 0;
		else
			tjraDays = do2.getTjra();
		if (lraDays == 0 && tjraDays == 0)
			return false;
		forpayDays = (tjraDays * 30) + lraDays;	
		if (forpayDays == noOfDays)
			return true;
		else
			return false;
	}

	public boolean zeroAuditCage(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> ac = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(employee.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE "
					+ " ac.employee=:employee AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false AND " 
					+ " ac.recordStatus!='P' AND ac.fromDate<=:frDt ORDER BY ac.fromDate desc")
				.setParameter("employee", employee).setParameter("frDt", do2.getFromDate()).getResultList();*/
		if (ac.size() > 0) {
			basicPay = ac.get(0).getBasicPay() == null ? 0 : ac.get(0).getBasicPay();
			gradePay = ac.get(0).getGradePay() == null ? 0 : ac.get(0).getGradePay();
			da = ac.get(0).getDa() == null ? 0 : ac.get(0).getDa();
			dp = ac.get(0).getDp() == null ? 0 : ac.get(0).getDp();
			msPay = ac.get(0).getMsPay() == null ? 0 : ac.get(0).getMsPay();
			classPay = ac.get(0).getClassPay() == null ? 0 : ac.get(0).getClassPay();
			xGroupPay = ac.get(0).getXGroupPay() == null ? 0 : ac.get(0).getXGroupPay();
			return true;
		}

		return false;
	}

	public boolean isValidRefDo2(Do2 do2) {
		boolean validity = false;
		GregorianCalendar cal2 = new GregorianCalendar();
		List<Do2> refdo2 = do2Repo.findByDo2NoDo2TyeDo2ItemNo(do2.getRefDo2No().substring(2, 6), 
													do2.getRefDo2No().substring(0, 1), do2.getRefDo2No().substring(7, 10));
				
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"select refdo from Do2 refdo where "
								+ "refdo.do2No=:refdo2no and refdo.do2Type=:refdo2type and refdo.do2ItemNo=:refdo2itemno ")
				.setParameter("refdo2no", do2.getRefDo2No().substring(2, 6))
				.setParameter("refdo2type", do2.getRefDo2No().substring(0, 1))
				.setParameter("refdo2itemno",do2.getRefDo2No().substring(7, 10))
				.getResultList();*/
		if (refdo2.size() > 0) {
			for (Do2 refdo : refdo2) {
				cal2.setTime(refdo.getDo2Date());
				int myYear = cal2.get(Calendar.YEAR);
				if (myYear == Integer.parseInt(do2.getRefDo2No().substring(11, 15))) {
					return true;
				}
			}
		} else {
			return true;
		}
		return false;
	}

}