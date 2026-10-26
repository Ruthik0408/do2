package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EnrollDo2;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Fix7pc extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;
	List<GroupCategory> gcList = null;

	public static final Double seven_pc_pay_index = 2.57;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
			
		do2.setStatus(Do2Manager.VALID);
	
		if (do2.getColumnA()!=null) {
 			if (!isValidColumnA(do2)) {
 				do2.setStatus(Do2Manager.INVALID);
 				do2.setReason("Invalid value given in column_a / Amount should be rounded off to nearest ten \n");
 				validity=false;
 			}
 		}
		
		if (isDuplicate(do2)) {
			do2.setStatus(Do2Manager.INVALID);
				do2.setReason("Duplicate Do2");
				validity=false;
		}

		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		List<PayMatrix> paymatrixList=null;
		List<AuditCage> auditCageList=null;
		int atcgGradePay=0;
		int sevenCpcBasicPay=0;
		int payMatrixBasicPay=0;
		int atcgBasicPay=0;		 
		String payLevel = null;
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		Rank clspayRank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"ALLCE");

		if (newAuditCage.getFromDate().equals(employee.getEnrolmentDate())){
			if (employee.getEnrolmentDate().equals(employee.getNbsubDate()) ||  employee.getEnrolmentDate().equals(employee.getSubDate())
					 || (employee.getEnrolmentDate().equals(employee.getSubmajDate()))) {
				 rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"ALLCE");
			}
		}
		
		if (Utils.getPao().getPaoCode().equals("94")) {
			if (newAuditCage.getFromDate().equals(employee.getEnrolmentDate())){
				if (employee.getEnrolmentDate().equals(employee.getNbsubDate()) ||  employee.getEnrolmentDate().equals(employee.getSubDate())
						 || (employee.getEnrolmentDate().equals(employee.getSubmajDate()))) {
					 rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"ALLCE");
				}
				if  (rank.getRankCode().equals("10")) {
					rank=rankRepo.findByRankCode("20");
				//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
				}
			}
		}		
		
		if ((employee.getSubstantiveRank()!=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))
				|| (baseAuditCage.getBasicPay()==11400 && baseAuditCage.getGradePay()==0)){
			newAuditCage.setOldBasicPay(11400);
			newAuditCage.setBasicPay(14600);
			newAuditCage.setDp(0);
			newAuditCage.setDa(0);
			newAuditCage.setMsPay(0);
			newAuditCage.setXGroupPay(0);
			newAuditCage.setTransport(0);
			newAuditCage.setPmha(0);
			newAuditCage.setLra(0);
			newAuditCage.setClassPay(0);
			newAuditCage.setOldClassPay(0);
			newAuditCage.setOldGradePay(0);
			newAuditCage.setPayLevel("0");
			return newAuditCage;
		}	 
		
		
		if (rank.getRankCode().equals(Do2Manager.RECRUIT) && employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate()))  {
			if (employee.getInitialMusteringDate()!=null && 
					!employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())) {
				// rank = (Rank) Utils.getForeignKey(new Rank(), "rankName",	"SEP");
				rank=rankRepo.findByRankCode("20");
			//	 rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
			}
			if (employee.getAttestationDate()!=null && 
					!employee.getAttestationDate().before(DateManager.getSeventhCpcDate())) {
				// rank = (Rank) Utils.getForeignKey(new Rank(), "rankName",	"SEP");
				rank=rankRepo.findByRankCode("20");
			//	 rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
			}
		}
		
		
		List<Do2> do2List = null;
		
		do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(newAuditCage.getDo2().getEmployee().getId(),
															"MACPS", newAuditCage.getDo2().getFromDate(), "HONYGRADE", "V");
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and " +
 				" d.status='V' and d.description='MACPS' and d.columnA='HONYGRADE' and d.fromDate <=:fromDate ")
 				.setParameter("emp",newAuditCage.getDo2().getEmployee())
 				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
 				.getResultList();*/
		if (do2List.size() > 0) {
			rank=rankRepo.findByRankCode("79");
		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"79");
		} 
		
		do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(newAuditCage.getDo2().getEmployee().getId(),
																				"HCOMMN", newAuditCage.getDo2().getFromDate(), "HONYLT", "V");
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and " +
 				" d.status='V' and d.description='HCOMMN' and d.columnA='HONYLT' and d.fromDate <=:fromDate ")
 				.setParameter("emp",newAuditCage.getDo2().getEmployee())
 				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
 				.getResultList();*/
		if (do2List.size() > 0) {
			rank=rankRepo.findByRankCode("80");
		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"80");
		}
		
		do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(newAuditCage.getDo2().getEmployee().getId(),
																			"HCOMMN", newAuditCage.getDo2().getFromDate(), "HONYCAPT", "V");
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and " +
 				" d.status='V' and d.description='HCOMMN' and d.columnA='HONYCAPT' and d.fromDate <=:fromDate ")
 				.setParameter("emp",newAuditCage.getDo2().getEmployee())
 				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
 				.getResultList();*/
		if (do2List.size() > 0) {
			rank=rankRepo.findByRankCode("90");
		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"90");
		}
		
		
		
		
		if (newAuditCage.getFromDate().equals(employee.getEnrolmentDate())) {
			Employee emp=Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
			auditCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(emp.getId(), newAuditCage.getDo2().getFromDate());
					
		/*			Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:employee "
									+ " AND ac.cancelled=false AND ac.recordStatus!='P' "
									+ " AND ac.fromDate<=:fromDate AND ac.effective=true ORDER BY fromDate DESC ")
									.setParameter("employee",Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()))
									.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
									.getResultList();*/
		} else {
	 		auditCageList = acRepo.findByFromDateLessThan(employee.getId(), newAuditCage.getDo2().getFromDate());
					
		/*			Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:employee "
					+ " AND ac.cancelled=false AND ac.recordStatus!='P' AND ac.fromDate<:fromDate AND ac.effective=true ")
					.setParameter("employee",Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()))
					.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
					.getResultList();*/
			if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcDate()) 
					&& newAuditCage.getEmployee().getRegularTaFlag().equals("T")) {
				List<Do2> taIncrDo2List = do2Repo.findByDescriptionStatusFromDateEqualToAndDo2IdNotEqualTo(employee.getId(), "TAINCR", Do2Manager.VALID, 
														newAuditCage.getFromDate(), newAuditCage.getDo2().getId());
				if (taIncrDo2List!=null && taIncrDo2List.size()>0) {
					auditCageList=null;
					auditCageList = acRepo.getAuditcageForTAincrOn01012016(employee.getId());
				}
			}
		}
		
		boolean fkDo2NullAvailable = false;
		 
		for (AuditCage ac : auditCageList) {		
		   if (ac.getDo2() == null){
			   fkDo2NullAvailable = true;
			   break;
		   }		   
		}
	    if (auditCageList.size() > 0) {
	    	if (!fkDo2NullAvailable){
	    		sortAuditCages(auditCageList);
	    	}
	    	AuditCage ac =  auditCageList.get(auditCageList.size() -1);
	    	if (ac != null){
				atcgGradePay = ac.getGradePay();
				atcgBasicPay = ac.getBasicPay();	
			}
	    }
			    
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
		if (groupCategory != null)
			soldierGroup = groupCategory.getSoldierGroup(); 
				
		/*if (auditCageList.size() > 0) {
			atcgGradePay = auditCageList.get(0).getGradePay();
			atcgBasicPay = auditCageList.get(0).getBasicPay();			 
		}*/
		
		
		
		if (atcgGradePay!=0 &&  !rank.getRankCode().equals("79")) {
			payLevel = Do2Manager.getPayLevel(atcgGradePay);
		} else {
			payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
			atcgGradePay = Do2Manager.getSevenCpcGradepay(payLevel);
		}
						
		sevenCpcBasicPay = (int)Math.round((atcgBasicPay+atcgGradePay)*seven_pc_pay_index);
		
		paymatrixList = matrixRepo.findByPayLevelAndGradePayAndBasicPayGreaterThanEqualOrderByBasicPayAsc(payLevel, atcgGradePay,sevenCpcBasicPay);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ " pm.gradePay=:gradepay and pm.basicPay>=:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", payLevel)
				.setParameter("gradepay", atcgGradePay)
				.setParameter("sevencpcpay", sevenCpcBasicPay)
				.getResultList();*/
		
		if (paymatrixList.size() > 0) {
			payMatrixBasicPay = paymatrixList.get(0).getBasicPay();
		}	
	
		newAuditCage.setOldBasicPay(baseAuditCage.getBasicPay());
		if (baseAuditCage.getGradePay()==0) {
			newAuditCage.setOldGradePay(atcgGradePay);
		} else {
			newAuditCage.setOldGradePay(baseAuditCage.getGradePay());	
		}
		
		newAuditCage.setDp(0);
		
		if (payMatrixBasicPay>0) {
			newAuditCage.setBasicPay(payMatrixBasicPay);
			newAuditCage.setPayLevel(payLevel);
		}
		
		if (baseAuditCage.getBasicPay() == 11400 && baseAuditCage.getGradePay() == 0){
			newAuditCage.setBasicPay(14600); // stipend revision
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)  
				&& baseAuditCage.getEmployee().getEnrolmentDate()!=null 
				&& !baseAuditCage.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())
				&& baseAuditCage.getEmployee().getEnrolmentDate().equals(newAuditCage.getDo2().getFromDate())) {
			
			/*if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==2000) {
				newAuditCage.setBasicPay(21700);
			}
			if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==2400) {
				newAuditCage.setBasicPay(25500);
			}
			if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==2800) {
				newAuditCage.setBasicPay(29200);
			}			
			if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==4200) {
				newAuditCage.setBasicPay(35400);
			}
			if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==4600) {
				newAuditCage.setBasicPay(44900);
			}
			if (baseAuditCage.getOldGradePay()!=null && baseAuditCage.getOldGradePay()==4800) {
				newAuditCage.setBasicPay(47600);
			}			*/ 
			if (rank!=null) {
				payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
				atcgGradePay = Do2Manager.getSevenCpcGradepay(payLevel);
				
				sevenCpcBasicPay = (int)Math.round((atcgBasicPay+atcgGradePay)*seven_pc_pay_index);
				if (payLevel!=null && atcgGradePay>0) {
					paymatrixList = matrixRepo.findByPayLevelAndGradePayOrderByMatrixLevelAsc(payLevel, atcgGradePay);
							
				/*			Utils.getEntityManager().createQuery( 
						 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ " pm.gradePay=:gradepay order by pm.matrixLevel asc ")
						.setParameter("paylevel", payLevel)
						.setParameter("gradepay", atcgGradePay) 
						.getResultList();*/
				}
				
				if (paymatrixList.size() > 0) {
					payMatrixBasicPay = paymatrixList.get(0).getBasicPay();
				}
				
				if (payMatrixBasicPay>0) {
					newAuditCage.setBasicPay(payMatrixBasicPay);
					newAuditCage.setOldBasicPay(baseAuditCage.getBasicPay());
					newAuditCage.setOldGradePay(atcgGradePay);
					newAuditCage.setDp(0);
					newAuditCage.setMsPay(5200);
					newAuditCage.setXGroupPay(0);
					newAuditCage.setPayLevel(payLevel);
				}	
			}  		
		}
		newAuditCage.setGradePay(0);
		if(Integer.parseInt(clspayRank.getRankCode()) > 49 ) {
			newAuditCage.setClassPay(0);
		}
		
		if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==2000) {
			newAuditCage.setMsPay(5200);
		} else {
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==6000) {
				newAuditCage.setMsPay(15500);
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && baseAuditCage.getOldGradePay()<5400
						&& baseAuditCage.getEmployee().getEnrolmentDate()!=null 
						&& !baseAuditCage.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
					newAuditCage.setMsPay(5200);
				}
			}			
		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			newAuditCage.setMsPay(0);
		}
		
		
	/*	if (Integer.parseInt(rank.getRankCode()) > 79 ) {
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==6000) {
				newAuditCage.setMsPay(15500);
			}			
		} else if (Integer.parseInt(clspayRank.getRankCode()) > 10  && (Integer.parseInt(clspayRank.getRankCode()) <= 79)) {
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==2000) {
				newAuditCage.setMsPay(5200);
			}
		}*/
		
		if (soldierGroup.equals("X") && baseAuditCage.getXGroupPay()!=null && baseAuditCage.getXGroupPay()==1400) {
			newAuditCage.setXGroupPay(3600);
		} else if (baseAuditCage.getXGroupPay()!=null && baseAuditCage.getXGroupPay()==1400) {
			newAuditCage.setXGroupPay(3600);
		}
	
		baseAuditCage.setOldBasicPay(baseAuditCage.getBasicPay());
		baseAuditCage.setOldGradePay(baseAuditCage.getGradePay());		
 		
		acRepo.save(baseAuditCage);
	//	Utils.getEntityManager().merge(baseAuditCage);
		return newAuditCage;
	}
                                                                                                   
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColumnA(Do2 do2) {
		
		try {
			if ((Integer.parseInt(do2.getColumnA()))%10==0)
				return true;
		} catch (Exception e) {
			Utils.handleException(e);
			return false;
		}
		return false;
	}
	
	
	
	public boolean isDuplicate(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByValidAndPendingAndDescription(emp.getId(), "FIX7PC");
				
		//		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='FIX7PC' and d.employee=:emp"
	//			+ " and status in ('V', 'P') ")
		//		.setParameter("emp", emp).getResultList();
		return (do2List.size()>1);
	}
	

	@SuppressWarnings("unchecked")
	public void sortAuditCages(List<AuditCage> auditCageList) {
	  
	   
	   for (AuditCage auditCage : auditCageList) {
		   String columnA = null;
       if (auditCage.getDo2().getColumnA() != null) {
			  columnA = auditCage.getDo2().getColumnA().trim();
		  }	  
		  if (Do2Manager.priorityDescList.contains(auditCage.getDo2().getDescription().trim()) ){
		      auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
		  } else if (columnA != null){
			 auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()+ " " + columnA));
		  } else {
			  auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
		  }
	   }
	   Collections.sort(auditCageList);
	}
	
}