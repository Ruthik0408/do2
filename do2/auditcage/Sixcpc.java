package itsdc.dolphin.do2.auditcage;
 
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
 
import itsdc.dolphin.do2.auditcage.*;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.Rank;

import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Arrays;

/**
 * 
 * @author K.RAMADEVI &  N. RAMACHANDRAN  Dt. 06.10.2009.
 * 
 */

@RestController
public class Sixcpc extends AuditCageBase implements 	Do2AuditCageProcessor, java.io.Serializable {
				
	 
	EmployeeServiceDetail esd;
	private List <GroupCategory> gcList; 
	private StringBuffer buffer = new StringBuffer();
	private String message;
	boolean validity = true;
	private List <Do2> do2List;
	private List<EmployeeServiceDetail> esdList;
	Employee employee;
	int nqsPeriod;
	Rank rank;
	
	static public final String ENROLLED_AFTER_6CPC_DATE= "AU:Option not available for those enrolled on or after 01.01.2006 \n";
	static public final String INVALID_OPTION_DATE = "VL:Option can be Given From 01-01-2006 to 11-10-2008 only \n";
	static public final String DESC_FROM_DATE_NOT_MATCH = "Promot/ACP/Incr does not exist on option date.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (!isEnrolledBefore6CPC(employee)) {
			buffer.append(ENROLLED_AFTER_6CPC_DATE);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		} 
			
		if (!isValidFromDate(do2)) {
			buffer.append(INVALID_OPTION_DATE);
			do2.setStatus(Do2Manager.PENDING);
			validity = false;
		}
		
		
		if (!isFromDateMatchingWithDescInColumn7A(do2)) {
			buffer.append(DESC_FROM_DATE_NOT_MATCH);
			do2.setStatus(Do2Manager.INVALID);
			validity=false;
		}
		
		if (validity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);			
		} else {
			message = buffer.toString();
			do2.setReason(message);
		}
	
		do2Repo.save(do2);
		
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		String soldierGroup="";
		GroupCategory gp = Utils.getGroupCategory(newAuditCage.getDo2(), "CUR");
		soldierGroup=gp.getSoldierGroup();

		System.out.println ( " SOLDIER GROUP " +soldierGroup);
		System.out.println ( " SOLDIER RANK " +Utils.getPborRank(employee.getArmyNo(), newAuditCage.getFromDate(),"ALLCE").getRankName());
		System.out.println ( " BASIC PAY " +newAuditCage.getBasicPay());
		System.out.println ( " from date " +newAuditCage.getDo2().getFromDate());
				
		List<ReadyReckoner> reckonerList = rrRepo.findByRankGroupAndBasic(Utils.getPborRank(employee.getArmyNo(), newAuditCage.getFromDate(), "ALLCE").getId(), 
																						soldierGroup, newAuditCage.getBasicPay());
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (soldierGroup.equals("Z") && newAuditCage.getDo2().getColumnA().equals("PROMOT")) {
				do2List=null;
				do2List=do2Repo.findByDescFromDateEqual(employee.getId(), "V", "PROMOT",newAuditCage.getFromDate());
				 
				if (do2List!=null && do2List.size()>0) {
		//			if (do2List.get(0).getColumnA()!=null && do2List.get(0).getColumnA().equals("NBSUB") 
		//					&& do2List.get(0).getColumnC()!=null && do2List.get(0).getColumnC().equals("DSCGD")) {
						soldierGroup="Y";		
		//			}
				}
			}
		}
		
		if (soldierGroup.equals("Z"))
			soldierGroup="Y";

		List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(Utils.getPborRank(employee.getArmyNo(), newAuditCage.getFromDate(),"ALLCE").getId(),
				soldierGroup, newAuditCage.getDo2().getFromDate());
				
		 
		if (reckonerList.size() > 0 && payList.size() > 0)	{	
			newAuditCage.setOldBasicPay(newAuditCage.getBasicPay());  	//  Added on 22.11.2019 wrt KRC Bhutan FSA case
			newAuditCage.setOldClassPay(newAuditCage.getClassPay());   //  Added on 22.11.2019 wrt KRC Bhutan FSA case
			newAuditCage.setBasicPay(reckonerList.get(0).getNewBasic());
			newAuditCage.setGradePay(payList.get(0).getGradePay());
			newAuditCage.setDp(0);
			if (soldierGroup.equals("X")) {
				newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
			}
		}
		
		return newAuditCage;
	}
	
	 
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		
		return Do2Manager.SUCCESS;
	}


	public boolean isEnrolledBefore6CPC(Employee employee) {
		return (employee.getEnrolmentDate().before(DateManager.getSixthCpcDate())); 
	}
		
	public boolean isValidFromDate(Do2 do2) {
		
		/*if (do2.getFromDate().after(DateManager.getSixthCpcDate()) 
				 && do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
		
		 if (do2.getFromDate().after(DateManager.getSixthCpcDate()) 
				 && !do2.getFromDate().after(DateManager.getSixCpcNotificationDate())){
			 return true;
		 }
Commented on 11.01.2022			*/
		
		 if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) 
				 && !do2.getFromDate().after(DateManager.getSixCpcNotificationDate())){
			 return true;
		 }
		 return false;
	}


	 public boolean isFromDateMatchingWithDescInColumn7A(Do2 do2) {
		 
		 if (do2.getColumnA()==null) {
			 return false;
		 }
		 
		 if (do2.getColumnA().equals("MACPS")) {
			 return false;
		 }
		 
		 if (do2.getColumnA().equals("PROMOT")) {
			 	
			 List<String> descList=Arrays.asList(new String[] {"MSTJCO","MSTHAV","PROMOT"});
			 
			 do2List = do2Repo.findByDescListFromDateEqual(employee.getId(), Do2Manager.VALID, descList, do2.getFromDate());
					 
				/*	 Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
			 						"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
		 							"do2.description in ('MSTJCO', 'MSTHAV','PROMOT') AND do2.status = :vld")
		 									.setParameter("employee", employee)
		 									.setParameter("fromDate", do2.getFromDate())
		 									.setParameter("vld", Do2Manager.VALID)
		 									.getResultList();*/
			 	return((do2List.size()==1));
		 } else if (do2.getColumnA().equals("ACP")) {
			 	do2List = do2Repo.findByDescFromDateEqualStatusEqual(employee.getId(),Do2Manager.VALID,do2.getColumnA(),do2.getFromDate());
			 			
			 /*			Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
			 						"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
		 							"do2.description =:descr AND do2.status = :vld")
		 									.setParameter("employee", employee)
		 									.setParameter("fromDate", do2.getFromDate())
		 									.setParameter("descr", do2.getColumnA())
		 									.setParameter("vld", Do2Manager.VALID)
		 									.getResultList();*/
			 	return((do2List.size()==1));
		 } else if (do2.getColumnA().equals("INCR")) {
			 try {	
				 do2List =  do2Repo.findByFromDateBetweenAndDescAndStatus(employee.getId(), DateManager.getDate("2005-01-01"), 
						 											DateManager.getDate("2005-12-31"), "INCR", Do2Manager.VALID);
						 
			/*			 Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
				 		"do2.employee =:employee AND (do2.fromDate BETWEEN :minDate AND :maxDate) " +
				 		"AND do2.description =:descr AND do2.status = :vld")
				 		.setParameter("employee",employee)
						.setParameter("minDate", DateManager.getDate("2005-01-01"))
						.setParameter("maxDate", DateManager.getDate("2005-12-31"))
						.setParameter("descr", "INCR")
						.setParameter("vld", Do2Manager.VALID)
						.getResultList();*/
			 } catch (Exception e) {
			 		Utils.handleException(e);
			 }
             if (do2List.size() > 0) {
				 Do2 incrDo2 =	new Do2();
				 incrDo2 = (Do2)Utils.getClonedEntity(do2List.get(0));
	             while ( !incrDo2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
					 Calendar cal = new GregorianCalendar();
					 cal.setTimeInMillis(incrDo2.getFromDate().getTime());
					 int year = cal.get(Calendar.YEAR)+1;
					 int month = cal.get(Calendar.MONTH)+1;
					 int day = cal.get(Calendar.DATE);
			 		 
					 try {		 
						 incrDo2.setFromDate(DateManager.getDate(year, month, day));  
				 	 } catch (Exception e) {
				 		Utils.handleException(e);
				 	 }
					 	
					 return (do2.getFromDate().equals(incrDo2.getFromDate()));	
	             }
	             return false;
             } else {
            	 return false;
             }
		 } else {
				return false;
		 }		 	
	 }
	
	 
}
