package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
 
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Revert extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee employee = null;
	Rank rank = null;
	Rank empRank = null;
	String previousRankName = "";
	Do2 previousPromotDo2;
	List<EmployeeServiceDetail> esdPromotList = null;
	int presentBasic = 0;
	
	public static final String INVALID_RANK = "VL:Invalid rank in Col 7A.\n";
	public static final String INVALID_CURRENT_RANK = "AU:Cannot be reverted from RECRUIT/SEPOY/NBSUB/SUBMAJ.\n";
	public static final String PROMOT_MISSING = "AU:Promot record of current rank missing in ESD.\n";
	public static final String INVALID_COLUMNA = "VL:ColumnA Is Blank Or Invalid Value In Col 7A.\n";
	public static final String PRIOR_TO_CUTOFFDATE = "VL:Pay Details Prior To CutOff Date Not Available.  Feed REFIXP. \n";
	
	
	static public List<String> col5AList = Arrays.asList("SEP","CFN","GDR","GDSM","GNR","RFN","SIGMN","SWR","SPR");
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean revertValidity = true;
		StringBuffer buffer = new StringBuffer();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	//	rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		// rank changed as stated below since PBOR got MACP-NK and after that reverted in sixcpc
		//on revision of gradepay has to be revised. Needs clarification whether ACP can be reverted 
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		if (do2.getAcceptedQe()==null) {
			if (rank==null) {
				buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
				revertValidity = false;
			}	
			if (rank!=null && rank.getRankCode()!=null && (rank.getRankCode().equals("10") || rank.getRankCode().equals("20") 
												       || rank.getRankCode().equals("50") || rank.getRankCode().equals("70"))) {
				buffer.append(INVALID_CURRENT_RANK);
				revertValidity = false;
			}
	
			if (!(isValidRank(do2))) {
				buffer.append(INVALID_RANK);
				revertValidity = false;
			}
			
			if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
				buffer.append(INVALID_COLUMNA);
				revertValidity=false;
			}			
			
			Date cutOffDate = DateManager.getCutOffDate();
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_JKLI) 
					|| Utils.getPao().getPaoCode().equals(Do2Manager.PAO_LADAK_SCOUTS)) {
				cutOffDate=DateManager.getOtherCutOffDate();
			} else if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
				cutOffDate=DateManager.getEstt22CutOffDate();
			} else if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
				cutOffDate=DateManager.getApsCutOffDate();	
			}
			
			Do2 previousPromotDo2 = getEsdPromotDo2(do2);
	//		System.out.println("33333333333333333 previouspromotdo2  "+previousPromotDo2.getFromDate()+"  "+cutOffDate);
			if (previousPromotDo2==null) {
				buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
				revertValidity = false;
			}
			
			if (previousPromotDo2!=null && !previousPromotDo2.getFromDate().after(cutOffDate)) {
				revertValidity=false;
				buffer.append(PRIOR_TO_CUTOFFDATE);
			}
	//	System.out.println("444444444444444444444 revertvalidity "+revertValidity);
			if (!revertValidity) {
				do2.setStatus(Do2Manager.INVALID);	
			} else {
				do2.setStatus(Do2Manager.VALID);
				Date previousIncrDate = getPreviousIncrDate(do2);
				if (previousIncrDate != null) {
					generateIncrDo2(do2, getPreviousIncrDate(do2));	
				}				
			}
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return revertValidity;
	}

	/*@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		previousPromotDo2 = getEsdPromotDo2(newAuditCage.getDo2());
		updateBdtInfo(newAuditCage.getDo2());
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(getPreviousRankBasic(newAuditCage.getDo2()));
		} else
			// No Change In Basic Pay only grade pay of reverted rank
			newAuditCage.setGradePay(getPreviousRankGradePay(newAuditCage.getDo2()));
		if (baseAuditCage.getTransport() != null && baseAuditCage.getTransport() != 0) {
			newAuditCage.setTransport(AuditCageHelper.getTransportRate(newAuditCage, baseAuditCage));
		}
		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {			
			updateOldBasicPay(newAuditCage.getDo2(), previousPromotDo2.getFromDate(), newAuditCage);
		}

		return newAuditCage;
	}
	*/
	
	// above method commented and modified as stated below due to sevencpc
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		presentBasic = newAuditCage.getBasicPay();
		rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		previousPromotDo2 = getEsdPromotDo2(newAuditCage.getDo2());
//		System.out.println("IIIIIIIIIIIIIIII  prev promot do2   " +previousPromotDo2.getId());
		updateBdtInfo(newAuditCage.getDo2());
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(getPreviousRankBasic(newAuditCage.getDo2()));
		} else if(newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			// No Change In Basic Pay only grade pay of reverted rank
			newAuditCage.setGradePay(getPreviousRankGradePay(newAuditCage.getDo2()));
			if (baseAuditCage.getTransport() != null && baseAuditCage.getTransport() != 0) {
				newAuditCage.setTransport(AuditCageHelper.getTransportRate(newAuditCage, baseAuditCage));
			}
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {			
				updateOldBasicPay(newAuditCage.getDo2(), previousPromotDo2.getFromDate(), newAuditCage);
			}
		} else {
			if (newAuditCage.getGradePay()==0) {
				newAuditCage.setOldBasicPay(newAuditCage.getOldBasicPay());
				newAuditCage.setOldGradePay(getPreviousRankGradePay(newAuditCage.getDo2()));
				get7cpcPay(newAuditCage, newAuditCage.getDo2());
			}
				
		}
			
			

		return newAuditCage;
	}


	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		final String[] alwNotAdmissible = { "APTPAY" };
		List<String> alwToStop = Arrays.asList(alwNotAdmissible);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		return null;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		reverseUpdateEmployee(refDo2);
		return null;
	}
	
	public boolean isValidRank(Do2 do2) {		
		
		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		String columnA = do2.getColumnA();

		if (col5AList.contains(columnA.trim()) && empRank.getRankCode().equals("30")) {
			return true;
		}
		if (columnA.equals("NK") && empRank.getRankCode().equals("40")) {
			return true;
		}
		if (columnA.equals("NBSUB") && empRank.getRankCode().equals("60")) {
			return true;
		}
		
		return false;
	}


   //this method replaced on 16042012
	public int getPreviousRankGradePay(Do2 do2) {
		int gradepay = 0;
		String rankName = do2.getColumnA();
		String rankCode = null;
		if (rankName.equals("SEP")){
			rankCode = "20";
		} else if (rankName.equals("NK")){
			rankCode = "30";
		} else if (rankName.equals("NBSUB")){
			rankCode = "50";
		} 
		empRank=rankRepo.findByRankCode(rankCode);
	//	empRank = (Rank) Utils.getForeignKey(new Rank(),"rankCode",rankCode);
		if (empRank.getRankCode().equals("20")) {
			gradepay=2000;
		} else if (empRank.getRankCode().equals("30")) {
			gradepay=2400;
		} else if (empRank.getRankCode().equals("50")) {
			gradepay=4200;
		} 
		return gradepay;
	}


	public int getPreviousRankBasic(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atcList = acRepo.findByFromDateLessThan(employee.getId(),previousPromotDo2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT atc FROM AuditCage atc WHERE atc.employee=:employee AND atc.absentFlag=false AND " +
				" atc.cancelled=false AND atc.effective=true AND atc.recordStatus!='P' AND " +
				" atc.fromDate < :frdt ORDER BY atc.fromDate DESC")
				.setParameter("employee", employee)
				.setParameter("frdt", previousPromotDo2.getFromDate()).getResultList();*/
//	System.out.println("OOOOOOOOOOOOOOOO  atcList size  " +atcList.size());
//		return arriveProbablePay(do2, atcList.get(0).getBasicPay());
		return arriveProbablePay(do2, atcList);
	}

//	public int arriveProbablePay(Do2 do2, int previousBasic) {
	public int arriveProbablePay(Do2 do2, List<AuditCage> acList) {
		int probableBasic = presentBasic;
		if (acList!=null && acList.size()>0) {
			probableBasic = acList.get(0).getBasicPay();
		}
		Date previousIncrDate = null;
		previousIncrDate=getPreviousIncrDate(do2);
	    String rankCode = null;
	    if (do2.getColumnA().trim().equals("SUB")){
	    	rankCode="60";
	    } else if (do2.getColumnA().trim().equals("NBSUB")){
	    	rankCode="50";
	    } else if (do2.getColumnA().trim().equals("NK")){
	    	rankCode="30";
	    } else if (do2.getColumnA().trim().equals("SEP")){
	    	rankCode="20";
	    } else if (do2.getColumnA().trim().equals("HAV")){
	    	rankCode="40";
	    }
		if (previousIncrDate != null){
			Rank rnk=rankRepo.findByRankCode(rankCode);
			List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rnk.getId(), 
																				employee.getGroupCategory().getSoldierGroup(), previousIncrDate);
					
			/*		Utils.getEntityManager().createQuery(
					"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
					+ " AND ps.soldierGroup.soldierGroup=:grp "
					+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
					+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
					+ " AND ps.startScale IS NOT null")
					.setParameter("rnk",Utils.getForeignKey(new Rank(), "rankCode", rankCode))
					.setParameter("grp", employee.getGroupCategory().getSoldierGroup())
					.setParameter("fromDate", previousIncrDate).getResultList();*/
	        
			
			previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
					
			while (previousIncrDate.before(do2.getFromDate())) {
				probableBasic += payScaleList.get(0).getIncr0();
				previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);			
			}
		}
	
		return probableBasic;
	}

	public void updateEmployee(Do2 do2) {
		
		String empRank = "";
		String rankCode=null;
		empRank = Do2Manager.getRankMap().get(do2.getColumnA());
		if (empRank.equals("SEP")){
			employee.setNaikDate(null);
			rankCode="20";
		} else if (empRank.equals("NK")) {
				employee.setNaikDate(do2.getFromDate());
				employee.setHavDate(null);
				rankCode="30";
		} else if (empRank.equals("NBSUB")) {
				employee.setNbsubDate(do2.getFromDate());
				employee.setSubDate(null);
				rankCode="50";
		} 		
		Rank rank=rankRepo.findByRankCode(rankCode);
		employee.setSubstantiveRank(rank);
	//	employee.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(),"rankCode", rankCode));
		employee.setSubstantiveRankDate(newDo2.getFromDate());
		employee.setActingRank(rank);
	//	employee.setActingRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", rankCode));
		employee.setActingRankDate(newDo2.getFromDate());
		employee.setAppointmentDate(null);
		employee.setAppointmentRate(null);
	//	List<Rank> rankList = null;
		 
				
			//	Utils.getEntityManager().createQuery("SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode")
		//	.setParameter("rankCode", rankCode).getResultList();
		
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getServiceRetention());

	}

	public Date getPreviousIncrDate(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date prevIncrDate = do2.getFromDate();
		Do2 promotDo2 = getEsdPromotDo2(do2);
		if (promotDo2 != null) {			
			prevIncrDate = promotDo2.getFromDate();
		} 
		List<Do2> do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(), "V", "INCR", prevIncrDate);
				
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d where d.employee=:employee AND d.description=:description AND " +
				" d.fromDate<:fromDate and d.status = 'V' order by d.fromDate desc")
				.setParameter("employee", employee)
				.setParameter("description", "INCR")
				.setParameter("fromDate", prevIncrDate)
				.getResultList();*/
		
        if (do2List != null && do2List.size() > 0){
        	prevIncrDate = do2List.get(0).getFromDate();        	
        }
        return prevIncrDate;
	}

	private void generateIncrDo2(Do2 do2, Date previousIncrDate) {
		if (!do2.getFromDate().before(DateManager.getSixthCpcDate())){
			return;
		}
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByDescStatusFromDateGreater(employee.getId(), "V", "INCR", do2.getFromDate());
				
			/*	Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d where d.employee=:employee AND d.description=:description AND " +
				" d.fromDate>:fromDate AND d.status = 'V'")
				.setParameter("employee", employee)
				.setParameter("description", "INCR")
				.setParameter("fromDate",do2.getFromDate())
				.getResultList();*/
		if (do2List != null && do2List.size() > 0) {
			for (int i = 0; i < do2List.size(); ++i){
				if (do2List.get(i).getFromDate().before(DateManager.getSixthCpcDate())){
					do2List.get(i).setStatus("C");
					do2Repo.save(do2List.get(0));
				//	Utils.getEntityManager().persist(do2List.get(i));
				}
			}
		}
		Date fromDate = DateManager.getToday();
		fromDate.setTime(previousIncrDate.getTime());
		while ((fromDate = Utils.getNextIncrementDate(fromDate)).before(Utils.getLastDayOfCurrentQuarter())) {
			if (fromDate.after(do2.getFromDate()) && !isIncrDo2Generated(do2, fromDate))
				AuditCageHelper.insertIncrmentDo2s(do2, fromDate, do2.getColumnC());
		}

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setIncrementDate(fromDate);
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);

	}

	private boolean isIncrDo2Generated(Do2 do2, Date fromDate) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V", "INCR", fromDate);
				
			/*	Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d where d.employee=:employee AND d.description=:description AND " +
				" d.fromDate=:fromDate AND d.status = 'V'")
				.setParameter("employee", employee)
				.setParameter("description", "INCR")
				.setParameter("fromDate",fromDate)
				.getResultList();*/
		return do2List.size() != 0;
	}
   // this method replaced on 16042012
	private void updateOldBasicPay(Do2 do2, Date previousPromotDate, AuditCage newAuditCage) {
	 	employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(),previousPromotDate);
				
			/*	Utils.getEntityManager().createQuery(
						"SELECT atc FROM AuditCage atc WHERE atc.employee=:employee AND atc.cancelled=false AND atc.absentFlag=false AND " +
						" atc.effective=true AND atc.recordStatus!='P' AND atc.fromDate<:frdt ORDER BY atc.fromDate DESC")
				        .setParameter("employee", employee)
				        .setParameter("frdt", previousPromotDate).getResultList();*/
		
		int probableBasic = 0;
		if (atcList.size()>0)
			probableBasic = atcList.get(0).getOldBasicPay();		
		
		Date previousIncrDate = getPreviousIncrDate(do2);
		if (previousIncrDate != null)
			previousIncrDate=DateManager.getNextIncrementDate(previousIncrDate);
        
        String soldierGroup = "";
		GroupCategory gc = Utils.getGroupCategory(do2,previousPromotDate);
		soldierGroup = gc.getSoldierGroup();
	
		empRank = Utils.getPborRank(do2.getArmyNo(), previousPromotDate, "ALLCE");

		List<PayScale> payScaleList=null;
		try{
		 payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(empRank.getId(),soldierGroup,
				 																			DateManager.getDate("1997-10-10"));
				 
				 
				/* Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale IS NOT null")
				.setParameter("rnk",empRank)
				.setParameter("grp", soldierGroup)
				.setParameter("fromDate", DateManager.getDate("1997-10-10")).getResultList();*/
		}
		catch(Exception e){
			
		}
        if (payScaleList.size() >0){
        	while (previousIncrDate.before(do2.getFromDate())) {
        		probableBasic += payScaleList.get(0).getIncr0();
        		previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
        	}
        }

		newAuditCage.setOldBasicPay(probableBasic);
	}

	public Do2 getEsdPromotDo2(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		esdPromotList = esdRepo.findByFromDateLessThanAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_PROMOT+"",  do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT pl FROM EmployeeServiceDetail  pl WHERE " +
				"pl.employee =:employee AND pl.status =:stat AND pl.fromDate <:frdate ORDER BY pl.fromDate DESC")
				.setParameter("employee", employee)
				.setParameter("stat", Do2Manager.ESD_PROMOT)
				.setParameter("frdate", do2.getFromDate())
				.getResultList();*/
       
		return esdPromotList.get(0).getDo2();
	}

	public void reverseUpdateEmployee(Do2 do2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(do2,	Do2Manager.ESD_PROMOT);
		List<Rank> rankList = null;
		esdPromotList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_PROMOT+"");
				
				
	/*			Utils.getEntityManager().createQuery(
						"SELECT epl FROM EmployeeServiceDetail AS epl WHERE epl.do2.employee=:employee AND " +
						"epl.status=:stat order by epl.fromDate desc")
				.setParameter("employee", employee)
				.setParameter("stat", Do2Manager.ESD_PROMOT).getResultList();*/
		
		Do2 promotDo2 = esdPromotList.get(0).getDo2();

		String pRankName = null;
		String pRankCode = null;
       
		if (promotDo2.getDescription().equals("MUSTER")	|| 
			promotDo2.getDescription().equals("ATTSBM")	|| 
			promotDo2.getDescription().equals("ATTSAM")) {
			pRankName="SEP";	
			pRankCode="20";
		} else if (promotDo2.getDescription().equals("MSTHAV")){
			pRankName="HAV";
			pRankCode="40";		
		} else if (promotDo2.getDescription().equals("MSTJCO")){
			pRankName="NBSUB";
			pRankCode="50";
		} else {
			pRankName = promotDo2.getColumnA();
			if (pRankName.equals("NBSUB")) {
				pRankCode="50";
			} else {			
				pRankName = pRankName.substring(2);
				if (pRankName.equals("NK")){
					pRankCode="30";
				} else if (pRankName.equals("HAV")){
					pRankCode="40";
				} else if (pRankName.equals("SUB")){
					pRankCode="60";
				} else if (pRankName.equals("SUBMAJ")){
					pRankCode="70";
				}
			}
		}
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
			rank=rankRepo.findByRankCode(pRankCode);
		/*	rankList = Utils.getEntityManager().createQuery(
					"SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode")
					.setParameter("rankCode", pRankCode).getResultList();*/

		String description = "PA" + do2.getColumnA().substring(2);
        List<Do2> do2List=null;
		if (do2.getColumnA().equals(description)) {
			employee.setActingRank(null);
			employee.setActingRankDate(null);
			do2List = Utils.getSubstantiveDo2(do2);

			if (do2List.size() > 0) {
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
			} else {
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(esdPromotList.get(0).getDo2()
						.getFromDate());
			}
		}

		description = "SB" + do2.getColumnA().substring(2);

		if (do2.getColumnA().equals(description)) {
			do2List = Utils.getPaidActingDo2(do2);

			if (do2List.size() > 0) {
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
				employee.setActingRank(rank);
				employee.setActingRankDate(do2List.get(0).getFromDate());
			} else {
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(esdPromotList.get(0).getDo2()
						.getFromDate());
			}
		}

		if (!Do2Manager.getSubstantiveRankDescription().contains(
				do2.getColumnA())
				&& !Do2Manager.getPaidActingDescription().contains(
						do2.getColumnA())) {
			employee.setActingRank(rank);
			employee.setActingRankDate(esdPromotList.get(0).getDo2()
					.getFromDate());
			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(esdPromotList.get(0).getDo2()
					.getFromDate());
		}
		
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getServiceRetention());
	
		if (((promotDo2.getColumnA().equals("TSNK")) || (promotDo2
				.getColumnA().equals("TSLD"))))
			employee.setTsnkDate(promotDo2.getFromDate());

		else if (((promotDo2.getColumnA().substring(2).equals("NK")) || (promotDo2
				.getColumnA().substring(2).equals("LD"))))
			employee.setNaikDate(promotDo2.getFromDate());

		else if (((promotDo2.getColumnA().substring(2).equals("HAV")) || (promotDo2
				.getColumnA().substring(2).equals("DFR"))))
			employee.setHavDate(promotDo2.getFromDate());

		else if (promotDo2.getColumnA().equals("NBSUB")
				|| (promotDo2.getColumnA().equals("NBRIS"))) {
			employee.setPreJcoCategory(employee.getGroupCategory());
			employee.setNbsubDate(promotDo2.getFromDate());
		}

		else if (((promotDo2.getColumnA().substring(2).equals("SUB")) || (promotDo2
				.getColumnA().substring(2).equals("RIS"))))
			employee.setSubDate(promotDo2.getFromDate());

		else if (((promotDo2.getColumnA().substring(2).equals("SUBMAJ")) || (promotDo2
				.getColumnA().substring(2).equals("RISMAJ"))))
			employee.setSubmajDate(promotDo2.getFromDate());

		else if (((promotDo2.getColumnA().equals("GRHAV")) || (promotDo2
				.getColumnA().equals("GRDFR")))) {
			employee.setHavDate(promotDo2.getFromDate());
			employee.setGraduateHavildarDate(promotDo2.getFromDate());
			employee.setGraduateHavildarFlag(true);
		}

		else if (((promotDo2.getColumnA().equals("SPHAV")) || (promotDo2
				.getColumnA().equals("SPDFR")))) {
			employee.setHavDate(promotDo2.getFromDate());
			employee.setSportHavildarDate(promotDo2.getFromDate());
			employee.setSportHavildarFlag(true);
		}

		else if (promotDo2.getColumnA().equals("HONYLT"))
			employee.setHonyLieuDate(promotDo2.getFromDate());

		else if (promotDo2.getColumnA().equals("HONYCAPT"))
			employee.setHonyCaptainDate(promotDo2.getFromDate());
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}

	public void get7cpcPay(AuditCage nAuditCage,Do2 do2 ) {
		Date previousRankDate = null;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		esdPromotList =  esdRepo.findByFromDateLessThanAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_PROMOT+"",
																									do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status=:status AND "
						+ " esd.fromDate < :fromDate AND esd.employee=:emp ORDER BY esd.fromDate DESC")
		.setParameter("fromDate", do2.getFromDate())
		.setParameter("emp", employee)
		.setParameter("status", Do2Manager.ESD_PROMOT)
		.getResultList();*/
		
		if(esdPromotList.size()>0){
			previousRankDate = DateManager.getPreviousDate(esdPromotList.get(0).getFromDate());
		}
		previousRankName = do2.getColumnA();
		
		if(!do2.getColumnA().equals("SEP")){
			previousRankName="NK";
		}
		String revertRankCode = null;
		 if (do2.getColumnA().trim().equals("SUB")){
			 revertRankCode="60";
		    } else if (do2.getColumnA().trim().equals("NBSUB")){
		    	revertRankCode="50";
		    } else if (do2.getColumnA().trim().equals("NK")){
		    	revertRankCode="30";
		    } else if (do2.getColumnA().trim().equals("SEP")){
		    	revertRankCode="20";
		    } else if (do2.getColumnA().trim().equals("HAV")){
		    	revertRankCode="40";
		    }
		
		int probable7pcBasic = 0;
		
		String revertRankPayLevel = Do2Manager.getPayLevelPromot(revertRankCode);
		
		List<AuditCage>atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(),previousRankDate);
				
		/*		Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
				" atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND atc.absentFlag=false " +
				" AND atc.recordStatus!='P' AND atc.fromDate<:frdt ORDER BY atc.fromDate DESC")
				.setParameter("employee",employee)
				.setParameter("frdt",previousRankDate)
				.getResultList();*/
		if (atcList.size() > 0) {
			probable7pcBasic = atcList.get(0).getBasicPay();
		}
				
		Date previousIncrDate=DateManager.getNextIncrementDate(previousRankDate);
		
		List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(revertRankPayLevel, probable7pcBasic);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ "  pm.basicPay>:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", revertRankPayLevel)
				.setParameter("sevencpcpay", probable7pcBasic)
				.getResultList();*/
		
		if (paymatrixList.size() > 0) {
			probable7pcBasic = paymatrixList.get(0).getBasicPay();
		}
		
		while (previousIncrDate.before(do2.getFromDate())) {
			List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(revertRankPayLevel,probable7pcBasic);
					
		/*			Utils.getEntityManager().createQuery( 
					 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
					+ " pm.basicPay>=:sevencpcpay order by basicPay asc ")
					.setParameter("paylevel", revertRankPayLevel)
					.setParameter("sevencpcpay", probable7pcBasic)
					.getResultList();*/
			if (paymatrixList.size() > 0) {
				probable7pcBasic = paymatrixList.get(0).getBasicPay();
			}
			previousIncrDate=DateManager.getNextIncrementDate(previousIncrDate);
		}
		nAuditCage.setBasicPay(probable7pcBasic);
		employee.setIncrementDate(previousIncrDate);
		
		
	}
	
}
