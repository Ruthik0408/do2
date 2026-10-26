package itsdc.dolphin.do2.auditcage;
 
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * 
 * @author K.RAMADEVI &  N. RAMACHANDRAN  Dt. 19.01.2010.
 * Is drawing in correct stage as per 5cpc to be checked.  DNI action to be seen. ColumnA to be made special in appendixj for 6cpc.
 */

@RestController
public class Stepup extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
				
	 
	EmployeeServiceDetail esd;
	private StringBuffer buffer = new StringBuffer();
	private String message;
	private Calendar fromDateCal = new GregorianCalendar();
	boolean validity = true, rateValidity = false, alreadyDrawingEqualOrMorePay = false;
	private List<AuditCage> acList;
	private List<PayScale> payList;
	private List<Do2> do2List;
	Date sevencpcDate = DateManager.getSeventhCpcDate();
	
	Employee employee;
	int nqsPeriod;
	Rank rank;
	String soldierGroup;
	
	//static public final String INVALID_FROM_DATE = "VL:Date should be the first of the month.\n";
	static public final String INVALID_RATE= "AU:Pay to be stepped is not in 5 CPC Pay scale / invalid stage as per 6CPC / PBOR already drawing more pay than proposed STEPUP pay. \n";
	static public final String INCORRECT_PAY_STAGE = "AU:Pay stage wrong / Pay mismatch. \n";
	static public final String INVALID_RATE_7CPC = "AU:Invalid Basic Pay (7CPC) Provided In Rate Column\n";
	static public final String RATE_NULL_ZERO = "VL:Rate Column Is Null Or Zero \n";
	static public final String STEPUP_BEFORE_MIGRATION_7CPC = "AU:Stepup Date Before Migrated Date In 7cpc\n";
	static public final String ALREADY_EQUAL_MORE_PAY_7CPC = "AU: Individual Already Drawing Equal Or More Pay.  Please Review.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		validity = true; alreadyDrawingEqualOrMorePay=false;
		if (employee == null) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("AU: Employee Record Not Found \n");
			do2Repo.save(do2);
			return false;
		}
		
		if (rank == null) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("AU: Incorrect Rank On Stepup Date \n");
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getRate()==null || (do2.getRate()!=null && do2.getRate()==0)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(RATE_NULL_ZERO);
			do2Repo.save(do2);
			return false;
		}
/*		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("VL: Stepup In 7th CPC. \n");
			if (do2.getId()==0) {
			 	Utils.getEntityManager().persist(do2);
			} else {
			 	Utils.getEntityManager().merge(do2);
			}
			return false;
		}
		
*/		if (!do2.getFromDate().before(sevencpcDate)) {
			List<Do2> do2List = do2Repo.findByDescColB(employee.getId(),"V","OPTCPC","SEVENPC");
					
		//			Utils.getEntityManager().createQuery("select d from Do2 d where d.status='V' and d.employee=:emp and "
			//		+ " d.description='OPTCPC' and d.columnB='SEVENPC' ").setParameter("emp", employee).getResultList();
			
			if (do2List!=null && do2List.size()>0) {
				sevencpcDate = do2List.get(0).getFromDate();
			}
			
			if (do2.getFromDate().before(sevencpcDate)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(STEPUP_BEFORE_MIGRATION_7CPC);
				do2Repo.save(do2);
			}
								
			if (do2.getId()==newDo2.getId()) {
				if (!validBasicPayInRateColumnIn7cpc(do2, employee)) {
					validity = false;
					if (alreadyDrawingEqualOrMorePay) {
						buffer.append(ALREADY_EQUAL_MORE_PAY_7CPC);					
					} else {
						buffer.append(INVALID_RATE_7CPC);
					}			
				}			
			}
			
		} else if (do2.getFromDate().before(sevencpcDate)) {
			if (!isValidRate(do2)) {
				buffer.append(INVALID_RATE);
				validity = false;
			}
		}
		
		if (do2.getId()==newDo2.getId()) {
			if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				if (do2.getColumnA()!= null){
					buffer.append("VL: Column-7A should be left blank. \n");
					validity = false;
				}
			} else //if (do2.getFromDate().before(sevencpcDate)) {
				if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().trim().length()==0)) {
					buffer.append("VL: Stepped up new pay to be given in Column-7A. \n");
					validity = false;
				}
			}
	//	}
		System.out.println ( "do2 from date xxxxxxxxxxxxxx " +do2.getFromDate()    + " SEVEN CPC DATE" +sevencpcDate );			
		if (do2.getFromDate().before(sevencpcDate)) {
			System.out.println ( "do2 from date " +do2.getFromDate()    + " SEVEN CPC DATE" +sevencpcDate );
			if (!isValidPayStage(do2)) {
				buffer.append(INCORRECT_PAY_STAGE);
				validity = false;
			}
		}
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			List<Do2> bunchDo2List=null;
			try {
				bunchDo2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V", "BUNCH", DateManager.getDate("2016-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			//		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and d.description='BUNCH' and d.fromDate='2016-01-01' ")
				//					 	.setParameter("emp", employee).getResultList();
			if (bunchDo2List!=null && bunchDo2List.size()>0) {
				validity = false;
				buffer.append("AU: BUNCHING Adjusted For The Individual. \n");				
			}		
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		
		message = buffer.toString();
		do2.setReason(message);
			
		do2Repo.save(do2);
		return validity;		
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		double basicPay = 0;
		
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())) {
			basicPay = newAuditCage.getDo2().getRate();
		} else if (newAuditCage.getFromDate().before(sevencpcDate)) {
			if (newAuditCage.getDo2().getColumnA()==null || (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().length()==0)) {
				basicPay = newAuditCage.getDo2().getRate();
			} else {
				basicPay = Double.parseDouble(newAuditCage.getDo2().getColumnA());	
			}			
		} else {
			basicPay = newAuditCage.getDo2().getRate();
		}
		
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay((int)basicPay);
		} else if (newAuditCage.getFromDate().before(sevencpcDate)) {
			newAuditCage.setBasicPay((int)basicPay);
			if (newAuditCage.getDo2().getColumnA()==null || (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().length()==0)) {
				;
			} else {
				newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			}				
		} else {
			newAuditCage.setBasicPay(newAuditCage.getDo2().getRate().intValue());		 
		}		
	 	return newAuditCage;
	}
	
 
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {		
		return Do2Manager.SUCCESS;
	}

	public boolean isValidFromDate(Do2 do2) {
		fromDateCal.setTimeInMillis(do2.getFromDate().getTime());
		return (((fromDateCal.get(Calendar.DATE)) == 1)); 
	}

	public boolean isValidRate(Do2 do2) {
		int basicPay = 0;		
		Date psFromDate = null;	
		
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			psFromDate = do2.getFromDate();
		} else {
			try {
				psFromDate = DateManager.getDate("1997-10-10");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
	 	soldierGroup = Utils.getGroupCategory(do2,"CUR").getSoldierGroup();
			
		acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(employee.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac " +
				"WHERE ac.employee=:emp AND ac.fromDate<=:do2FromDate AND ac.effective=true AND ac.recordStatus!='P' " +
				"AND ac.cancelled=false and ac.absentFlag=false ORDER BY ac.fromDate DESC, ac.toDate DESC")
				.setParameter("emp", employee)
				.setParameter("do2FromDate",do2.getFromDate())
				.getResultList();*/

		payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),soldierGroup,psFromDate);
				
		/*		Utils.getEntityManager().createQuery("SELECT pl FROM PayScale AS pl WHERE "
											+ " pl.rank=:rank AND pl.soldierGroup.soldierGroup=:soldierGroup AND "
											+ " ((:fromDate BETWEEN pl.fromDate AND pl.toDate ) OR "
											+ "  (:fromDate >= pl.fromDate AND pl.toDate IS null )) AND "
											+ " pl.startScale IS NOT null")
											.setParameter("rank",rank)
											.setParameter("soldierGroup",soldierGroup)
											.setParameter("fromDate", psFromDate)
											.getResultList();*/
		
		if (acList.size()>0){
			if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				basicPay = acList.get(0).getBasicPay();
			} else {
				basicPay = acList.get(0).getOldBasicPay();
			}
			
			/*if (!do2.getFromDate().after(DateManager.getSixthCpcDate())) {
				basicPay = acList.get(0).getBasicPay();
			} else {
				basicPay = acList.get(0).getOldBasicPay();
			}*/
			while (basicPay <= payList.get(0).getScaleEnd0()) {
//				if ((basicPay+payList.get(0).getIncr0())==do2.getRate()) {
				System.out.println ( " AUDIT CAGE BASIC PAY " +basicPay  +" do2 rate " +do2.getRate());
				if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
					if (basicPay == do2.getRate())
		                return true;	
				} else {
					if (do2.getColumnA()!=null && do2.getColumnA().length()>0) {
						if (basicPay == do2.getRate())
			                return true;
					}
					
					if (do2.getRate()>payList.get(0).getScaleEnd0()) {
						return true;
					}
				}				
				basicPay = basicPay + payList.get(0).getIncr0();
			}
		}			
		
		return false;
	}
	
	public boolean isValidPayStage(Do2 do2) {
		
	 	if (acList == null || acList.size() == 0)
			return false;
	 	
	 	if (do2.getFromDate().before(DateManager.getSixthCpcDate()) 
	 			&& (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().trim().length()==0))) {
	 		return true; 
	 	} else 	if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
	 			&& (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().trim().length()==0))) {
	 		return true;
	 	}
	 	double steppedupPay = 0;
	 	
	 	
	 	if (do2.getId()==newDo2.getId()) {
	 		if (! do2.getFromDate().before(DateManager.getSixthCpcDate())) {
	 			if (do2.getColumnA() == null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
		 			return false;
		 		}
	 			if (!Utils.isValidNumber(do2.getColumnA())) {
	 				return false;
	 			}
		 		steppedupPay = Integer.parseInt(do2.getColumnA());
		 		if ((steppedupPay%10)!=0)
					return false;
	 			
	 		} else if (do2.getFromDate().before(DateManager.getSixthCpcDate())){ 
	 			steppedupPay = do2.getRate();
	 			
	 		}
	 	}		
	 			
	 		 /*else {
	 		if (do2.getColumnA() == null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
	 			steppedupPay =  do2.getRate();
	 		} else {
	 			if (!Utils.isValidNumber(do2.getColumnA())) {
	 				return false;
	 			}
	 			steppedupPay = Integer.parseInt(do2.getColumnA());
	 		}
	 	}		*/
			
		
		
		if (rank!=null) {
			List<PayScale> psList=null;
			try {
				psList = psRepo.findByFromDateGreaterThanEqualToAndRankOrderByRankFromDateId(DateManager.getDate("2006-01-01"), rank.getId());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
					
		/*			Utils.getEntityManager().createQuery("select p from PayScale p where p.fromDate>='2006-01-01' "
					+ " and p.rank=:rank and p.startScale is not null order by p.rank, p.fromDate, p.id ").setParameter("rank", rank).getResultList();*/
 			System.out.println("11111111111111 stepup " +psList.size() + " stepped up pay "+steppedupPay +"  aclist size "+acList.size());
			if (psList!=null && psList.size()>0) {
				for (PayScale ps : psList) {
					System.out.println("  ps startscale " +ps.getStartScale()+  "  "+ps.getScaleEnd0());
	 				if (steppedupPay >= ps.getStartScale() && steppedupPay <= ps.getScaleEnd0()) {
		 				if (acList==null) 
							return false;
		 
		 				if ((acList.get(0).getBasicPay()%10) != 0)
							return false;
		 				
						if (steppedupPay >= acList.get(0).getBasicPay())
							return true;					
					}  
				}
			}
			return false;			
		}  
		
		if ((acList.get(0).getBasicPay()%10) != 0)
			return false;
		
		if (steppedupPay >= acList.get(0).getBasicPay())
			return true;
		
		return false; 
	}
		
	public void generateIncrDo2(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		fromDateCal.setTimeInMillis(do2.getFromDate().getTime());
		int year = fromDateCal.get(Calendar.YEAR);
		int month = fromDateCal.get(Calendar.MONTH);
		int date = fromDateCal.get(Calendar.DATE);
		
		do2List = do2Repo.findByFromDateLessThanEqualToOrderByFromDateDesc(employee.getId(),"INCR",do2.getFromDate(),Do2Manager.VALID);
				
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee = :employee AND d.description = :descr AND " +
		" d.fromDate <= :fromDate AND d.status = :vld ORDER BY d.fromDate DESC ").setParameter("employee", employee)
		.setParameter("descr", "INCR").setParameter("fromDate", do2.getFromDate()).setParameter("vld", Do2Manager.VALID)
		.getResultList();*/
		
		Do2 incrDo2 = do2List.get(0);
		Date incrDate = fromDateCal.getTime();
		employee.setIncrementDate(incrDate);
		
		while (!Utils.isDateAfterCurrentQuarterEnding(incrDate)) {
			try {
			if ((year+1) >= 2006 && (year+1)<2016)
				incrDate = DateManager.getDate(year+1, 7, date);
			else  
				incrDate = DateManager.getDate(year+1, month+1, 1);
			} catch (Exception e) {
				Utils.handleException(e);
			}
			year = year+1;
			incrDo2.setFromDate(incrDate);
			incrDo2.setColumnC(do2.getDescription());
			incrDo2.setDo2Date(new Date());
		
			if (incrDate.before(new Date()))
				AuditCageHelper.regenerateCancelledIncrmentDo2s(incrDo2);
		}					
		employee.setIncrementDate(incrDate);
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
	}
	
	public boolean validBasicPayInRateColumnIn7cpc(Do2 do2, Employee employee) {
		acList = null; String payLevel="3"; int basicPay=0; alreadyDrawingEqualOrMorePay=false; double payMatrixValue=0;
		acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(employee.getId(), do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac " +
				" WHERE ac.employee=:emp AND ac.fromDate<=:do2FromDate AND ac.effective=true AND ac.recordStatus!='P' " +
				" AND ac.cancelled=false and ac.absentFlag=false and ac.gradePay=0 ORDER BY ac.fromDate DESC, ac.toDate DESC")
				.setParameter("emp", employee).setParameter("do2FromDate", do2.getFromDate()).getResultList();	*/	
		if (acList!=null && acList.size()>0) {
			payLevel=acList.get(0).getPayLevel();
			basicPay=acList.get(0).getBasicPay();
		}	
		
		if (basicPay>0 && do2.getRate()!=null && basicPay>=do2.getRate()) {
			alreadyDrawingEqualOrMorePay=true;
			return false;
		}
		System.out.println(".................. basicpay " +basicPay + " do2 get rate " + do2.getRate() +" pl "+payLevel);
		if (basicPay>0) {
			List<PayMatrix> paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(payLevel, basicPay);
					
				//	Utils.getEntityManager().createQuery("select p from PayMatrix p where p.payLevel=:pl and "
			//		+ " p.basicPay>:bp order by p.basicPay ").setParameter("pl", payLevel).setParameter("bp", basicPay).getResultList();
			if (paymatrixList!=null && paymatrixList.size()>0) {
				payMatrixValue=paymatrixList.get(0).getBasicPay();
				// Assumed that stepup will normally be next cell of present basic pay in the pay matrix.				
			}
			return (do2.getRate()==payMatrixValue);		
		}				
		return false;		
	}
}