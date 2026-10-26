package itsdc.dolphin.do2.auditcage;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageHelper;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.AppendixJ;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.repositories.AdjustmentCbRepository;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.AppointmentRateRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.BhutanSlabRepository;
import itsdc.dolphin.repositories.ClassPayRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeEarningRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.EnrollDo2Repository;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.repositories.FundSummaryRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LpcDeputationRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RankRepository;
import itsdc.dolphin.repositories.ReadyReckonerRepository;
import itsdc.dolphin.repositories.RhLocationRepository;
import itsdc.dolphin.repositories.StatusCodeRepository;
import itsdc.dolphin.repositories.TransferInOutRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author Y Raja Reddy, Venkat Date: 2009-07-09
 */

@RestController
public class AuditCageProcessor implements Serializable {

	List<AuditCage[]> dueDrawnAuditCageList = new ArrayList<AuditCage[]>();
	List<AuditCageAllowance[]> dueDrawnAllowanceList = new ArrayList<AuditCageAllowance[]>();
	List<AuditCageAllowance[]> prevDueDrawnAllowanceList = new ArrayList<AuditCageAllowance[]>();
	List<AuditCage> auditCageList = new ArrayList<AuditCage>();
	List<ArrearsObject> arrearsObjectList = new ArrayList<ArrearsObject>();
	List<Do2> pendingDo2List = new ArrayList<Do2>();
	List<AuditCage> cancelAuditCageList;
	private List<AuditCageAllowance> drawnList;
	private List<AuditCageAllowance> dueList;
	//new variable declared
	private List<AuditCageAllowance> previousDrawnList;
	Date incrementDate = new Date();
	private int do2cnt = 0;
	private List<String> leaveList = Arrays.asList(new String[] { "AL", "ADVAL", "EXTNLV",
			"BAL", "ACCMAL", "EXTNAL", "EXTNCL", "EXTNSL", "PAL", "RL", "CL", "SL", "REGOSL" });

	Date lastDayOfPreviousQuarter, regularisedToDate, cancelStopAllToDate,
			restoreToDate, nextOslToDate, nextOslFromDate, reduceFromDate, reduceToDate, clDateForOsl,nextTfrDepDate, revdepDate,posoutDate;
	Employee employee;
	 

	Do2 newDo2, currentDo2, refDo2;
	
	AuditCage dueAuditCage, drawnAuditCage, base, newAuditCage, nextOslAuditCage, previousDrawnAuditCageForPending,
	currentAuditCage, newDo2AuditCage, previousCancelAuditCage,previousDrawnAuditCage;

	Do2AuditCageProcessor do2Processor;
	Do2AccountingCageProcessor do2AccountingCageProcessor;
	private SpecialProcessor processor;

	private AuditCage[] dueDrawnAuditCage = new AuditCage[2];
	boolean cancelledAuditCage, pendingAuditCage, absentDo2, restoreDo2,
			validToPending, regularisationDo2, absentDo2Cancel,
			restoreDo2Cancel, regulrisationDo2Cancel, nextOslAvaialble,nextTfrDepAvailable,
			reduceDo2, reduceDo2Cancel, closing,newRestoreDo2,arrearsRequired=true;

	SpecialProcessor specialProcessor = new SpecialProcessor();
	AccountingCageHelper accountingCageHelper;
	static public final String MISSING_BASE_AUDIT_CAGE = "AU: Base audit cage is missing. \n";
	static public final String IS_ABSENT = "AU:PBOR is absent . \n";
	static public final String AUDIT_CAGE_DO2_MISSING = "AU:Do2 In AuditCage is Missing. AuditCage Id: \n";
	HashSet<Do2> auditCageDo2Set = new HashSet<Do2>();
	HashSet arrestDescriptions;
	
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	AuditCageAllowanceRepository acaRepo;
	ArrearsRepository arrRepo;
	AppendixJRepository appjRepo;
	PayScaleRepository psRepo;
	EmployeeServiceDetailRepository esdRepo;
	   
	    RankRepository rankRepo;
	    GroupCategoryRepository gcRepo;
	    PayMatrixRepository matrixRepo;
	   
	    ClassPayRateRepository classPayRepo;
	    AppointmentRateRepository apptRateRepo;
	    AllowanceRateRepository alwRateRepo;
	    EmployeeRepository empRepo;
	    StatusCodeRepository scRepo;
	    
	     
	    UnitRepository unitRepo;
	    ReadyReckonerRepository rrRepo;
	    LpcRegisterRepository lpcRepo;
	    
	    LpcDeputationRepository lpcDepRepo;
	    BhutanSlabRepository bhutanSlabRepo;
	    
	    EmployeeDeductionRepository edRepo;
	    FundSummaryRepository fsRepo;
	    EmployeeEarningRepository eeRepo;
	    Form16Repository f16Repo;
	    AdjustmentCbRepository adjCbRepo;
	    FamilyDetailRepository fdRepo;
	    TransferInOutRepository tioRepo;
	    JdbcTemplate jdbcTemplate;
	
	    RhLocationRepository rhLocationRepo;
	

	    EnrollDo2Repository enrollDo2Repo;
	
	
	public EnrollDo2Repository getEnrollDo2Repo() {
			return enrollDo2Repo;
		}

		public void setEnrollDo2Repo(EnrollDo2Repository enrollDo2Repo) {
			this.enrollDo2Repo = enrollDo2Repo;
		}
	
	public RhLocationRepository getRhLocationRepo() {
			return rhLocationRepo;
		}

		public void setRhLocationRepo(RhLocationRepository rhLocationRepo) {
			this.rhLocationRepo = rhLocationRepo;
		}

	public EmployeeRepository getEmpRepo() {
			return empRepo;
		}

		public void setEmpRepo(EmployeeRepository empRepo) {
			this.empRepo = empRepo;
		}

	public EmployeeServiceDetailRepository getEsdRepo() {
			return esdRepo;
		}

		public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
			this.esdRepo = esdRepo;
		}

		public RankRepository getRankRepo() {
			return rankRepo;
		}

		public void setRankRepo(RankRepository rankRepo) {
			this.rankRepo = rankRepo;
		}

		public GroupCategoryRepository getGcRepo() {
			return gcRepo;
		}

		public void setGcRepo(GroupCategoryRepository gcRepo) {
			this.gcRepo = gcRepo;
		}

		public PayMatrixRepository getMatrixRepo() {
			return matrixRepo;
		}

		public void setMatrixRepo(PayMatrixRepository matrixRepo) {
			this.matrixRepo = matrixRepo;
		}

		public ClassPayRateRepository getClassPayRepo() {
			return classPayRepo;
		}

		public void setClassPayRepo(ClassPayRateRepository classPayRepo) {
			this.classPayRepo = classPayRepo;
		}

		public AppointmentRateRepository getApptRateRepo() {
			return apptRateRepo;
		}

		public void setApptRateRepo(AppointmentRateRepository apptRateRepo) {
			this.apptRateRepo = apptRateRepo;
		}

		public AllowanceRateRepository getAlwRateRepo() {
			return alwRateRepo;
		}

		public void setAlwRateRepo(AllowanceRateRepository alwRateRepo) {
			this.alwRateRepo = alwRateRepo;
		}

		public StatusCodeRepository getScRepo() {
			return scRepo;
		}

		public void setScRepo(StatusCodeRepository scRepo) {
			this.scRepo = scRepo;
		}

		public UnitRepository getUnitRepo() {
			return unitRepo;
		}

		public void setUnitRepo(UnitRepository unitRepo) {
			this.unitRepo = unitRepo;
		}

		public ReadyReckonerRepository getRrRepo() {
			return rrRepo;
		}

		public void setRrRepo(ReadyReckonerRepository rrRepo) {
			this.rrRepo = rrRepo;
		}

		public LpcRegisterRepository getLpcRepo() {
			return lpcRepo;
		}

		public void setLpcRepo(LpcRegisterRepository lpcRepo) {
			this.lpcRepo = lpcRepo;
		}

		public LpcDeputationRepository getLpcDepRepo() {
			return lpcDepRepo;
		}

		public void setLpcDepRepo(LpcDeputationRepository lpcDepRepo) {
			this.lpcDepRepo = lpcDepRepo;
		}

		public BhutanSlabRepository getBhutanSlabRepo() {
			return bhutanSlabRepo;
		}

		public void setBhutanSlabRepo(BhutanSlabRepository bhutanSlabRepo) {
			this.bhutanSlabRepo = bhutanSlabRepo;
		}

		public EmployeeDeductionRepository getEdRepo() {
			return edRepo;
		}

		public void setEdRepo(EmployeeDeductionRepository edRepo) {
			this.edRepo = edRepo;
		}

		public FundSummaryRepository getFsRepo() {
			return fsRepo;
		}

		public void setFsRepo(FundSummaryRepository fsRepo) {
			this.fsRepo = fsRepo;
		}

		public EmployeeEarningRepository getEeRepo() {
			return eeRepo;
		}

		public void setEeRepo(EmployeeEarningRepository eeRepo) {
			this.eeRepo = eeRepo;
		}

		public Form16Repository getF16Repo() {
			return f16Repo;
		}

		public void setF16Repo(Form16Repository f16Repo) {
			this.f16Repo = f16Repo;
		}

		public AdjustmentCbRepository getAdjCbRepo() {
			return adjCbRepo;
		}

		public void setAdjCbRepo(AdjustmentCbRepository adjCbRepo) {
			this.adjCbRepo = adjCbRepo;
		}

		public FamilyDetailRepository getFdRepo() {
			return fdRepo;
		}

		public void setFdRepo(FamilyDetailRepository fdRepo) {
			this.fdRepo = fdRepo;
		}

		public TransferInOutRepository getTioRepo() {
			return tioRepo;
		}

		public void setTioRepo(TransferInOutRepository tioRepo) {
			this.tioRepo = tioRepo;
		}

		public JdbcTemplate getJdbcTemplate() {
			return jdbcTemplate;
		}

		public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public AppendixJRepository getAppjRepo() {
		return appjRepo;
	}

	public void setAppjRepo(AppendixJRepository appjRepo) {
		this.appjRepo = appjRepo;
	}

	public ArrearsRepository getArrRepo() {
		return arrRepo;
	}

	public void setArrRepo(ArrearsRepository arrRepo) {
		this.arrRepo = arrRepo;
	}

	public AuditCageAllowanceRepository getAcaRepo() {
		return acaRepo;
	}

	public void setAcaRepo(AuditCageAllowanceRepository acaRepo) {
		this.acaRepo = acaRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public boolean isClosing() {
		return closing;
	}

	public void setClosing(boolean closing) {
		this.closing = closing;
	}
	public AuditCageProcessor() { 
		arrestDescriptions = Do2Manager.getArrestDescription();
		lastDayOfPreviousQuarter = Utils.getLastDayOfPreviousQuarter();
	}

	public String process(Do2 do2) {
		newDo2 = do2;
		closing = false;
	 	if (do2.getDescription().equals("CANCEL")) {		
			refDo2 = Utils.getReferenceDo2(do2, do2.getColumnA());
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2.setAcceptedQe(Utils.getCurrentQuarter());			
		}
		
		if (!isValid(do2))
			return Do2Manager.FAILURE;
		
		if (!isBaseAuditCageAvailable(do2))
			return Do2Manager.FAILURE;		

		processFirstPair(do2);
		
		processSecondPairOnwards(do2);
		
		return Do2Manager.SUCCESS;
	}

	public boolean isValid(Do2 do2) {
		if (do2.getTranscriptionType() != null && do2.getTranscriptionType()==(Do2Manager.SYSTEM_GENERATED)) {
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2.setReason(null);
			return true;
		}
		
		if (do2.getDescription().equals("CANCEL")) {
				return true;
		}
		
		/* Bringing The Date To First Of The Month For TAINCR do2 Commented (below method) wef 09.01.2018.
		 * 
		 * if (do2.getDescription().equals("TAINCR")) {
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
			do2.setColumnC(sdf.format(do2.getFromDate()));
			GregorianCalendar do2Cal = new GregorianCalendar();
			do2Cal.setTime(do2.getFromDate());
			int do2Year = do2Cal.get(Calendar.YEAR);
			int do2Month = do2Cal.get(Calendar.MONTH);	
			String frDate = do2Year+"-"+(do2Month+1)+"-"+"01";
			Date fromDate=null;
			try {
				fromDate = DateManager.getDate(frDate);	
			} catch(Exception e) {
				Utils.handleException(e);
			}
			do2.setFromDate(fromDate);
		}*/
		
		// As per Rule 7 of pay & allowances of increment is due from the first of the month in which it falls due.
		// Hence restored above method on 22-03-2018.
		
		/* if (do2.getDescription().equals("TAINCR")) {
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
				do2.setColumnC(sdf.format(do2.getFromDate()));
				GregorianCalendar do2Cal = new GregorianCalendar();
				do2Cal.setTime(do2.getFromDate());
				int do2Year = do2Cal.get(Calendar.YEAR);
				int do2Month = do2Cal.get(Calendar.MONTH);	
				String frDate = do2Year+"-"+(do2Month+1)+"-"+"01";
				Date fromDate=null;
				try {
					fromDate = DateManager.getDate(frDate);	
				} catch(Exception e) {
					Utils.handleException(e);
				}
				do2.setFromDate(fromDate);
		 }
		 
		 THE ABOVE METHOD HAS BEEN COMMENTED WEF 19.12.2018 (DOLPHIN SYSTEM CIRCULAR 301) IN VIEW OF HQRS. OFFICE CLARIFICATION TO
		 ADJUST TAINCR ON THE DAY AFTER COMPLETION OF 365 DAYS		 
		 */
		
		
		
		 
		lastDayOfPreviousQuarter = Utils.getLastDayOfPreviousQuarter();
		newDo2 = do2;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		try {			 	
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"itsdc.dolphin.do2.auditcage."
							+ Utils.getCamelCase(do2.getDescription())).getDeclaredConstructor()
					.newInstance();
			 
			System.out.println("ac processor setting do2 repo "+do2Repo+"--");
			((AuditCageBase) do2Processor).setDo2Repo(do2Repo);
			//((PromotBase) do2Processor).setDo2Repo(do2Repo);
			((AuditCageBase) do2Processor).setPsRepo(psRepo);
			((AuditCageBase) do2Processor).setAcRepo(acRepo);
			((AuditCageBase) do2Processor).setEmpRepo(empRepo);
			((AuditCageBase) do2Processor).setAcaRepo(acaRepo);
			((AuditCageBase) do2Processor).setNewDo2(newDo2);
			
			((AuditCageBase) do2Processor).setEsdRepo(esdRepo);
			((AuditCageBase) do2Processor).setRankRepo(rankRepo);
			((AuditCageBase) do2Processor).setGcRepo(gcRepo);
			((AuditCageBase) do2Processor).setMatrixRepo(matrixRepo);
			((AuditCageBase) do2Processor).setClassPayRepo(classPayRepo);
			((AuditCageBase) do2Processor).setApptRateRepo(apptRateRepo);
			((AuditCageBase) do2Processor).setAlwRateRepo(alwRateRepo);
			((AuditCageBase) do2Processor).setScRepo(scRepo);
			((AuditCageBase) do2Processor).setUnitRepo(unitRepo);
			((AuditCageBase) do2Processor).setRrRepo(rrRepo);
			((AuditCageBase) do2Processor).setLpcRepo(lpcRepo);
			((AuditCageBase) do2Processor).setArrRepo(arrRepo);
			((AuditCageBase) do2Processor).setLpcDepRepo(lpcDepRepo);
			((AuditCageBase) do2Processor).setBhutanSlabRepo(bhutanSlabRepo);
			((AuditCageBase) do2Processor).setEdRepo(edRepo);
			((AuditCageBase) do2Processor).setFsRepo(fsRepo);
			((AuditCageBase) do2Processor).setEeRepo(eeRepo);
			((AuditCageBase) do2Processor).setF16Repo(f16Repo);
			((AuditCageBase) do2Processor).setAdjCbRepo(adjCbRepo);
			((AuditCageBase) do2Processor).setFdRepo(fdRepo);
			((AuditCageBase) do2Processor).setTioRepo(tioRepo);
			((AuditCageBase) do2Processor).setJdbcTemplate(jdbcTemplate);
			((AuditCageBase) do2Processor).setRhLocationRepo(rhLocationRepo);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (!do2Processor.isValid(do2)) {					
			do2Repo.save(do2);	
			return false;
		}
		
		if (do2.getAcceptedQe()==null){
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		} else {
			if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		return true;
	}

	boolean isBaseAuditCageAvailable(Do2 do2) {
		boolean do2NullInAuditCage=false;
		auditCageList = getAffectedAuditCages(do2);
        
		if (auditCageList == null || auditCageList.size() == 0) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(MISSING_BASE_AUDIT_CAGE);
			return false;
		}		
		
		for (AuditCage ac : auditCageList) {
			if (ac.getDo2() == null ) {
				do2NullInAuditCage = true;
				do2.setStatus(Do2Manager.INVALID);
				if (do2.getReason() != null) {
					do2.setReason(do2.getReason() + AUDIT_CAGE_DO2_MISSING + ac.getId());
				} else {
					do2.setReason( AUDIT_CAGE_DO2_MISSING + ac.getId());
				}
			}
			if (ac.getBasicPay() == null ||
				ac.getDp() == null ||
				ac.getGradePay() == null ||
				ac.getMsPay() == null ||
				ac.getXGroupPay() == null ||
				ac.getClassPay() == null ||
				ac.getDa() == null ||
				ac.getTransport() == null ||
				ac.getPmha() == null ||
				ac.getLra() == null ||
				ac.getOldBasicPay() == null ||
				ac.getOldClassPay() == null ||
				ac.getOldGradePay() == null ||
				(ac.getPayLevel() == null || ac.getPayLevel().equals('0'))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason( "AU : Null values in audit cage - Audit Cage Id : " + ac.getId());
				do2Repo.save(do2);
				return false;
			}
			
	       /* if (ac.getFromDate().after(DateManager.getSeventhCpcDate())) {
				 Date optCpcDate = null;
				 if (ac.getPayLevel()==null || ac.getPayLevel().equals("0")) {
					 List<Do2> optcpcList = Utils.getEntityManager().createQuery
							 (" SELECT d from Do2 d where d.employee=:emp AND d.description=:desc " +
							 		" and d.status=:stat ")
							 .setParameter("emp", ac.getEmployee())
							 .setParameter("desc", "OPTCPC")
							 .setParameter("stat", Do2Manager.VALID).getResultList();
					
					  if (optcpcList.size() > 0) {
						  optCpcDate = optcpcList.get(0).getFromDate();
					  }
					
					  if ((optCpcDate==null) || (optCpcDate!=null && !ac.getFromDate().before(optCpcDate))){
						  do2.setReason("AU : Paylevel in auditcage is null or 0. Please feed ATCORR as on 01-01-2016 " +
						  		" for updation of Paylevel ");
						  return false;
					  }
						 
					 }
			}*/
		}       
		if (do2NullInAuditCage)
			return false;
		
		sortAuditCages(auditCageList);		
		
		if (!Do2Manager.getMusterAttesttDescription().contains(do2.getDescription())
				&& !Do2Manager.getMusterAttesttDescription().contains(do2.getColumnA())) {
			for (int i = 0; i < auditCageList.size() - 1; i++) {
				if (auditCageList.get(i).getDo2().getDescription().equals("ENROLL")){
					break;
				}
				if (auditCageList.get(i).getFromDate().equals(auditCageList.get(i + 1).getFromDate())) {
					if (!auditCageList.get(i + 1).getRecordStatus().equals("P")) {
						auditCageList.remove(i);
						i--;
						continue;
					}
				}
				break;
			}
		}
		/*
		 * Commented on 04.02.2014 
		copyAuditCages(auditCageList);		
		*/
		for (AuditCage ac : auditCageList) {			
			auditCageDo2Set.add(ac.getDo2());
		}
		return true;
	}	
	
    public void copyAuditCages(List<AuditCage> auditCageList){
    	AuditCage auditCageCloned=null;
    	AuditCageAllowance auditCageAllowanceCloned=null;
    
    	Date currentDate = new Date();
    	for(int i=1;i<auditCageList.size();i++){
    		 if(auditCageList.get(i).getQe().equals(Utils.getCurrentQuarter())){
    			 continue;
    		 }
    		List<AuditCageAllowance> allowanceList = acaRepo.findAllByAuditCageId(auditCageList.get(i).getId()); 
     		auditCageCloned = (AuditCage) Utils	.getClonedEntity(auditCageList.get(i));
    		auditCageCloned.setEffective(false);
    		acRepo.save(auditCageCloned);
    		 
    		for(AuditCageAllowance aca : allowanceList){
    			auditCageAllowanceCloned= (AuditCageAllowance) Utils.getClonedEntity(aca);
    			auditCageAllowanceCloned.setAuditCage(auditCageCloned);
    			acaRepo.save(auditCageAllowanceCloned);
    		}
    	}
    }
	void processFirstPair(Do2 do2) {
		 
        
		newDo2AuditCage = getAuditCageForInsertDo2(do2);
		
		do2Repo.save(do2);

		if (!do2.getDescription().equals(Do2Manager.CANCEL) && ((newDo2AuditCage.getId()!=null && newDo2AuditCage.getId() == 0) || newDo2AuditCage.getId()==null) )
			auditCageList.add(newDo2AuditCage);	
		
		addPendingDo2s(auditCageList);
		
		for (int i = 0; i < auditCageList.size() - 1; i++) {
			//lines added some cases where do2 is marked as cancelled and auditcage is still not cancelled
			if (auditCageList.get(i).getDo2().getStatus().equals(Do2Manager.DO2_CANCEL_STATUS)){
				   if (auditCageList.get(i).getCancelled()==false){
					   cancelAuditCageList.add(auditCageList.get(i));
				   }				  
			}
		}
		
		sortAuditCages(auditCageList);
		
		// Only for APS Nagpur for OPTCPC -- manipulation  -- 13/07/2018
				if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && 
				   (newDo2.getDescription().equals("REFIXP") && (newDo2.getColumnC() != null && 
				   (newDo2.getColumnC().equals("MACPS") || newDo2.getColumnC().equals("INCR") || newDo2.getColumnC().equals("PROMOT"))))) {
					if (auditCageList.get(0).getOldBasicPay() != null){
						auditCageList.get(0).setBasicPay(auditCageList.get(0).getOldBasicPay());
					}
					if (auditCageList.get(0).getOldGradePay() != null){
						auditCageList.get(0).setGradePay(auditCageList.get(0).getOldGradePay());
					}			
					Rank rank = Utils.getPborRank(newDo2.getArmyNo(), DateManager.getNextDate(newDo2.getFromDate()),"PAYFIX");
					if (Integer.parseInt(rank.getRankCode()) < 80){
						auditCageList.get(0).setMsPay(2000);
					} else {
						auditCageList.get(0).setMsPay(6000);
					}
					auditCageList.get(0).setPayLevel("0");
					auditCageList.get(0).setDa(getFullMonthDa(auditCageList.get(0)));
				}


		
		
		base = auditCageList.get(0);
		
		previousDrawnAuditCageForPending=base;
		previousDrawnAuditCageForPending=(AuditCage) Utils.getClonedEntity(base);
    	previousDrawnAuditCageForPending.setId(base.getId());
    	previousDrawnList = getDrawnList(base);

		previousDrawnAuditCage = null;
		
		/*if (newDo2.getDescription().equals("PROMOT") && Do2Manager.PAID_ACTING_DESCRIPTIONS.contains(newDo2.getColumnA())){
	 		for (int i = 0; i < auditCageList.size(); i++){
	 			if (auditCageList.get(i).getDo2().equals(newDo2) && isSubstantiveDateBeforeActing(newDo2)){
	 				previousDrawnAuditCage =(AuditCage) Utils.getClonedEntity(getDrawnAuditCage(auditCageList.get(i),i));
	 			}
	 		}
		}*/
		
	    printAuditCageList(newDo2.getDescription() + " " +
		newDo2.getColumnA() +" "+"===========================FIRST PAIR : AFTER PRIORITY REMOVAL==============");

		newAuditCage = newDo2AuditCage;
	}

	void processSecondPairOnwards(Do2 do2) {

		ArrearsObject arrearsObject = null;
		do2cnt=0;
		
		try {
			for (int i = 1; i < auditCageList.size(); i++) {
				
				validToPending = false;
				pendingAuditCage = false;
				dueDrawnAuditCage = new AuditCage[2];
				currentAuditCage = auditCageList.get(i);	     
				currentAuditCage.setCurrentQuarter(true);				
				currentDo2 = auditCageList.get(i).getDo2();
				convertDo2(currentDo2);
				if (!cancelAuditCageList.contains(currentAuditCage)) {
					if (leaveList.contains(currentDo2.getDescription())) {
						List<AuditCageAllowance> acaList =acaRepo.findAllByAuditCageId(base.getId()); 
								
						for (AuditCageAllowance aca : acaList) {
							/*
							if (aca.getPayCode().getPayCode().trim().equals("ADPOST")) {
								List<Do2> do2List =do2Repo.findByDescColAFromDateLessThanEqual(base.getEmployee().getId(),Do2Manager.VALID,"ADPOST","GRANTED",currentDo2.getFromDate()); 
									 
								if (do2List!=null && do2List.size()>0) {
									Do2 adpostDo2 = do2List.get(0);
									Date adpostCeaseDate = currentDo2.getFromDate();
									
									//  Please refer para d of DoPT letter No.2/13/2017-Estt.(Pay-II) dated 08.08.2018 
 									//  Circulated vide MoD ID No.PC-1(16)/2017D(Pay/Services) dated 29.08.2018 - 
									//  Received vide Hqrs. Office letter No.AT/I/3510/7thCPC/III dated 10.09.2018. 
									//  -- The allowance will be admissible only duration of additional charge exceeds 45 days
									
									
									if ((DateManager.getNoOfDays(do2List.get(0).getFromDate(), currentDo2.getFromDate())<45)) {
										adpostCeaseDate = DateManager.getNextDate(do2List.get(0).getFromDate()); 
									}
									generatetptlCeaseDo2(currentDo2, adpostCeaseDate, "ADPOST");
							 	    sortAuditCages(auditCageList);								 	  
								}
								break;
							}
							*/  // Ceased do2 generation during adjustment of leave removed on 14.03.2024.
							
							if (aca.getPayCode().getPayCode().trim().equals("ISLNDA") && (!currentDo2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())
									 	|| (currentDo2.getToDate()!=null && !currentDo2.getToDate().before(DateManager.getSeventhCpcAllowanceDate())))) {
									 			Date islndaCeasedDate = getTransportAllceDate(currentDo2);
									 								 
									 		if (islndaCeasedDate!=null) {
									 			generatetptlCeaseDo2(currentDo2, islndaCeasedDate, "ISLNDA");
									 			sortAuditCages(auditCageList);
									 		}
									 		break;
							}
						}					 
					}
					if ((leaveList.contains(currentDo2.getDescription()) && (base.getTransport() != null && base.getTransport() > 0))){
						Date tptlCeasedDate = getTransportAllceDate(currentDo2);								
						if (tptlCeasedDate != null){
							Do2 tptldo2 = getTransportAllceDo2(currentDo2);
							if (tptldo2 != null){
								String description = tptldo2.getDescription();
								double rate        = tptldo2.getRate();
								Do2	 tptlEarlierGrantDo2 = tptldo2;
								String columnC = tptldo2.getColumnC();
								generatetptlCeaseDo2(currentDo2, tptlCeasedDate, description);
							    generateTptlGrantDo2(currentDo2, description, rate, tptlEarlierGrantDo2, columnC);
							    sortAuditCages(auditCageList);
							}
							
						}				
					}				
				}
	/*	 System.out.println("current audit cage  "+currentAuditCage.getDo2().getDescription()+"  "+currentAuditCage.getDo2().getFromDate()
				+ " " + currentAuditCage.getAbsentFlag() + " " + " do2cnt " + do2cnt + " i " + i    +" ARMY NO" +currentAuditCage.getDo2().getArmyNo());
	 */            //****lines added
				if (pendingDo2List.contains(currentDo2 ) || newDo2.getId()==currentDo2.getId()){ 
					 //Lines added to store the previous drawn auditcage when
					 //do2 is kept pending manually in database and processed as new do2 or pendingdo2 list
					 //where as the corresponding auditcage record status is V
			    	if (currentAuditCage.getRecordStatus()!= null && currentAuditCage.getRecordStatus().equals("V")){
			    		previousDrawnAuditCageForPending = currentAuditCage;
			        	previousDrawnAuditCageForPending = (AuditCage) Utils.getClonedEntity(currentAuditCage);
			        	previousDrawnAuditCageForPending.setId(currentAuditCage.getId());
			        	previousDrawnList = getDrawnList(currentAuditCage);
			    	} else {
			    		;
			    	}
				} else {
			    	//***********THIS CONDITION ADDED FOR PENDING DO2 ARREARS
		        	previousDrawnAuditCageForPending = currentAuditCage;
		        	previousDrawnAuditCageForPending = 	(AuditCage) Utils.getClonedEntity(currentAuditCage);
		        	previousDrawnAuditCageForPending.setId(currentAuditCage.getId());
		        	previousDrawnList = getDrawnList(currentAuditCage);
			    }
				
				drawnAuditCage = getDrawnAuditCage(currentAuditCage, i);				
				dueDrawnAuditCage[1] = (AuditCage) Utils.getClonedEntity(drawnAuditCage);
				dueDrawnAuditCage[1].setId(drawnAuditCage.getId());		
				
				if (newDo2.getDescription().equals("FIX7PC")) {
					if ( dueDrawnAuditCage[1].getDeputationStatus()!=null && (dueDrawnAuditCage[1].getDeputationStatus().equals("T") 
							|| dueDrawnAuditCage[1].getDeputationStatus().equals("K")) && dueDrawnAuditCage[1].getDa()==0) {
						dueDrawnAuditCage[1].setDa(getFullMonthDaBhutan(dueDrawnAuditCage[1]));
					}
				}
	/*				System.out.println("current do2 desc   "+currentDo2.getDescription()+"  "+currentDo2.getColumnA()+"  "+dueDrawnAuditCage[1].getDo2().getDescription()+"  "+dueDrawnAuditCage[1].getId()
							 + " ARMY NO " +currentDo2.getArmyNo());
	*/			arrearsObject = new ArrearsObject();
				arrearsObject.setRegularPay(false);
			
				try {
					processor = getProcessor(currentAuditCage);
					if (i == auditCageList.size() -1  && processor instanceof AuditCageCancelProcessor ){
						 base.setToDate(null);
					}
					processor.setDueDrawnAuditCage(dueDrawnAuditCage);
					processor.setNewDo2(newDo2);
					processor.setBase(base);
					processor.setDo2Repo(do2Repo);
					/* for(int x=0;x<dueDrawnAuditCage.length-1;x++) {
						 if(x==0) {
							 System.out.println("due bp "+dueDrawnAuditCage[x].getBasicPay());
							 System.out.println("due da "+dueDrawnAuditCage[x].getDa());
						 }
					 }*/
					processor.setPsRepo(psRepo);
					processor.setAcRepo(acRepo);
					processor.setEmpRepo(empRepo);
					processor.setAcaRepo(acaRepo);
					processor.setNewDo2(newDo2);
					
					processor.setEsdRepo(esdRepo);
					processor.setRankRepo(rankRepo);
					processor.setGcRepo(gcRepo);
					processor.setMatrixRepo(matrixRepo);
					processor.setClassPayRepo(classPayRepo);
					processor.setApptRateRepo(apptRateRepo);
					processor.setAlwRateRepo(alwRateRepo);
					processor.setScRepo(scRepo);
					processor.setUnitRepo(unitRepo);
					processor.setRrRepo(rrRepo);
					processor.setLpcRepo(lpcRepo);
					processor.setArrRepo(arrRepo);
					processor.setLpcDepRepo(lpcDepRepo);
					processor.setBhutanSlabRepo(bhutanSlabRepo);
					processor.setEdRepo(edRepo);
					processor.setFsRepo(fsRepo);
					processor.setEeRepo(eeRepo);
					processor.setF16Repo(f16Repo);
					processor.setAdjCbRepo(adjCbRepo);
					processor.setFdRepo(fdRepo);
					processor.setTioRepo(tioRepo);
					processor.setJdbcTemplate(jdbcTemplate);
					processor.setRhLocationRepo(rhLocationRepo);
					
				 	if ((pendingDo2List.contains(currentDo2)
							&& !(currentAuditCage.getRecordStatus() + "").equals(Do2Manager.PENDING))
							||( currentDo2.getId()==newDo2.getId() 
									&& !(currentAuditCage.getRecordStatus() + "").equals(Do2Manager.PENDING))) {			
						// Audit cage does not exist for the do2.					
						if (!processor.isValid(currentDo2) || (Utils
								.isAbsent(currentDo2) || Utils.isOnDeputation(currentDo2))) {
								if(currentDo2.getAcceptedQe()==null){
								currentDo2.setAcceptedQe(Utils.getCurrentQuarter());	
								if(currentDo2.getTranscriptionType()!=null && currentDo2.getTranscriptionType().equals("T")) {
									currentDo2.setApproved(false);
								}
							}else{
								if(!currentDo2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
									currentDo2.setReviewQe(Utils.getCurrentQuarter());
								}
								if(currentDo2.getTranscriptionType()!=null && currentDo2.getTranscriptionType().equals("T")) {
									currentDo2.setApproved(false);
								}
							}
						if (i == auditCageList.size() - 1) {
							//setting to date of base to null if the last auditcage is becoming pending 
							//arrears object to date also to null if last auditcage is becoming pending
							base.setToDate(null);
						}
							acRepo.save(base);
							//Added on 30082012 - to make the record status of auditcage P
							//if the do2 is rejected in reaudit
							currentAuditCage.setRecordStatus(Do2Manager.PENDING.charAt(0)+"");
							auditCageList.remove(i);
							i--;
							continue;
						}
						
						currentAuditCage.setAbsentFlag(base.getAbsentFlag());
						currentAuditCage.setDeputationFlag(base.getDeputationFlag());
						currentAuditCage.setDeputationStatus(base.getDeputationStatus());
						currentAuditCage.setCancelled(base.getCancelled());
						Date d7 = new Date();
						i = processFreshAuditCage(i);
						currentAuditCage = auditCageList.get(i);
					
						Date d8 = new Date();
						//System.out.println("is fresh atcage  "+(d8.getTime()-d7.getTime()));
						
					} else {// AuditCage exists for the do2
						if (!cancelledAuditCage) {							
							currentAuditCage.setAbsentFlag(base.getAbsentFlag());
							currentAuditCage.setDeputationFlag(base.getDeputationFlag());
							currentAuditCage.setDeputationStatus(base.getDeputationStatus());
							i = processNotCancelledAuditCage(i);
						}
						
						processor.setBase(base);
						processor.setCurrentAuditCage(currentAuditCage);
					}				 	
				 	
					processAuditCage(i);
					acRepo.save(base);
					
					//*********lines added base becomes due having transport - made zero when stop all comes in the loop
			        if (newDo2.getDescription().equals("CANCEL") && Do2Manager.STOP_ALL.contains(newDo2.getColumnA())){
			        	if	(refDo2.getId() != currentDo2.getId() && Do2Manager.STOP_ALL.contains(currentDo2.getDescription())){
			        		dueDrawnAuditCage[0].setTransport(0);
			        	}
					}
					
					if(dueDrawnAuditCage[0].getTransport() !=0 && !dueDrawnAuditCage[0].getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
						
						if (!((currentDo2.getDescription().equals("TPTL") || currentDo2.getDescription().equals("TPTH"))
								&& currentDo2.getColumnA().equals("GRANTED"))) {			     
							dueDrawnAuditCage[0].setTransport(AuditCageHelper.getTransportRate(dueDrawnAuditCage[0], base));
						}
					}
					
					//condition for DANTAK
					 
					
					if (newDo2.getDescription().equals(Do2Manager.DA) && currentDo2.getFromDate().equals(newDo2.getFromDate()) && currentDo2.getId() != newDo2.getId() && currentAuditCage.getId() != 0 ){
				    	dueDrawnAuditCage[0].setDa(dueDrawnAuditCage[1].getDa());
				    		
				    } else {
				    	dueDrawnAuditCage[0].setDa(getFullMonthDa(dueDrawnAuditCage[0]));
				    }
					
					//code for revision of pmha,lra in reaudit in case of pbor
					//is absent on the crucial date can be added here		
	
				/*	if (!dueDrawnAuditCage[0].getFromDate().before(DateManager.getDPDate()) || dueDrawnAuditCage[0].getFromDate().equals(DateManager.getDPDate())){
						  if (dueDrawnAuditCage[0].getFromDate().before(DateManager.getSixthCpcDate())){
							  	if (employee.getSubstantiveRank() !=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
									   dueDrawnAuditCage[0].setDp(0);
							  	} else if (dueDrawnAuditCage[0].getBasicPay()==2440 || dueDrawnAuditCage[0].getBasicPay()==2850){
								   dueDrawnAuditCage[0].setDp(0);
							  	} else {
									dueDrawnAuditCage[0].setDp((int) Math.round(dueDrawnAuditCage[0].getBasicPay() * .5));
							  	}
							 
						  } else {
								if (employee.getSubstantiveRank() !=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
									   dueDrawnAuditCage[0].setDp(0);				
								} else if (dueDrawnAuditCage[0].getBasicPay()==5700){
									   dueDrawnAuditCage[0].setDp(0);
								} else if (dueDrawnAuditCage[0].getGradePay() !=null && dueDrawnAuditCage[0].getGradePay()!=0){
									   dueDrawnAuditCage[0].setDp(0);							  		
								} else {							  	  
								  	   dueDrawnAuditCage[0].setDp((int) Math.round(dueDrawnAuditCage[0].getBasicPay() * .5));
								}
						  }
										  
					}*/
	
					// above lines commented and following lines added due to implementation of sevencpc
					
					if (!dueDrawnAuditCage[0].getFromDate().before(DateManager.getDPDate()) || dueDrawnAuditCage[0].getFromDate().equals(DateManager.getDPDate())){
						  if (dueDrawnAuditCage[0].getFromDate().before(DateManager.getSixthCpcDate())){
							  	if (employee.getSubstantiveRank() !=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
									   dueDrawnAuditCage[0].setDp(0);
							  	} else if (dueDrawnAuditCage[0].getBasicPay()==2440 || dueDrawnAuditCage[0].getBasicPay()==2850){
								   dueDrawnAuditCage[0].setDp(0);
							  	} else {
							 // 		System.out.println ( " IN DP  " +dueDrawnAuditCage[0].getArmyNo()  +" " +dueDrawnAuditCage[0].getDp());
									dueDrawnAuditCage[0].setDp((int) Math.round(dueDrawnAuditCage[0].getBasicPay() * .5));
							  	}
							 
						  } else {
								if (employee.getSubstantiveRank() !=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
									   dueDrawnAuditCage[0].setDp(0);				
								} else if (dueDrawnAuditCage[0].getBasicPay()==5700 || dueDrawnAuditCage[0].getBasicPay()==8550 || dueDrawnAuditCage[0].getBasicPay()==11400){
									   dueDrawnAuditCage[0].setDp(0);
								} else if (dueDrawnAuditCage[0].getGradePay() !=null && dueDrawnAuditCage[0].getGradePay()!=0){
									   dueDrawnAuditCage[0].setDp(0);							  		
								} else if (dueDrawnAuditCage[0].getFromDate().before(DateManager.getSeventhCpcDate())) {
									  dueDrawnAuditCage[0].setDp((int) Math.round(dueDrawnAuditCage[0].getBasicPay() * .5));
								}
						  }
										  
					}
					
					if (Do2Manager.getRestorePayDescriptions().contains(newDo2.getDescription()) && currentDo2.getId() == newDo2.getId()) {
						  if (employee.getSubstantiveRank() != null	&& !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT) && 
							  !dueDrawnAuditCage[0].getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
							  if (dueDrawnAuditCage[0].getGradePay() !=0 ) {
								  dueDrawnAuditCage[0].setMsPay(getMsPay(newDo2));
							  } 
						  }
						    
						  if (employee.getSubstantiveRank() != null
								&& !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
							  dueDrawnAuditCage[0].setLra((int) Math.round(AuditCageHelper.getLraRate(dueDrawnAuditCage[0])));
							  dueDrawnAuditCage[0].setPmha((int) Math.round(AuditCageHelper.getPmhaRate(currentDo2, dueDrawnAuditCage[0])));
						  }
					}
					
					if (employee.getSubstantiveRank() != null && 
						!employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT) && 
						!dueDrawnAuditCage[0].getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
						if (dueDrawnAuditCage[0].getGradePay() != 0 && dueDrawnAuditCage[0].getMsPay() == 0) {
							dueDrawnAuditCage[0].setMsPay(getMsPay(dueDrawnAuditCage[0].getDo2()));
						}
					}
					//Making the lra and pmha zero in case of TA personnel who are departmental and others 
					if (employee.getRegularTaFlag() !=null && employee.getRegularTaFlag().equals("T")
						&& employee.getTaAppointmentCategory() !=null && 
						(employee.getTaAppointmentCategory().equals("D") || employee.getTaAppointmentCategory().equals("D"))){
						dueDrawnAuditCage[0].setLra(0);
						dueDrawnAuditCage[0].setPmha(0);
					}
	
					if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22))
						dueDrawnAuditCage[0].setMsPay(0);
					
					dueDrawnAuditCage[0].setQe(Utils.getCurrentQuarter());
					acRepo.save(dueDrawnAuditCage[0]);
				 
					  
					dueAuditCage = dueDrawnAuditCage[0];// for debugging
					dueDrawnAuditCage[0] = (AuditCage) Utils.getClonedEntity(dueDrawnAuditCage[0]);
					
					dueDrawnAuditCage[0].setArrestPayment(false);
	
					setDueDrawnLists();
		
				//	printAuditCageDetails("FOR LOOP");
					if (i == 0) {					
						base = auditCageList.get(0); 					
					} else {
				//		System.out.println(" XXXXXXXXXXXXXXXXXXXX ac processor i value "+i);
						base = getBaseAuditCage(currentAuditCage, i);
					}  
				} catch (Exception e) {
					Utils.handleException(e); 
				}				
				
			    if (!pendingAuditCage || validToPending) {			    	
					if (dueList == null)
						dueList = blankList;
					if (drawnList == null)
						drawnList = blankList;  
					if (validToPending || cancelledAuditCage){
						dueDrawnAuditCage[0].setFromDate(dueDrawnAuditCage[1].getFromDate());					
					}
					addDueDrawnElements(arrearsObject, dueDrawnAuditCage, dueList, drawnList);
			    }	
			    do2cnt=i;
			}
		} catch(Exception e) {
	//	  System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXX ");
		  e.printStackTrace();
		}
		/*absentDo2 = false;
		restoreDo2 = false;
		regularisationDo2 = false; 
		absentDo2Cancel = false;
		restoreDo2Cancel = false;
		regularisedToDate = null;
		restoreToDate = null;
		nextOslFromDate = null;
		nextOslToDate=null;
		nextOslAvaialble = false;
		nextOslAuditCage = null;
		reduceDo2 = false; 
		reduceFromDate = null;
		reduceToDate = null;
		reduceDo2Cancel = false;
		nextTfrDepDate =null;
		nextTfrDepAvailable =false;
		newRestoreDo2 =false;
		revdepDate=null;
		posoutDate=null;*/
		//setting to date to null of the auditcage. useful if lastauditcage become valid from pending
		AuditCage lastAuditCageInList = auditCageList.get(auditCageList.size()-1);
		if (lastAuditCageInList.getRecordStatus().equals("V")){
			 lastAuditCageInList.setToDate(null);
		}
		if (do2cnt != auditCageList.size() - 1){
			do2.setReason("SY:aborted due to null pointer");
			do2.setStatus("I");
		} else {
			if (arrearsObjectList.size() > 0){
				updateArrears(arrearsObjectList);		
			}	
		}
		if (newDo2.getTranscriptionType() != null && newDo2.getTranscriptionType().equals("T")){
			do2.setApproved(true);
		}
		//added on 30082012 - to generate embodi and disembodi
		if (newDo2.getDescription().equals("TATRNG")){
			AuditCageHelper.generateDo2sForTatrng(employee, newDo2, "EMBODI",newDo2.getFromDate());
			AuditCageHelper.generateDo2sForTatrng(employee, newDo2, "DISEMB",DateManager.getNextDate(newDo2.getToDate()));
		}

		//added on 30082012 to generate cancel for embodi and disembodi
		if (newDo2.getDescription().equals("CANCEL") && newDo2.getColumnA().equals("TATRNG")){
			AuditCageHelper.generateDo2sForTatrngCancel(employee, newDo2, "EMBODI",refDo2.getFromDate());
			AuditCageHelper.generateDo2sForTatrngCancel(employee, newDo2, "DISEMB",DateManager.getNextDate(refDo2.getToDate()));
		}		
	}

	public List<DueDrawnElement> getAuditCageOnlyDueDrawnList(AuditCage[] dueDrawnAuditCage) {

		List<DueDrawnElement> dueDrawnElementList = new ArrayList<DueDrawnElement>();

		AuditCage dueAuditCage = dueDrawnAuditCage[0];
		AuditCage drawnAuditCage = dueDrawnAuditCage[1];       
		if (dueAuditCage.getFromDate().after(lastDayOfPreviousQuarter)
				&& !closing)
			return dueDrawnElementList;
		if (dueAuditCage.getToDate() == null
				|| dueAuditCage.getToDate().after(lastDayOfPreviousQuarter)
				&& !closing) {
					dueAuditCage.setToDate(lastDayOfPreviousQuarter);
		}
		
	 	if (dueAuditCage.getBasicPay() != drawnAuditCage.getBasicPay()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.BASIC_PAY,
					dueAuditCage.getBasicPay(), drawnAuditCage.getBasicPay()));

		}

		if (dueAuditCage.getDp() != drawnAuditCage.getDp()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.DP,
					dueAuditCage.getDp(), drawnAuditCage.getDp()));
		}
		if (dueAuditCage.getGradePay() != drawnAuditCage.getGradePay()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.GRADE_PAY,
					dueAuditCage.getGradePay(), drawnAuditCage.getGradePay()));
		}
		if (dueAuditCage.getMsPay() != drawnAuditCage.getMsPay()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.MS_PAY,
					dueAuditCage.getMsPay(), drawnAuditCage.getMsPay()));
		}
		if (dueAuditCage.getXGroupPay() != drawnAuditCage.getXGroupPay()) {
			dueDrawnElementList
					.add(getDueDrawnElement(Do2Manager.X_GROUP_PAY,
							dueAuditCage.getXGroupPay(), drawnAuditCage
									.getXGroupPay()));
		}
		if (dueAuditCage.getClassPay() != drawnAuditCage.getClassPay()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.CLASS_PAY,
					dueAuditCage.getClassPay(), drawnAuditCage.getClassPay()));
		}
		if (dueAuditCage.getPmha() != drawnAuditCage.getPmha()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.PMHAL,
					dueAuditCage.getPmha(), drawnAuditCage.getPmha()));
		}
		if (dueAuditCage.getLra() != drawnAuditCage.getLra()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.LRA,
					dueAuditCage.getLra(), drawnAuditCage.getLra()));
		}
		if (dueAuditCage.getTransport() != drawnAuditCage.getTransport()) {
			dueDrawnElementList
					.add(getDueDrawnElement(Do2Manager.TPTL, dueAuditCage
							.getTransport(), drawnAuditCage.getTransport()));
		}
		if (dueAuditCage.getDa() != drawnAuditCage.getDa()) {
			dueDrawnElementList.add(getDueDrawnElement(Do2Manager.DA,
					dueAuditCage.getDa(), drawnAuditCage.getDa()));
			
		}

		return dueDrawnElementList;
	}

	private DueDrawnElement getDueDrawnElement(String payCode, double due,
			double drawn) {
		DueDrawnElement element = new DueDrawnElement();
		element.setPayCode(Utils.getPayCode(payCode));
		element.setDue(due);
		element.setDrawn(drawn);
		return element;
	}

	public void updateArrears(List<ArrearsObject> arrearsObjectList) {
		HashMap<PayCode, Double> roundOffMap = new HashMap<PayCode, Double>();
		AuditCage dueAuditCage = null;
		PayCode payCode = null;
		double monthCount = 0.0;
		double dueAmount = 0.0;
		double drawnAmount = 0.0;
		Date fromDate = null;
		Date toDate = null; 
		ArrearsObject arrearsObject = null;		
		
		//lines added on 02072012
		if (!closing){
			for (int i = 0; i < arrearsObjectList.size(); i++) {
				arrearsObject = arrearsObjectList.get(i);
				dueAuditCage = arrearsObject.getDueAuditCage();
				if (i < arrearsObjectList.size() -1){
					
					if (dueAuditCage.getFromDate().equals(arrearsObjectList.get(i+1).getDueAuditCage().getFromDate())){
						  dueAuditCage.setToDate(dueAuditCage.getFromDate());
					} else {
						dueAuditCage.setToDate(DateManager.getPreviousDate(arrearsObjectList.get(i+1).getDueAuditCage().getFromDate()));
					}
						 
				} else {//LINES ADDED TO SET THE LAST ARRREARSOBJECT LIST TO DATE TO NULL
	                if (do2cnt != auditCageList.size() - 1){
	                	;
	                } else {
					   dueAuditCage.setToDate(null);
	                }
				}
			}
		}
		
		for (int i = 0; i < arrearsObjectList.size(); i++) {
			arrearsObject = arrearsObjectList.get(i);
			dueAuditCage = arrearsObject.getDueAuditCage();			
			
			if (i < arrearsObjectList.size() -1){
				
				if (dueAuditCage.getFromDate().equals(arrearsObjectList.get(i+1).getDueAuditCage().getFromDate()))
					continue;
				
			}
			List<DueDrawnElement> dueDrawnElementList = arrearsObject.getDueDrawnElementList();

			dueAuditCage = arrearsObject.getDueAuditCage();
	
			fromDate = dueAuditCage.getFromDate(); 			
			//CODE MODIFIED TO INITIALIZE TO DATE (MODIFIED ON 09032012)
			if (!closing){
				if (dueAuditCage.getToDate()==null){
					toDate= lastDayOfPreviousQuarter;
				} else if (dueAuditCage.getToDate().after(lastDayOfPreviousQuarter)){
					toDate= lastDayOfPreviousQuarter;
				} else {
					toDate= dueAuditCage.getToDate();
				}
			} else {
				toDate = dueAuditCage.getToDate();
			}
			monthCount = DateManager.getNoOfMonths(fromDate, toDate);

			for (DueDrawnElement element : dueDrawnElementList) {

				payCode = element.getPayCode();
				dueAmount = element.getDue() * monthCount;
				drawnAmount = element.getDrawn() * monthCount;
				double fullValue = dueAmount - drawnAmount;

				if (fullValue == 0.0)
					continue;
				Arrears arrears = new Arrears();
				if (!closing) {
					arrears.setDo2(newDo2);
					arrears.setDo2ReverseArrears(getReverseArrearsDo2(newDo2));
					arrears.setDue(element.getDue());
					arrears.setDrawn(element.getDrawn());
				}
				arrears.setPayCode(payCode);
				arrears.setFromDate(fromDate);
				arrears.setToDate(toDate);
				arrears.setRegularPay(arrearsObject.regularPay);
				arrears.setEmployee(dueAuditCage.getEmployee());
				arrears.setCreatedAt(DateManager.getCurrentTimestamp());
				arrears.setPaid(false);
				Double amount = roundOffMap.get(payCode);

				if (amount == null) {
					amount = new Double(0.0);
					roundOffMap.put(payCode, amount);
				}

				amount += new BigDecimal(fullValue - (int) fullValue).setScale(
						2, BigDecimal.ROUND_HALF_UP).doubleValue();

				roundOffMap.put(payCode, amount);
				arrears.setAmount((double) ((int) fullValue));
				arrears.setRoundOffAmount(false);
				arrears.setArrestPayment(dueAuditCage.getArrestPayment());
				arrears.setQe(Utils.getCurrentQuarter());
				arrRepo.save(arrears);
			}
		}
		fromDate = arrearsObjectList.get(0).getDueAuditCage().getFromDate();
		toDate = arrearsObjectList.get(arrearsObjectList.size() - 1).getDueAuditCage().getToDate();
		if (!closing) {
			if (toDate == null)
				toDate = lastDayOfPreviousQuarter;
		}
		Set<PayCode> payCodes = roundOffMap.keySet();
		for (PayCode roundOffPayCode : payCodes) {
			double amount = Math.round(roundOffMap.get(roundOffPayCode));
			amount = (double) Math.round(amount);
			if (amount == 0.0)
				continue;

			Arrears arrears = new Arrears();
			if (!closing) {
				arrears.setDo2(newDo2);
				arrears.setDo2ReverseArrears(getReverseArrearsDo2(newDo2));
			}
			arrears.setPayCode(roundOffPayCode);
			arrears.setFromDate(fromDate);
			arrears.setToDate(toDate);
			arrears.setAmount(amount);
			arrears.setArrestPayment(false);
			arrears.setRegularPay(arrearsObject.regularPay);
			arrears.setEmployee(dueAuditCage.getEmployee());
			arrears.setPaid(false);
			arrears.setRoundOffAmount(true);
			arrears.setQe(Utils.getCurrentQuarter());
			arrRepo.save(arrears);
		}
	}

	HashMap<String, AuditCageAllowance> drawnAllowanceMap = new HashMap<String, AuditCageAllowance>();
	HashMap<String, AuditCageAllowance> dueAllowanceMap = new HashMap<String, AuditCageAllowance>();

	private List<DueDrawnElement> getDueDrawnAllowanceList(
			List<AuditCageAllowance> dueAllowanceList,
			List<AuditCageAllowance> drawnAllowanceList) {
		List<DueDrawnElement> dueDrawnElementList = new ArrayList<DueDrawnElement>();		
		if (dueAuditCage.getFromDate().after(lastDayOfPreviousQuarter)
				&& !closing)
			return dueDrawnElementList;
		drawnAllowanceMap.clear();
		dueAllowanceMap.clear();

		for (AuditCageAllowance drawnAllowance : drawnAllowanceList) {
			drawnAllowanceMap.put(drawnAllowance.getPayCode().getPayCode(),
					drawnAllowance);
		}

		for (AuditCageAllowance dueAllowance : dueAllowanceList) {
			AuditCageAllowance drawnAllowance = drawnAllowanceMap
					.get(dueAllowance.getPayCode().getPayCode());
			if ((drawnAllowance == null)
					|| (dueAllowance.getAmount() != drawnAllowance.getAmount())) {
				DueDrawnElement element = new DueDrawnElement();
				element.setPayCode(dueAllowance.getPayCode());
				element.setDue(dueAllowance.getAmount());
				element.setDrawn(drawnAllowance == null ? 0 : drawnAllowance
						.getAmount());
				dueDrawnElementList.add(element);
			}
		}

		for (AuditCageAllowance dueAllowance : dueAllowanceList) {
			dueAllowanceMap.put(dueAllowance.getPayCode().getPayCode(),
					dueAllowance);
		}

		for (AuditCageAllowance drawnAllowance : drawnAllowanceList) {
			AuditCageAllowance dueAllowance = dueAllowanceMap
					.get(drawnAllowance.getPayCode().getPayCode());
			if (dueAllowance == null) {
				DueDrawnElement element = new DueDrawnElement();
				element.setPayCode(drawnAllowance.getPayCode());
				element.setDrawn(drawnAllowance.getAmount());
				element.setDue(dueAllowance == null ? 0 : dueAllowance
						.getAmount());
				dueDrawnElementList.add(element);
			}
		}
		return dueDrawnElementList;
	}

	public void addDueDrawnElements(ArrearsObject arrearsObject,
			AuditCage[] dueDrawnAuditCage, List<AuditCageAllowance> dueList,
			List<AuditCageAllowance> drawnList) {
		arrearsObject.setDueAuditCage(dueDrawnAuditCage[0]);	
		
	/*	System.out.println("ARMY NO " +dueDrawnAuditCage[0].getArmyNo()  +"   "+ dueDrawnAuditCage[0].getDo2().getDescription()
				+ "  "
				+ dateFormat.format(dueDrawnAuditCage[0].getFromDate())
				+ "  "
				+ (dueDrawnAuditCage[0].getToDate() == null ? null : dateFormat.format(dueDrawnAuditCage[0].getToDate())
						+ " "
						+ dueDrawnAuditCage[0].getId()  
						+ " " 
						+ dueDrawnAuditCage[1].getId()  
						+ " "
						+ dueDrawnAuditCage[1].getArmyNo()  
						+ " " 
						+ dueDrawnAuditCage[1].getDo2().getDescription()
						+ " "
						+ dueDrawnAuditCage[0].getAbsentFlag()
						+ " "
						+ dueDrawnAuditCage[1].getAbsentFlag()
						+ " "
						+ dueDrawnAuditCage[0].getCancelled()
						+ " "
						+ dueDrawnAuditCage[1].getCancelled()
						+ " "
						+ dueDrawnAuditCage[0].getRecordStatus()
						+ " "
						+ dueDrawnAuditCage[1].getRecordStatus()
						+ " "
						+ dueDrawnAuditCage[0].getGradePay()
						+ "  "
						+ dueDrawnAuditCage[1].getGradePay()));*/
		
		
		dueAuditCage = dueDrawnAuditCage[0];
		if (!closing) {
			if (dueDrawnAuditCage[0].getAbsentFlag() == true){
				setZeroValues(dueDrawnAuditCage[0]);
				dueList=blankList;
			}			
			if (dueDrawnAuditCage[1].getAbsentFlag() == true){
				setZeroValues(dueDrawnAuditCage[1]);
				drawnList=blankList;
			}			
		}
		List<DueDrawnElement> dueDrawnElementList = getAuditCageOnlyDueDrawnList(dueDrawnAuditCage);
		dueDrawnElementList.addAll(getDueDrawnAllowanceList(dueList, drawnList));
		arrearsObject.setDueDrawnElementList(dueDrawnElementList);
		arrearsObjectList.add(arrearsObject);
		
	}

	// Picks up the drawn audit cage. Useful to skip the audit cages with
	// same fromDate
	protected AuditCage getDrawnAuditCage(AuditCage processingAuditCage, int processingAuditCageIndex) {
		
		Do2 processDo2 = processingAuditCage.getDo2();
		
		if (pendingDo2List.contains(processingAuditCage.getDo2()) || 
			processingAuditCage.getDo2().getId()== newDo2.getId()) {
			
			for (int i = processingAuditCageIndex; i >= 1; i--) {

				if (auditCageList.get(i - 1).getCancelled() == true || 
					auditCageList.get(i - 1).getRecordStatus().equals("P") ||
					(auditCageList.get(i-1).getDo2().getId()==newDo2.getId() && 
					 pendingDo2List.contains(processingAuditCage.getDo2()))	|| 
					 pendingDo2List.contains(auditCageList.get(i - 1).getDo2())) {
					// to be reviewed
					continue;
				} else {
					return auditCageList.get(i - 1);
				}
			}
		}
		return processingAuditCage;
	}

	protected double getDoubleDecimalValue(double value) {
		return new BigDecimal(value).setScale(2,RoundingMode.HALF_UP)
				.doubleValue();
	}

	/*public int getFullMonthDa(AuditCage dueAuditCage) {
		
		employee = Utils.getCurrentEmployee(newDo2.getArmyNo());

		if (employee.getRegularTaFlag() == Do2Manager.TERRITORIAL_ARMY
				&& employee.getTaAppointmentCategory() == Do2Manager.STOP_DA_TA_CATEGORY) {
			return 0;
		}
		//*************************** to be reviewed
		if ((employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
			return 0;
		}
		if ((employee.getSubstantiveRank() != null
				&& !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))){
				if(!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
					if(dueAuditCage.getBasicPay() == 5700)
						return 0;
				}
				if(!dueAuditCage.getFromDate().after(DateManager.getSixthCpcDate())){
					if(dueAuditCage.getBasicPay() == 2440 || dueAuditCage.getBasicPay() == 2850)
						return 0;
			}
			
		}
		if(dueAuditCage.getDeputationStatus() !=null && dueAuditCage.getDeputationStatus() =='T'){
			return 0;
		}
		//condition added for DANTAK
		if(dueAuditCage.getDeputationStatus() !=null && dueAuditCage.getDeputationStatus() =='K'){
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
		int transport = dueAuditCage.getTransport() == null ? 0 : dueAuditCage
				.getTransport();
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			
			if (!dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					&& dp != 0 && dueAuditCage.getGradePay()==0) {
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
				variableComponents = gradePay + msPay + xGroupPay + transport;
			}
			da += (dueAuditCage.getBasicPay() + variableComponents) * daRate;
		}
		
		return ((int) Math.round(da));
	}*/

	// above method commended and amended as given below due to sevencpc
	
public int getFullMonthDa(AuditCage dueAuditCage) {
		
		employee = Utils.getCurrentEmployee(newDo2.getArmyNo());

		if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY+"")
				&& employee.getTaAppointmentCategory().equals(Do2Manager.STOP_DA_TA_CATEGORY+"")) {
			return 0;
		}
		//*************************** to be reviewed
		if ((employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
			return 0;
		}
		if ((employee.getSubstantiveRank() != null
				&& !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))){
				if(!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
					if(dueAuditCage.getBasicPay() == 5700)
						return 0;
				}
				if(!dueAuditCage.getFromDate().after(DateManager.getSixthCpcDate())){
					if(dueAuditCage.getBasicPay() == 2440 || dueAuditCage.getBasicPay() == 2850)
						return 0;
				}
				if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					if(dueAuditCage.getBasicPay() == 14600) {
						return 0;
					}
				}
			
		}
		if(dueAuditCage.getDeputationStatus() !=null && dueAuditCage.getDeputationStatus().equals("T")){
			return 0;
		}
		//condition added for DANTAK
		if(dueAuditCage.getDeputationStatus() !=null && dueAuditCage.getDeputationStatus().equals("K")){
			return 0;
		}
				
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;
		double tptlda = 0.0;

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
		int transport = dueAuditCage.getTransport() == null ? 0 : dueAuditCage
				.getTransport();
		double daRate = 0.0;
		double tptldaRate = 1.25;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			/*if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				tptldaRate = allowance.getRateAmount();
			}
			*/
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					&& dp != 0 && dueAuditCage.getGradePay()==0) {
				daRate = allowance.getRateAmount();
			}
			
			if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())
					&& dueAuditCage.getPayLevel()!=null && dueAuditCage.getPayLevel().equals("0")
					&& dueAuditCage.getGradePay()!=null && dueAuditCage.getGradePay()>0
					&& !dueAuditCage.getFromDate().after(DateManager.getSeventhCpcNotificationDate())) {
				daRate = allowance.getRateAmount();
			}
			
			if (dueAuditCage.getFromDate().after(DateManager.getSeventhCpcNotificationDate())) {
				daRate = allowance.getRatePercentage();
			}
			
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(
							DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSeventhCpcDate())){
				variableComponents = gradePay + msPay + xGroupPay + transport;
			} else {
				
				variableComponents = gradePay + msPay + xGroupPay;
				
			}
			
			//added on 08.02.2017
			if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				tptldaRate=daRate;
			}
			
			/*System.out.println (  " DA " +da);
			System.out.println ( " VARIABLE COMPONENTS  " +variableComponents  +" FROM DATE " +dueAuditCage.getFromDate()  +"   "+daRate  +"  "+tptldaRate);
	*/		if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				//System.out.println ( " VARIABLE COMPONENTS1  " +variableComponents  +" FROM DATE " +dueAuditCage.getFromDate()  +"   "+daRate  +"  "+tptldaRate);
				da += (dueAuditCage.getBasicPay() + variableComponents) * daRate;
			//	System.out.println (  " DA1 " +da);
			} else {
				da += (((dueAuditCage.getBasicPay() + variableComponents) * daRate)  + (transport * tptldaRate));
				/*System.out.println ( " Basic Pay " + dueAuditCage.getBasicPay()   +" vc " +variableComponents   +  " tptl " +transport);
				System.out.println (  " DA2 " +da);*/
			}
			
		} 
			//  Additional check to be released in April 2026 if needed
			 if ((allowanceList.size()==0) && dueAuditCage.getFromDate().after(Utils.getLastDayOfCurrentQuarter()))  {
					variableComponents = gradePay + msPay + xGroupPay;
				
				 List<AllowanceRate> allowanceRateList = alwRateRepo.findByAlwNameToDateNull("DA");
					daRate = allowanceRateList.get(0).getRatePercentage();
					
					if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						tptldaRate=daRate;
					}
				 da += (((dueAuditCage.getBasicPay() + variableComponents) * daRate)  + (transport * tptldaRate));
			 }
					
		
		return ((int) Math.round(da));
	}
	

public int getFullMonthDaBhutan(AuditCage dueAuditCage) {
	
	employee = Utils.getCurrentEmployee(newDo2.getArmyNo());

	if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY+"")
			&& employee.getTaAppointmentCategory().equals(Do2Manager.STOP_DA_TA_CATEGORY+"")) {
		return 0;
	}
	//*************************** to be reviewed
	if ((employee.getSubstantiveRank() != null
			&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
		return 0;
	}
	if ((employee.getSubstantiveRank() != null
			&& !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))){
			if(!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
				if(dueAuditCage.getBasicPay() == 5700)
					return 0;
			}
			if(!dueAuditCage.getFromDate().after(DateManager.getSixthCpcDate())){
				if(dueAuditCage.getBasicPay() == 2440 || dueAuditCage.getBasicPay() == 2850)
					return 0;
			}
			if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				if(dueAuditCage.getBasicPay() == 14600) {
					return 0;
				}
			}		
	}
 	double da = 0.0;

	int variableComponents = 0;
	int xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);
	int gradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage
			.getOldGradePay();
 	int msPay = Utils.get6cpcMsPay(dueAuditCage);
	int transport = dueAuditCage.getTransport() == null ? 0 : dueAuditCage
			.getTransport();
	double daRate = 1.25;
  
	variableComponents = gradePay + msPay + xGroupPay + transport;
	da += ((dueAuditCage.getOldBasicPay() + variableComponents) * daRate);
		
		
	return ((int) Math.round(da));
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
            if (auditCage.getDo2()!=null && auditCage.getDo2().getEmployee()!=null
                    && auditCage.getDo2().getEmployee().getRegularTaFlag().equals("T")
                    && auditCage.getDo2().getDescription()!=null
                    && auditCage.getDo2().getDescription().equals("TAINCR")
                    && auditCage.getDo2().getStatus()!=null
                    && auditCage.getDo2().getStatus().equals(Do2Manager.VALID)
                    && auditCage.getDo2().getFromDate()!=null
                    && auditCage.getDo2().getFromDate().equals(DateManager.getSeventhCpcDate())) {  // added on 16.06.2025
                auditCage.setPriority(8);
            }
            if (Utils.getPao().getPaoCode().equals("57")) {  // multiple promotions Within 6-8 months in 6thCPC - Only in PAO/Signals   -- // added on 31.10.2025
                if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
                    continue;
                }
                if (!auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
                    continue;
                }
                // Resolve employee safely: prefer auditCage.getEmployee(), fall back to do2's employee
                Employee acEmployee = auditCage.getEmployee() != null
                        ? auditCage.getEmployee()
                        : (auditCage.getDo2() != null ? auditCage.getDo2().getEmployee() : null);
                if (acEmployee == null) {
                    continue;
                }
                Calendar cal = new GregorianCalendar();
                cal.setTime(auditCage.getFromDate());
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DATE);
                if (month==Calendar.JULY && day==1) {
                    if (auditCage.getDo2().getDescription().equals("PROFIX")) {
                        if (acEmployee.getGroupCategory()!=null
                                && (acEmployee.getGroupCategory().getSoldierCategory().equals("FOS")
                                || acEmployee.getGroupCategory().getSoldierCategory().equals("JENE"))) {
                            System.out.println("555555555555");
                            List<Do2> promotDo2List = do2Repo.findPromotOnProfixDateForSignals(acEmployee.getId(), auditCage.getFromDate());
                            System.out.println("666666666666");
                            if (promotDo2List!=null && promotDo2List.size()>0) {
                                auditCage.setPriority(59); // profix priority being set before promot priority
                                System.out.println(".....222......priority refixing for Army No."+acEmployee.getArmyNo() + " for description "+auditCage.getDo2().getDescription());
                            }
                        }
                    }

                    if (auditCage.getDo2().getDescription().equals("REMUST")) {             // Signals -- added on 05.12.2025
                        System.out.println("..................signals........REMUST....");
                        if (acEmployee.getGroupCategory()!=null
                                && (acEmployee.getGroupCategory().getSoldierCategory().equals("FOS")
                                || acEmployee.getGroupCategory().getSoldierCategory().equals("JENE"))) {
                            System.out.println("55555555555511111111");
                            List<Do2> remustDo2List = do2Repo.findRemustOnProfixPromotDateForSignals(acEmployee.getId(), auditCage.getFromDate());
                            System.out.println("666666666666111111111");
                            if (remustDo2List!=null && remustDo2List.size()>0) {
                                auditCage.setPriority(59); // profix priority being set before promot priority
                                System.out.println(".....222.33333333333.....priority refixing for Army No."+acEmployee.getArmyNo() + " for description "+auditCage.getDo2().getDescription());
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(auditCageList);
    }

	static private Do2 getReverseArrearsDo2(Do2 do2) {
		if (!do2.getDescription().equals("CANCEL")) {
			return do2;
		}
		return Utils.getReferenceDo2(do2);
	}

	private SpecialProcessor getProcessor(AuditCage auditCage) {
		cancelledAuditCage = false;
		
		if (cancelAuditCageList.contains(auditCage)) {
			cancelledAuditCage = true;
			return new AuditCageCancelProcessor();
		}
		return new AuditCageNormalProcessor();
	}

	private AuditCage getBaseAuditCage(AuditCage currentAuditCage, int i) {

	//	System.out.println("............... auditcage list get " + auditCageList.get(i).getId()+ "  description " +auditCageList.get(i).getDo2().getDescription());
		while (auditCageList.get(i).getCancelled()!=null && auditCageList.get(i).getCancelled() == true
				|| auditCageList.get(i).getRecordStatus().equals("P")) {
			i--;
		}

		return auditCageList.get(i);
	}

	private AuditCage getAuditCageForInsertDo2(Do2 do2) {
		if (do2.getDescription().equals(Do2Manager.CANCEL)) { 
			AuditCage atCage = Utils.getAuditCage(Utils.getReferenceDo2(do2));
			newDo2AuditCage = atCage;
		} else {	
			newDo2AuditCage = Utils.getAuditCage(newDo2);
			if(newDo2AuditCage == null ){
				newDo2AuditCage = new AuditCage();
				if (Do2Manager.getValidNextDayDo2s().contains(newDo2.getDescription())) {
					newDo2AuditCage.setFromDate(DateManager.getNextDate(newDo2
							.getFromDate()));
				} else {
					newDo2AuditCage.setFromDate(newDo2.getFromDate());
				}
				newDo2AuditCage.setDo2(do2);
			}
		}
		return newDo2AuditCage;
	}

	protected void addPendingDo2s(List<AuditCage> auditCages) {
		Date fromDate = null;
		cancelAuditCageList = new ArrayList<AuditCage>();		
		fromDate=auditCageList.get(0).getFromDate();
		
		if (newDo2.getDescription().equals("OPTCPC") && newDo2.getColumnB().equals("SEVENPC")) {
			if (!newDo2.getColumnA().equals("INCR")) {
				Rank sevenCpcEffectiveDateRank = Utils.getPborRank(newDo2.getArmyNo(), DateManager.getPreviousDate(DateManager.getSeventhCpcDate()), "PAYFIX");
				Rank sevenCpcOptionDateRank = Utils.getPborRank(newDo2.getArmyNo(), DateManager.getNextDate(newDo2.getFromDate()), "PAYFIX");
				if (sevenCpcEffectiveDateRank!=null && sevenCpcOptionDateRank!=null 
						&& (Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode())>=Integer.parseInt(sevenCpcOptionDateRank.getRankCode()))) {
					 ;
		 		} else {
		  			Do2 fix7pcDo2 = Utils.getpayfixDo2(newDo2);			
		  			if (fix7pcDo2!=null) {
		  				AuditCage fix7pcAuditCage = Utils.getAuditCage(fix7pcDo2);
		  				fix7pcAuditCage.setPayLevel("0");
		  				cancelAuditCageList.add(fix7pcAuditCage);	 				
		  			
		  			 	fix7pcDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
		  			 	pendingDo2List.add(fix7pcDo2);	
		  			 	Date date1=null,date2=null;
		  			 	try {
							date1=DateManager.getDate("2016-07-01");
							date2=DateManager.getDate("2017-07-01");
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
		  			 	
		  			 	
		  			 	List<Do2> incrDo2List =do2Repo.findByDescFromDateEqual(newDo2.getEmployee().getId(),Do2Manager.VALID,"INCR", date1) ;
		  			 		 
		 				try {		
		 					if (incrDo2List==null || (incrDo2List!=null && incrDo2List.size()==0)) {
		 						AuditCageHelper.insertIncrmentDo2s(newDo2, DateManager.getDate("2016-07-01"), newDo2.getDescription());;
		 					}		
		 					
		 					incrDo2List=null;
		 					
		 					incrDo2List =do2Repo.findByDescFromDateEqual(newDo2.getEmployee().getId(),Do2Manager.VALID,"INCR", date2) ;
		 					
		 					if (incrDo2List==null || (incrDo2List!=null && incrDo2List.size()==0)) {
		 						AuditCageHelper.insertIncrmentDo2s(newDo2, DateManager.getDate("2017-07-01"), newDo2.getDescription());;
		 					}						
		 				} catch (Exception e) {
		 					Utils.handleException(e);
		 				} 			 	
		  			  
 		  			}
		  		} 
			} else {
				Do2 fix7pcDo2 = Utils.getpayfixDo2(newDo2);			
	  			if (fix7pcDo2!=null) {
	  				AuditCage fix7pcAuditCage = Utils.getAuditCage(fix7pcDo2);
	  				fix7pcAuditCage.setPayLevel("0");
	  				cancelAuditCageList.add(fix7pcAuditCage);	 				
	  			
	  			 	fix7pcDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
	  			 	pendingDo2List.add(fix7pcDo2);	
	  			 	Date date1=null,date2=null;
	  			 	
	  			 	try {
						date2=DateManager.getDate("2017-07-01");
						date1=DateManager.getDate("2016-07-01");
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	  			 	
	  			 	List<Do2> incrDo2List = do2Repo.findByDescFromDateEqual(newDo2.getEmployee().getId(),Do2Manager.VALID,"INCR", date1) ;
	 				try {		
	 					if (incrDo2List==null || (incrDo2List!=null && incrDo2List.size()==0)) {
	 						AuditCageHelper.insertIncrmentDo2s(newDo2, DateManager.getDate("2016-07-01"), newDo2.getDescription());;
	 					}		
	 					
	 					incrDo2List=null;
	 					
	 					incrDo2List = do2Repo.findByDescFromDateEqual(newDo2.getEmployee().getId(),Do2Manager.VALID,"INCR", date2) ;
	 					
	 					if (incrDo2List==null || (incrDo2List!=null && incrDo2List.size()==0)) {
	 						AuditCageHelper.insertIncrmentDo2s(newDo2, DateManager.getDate("2017-07-01"), newDo2.getDescription());;
	 					}						
	 				} catch (Exception e) {
	 					Utils.handleException(e);
	 				} 			 	
	  			 	 
	  			}
			}
		}
  		
  		if (newDo2.getDescription().equals(Do2Manager.CANCEL) && newDo2.getColumnA().equals("OPTCPC")) {
 			Do2 fix7pcDo2 = Utils.getpayfixCancelledDo2(newDo2);
 			if (fix7pcDo2!=null && !fix7pcDo2.getDescription().equals("OPTCPC")) {
 				fix7pcDo2.setStatus(Do2Manager.PENDING);
 				pendingDo2List.add(fix7pcDo2);	
  			  
 			}
		}
	
		if (newDo2.getDescription().equals("SIXCPC")) {
			Do2 payfixDo2 = Utils.getpayfixDo2(newDo2);
			AuditCageHelper.generateIncrCancelDo2s(payfixDo2);
			//There will be no profix for TA ,add to cancel auditcage only regular army
			if (employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("R")){
				if (newDo2.getColumnA().equals("PROMOT")){
		 			 Do2 profixDo2 = Utils.getprofixDo2ForPromot(newDo2);
				 		
					 if (profixDo2 !=null) {
						 cancelAuditCageList.add(Utils.getAuditCage(profixDo2));
					 }
	 			}
			}
			cancelIncrSixcpc(newDo2);
			AuditCageHelper.generateIncrIn5pcScales(newDo2);
			AuditCageHelper.generatesixcpcIncrment(newDo2);			
		}
				
		if (newDo2.getDescription().equals(Do2Manager.CANCEL) && newDo2.getColumnA().equals("SIXCPC")) {
			Do2 payfixDo2 = Utils.getpayfixCancelledDo2(newDo2);
			if (payfixDo2!=null) {
				AuditCageHelper.regenerateCancelledIncrmentDo2s(payfixDo2);	
			}
			
			if(refDo2.getColumnA() !=null && refDo2.getColumnA().equals("PROMOT")){
	 			 Do2 profixDo2 = Utils.getprofixDo2CancelledForSixcpc(newDo2);
	 			 if(profixDo2 !=null){
	 				 AuditCageHelper.regenerateCancelledProfixDo2s(profixDo2);
	 			 }
			}
			//There will be no incr for TA ,generation cancel do2 only for regular army
			if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("R")){
				cancelIncrSixcpc(newDo2);
				AuditCageHelper.generateSixcpcCancel(newDo2);
			}
		}


		if (newDo2.getDescription().equals("FIX7PC")) {
			Date jul2016IncrDate = null;
			try {
				jul2016IncrDate = DateManager.getDate(2016,7,1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			if (auditCages.size()>0) {
				if (auditCages.get(0).getEmployee().getRegularTaFlag().equals("T")) {
					;
				} else 	if (auditCages.get(0).getEmployee().getRegularTaFlag().equals("R")) {					
					for (int i=0; i<auditCages.size(); i++) {	
						Do2 promotMacpDo2  = null, attsMusterDo2 = null;
						if (Do2Manager.getIncrCancelDescription().contains(auditCages.get(i).getDo2().getDescription()) 				
								&& !auditCages.get(i).getFromDate().before(DateManager.getSeventhCpcDate())) {
							promotMacpDo2 = auditCages.get(i).getDo2();	
							convertDo2(promotMacpDo2);
						 }
						 if (Do2Manager.getMusterAttesttDescription().contains(auditCages.get(i).getDo2().getDescription()) 				
								&& !auditCages.get(i).getFromDate().before(DateManager.getSeventhCpcDate())) {
							attsMusterDo2 = auditCages.get(i).getDo2();		
						 }
						 if (auditCages.get(i).getDo2().getDescription().equals("INCR") && 
							 auditCages.get(i).getDo2().getFromDate().after(jul2016IncrDate)){
							 auditCages.get(i).getDo2().setStatus(Do2Manager.DO2_CANCEL_STATUS);
						 }
						 if (promotMacpDo2!=null) {		
							if (Do2Manager.getSubstantiveRankDescription().contains(promotMacpDo2.getColumnA())) {
								List<Do2> do2List = Utils.getPaidActingDo2(promotMacpDo2);
								if (do2List.size()==0) {
									if (!Utils.isAcpBeforeCurrentDo2(promotMacpDo2)) {
										if (employee.getRegularTaFlag().equals("R")) {
											Utils.generateIncrDo2(promotMacpDo2, promotMacpDo2.getFromDate());
										}
								 	}
								}
							} else if (promotMacpDo2.getColumnA().equals("NBSUB")) {
								if (employee.getRegularTaFlag().equals("R")) {
								 	Utils.generateIncrDo2(promotMacpDo2, promotMacpDo2.getFromDate());
								}							
							} else {
								if (employee.getRegularTaFlag().equals("R")) {
								 	Utils.generateIncrDo2(promotMacpDo2, promotMacpDo2.getFromDate());
								}	
							}
						}					
						if (attsMusterDo2!=null && !attsMusterDo2.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
							AuditCageHelper.insertIncrementDos2ForAttestMusterIn7cpc(attsMusterDo2, 
									attsMusterDo2.getEmployee().getEnrolmentDate(), attsMusterDo2.getDescription());
						}
					}
				}
			}			
		}


		
		if (newDo2.getDescription().equals(Do2Manager.CANCEL) &&
           (newDo2.getColumnA().equals("ATTSAM") || newDo2.getColumnA().equals("ATTSBM") ||
            newDo2.getColumnA().equals("MUSTER") || newDo2.getColumnA().equals("OWNMST") ||
            newDo2.getColumnA().equals("REMUST"))){
             
			List<Do2> mergedTradeDo2List =do2Repo.findByDescStatus(employee.getId(),Do2Manager.VALID,"TDMERG");
					 

             if (mergedTradeDo2List.size()>0 && newDo2.getFromDate().before(mergedTradeDo2List.get(0).getFromDate())) {
                 mergedTradeDo2List.get(0).setStatus("C");
                 cancelAuditCageList.add(Utils.getAuditCage(mergedTradeDo2List.get(0)));
             }
		 }

		if (newDo2.getDescription().equals(Do2Manager.CANCEL) && Do2Manager.getIncrCancelDescription().contains(newDo2.getColumnA())) {
			AuditCage cancelAuditCage = Utils.getAuditCage(refDo2);
			cancelAuditCageList.add(cancelAuditCage);
			//cancel of increment required only for regular army
			if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("R")){
				doPromotCancel(refDo2);
			}
		}
		 
		if (Do2Manager.getIncrCancelDescription().contains(newDo2.getDescription())) {
			
			if (!newDo2.getDescription().equals("MUSTER") && !newDo2.getDescription().equals("ATTSBM")) {
				//increment generation required only for regular army
				if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("R")){
					generatePromotIncrments(newDo2);
				}
			}
		}
		if (newDo2.getDescription().equals(Do2Manager.CANCEL) && (newDo2.getColumnA().equals("MUSTER"))) {			
			AuditCage cancelAuditCage = Utils.getAuditCage(refDo2);
			cancelAuditCageList.add(cancelAuditCage);
			doMusterCancel(refDo2);
		}

		if (newDo2.getDescription().equals("RINSER")) {
			Do2 dischDo2 = Utils.getDischDo2(newDo2);			
			AuditCage dischargeAuditCage = Utils.getAuditCage(dischDo2);
			cancelAuditCageList.add(dischargeAuditCage);
	//		dischargeAuditCage.setCancelled(true);
	//		dischargeAuditCage.setCancelDo2(newDo2);
		}
		
		employee = Utils.getCurrentEmployee(newDo2.getArmyNo());

		
		try {
			//Pending do2 list query modified on 30082012 removing milri and transcription type t
			if(!newDo2.getDescription().equals("DA") || !newDo2.getDescription().equals("FIX7PC")){
				if(newDo2.getId()>0)
						pendingDo2List =do2Repo.findPendingDo2List(employee.getId(),newDo2.getId(),Do2Manager.PENDING, fromDate,Utils.getLastDayOfCurrentQuarter());
				else
					pendingDo2List =do2Repo.findPendingDo2List(employee.getId(),0L,Do2Manager.PENDING, fromDate,Utils.getLastDayOfCurrentQuarter());
					 
			}else{
				pendingDo2List = new ArrayList<Do2>();
			}
			
//				The above method commented  commented and changed as stated below on 11-07-2019 for non processing of any other do2s along with LRA do2s
// 				The above method has to be restored by deleting this method in the next version			
			
			// if(!newDo2.getDescription().equals("DA") || !newDo2.getDescription().equals("FIX7PC") || !newDo2.getDescription().equals("LRA") ){
			/*
			if (!(newDo2.getDescription().equals("DA") || newDo2.getDescription().equals("FIX7PC") || newDo2.getDescription().equals("LRA"))){
				pendingDo2List = Utils.getEntityManager().createQuery(
						" SELECT d FROM Do2 d WHERE d.id !=:newDo2Id and d.auditCage = true AND d.employee=:employee   " +
						" and d.description!=:desc and (d.reason not like 'Batch%' or d.reason is null) AND d.status=:pending AND d.fromDate>=:fromDate " +
						" AND d.fromDate<=:lastDayOfQE")
				.setParameter("employee", employee)
				.setParameter("newDo2Id", newDo2.getId())
				.setParameter("desc",Do2Manager.LRA)
				.setParameter("pending", Do2Manager.PENDING)
				.setParameter("fromDate",fromDate)
				.setParameter("lastDayOfQE", Utils.getLastDayOfCurrentQuarter())
				.getResultList();
			} else if (newDo2.getDescription().equals("LRA")) {
				pendingDo2List = Utils.getEntityManager().createQuery(
						" SELECT d FROM Do2 d WHERE d.id !=:newDo2Id and d.auditCage = true AND d.armyNo=:ano " +
						" AND d.description=:desc AND d.reason like '%Generated%' AND d.status=:pending AND d.fromDate>=:fromDate " +
						" AND d.fromDate<=:lastDayOfQE")
				.setParameter("ano", newDo2.getArmyNo())
				.setParameter("newDo2Id", newDo2.getId())
				.setParameter("pending", Do2Manager.PENDING)
				.setParameter("desc", Do2Manager.LRA)
				.setParameter("fromDate",newDo2.getFromDate())
				.setParameter("lastDayOfQE", Utils.getLastDayOfCurrentQuarter())
				.getResultList();
				
			} else{
				pendingDo2List = new ArrayList<Do2>();
			}
			Commented the method vide DolSysCir 363 dated 08.09.2020*/
				
				
				
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	 	
		if (newDo2.getDescription().equals(Do2Manager.CANCEL)) {		
			
			List<Do2> refDo2List = Utils.getReferenceDo2List(newDo2);
			for (Do2 refDo2 : refDo2List) {
				//added on 30082012-NOT TO cancel embodi do2 
				//with do2 no of tatrng
				if (newDo2.getColumnA().equals("TATRNG")){
			    	if (refDo2.getDescription().equals("EMBODI") || refDo2.getDescription().equals("DISEMB")){
			    		continue;
			    	}
			    }
				//added on 30082012
			    if (newDo2.getColumnA().equals("EMBODI")){
			    	if (refDo2.getDescription().equals("DISEMB")){
			    		continue;
			    	}
			    }
				AuditCage cancelAuditCage = Utils.getAuditCage(refDo2);
				if (cancelAuditCage != null) {
					cancelAuditCageList.add(cancelAuditCage);
				}
			}
		}	
		
		// modified below on 30082012
		if (pendingDo2List != null){
			for (Do2 do2 : pendingDo2List) {

				try {
					if (do2.getDescription().equals("BUNCH")) { // Added on 12.03.2021 to exlucde adding into pending list 
						if (do2.getAo()==null && do2.getAoDate()==null && do2.getAcceptedQe()==null) { 	// added on 14.07.2023
							continue;	
						}						
					}
					
					 if (do2.getReviewed() != null && do2.getReviewed() == true)
						 continue;
					 if (employee.getFsDueDate() != null && do2.getFromDate().after(employee.getFsDueDate())){
						 continue;
					 }
					//ADDED ON 30082012
					 
					if (do2.getTranscriptionType() != null && do2.getTranscriptionType().equals("T") && 
							(do2.getReason()==null || (do2.getReason()!=null && !do2.getReason().equals("SYSTEM GENERATED")))) {
						if (do2.getAo() == null && do2.getAoDate() == null){
							continue;
						}
					}					
					
					if (do2.getDescription() != null && do2.getDescription().equals(Do2Manager.CANCEL)) {
						if (do2.getColumnA() == null)
							continue;
						if (!isAuditCage(do2.getColumnA()))
							continue;
						//LINES added 30082012
						if (Do2Manager.STOP_ALL.contains(do2.getColumnA())){
							continue;
						}
						//LINES added 30082012
						if(Do2Manager.getRestorePayDescriptions().contains(do2.getColumnA())){
							continue;
						}						
						List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
						for (Do2 refDo2 : refDo2List) {
								AuditCage cancelAuditCage = Utils.getAuditCage(refDo2);
								if (cancelAuditCage != null) {
									cancelAuditCageList.add(cancelAuditCage);
									do2.setStatus(Do2Manager.VALID);
								}
						}
						continue;
					}
					
					//ADDED ON 30082012
					if(do2.getDescription() != null && do2.getDescription().equals("MILRI")){
						continue;
					}
					//ADDED ON 30082012
					if(Do2Manager.STOP_ALL.contains(do2.getDescription())
							|| Do2Manager.STOP_ALL.contains(do2.getColumnA())){
						continue;
					}
					//not to take any pending do2 on base audit cage date -- dnr
					if (auditCageList.get(0).getDo2().getDescription().equals("ENROLL")) {
						; 
					} else if (auditCageList.get(0).getFromDate().equals(do2.getFromDate())){
						continue;
					}					
					if (auditCageDo2Set.contains(do2)){
						//ADDED ON 30082012 to take care of cases where do2 is kept pending
						 AuditCage atCage = Utils.getAuditCage(do2);
						 if(atCage !=null){
							 if(atCage.getRecordStatus().equals("V") && do2.getStatus().equals(Do2Manager.PENDING)){
								 do2.setStatus("V");
							 }
						 }
						continue;
					}
						
					AuditCage auditCage = new AuditCage();
					auditCage.setFromDate(do2.getFromDate());
					auditCage.setDo2(do2);
					auditCageList.add(auditCage);
				} catch (Exception e) {
					Utils.handleException(e);
				}
			}
		}
	}

	public List<AuditCage> getAffectedAuditCages(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date fromDate = null;
		fromDate = AuditCageHelper.getAuditCageMaxDate(employee, do2);	
		 
		List<AuditCage> auditCageList=null;
		if (fromDate == null)
			return auditCageList;
		auditCageList =acRepo.findByFromDateGreaterThanEqualEffectiveOrderByFromDate(employee.getId(), fromDate);
		 
		if (newDo2.getDescription().equals(Do2Manager.CANCEL)
				&& Do2Manager.getMusterAttesttDescription().contains(newDo2.getColumnA())) {
		 	
			List<Do2> enrmstDo2List =do2Repo.findByDescFromDateEqual(employee.getId(),Do2Manager.VALID, "ENRMST",employee.getEnrolmentDate()); 
			 
			if (enrmstDo2List.size()>0) {
				Do2 enrmstDo2 = enrmstDo2List.get(0);
				String enrmstDo2RefNo = enrmstDo2.getRefDo2No();
				if (enrmstDo2RefNo==null || (enrmstDo2RefNo!=null && enrmstDo2RefNo.length()==0)) {
					enrmstDo2RefNo = Utils.getReferenceDo2No(enrmstDo2);
				}
				if (enrmstDo2RefNo.equals(newDo2.getRefDo2No())) {
		 			String otherDescriptionofMusterAttestDescription = null;
					if (newDo2.getColumnA().equals("MUSTER")) {
						otherDescriptionofMusterAttestDescription = "ATTS";
					} else {
						otherDescriptionofMusterAttestDescription = "MUSTER";
					}
					List<Do2> otherDo2List =do2Repo.findByArmyNoDescLikeStatus(newDo2.getArmyNo(),Do2Manager.VALID,otherDescriptionofMusterAttestDescription);
					 
		 			if (otherDo2List.size()>0) {
						Do2 otherDo2 = otherDo2List.get(0); 
						enrmstDo2.setDo2Type(otherDo2.getDo2Type());
						enrmstDo2.setDo2No(otherDo2.getDo2No());
						enrmstDo2.setDo2ItemNo(otherDo2.getDo2ItemNo());
						enrmstDo2.setDo2Date(otherDo2.getDo2Date());
						enrmstDo2.setRefDo2No(Utils.getReferenceDo2No(otherDo2));
						return auditCageList;
					}  
				} else {
		 			return auditCageList;
				}
				
				AuditCage enrmstAuditCage = Utils.getAuditCage(enrmstDo2List.get(0));
				if (enrmstAuditCage==null ) {
					enrmstDo2List.get(0).setStatus("C");
					for (AuditCage enrollAuditCage : auditCageList) {
						if (enrollAuditCage.getDo2().getDescription().equals("ENROLL")) {
							setZeroValues(enrollAuditCage);
							enrollAuditCage.setOldBasicPay(0);
							if (!enrollAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
								enrollAuditCage.setBasicPay(14600);
							} else if (!enrollAuditCage.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
								enrollAuditCage.setBasicPay(11400);
							} else if (!enrollAuditCage.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
								enrollAuditCage.setBasicPay(8550);
							} else if (!enrollAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
								enrollAuditCage.setBasicPay(5700);
							} else {
							    enrollAuditCage.setBasicPay(2440);
							}
						 		break;
						}
					}
			 	}
			}
		}		
		return auditCageList;
	}


	public void setZeroValues(AuditCage auditCage) {		
		auditCage.setBasicPay(0);
		auditCage.setDp(0);
		auditCage.setGradePay(0);
		auditCage.setMsPay(0);
		auditCage.setXGroupPay(0);
		auditCage.setClassPay(0);
		auditCage.setDa(0);
		auditCage.setTransport(0);
		auditCage.setPmha(0);
		auditCage.setLra(0);
		auditCage.setOldBasicPay(0);
		auditCage.setOldClassPay(0);
		auditCage.setOldGradePay(0);
	}	

	private Date getToDateForBase(int processingIndex) {
	//	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");  
		if (base.getFromDate().equals(auditCageList.get(processingIndex).getFromDate())){
			return base.getFromDate();
		}
		return DateManager.getPreviousDate(currentAuditCage.getFromDate());
	}

	List<AuditCageAllowance> blankList = new ArrayList<AuditCageAllowance>();

	private void setDueDrawnLists() {
		specialProcessor.setNewDo2(newDo2);
		specialProcessor.setBase(base);
		specialProcessor.setDo2Repo(do2Repo);
		 
		specialProcessor.setPsRepo(psRepo);
		specialProcessor.setAcRepo(acRepo);
		specialProcessor.setEmpRepo(empRepo);
		specialProcessor.setAcaRepo(acaRepo);
		specialProcessor.setNewDo2(newDo2);
		
		specialProcessor.setEsdRepo(esdRepo);
		specialProcessor.setRankRepo(rankRepo);
		specialProcessor.setGcRepo(gcRepo);
		specialProcessor.setMatrixRepo(matrixRepo);
		specialProcessor.setClassPayRepo(classPayRepo);
		specialProcessor.setApptRateRepo(apptRateRepo);
		specialProcessor.setAlwRateRepo(alwRateRepo);
		specialProcessor.setScRepo(scRepo);
		specialProcessor.setUnitRepo(unitRepo);
		specialProcessor.setRrRepo(rrRepo);
		specialProcessor.setLpcRepo(lpcRepo);
		specialProcessor.setArrRepo(arrRepo);
		specialProcessor.setLpcDepRepo(lpcDepRepo);
		specialProcessor.setBhutanSlabRepo(bhutanSlabRepo);
		specialProcessor.setEdRepo(edRepo);
		specialProcessor.setFsRepo(fsRepo);
		specialProcessor.setEeRepo(eeRepo);
		specialProcessor.setF16Repo(f16Repo);
		specialProcessor.setAdjCbRepo(adjCbRepo);
		specialProcessor.setFdRepo(fdRepo);
		specialProcessor.setTioRepo(tioRepo);
		specialProcessor.setJdbcTemplate(jdbcTemplate);
		specialProcessor.setRhLocationRepo(rhLocationRepo);
		if ((pendingAuditCage || cancelledAuditCage) ) {
			dueList = specialProcessor.getAllowances(base);
			drawnList = specialProcessor.getAllowances(currentAuditCage);
		} else if (pendingDo2List.contains(currentDo2)){
		    dueDrawnAuditCage[1]=previousDrawnAuditCageForPending;
		    drawnList = previousDrawnList;
		    dueList = specialProcessor.getAllowances(currentAuditCage);
		} else if (newDo2.getId()==currentDo2.getId()){
		    dueDrawnAuditCage[1]=previousDrawnAuditCageForPending;
		    drawnList = previousDrawnList;
		    dueList = specialProcessor.getAllowances(currentAuditCage);
		} else {
			dueList = processor.getDueList();
			drawnList = processor.getDrawnList();
		}
	}

	private int processFreshAuditCage(int i) {
		//Lines added on 05072012 to avoid auditcage is written
		//again if the do2 is kept pending whereas the auditcage
		//is already exists
		newDo2AuditCage = Utils.getAuditCage(currentDo2);
		if (newDo2AuditCage ==null){
			newDo2AuditCage = (AuditCage) Utils.getClonedEntity(base);
			newDo2AuditCage.setDo2(currentDo2);
			if (Do2Manager.getValidNextDayDo2s().contains(currentDo2.getDescription())) {
				newDo2AuditCage.setFromDate(DateManager.getNextDate(currentDo2.getFromDate()));
			} else {
				newDo2AuditCage.setFromDate(currentDo2.getFromDate());
			}		
			newDo2AuditCage.setQe(Utils.getCurrentQuarter());
			newDo2AuditCage.setRecordStatus("V");
			newDo2AuditCage.setCreatedAt(DateManager.getCurrentTimestamp());
			newDo2AuditCage.setToDate(null);
			auditCageList.set(i, newDo2AuditCage);
			acRepo.save(auditCageList.get(i)); 
			
		} else {
			AuditCageHelper.copyAuditCagePayElements(auditCageList.get(i), base);
			auditCageList.get(i).setAbsentFlag(base.getAbsentFlag());
			auditCageList.get(i).setDeputationFlag(base.getDeputationFlag());
			auditCageList.get(i).setDeputationStatus(base.getDeputationStatus());
			auditCageList.get(i).setCancelled(base.getCancelled());			
		}
		
		processor.setCurrentAuditCage(newDo2AuditCage);
		if (currentDo2.getAcceptedQe()==null){
			currentDo2.setAcceptedQe(Utils.getCurrentQuarter());	
		} else {
			if (!currentDo2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				currentDo2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		return i;
	}
	private int processNotCancelledAuditCage(int i) {
		
		try {
			Date d1 = new Date();
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"itsdc.dolphin.do2.auditcage."
							+ Utils.getCamelCase(currentDo2.getDescription())).getDeclaredConstructor()
					.newInstance();
			Date d2 = new Date();
			//System.out.println("get processor notcancel  "+(d2.getTime()-d1.getTime()));
			((AuditCageBase) do2Processor).setNewDo2(newDo2);
			((AuditCageBase) do2Processor).setDo2Repo(do2Repo);
			//((PromotBase) do2Processor).setDo2Repo(do2Repo);
			((AuditCageBase) do2Processor).setPsRepo(psRepo);
			((AuditCageBase) do2Processor).setAcRepo(acRepo);
			((AuditCageBase) do2Processor).setEmpRepo(empRepo);
			((AuditCageBase) do2Processor).setAcaRepo(acaRepo);
			((AuditCageBase) do2Processor).setNewDo2(newDo2);
			
			((AuditCageBase) do2Processor).setEsdRepo(esdRepo);
			((AuditCageBase) do2Processor).setRankRepo(rankRepo);
			((AuditCageBase) do2Processor).setGcRepo(gcRepo);
			((AuditCageBase) do2Processor).setMatrixRepo(matrixRepo);
			((AuditCageBase) do2Processor).setClassPayRepo(classPayRepo);
			((AuditCageBase) do2Processor).setApptRateRepo(apptRateRepo);
			((AuditCageBase) do2Processor).setAlwRateRepo(alwRateRepo);
			((AuditCageBase) do2Processor).setScRepo(scRepo);
			((AuditCageBase) do2Processor).setUnitRepo(unitRepo);
			((AuditCageBase) do2Processor).setRrRepo(rrRepo);
			((AuditCageBase) do2Processor).setLpcRepo(lpcRepo);
			((AuditCageBase) do2Processor).setArrRepo(arrRepo);
			((AuditCageBase) do2Processor).setLpcDepRepo(lpcDepRepo);
			((AuditCageBase) do2Processor).setBhutanSlabRepo(bhutanSlabRepo);
			((AuditCageBase) do2Processor).setEdRepo(edRepo);
			((AuditCageBase) do2Processor).setFsRepo(fsRepo);
			((AuditCageBase) do2Processor).setEeRepo(eeRepo);
			((AuditCageBase) do2Processor).setF16Repo(f16Repo);
			((AuditCageBase) do2Processor).setAdjCbRepo(adjCbRepo);
			((AuditCageBase) do2Processor).setFdRepo(fdRepo);
			((AuditCageBase) do2Processor).setTioRepo(tioRepo);
			((AuditCageBase) do2Processor).setJdbcTemplate(jdbcTemplate);
			((AuditCageBase) do2Processor).setRhLocationRepo(rhLocationRepo);
			boolean originalStatus = currentAuditCage.getRecordStatus().equals("V");
			
			Date d3 = new Date();
			if ((!do2Processor.isValid(currentDo2) || (Utils.isAbsent(currentDo2) || Utils.isOnDeputation(currentDo2)))) {
				Date d4 = new Date();
				//System.out.println("is absent not cancel "+(d4.getTime()-d3.getTime()));
				if (currentDo2.getAcceptedQe()==null){
					currentDo2.setAcceptedQe(Utils.getCurrentQuarter());	
					if (currentDo2.getTranscriptionType()!=null && currentDo2.getTranscriptionType().equals("T")) {
						currentDo2.setApproved(false);
					}
				} else {
					if (!currentDo2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
						currentDo2.setReviewQe(Utils.getCurrentQuarter());
					}
					if (currentDo2.getTranscriptionType()!=null && currentDo2.getTranscriptionType().equals("T")) {
						currentDo2.setApproved(false);
					}
				}
				
				currentAuditCage.setRecordStatus(Do2Manager.PENDING);
				pendingAuditCage = true;
				if (originalStatus == true) {
					validToPending = true;
				}
				if (!(currentDo2.getColumnA() != null && currentDo2.getColumnA().equals(Do2Manager.CEASED))){
					dueDrawnAuditCage[0] = base;	
				} else {
					dueDrawnAuditCage[0] = drawnAuditCage;
				}
				Date d5 = new Date();
				do2Processor.processCancel(currentDo2, currentDo2);
				Date d6 = new Date();
				if (i == auditCageList.size() - 1) {
					//SETTING TO DATE OF BASE/ARRERAS OBJECT TO NULL IF IT BECOMES PENDING
					base.setToDate(null);
				}				
				acRepo.save(base);
				auditCageList.remove(i);
				i--;
			}

			AuditCageHelper.copyAuditCagePayElements(auditCageList.get(i), base);

		} catch (Exception e) {
			Utils.handleException(e);
		}
		return i;
	}

	private void processAuditCage(int i) {
		if (!pendingAuditCage && !cancelledAuditCage) {
			currentAuditCage.setRecordStatus(Do2Manager.VALID);			
			base.setToDate(getToDateForBase(i));
		} else if (cancelledAuditCage) {
				base.setToDate(currentAuditCage.getToDate());
		}
        Date d1 = new Date();
		if (!pendingAuditCage) {
			processor.process(auditCageList.get(i).getDo2());
		}
		Date d2 = new Date();
		//System.out.println("process time process auditcage  "+(d2.getTime()-d1.getTime()));

		if ((!pendingAuditCage && !cancelledAuditCage)
				|| (!currentDo2.getDescription().equals("ENRMST") || (currentDo2.getColumnC() !=null &&
				!currentDo2.getColumnC().equals("MUSTER")))) {
			if (i < auditCageList.size() - 1) {
				if (!auditCageList.get(i).getFromDate().equals(auditCageList.get(i + 1).getFromDate()) &&
					 dueDrawnAuditCage[0].getAbsentFlag() == false) {
					dueDrawnAuditCage[0].setToDate(DateManager.getPreviousDate(auditCageList.get(i + 1).getFromDate()));
					
				} else if (dueDrawnAuditCage[0].getAbsentFlag() == false) {

				}
			}
		}
	}

	private Date getToDateForDueAuditCage(int i) {
		for (int j = i + 1; j < auditCageList.size(); j++) {
			if (auditCageList.get(j).getRecordStatus() != null
					&& !auditCageList.get(j).getRecordStatus().equals("V")) {
				continue;
			} else {
				if (auditCageList.get(i).getFromDate().equals(auditCageList.get(j).getFromDate())) {
					return auditCageList.get(i).getFromDate();
				} else {
					return DateManager.getPreviousDate(auditCageList.get(j).getFromDate());
				}
			}
		}
		return null;
	}
	
	public void generateIncrementDo2s(Do2 refDo2) {
		employee =Utils.getCurrentEmployee(refDo2.getArmyNo());
        List<Do2> incrList = do2Repo.findByDescFromDateLessThanEqualDesc(employee.getId(), Do2Manager.VALID, Arrays.asList("INCR", "STEPUP"), refDo2.getFromDate());
		 
		if (incrList.size() > 0) {
			Date listFromDate = refDo2.getFromDate();
			if (refDo2.getColumnA().substring(0, 2).equals("PA")) {
				List<Do2> do2List = Utils
						.getSubstantiveDo2BeforePaidActing(refDo2);
				if (do2List.size() > 0) {
					listFromDate = do2List.get(0).getFromDate();
				}
			}
			List<Do2> promotDo2List = Utils.getPromotList(refDo2);
			incrementDate.setTime(incrList.get(0).getFromDate().getTime());
			Date promotIncrementDate = null;
			String columnC = "";
			if (promotDo2List.size() == 0) {
				columnC = incrList.get(0).getColumnC();
				while ((incrementDate = Utils.getNextIncrementDate(incrementDate))
						.before(Utils.getLastDayOfCurrentQuarter())) {
					AuditCageHelper.insertIncrmentDo2s(refDo2, incrementDate,
							columnC);
				}

			} else {
				if (promotDo2List.get(0).getDescription().equals("REVERT")) {
					Do2 revertDo2 = promotDo2List.get(0);
					List<Do2> revertDo2List = Utils.getPromotList(revertDo2);
					promotIncrementDate = revertDo2List.get(0).getFromDate();
				} else if (promotDo2List.get(0).getDescription()
						.equals("RANKS")) {
					Do2 ranksDo2 = promotDo2List.get(0);
					List<Do2> ranksDo2List = Utils.getPromotList(ranksDo2);
					promotIncrementDate = ranksDo2List.get(0).getFromDate();
				} else {
					promotIncrementDate = promotDo2List.get(0).getFromDate();
				}

				columnC = promotDo2List.get(0).getColumnC();
				while ((incrementDate = Utils.getNextIncrementDate(incrementDate))
						.before(promotIncrementDate)) {
					AuditCageHelper.insertIncrmentDo2s(refDo2, incrementDate,
							columnC);
				}
				incrementDate = promotDo2List.get(0).getFromDate();
				while ((incrementDate = Utils.getNextIncrementDate(incrementDate))
						.before(Utils.getLastDayOfCurrentQuarter())) {
					AuditCageHelper.insertIncrmentDo2s(refDo2, incrementDate,
							columnC);
				}
			}

		}
	}

	public void doPromotCancel(Do2 refDo2) {
		if (refDo2.getDescription().equals(Do2Manager.CANCEL)
				&& Do2Manager.getSubstantiveRankDescription().contains(
						Utils.getReferenceDo2(refDo2).getColumnA())) {
			List<Do2> do2List = Utils.getPaidActingDo2(Utils
					.getReferenceDo2(refDo2));
			if (do2List.size() > 0) {
				refDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);

				return;
			}
		}
		if (!refDo2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			Do2 promotMacpDo2 = Utils.getReferenceDo2(refDo2);			
			if (promotMacpDo2!=null) {
				List<Do2> optfixDo2List =do2Repo.findByDescColAFromDateEqual(refDo2.getEmployee().getId(),Do2Manager.DO2_CANCEL_STATUS,"OPTFIX",promotMacpDo2.getDescription(),refDo2.getFromDate());
				 			
				if (optfixDo2List.size() > 0) {
					addCancelAuditCageList(optfixDo2List.get(0));
					optfixDo2List.get(0).setStatus(Do2Manager.DO2_CANCEL_STATUS);
					do2Repo.save(optfixDo2List.get(0));
					 
				}	
			}				
		}
	 
		employee =Utils.getCurrentEmployee(refDo2.getArmyNo());
        List<Do2> previousIncrList = do2Repo.findByDescFromDateLessThanEqualDesc(employee.getId(), Do2Manager.VALID, Arrays.asList("INCR", "STEPUP"), refDo2.getFromDate());
		 
		if(previousIncrList.size() > 0){
		List<Do2> incrList = Utils.getIncrmentDo2s(previousIncrList.get(0));

		for (int i = 0; i < incrList.size(); i++) {
			addCancelAuditCageList(incrList.get(i));
			incrList.get(i).setStatus(Do2Manager.DO2_CANCEL_STATUS);
			do2Repo.save(incrList.get(i));
			 
		}

		List<Do2> promotDo2List = Utils.getPromotList(previousIncrList.get(0));
		refDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
		if (promotDo2List.size() > 0)
			generatePromotIncrments(previousIncrList.get(0));
		}
	}

	public void generatePromotIncrments(Do2 do2) {

		if (Do2Manager.getSubstantiveRankDescription().contains(
				do2.getColumnA())) {
			List<Do2> do2List = Utils.getPaidActingDo2(do2);
			if (do2List.size() > 0) {
				return;
			}
		}
		
		if (do2.getDescription().equals("STEPUP") && !do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			return;
		}

		if (!Utils.isAcpBeforeCurrentDo2(do2)) {

			List<Do2> promotDo2List = new ArrayList<Do2>();

			if (do2.getStatus().equals(Do2Manager.DO2_CANCEL_STATUS)) {
				employee= Utils.getCurrentEmployee(do2.getArmyNo());
                promotDo2List = do2Repo.findByDescFromDateLessThanEqualDesc(employee.getId(), Do2Manager.VALID, Arrays.asList("INCR", "STEPUP"), do2.getFromDate());
						
				 

			} else
				promotDo2List = Utils.getPromotList(do2);
			if (promotDo2List.size() == 0
					|| (promotDo2List.size() > 0 && (promotDo2List.get(0)
							.getDescription().equals("MUSTER") || promotDo2List
							.get(0).getDescription().equals("ATTSBM")))) {

				List<Do2> incrList = Utils.getIncrmentDo2s(do2);
				for (int i = 0; i < incrList.size(); i++) {
					addCancelAuditCageList(incrList.get(i));
					incrList.get(i).setStatus(Do2Manager.DO2_CANCEL_STATUS);
					do2Repo.save(incrList.get(i));
					 
				}
				
// Below 6 lines commented for optfix code which follows the commented code
				/*if ((!((do2.getDescription().equals("PROMOT") || do2.getDescription().equals("MSTJCO")) 
									&& do2.getColumnA().equals("NBSUB"))) || (isIncrDo2RequiredForNbsub(do2))) {
					Utils.generateIncrDo2(do2, do2.getFromDate());
				} else {
					Utils.generateIncrDo2(do2, DateManager.getNextYear(do2.getFromDate()));
				}*/
				
				Date probableIncrDate = do2.getFromDate();
				 
				if (do2.getDescription().equals("OPTFIX")) {
					probableIncrDate = do2.getToDate();
				}

				if ((!((do2.getDescription().equals("PROMOT") || do2.getDescription().equals("MSTJCO")) 
									&& do2.getColumnA().equals("NBSUB"))) || (isIncrDo2RequiredForNbsub(do2))) {
					Utils.generateIncrDo2(do2, probableIncrDate); 					
				} else {
 					Utils.generateIncrDo2(do2, DateManager.getNextYear(probableIncrDate));
				}

			} else {

				if (!do2.getDescription().equals(Do2Manager.CANCEL)
						&& do2.getFromDate().before(
								promotDo2List.get(0).getFromDate())) {
					promotDo2List.add(0, do2);
				}

				for (int i = 0; i < promotDo2List.size(); i++) {
					Do2 nextIncrDo2 = promotDo2List.get(i);
					Date previousIncrDate = promotDo2List.get(i).getFromDate();
					if (Utils.isAcpBeforeCurrentDo2(nextIncrDo2)) {
				//		System.out.println(".................going to check acp/macps for promot " +nextIncrDo2.getDescription()+" frdt "+nextIncrDo2.getFromDate()+"  acp b4 pro true"  ) ;
						continue;
					}
					if (Do2Manager.getSubstantiveRankDescription().contains(nextIncrDo2.getColumnA())) {
						List<Do2> paidActingList = Utils.getPaidActingDo2(nextIncrDo2);
						if (paidActingList.size() > 0){
							if (paidActingList.get(0).getFromDate().before(nextIncrDo2.getFromDate())) {
								continue;
								/*employee =Utils.getCurrentEmployee(do2.getArmyNo());
								List<Do2> incrDo2List = Utils.getEntityManager().createQuery(
										" SELECT d FROM Do2 d where  d.employee=:employee and " +
										" d.fromDate <=:fromDate AND d.status=:status AND " +
										" d.description=:incr order by d.fromDate DESC ")
										.setParameter("employee", employee)
										.setParameter("fromDate", paidActingList.get(0).getFromDate())
										.setParameter("status", Do2Manager.VALID)
										.setParameter("incr", "INCR").getResultList();
								if (incrDo2List.size() > 0) {
									nextIncrDo2 = incrDo2List.get(0);
									previousIncrDate = paidActingList.get(0).getFromDate();
								}*/
								
							} else {
								List<Do2> incrDo2List =do2Repo.findByDescFromDateLessThanEqualValidAndPending(employee.getId(), Do2Manager.VALID,Do2Manager.PENDING,"INCR",nextIncrDo2.getFromDate());
								 
								if (incrDo2List.size() > 0) {
									nextIncrDo2 = incrDo2List.get(0);
									previousIncrDate = incrDo2List.get(0).getFromDate();
								}
							}
						}
					}
					List<Do2> incrList = Utils.getIncrmentDo2s(nextIncrDo2);
					for (int j = 0; j < incrList.size(); j++) {
						if (!nextIncrDo2.equals(incrList.get(j))) {
							addCancelAuditCageList(incrList.get(j));
							incrList.get(j).setStatus(Do2Manager.DO2_CANCEL_STATUS);
							do2Repo.save(incrList.get(j));
							 
						}
					}

					//Below 6 lines commented for optfix which follows the commented lines
				/*	if ((!((do2.getDescription().equals("PROMOT") || do2.getDescription().equals("MSTJCO")) 
										&& do2.getColumnA().equals("NBSUB"))) || (isIncrDo2RequiredForNbsub(do2))) {
						Utils.generateIncrDo2(nextIncrDo2, previousIncrDate);
					} else {
						Utils.generateIncrDo2(nextIncrDo2, DateManager.getNextYear(previousIncrDate));
					}
					*/
					if (do2.getDescription().equals("OPTFIX")) {
						previousIncrDate = do2.getToDate();
					}
					if ((!((do2.getDescription().equals("PROMOT") || do2.getDescription().equals("MSTJCO")) 
										&& do2.getColumnA().equals("NBSUB"))) || (isIncrDo2RequiredForNbsub(do2))) {
						Utils.generateIncrDo2(nextIncrDo2, previousIncrDate);
					} else {
						Utils.generateIncrDo2(nextIncrDo2, DateManager.getNextYear(previousIncrDate));
					}

				}

			}

		}
	}

	public boolean isIncrDo2RequiredForNbsub(Do2 do2) {

		Date xDate = do2.getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();

		if (!(xDate.after(jan01) && xDate.before(jul01))) {
			return true;
		} else {
			List<AuditCage> auditCageList = acRepo.findByEffectiveCancelledFalseFromDateLessThanEqualOrderByFromDateDesc(do2.getEmployee().getId(),do2.getFromDate());
				
			if (auditCageList.size() > 0) {
				if ((auditCageList.get(0).getBasicPay() < 8600 && auditCageList
						.get(0).getXGroupPay() == 0)
						|| (auditCageList.get(0).getBasicPay() < 8520 && auditCageList
								.get(0).getXGroupPay() == 1400)) {
					return false;
				}
			}
		}
		return true;
	}
	public void doMusterCancel(Do2 refDo2) {
	 	employee =Utils.getCurrentEmployee(refDo2.getArmyNo());
		List<Do2> incrList =do2Repo.findByDescStatusNotEqualFromDateAsc(employee.getId(),Do2Manager.CANCEL,"INCR");
			 
		for (int i = 0; i < incrList.size(); i++) {
			addCancelAuditCageList(incrList.get(i));
			incrList.get(i).setStatus(Do2Manager.DO2_CANCEL_STATUS);
			do2Repo.save(incrList.get(i));
			 
		}
	}

	public void addCancelAuditCageList(Do2 do2) {
		AuditCage incrAuditCage = Utils.getAuditCage(do2);
		if (incrAuditCage != null) {

			if (!newDo2.getDescription().equals(Do2Manager.CANCEL)) {
				incrAuditCage.setCancelDo2(newDo2);
			} else {
				incrAuditCage.setCancelDo2(refDo2);
			}
			acRepo.save(incrAuditCage);
			 
			cancelAuditCageList.add(incrAuditCage);
		}
	}
	
	public boolean isSubstantiveDateBeforeActing(Do2 currentDo2){
		Date startDate=null;
		startDate = auditCageList.get(0).getFromDate();
		if(startDate !=null)
		return startDate.before(newDo2.getFromDate());
		return false;
	}
	

	// DEBUG code
	SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

	private void printAuditCageList(String label) {
		System.out.println(label);
		for (AuditCage ac : auditCageList) {
			System.out.println("LIST  "
					+ ac.getDo2().getArmyNo()
					+ "  "
					+ ac.getId()
					+ "  "
					+ ac.getDo2().getDescription()
					+ "  "
					+ ac.getDo2().getId()
					+ "      "
					+ ac.getDo2().getColumnA()
					+ "  "
					+ ac.getDo2().getStatus()
					+ "  "
					+ ac.getRecordStatus()
					+ " "
					+ ac.getAbsentFlag()
					+" "
					+ac.getGradePay()
					+ "  "
					+ ac.getCancelled()
					+ "   "
					+ dateFormat.format(ac.getFromDate())
					+ "::"
					+ (ac.getToDate() == null ? null : dateFormat.format(ac
							.getToDate())));
		}
	}

	private void printAuditCageDetails(String label) {
		System.out.println(label);
		System.out.println("");
		System.out.print("CURRENT: " + currentAuditCage.getId() + "-"
				+ currentAuditCage.getDo2().getDescription());

		if (dueAuditCage != null)
			System.out.print("      DUE: " + dueAuditCage.getId() + "-"
					+ dueAuditCage.getDo2().getDescription());
		if (drawnAuditCage != null)
			System.out.print("      DRAWN: " + drawnAuditCage.getId() + "-"
					+ drawnAuditCage.getDo2().getDescription());
		if (base != null)
			System.out.print("      BASE: "
					+ base.getId()
					+ "-"
					+ base.getDo2().getDescription()
					+ "         "
					+ dateFormat.format(base.getFromDate())
					+ "::"
					+ (base.getToDate() == null ? null : dateFormat.format(base
							.getToDate())));
		System.out.println("");
	}

	private void printDueDrawnAllowanceList(String label) {
		System.out.println(label);
		for (AuditCageAllowance[] aca : dueDrawnAllowanceList) {
			System.out.println("dueList "
					+ " "
					+ currentAuditCage.getDo2().getDescription()
					+ "  "
					+ aca[0].getPayCode().getPayCode()
					+ "  "
					+ aca[0].getAmount()
					+ " "
					+ dateFormat.format(aca[0].getFromDate())
					+ "::"
					+ (aca[0].getToDate() == null ? null : dateFormat
							.format(aca[0].getToDate())) + "  "
					+ aca[0].hashCode());
			System.out.println("drawnList "
					+ " "
					+ currentAuditCage.getDo2().getDescription()
					+ "  "
					+ aca[1].getPayCode().getPayCode()
					+ "  "
					+ aca[1].getAmount()
					+ " "
					+ (aca[1].getFromDate() == null ? null : dateFormat
							.format(aca[1].getFromDate())
							+ "::"
							+ (aca[1].getToDate() == null ? null : dateFormat
									.format(aca[1].getToDate()))));
		}
	}

	private void printPendingConditions(String label) {
		System.out
				.println(currentDo2.getDescription()
						+ " ("
						+ (!do2Processor.isValid(currentDo2)
								+ " || ("
								+ (!Do2Manager.getRestorePayDescriptions()
										.contains(currentDo2.getDescription())
										+ " && " + Utils.isAbsent(currentDo2)+Utils.isOnDeputation(currentDo2))
								+ " )) && " + !(currentDo2.getColumnA() != null && currentDo2
								.getColumnA().equals(Do2Manager.CEASED))));
	}

	private void printDueDrawnAuditCageList(String label) {
		System.out.println(label);
		for (AuditCage[] dd : dueDrawnAuditCageList) {
			System.out.println(dd[0].getDo2().getDescription()
							+ "  "
							+ dateFormat.format(dd[0].getFromDate())
							+ "  "
							+ (dd[0].getToDate() == null ? null : dateFormat.format(dd[0].getToDate())
									+ " "
									+ dd[1].getDo2().getDescription()
									+ " "
									+ dd[0].getBasicPay()
									+ "  "
									+ dd[1].getBasicPay()));
		}
	}

	private void printPendingDo2List(String label) {
		System.out.println(label);
		for (Do2 do2 : pendingDo2List) {
			System.out.println(do2.getId() + "  " + do2.getDescription() + "  "+do2.getArmyNo()+" "
					+ do2.getFromDate() + " " + do2.getColumnA()+"  "+do2.getId());
		}
		for (AuditCage ac : cancelAuditCageList) {
			System.out.println("CANCEL LIST        " + ac.getId() + "  "
					+ ac.getDo2().getDescription() + "  " +ac.getDo2().getArmyNo()+" "+ ac.getFromDate()+"  "+ac.getDo2().getId());
		}
	}
	
	public void reprocessPlbon(Do2 do2){
	     Do2 plbonDo2=null;
	 
	     List<Do2> do2List =do2Repo.findByEmployeeIdAndDescriptionAndQe(do2.getEmployee().getId(),"PLBON","10/2010");
	    		 
	     if	(do2List.size() >0){
		   plbonDo2 = do2List.get(0);		   
	     }
	  
	     if (plbonDo2!=null) {
	    	 String description = "Plbonreview";
	    	 do2AccountingCageProcessor = AccountingCageHelper.getDo2SpecificAccountingCageProcessor(description);
	    	 do2AccountingCageProcessor.process(plbonDo2);
	     }	  
	}
	
	public void reprocessNraDo2s(String armyNo){
	     Employee emp = Utils.getCurrentEmployee(armyNo);
	     List<Do2> rationList = do2Repo.findBy2DescStatusAnd2Qe(employee.getId(),"NRA","SRA",Do2Manager.VALID,Utils.getPreviousQuarter(),Utils.getCurrentQuarter());
	     
	     for(Do2 ration : rationList){
	    	 double revisedAmount=0;
	    	 double paidAmount=0;
	    	 List<Arrears> arrearsList = arrRepo.findByDo2Id(ration.getId());
	    	 
	    	 for(Arrears arr : arrearsList){
	    		 paidAmount += arr.getAmount();
	    	 }
	    	  
	    	 List<Allowance> allowances = Utils.getApplicableAllowanceRates(ration.getDescription(),
	    			 ration.getFromDate(), ration.getToDate());
				for (Allowance allow : allowances) {
                    revisedAmount += Math.round(allow.getRateAmount()
							* DateManager.getNoOfDays(allow.getFromDate(), allow
									.getToDate()));
				}
				double diffAmount = revisedAmount - paidAmount;
			//	//System.out.println("revised amounts  "+diffAmount +"  "+revisedAmount+"  "+paidAmount);
				Utils.addArrears(ration,diffAmount);
	     }
	     
	    		 										
	}
	public void reprocessDaily(Do2 do2){
	  //System.out.println("inside repro daily "+do2.getArmyNo());
	     List<Do2> dailyList = do2Repo.findByDesc2StatusAnd2Qe(do2.getEmployee().getId(),Do2Manager.DAILY,Do2Manager.PENDING,Do2Manager.INVALID, Do2Manager.PENDING,Do2Manager.INVALID);
	      
//	     //System.out.println("DAILY LIST   "+dailyList.size());
	     for(Do2 daily : dailyList){
	    	 
	    	 do2AccountingCageProcessor = AccountingCageHelper
				.getDo2SpecificAccountingCageProcessor(daily.getDescription());
	    	 
	    	 do2AccountingCageProcessor.process(daily);

	     }
	}
	public void updateMonthPay(List<ArrearsObject> arrearsObjectList){
		 updateArrears(arrearsObjectList);
		 
	}
//****this method added	
	public List<AuditCageAllowance> getDrawnList(AuditCage auditCage) {
		List<AuditCageAllowance> drawnAllowanceList =acaRepo.findAllByAuditCageId(auditCage.getId());
				
			 
		return drawnAllowanceList;
	}

	
	private boolean isAuditCage(String do2Description) {
		
		List<AppendixJ> appjList =appjRepo.findByDescription(do2Description);
			 
		if(appjList.size()>0){
			return appjList.get(0).getAuditAccountCage().equals("AT");
		}
		return false;
	}
	//new method added on 30082012 to cancel incr of sixcpc
	 //called when sixcpc is fed
	 public void cancelIncrSixcpc(Do2 do2){
		 List<Do2> promotList = do2Repo.findByDescListAndFromDateGreater(do2.getEmployee().getId(),Do2Manager.VALID,Do2Manager.getIncrCancelDescription(),do2.getFromDate());
				 
		 
		 Date toDate=Utils.getLastDayOfCurrentQuarter();
		 if(promotList.size()>0){
			 toDate= promotList.get(0).getFromDate();
		 }
         List<Do2> do2List = do2Repo.findByFromDateGreaterAndFromDateLessThan(do2.getEmployee().getId(),"INCR",DateManager.getSixthCpcDate(), toDate);
        		 
        	 
         for(Do2 d : do2List){
        	  AuditCage incrAtCage= Utils.getAuditCage(d);
        	  if(incrAtCage !=null){
        		  cancelAuditCageList.add(incrAtCage);
        	  }
         }
       //  System.out.println("after for loop incr cancel list "+toDate);
         
	 }
	 private Date getTransportAllceDate(Do2 do2) {
		Date fromDate = do2.getFromDate();
		Date toDate = do2.getToDate();
		Date tptlDate = DateManager.getToday();
		Date ceaseDate = DateManager.getToday();
		GregorianCalendar fromCal = new GregorianCalendar();
		GregorianCalendar toCal = new GregorianCalendar();
		if (!fromDate.after(DateManager.getTptlDate())) {
			double do2Days = DateManager.getNoOfDays(fromDate, toDate);
			if (do2Days > 30) {
				fromCal.setTime(fromDate);
				ceaseDate.setTime(fromCal.getTimeInMillis());
				tptlDate = ceaseDate;
			} else {
				return null;
			}
		} else {
			double monthCount = DateManager.getNoOfMonths(fromDate, toDate);
			if (monthCount < 1.0)
				return null;

			fromCal.setTime(fromDate);
			toCal.setTime(toDate);
			int toMonthMaxDays = DateManager.getNoOfDaysInMonth(toCal
					.get(Calendar.MONTH) + 1, toCal.get(Calendar.YEAR));

			// Doesn't span full month
			if (monthCount < 2.0){ 
			
				if((fromCal.get(Calendar.MONTH)+1 == toCal.get(Calendar.MONTH) && fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) !=toMonthMaxDays)
						|| (fromCal.get(Calendar.MONTH)==Calendar.DECEMBER && toCal.get(Calendar.MONTH)==Calendar.JANUARY )
						&& fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) != toMonthMaxDays)
					
				return null;
			}

			if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) == toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
			} else if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) != toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
			} 
			fromCal.set(Calendar.DATE, 1);
			ceaseDate.setTime(fromCal.getTimeInMillis());
			tptlDate = ceaseDate;
		}
		return tptlDate;
	}

	private void generatetptlCeaseDo2(Do2 do2, Date ceaseDate, String description){
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());	
		if (!isDo2Generated(description,Do2Manager.CEASED,ceaseDate,do2)){
			Do2 tptlCeaseDo2 = new Do2();			
			tptlCeaseDo2.setEmployee(employee);
			tptlCeaseDo2.setArmyNo(employee.getArmyNo());
			tptlCeaseDo2.setCheckDigit(employee.getCheckDigit());
			tptlCeaseDo2.setUnitCode(do2.getUnitCode());
			tptlCeaseDo2.setUnit(do2.getUnit());
			tptlCeaseDo2.setDo2Group("IA");
			tptlCeaseDo2.setDo2Type(do2.getDo2Type());
			tptlCeaseDo2.setDo2No(do2.getDo2No());
			tptlCeaseDo2.setDo2ItemNo(do2.getDo2ItemNo());
			tptlCeaseDo2.setDo2Date(do2.getDo2Date());
			tptlCeaseDo2.setDescription(description);
			tptlCeaseDo2.setFromDate(ceaseDate);
			tptlCeaseDo2.setColumnA(Do2Manager.CEASED);
			tptlCeaseDo2.setStatus(Do2Manager.PENDING);
			tptlCeaseDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlCeaseDo2.setAuditCage(true);
			tptlCeaseDo2.setQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setAcceptedQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlCeaseDo2);	
			AuditCage auditCage = new AuditCage();
			auditCage.setFromDate(tptlCeaseDo2.getFromDate());
			auditCage.setDo2(tptlCeaseDo2);
			auditCageList.add(auditCage);
			pendingDo2List.add(tptlCeaseDo2);
			System.out.println("............... cease do2 generation " +description );

			System.out.println("............... cease do2 generation ac rs " +auditCage.getRecordStatus());
		}				
	}
		
	private void generateTptlGrantDo2(Do2 do2, String description, double rate, Do2 tptlEarlierGrantDo2, String columnC ){
		Date tptlRegrantDate = getTptlRegrantDate(do2.getToDate());
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());		 
		if (!isDo2Generated(description,Do2Manager.GRANTED,tptlRegrantDate,do2)){
//		if (!isGrantDo2Generated(description,Do2Manager.GRANTED,tptlRegrantDate,do2,columnC)){
			Do2 tptlGrantDo2 = new Do2();
			tptlGrantDo2.setEmployee(employee);
			tptlGrantDo2.setArmyNo(employee.getArmyNo());
			tptlGrantDo2.setCheckDigit(employee.getCheckDigit());
			tptlGrantDo2.setUnitCode(do2.getUnitCode());
			tptlGrantDo2.setUnit(do2.getUnit());
			tptlGrantDo2.setDo2Group("IA");
			tptlGrantDo2.setDo2Type(tptlEarlierGrantDo2.getDo2Type());
			tptlGrantDo2.setDo2No(tptlEarlierGrantDo2.getDo2No());
			tptlGrantDo2.setDo2ItemNo(tptlEarlierGrantDo2.getDo2ItemNo());
			tptlGrantDo2.setDo2Date(tptlEarlierGrantDo2.getDo2Date());
			tptlGrantDo2.setDescription(description);
			tptlGrantDo2.setFromDate(tptlRegrantDate);
			tptlGrantDo2.setColumnA(Do2Manager.GRANTED);
			tptlGrantDo2.setColumnC(columnC);
			tptlGrantDo2.setRate(rate);
			tptlGrantDo2.setStatus(Do2Manager.PENDING);
			tptlGrantDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlGrantDo2.setAuditCage(true);
			tptlGrantDo2.setQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setInputQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlGrantDo2);	
			if (tptlGrantDo2.getFromDate().before(Utils.getLastDayOfCurrentQuarter())) {
				AuditCage auditCage = new AuditCage();
				auditCage.setFromDate(tptlGrantDo2.getFromDate());
				auditCage.setDo2(tptlGrantDo2);
				auditCageList.add(auditCage);
				pendingDo2List.add(tptlGrantDo2);
			}
		}		 
	}
	private Date getTptlRegrantDate(Date toDate){
		   Date tptlRegrantDate=null;
		   GregorianCalendar fromCal = new GregorianCalendar();
		   fromCal.setTime(toDate);
		   int noOfDays = DateManager.getNoOfDaysInMonth(fromCal.get(Calendar.MONTH)+1,fromCal.get(Calendar.YEAR));
		   int datedays = fromCal.get(Calendar.DAY_OF_MONTH);
		  
		   if ((noOfDays - datedays) == 0) {
			   fromCal.set(Calendar.MONTH,fromCal.get(Calendar.MONTH)+1);
			   fromCal.set(Calendar.DATE,1);
			    tptlRegrantDate = fromCal.getTime();			  
		   } else {
			   fromCal.set(Calendar.DATE,1);
			   tptlRegrantDate = fromCal.getTime();
		   }
		   return tptlRegrantDate;
	}
	public boolean isDo2Generated(String description,String columnA,Date fromDate,Do2 do2){           // Status-Invalid changed as PENDING on 12.03.2024
		   List<Do2> do2List = do2Repo.findByDescColAFromDateEqualValidPending(do2.getEmployee().getId(), Do2Manager.VALID, Do2Manager.PENDING, description, columnA, fromDate);			 
		   return do2List.size() > 0;
	}
	
	public boolean isGrantDo2Generated(String description,String columnA,Date fromDate,Do2 do2, String columnC){
		
		 List<Do2> do2List = do2Repo.findByDescColAFromDateEqual(do2.getEmployee().getId(), Do2Manager.VALID, description, columnA, fromDate);
				 
		 
		 	if (do2List.size() > 0 ) {
		 		for (Do2 d : do2List) {
		 			if (!d.getColumnC().equals(columnC)) {
		 				d.setStatus("C");
		 				do2Repo.save(d);
		 				for (int i = 0; i < auditCageList.size() - 1; i++) {
		 					//lines added some cases where do2 is marked as cancelled and auditcage is still not cancelled
		 					if (auditCageList.get(i).getDo2().getId()==d.getId()){
		 						   if (auditCageList.get(i).getCancelled()==false){
		 							   cancelAuditCageList.add(auditCageList.get(i));
		 						   }				  
		 					}
		 				}
		 			}
		 		}
		 		
		 	}
		 
		    do2List =do2Repo.findByDescColAColCFromDateEqualValidPending(do2.getEmployee().getId(), description, columnA, columnC, fromDate);
		    		
		     
		                                 
		   return do2List.size() > 0;
	}
	  
	public Do2 getTransportAllceDo2(Do2 do2){
		  List<Do2> tptlList = do2Repo.findBy2DescColAFromDateLessThanEqual(do2.getEmployee().getId(), Do2Manager.VALID, Do2Manager.TPTL,Do2Manager.TPTH,Do2Manager.GRANTED, do2.getFromDate());
		   
		  if (tptlList.size() == 0)
			  return null;
		  return tptlList.get(0);
	}	
	/*public int getMsPay(){
		int mspay = 2000;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		return mspay;
	}*/
	
	public int getMsPay(Do2 do2){
 		int mspay=0;
 		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 			mspay=2000;
 		} else {
 			mspay=5200;
 		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		return mspay;
	}
	
	public void convertDo2(Do2 do2){
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			if (Do2Manager.getEstt22Map() == null) {
				;
			} else {
				if (do2.getColumnA() != null && !do2.getColumnA().trim().isEmpty()){
					if (Do2Manager.getEstt22Map().get(do2.getColumnA()) == null)
						;
					else
				       do2.setColumnA(Do2Manager.getEstt22Map().get(do2.getColumnA()));
			    }
				if (do2.getColumnB() != null && !do2.getColumnB().trim().isEmpty()) {
					if (Do2Manager.getEstt22Map().get(do2.getColumnB()) == null)
						;
					else
				       do2.setColumnB(Do2Manager.getEstt22Map().get(do2.getColumnB()));
			    }					
			}	
		}
	}
	
	public void cancelIncrSevenCpc(Do2 do2){
 		 Date toDate=Utils.getLastDayOfCurrentQuarter();
 		 
 		 List<Do2> promotList = do2Repo.findByDescListAndFromDateGreater(do2.getEmployee().getId(),Do2Manager.VALID, Do2Manager.getIncrCancelDescription(), do2.getFromDate());
 		 
 		 if (promotList.size()>0){
 			 toDate= promotList.get(0).getFromDate();
 		 }
 		 
 	     List<Do2> do2List = do2Repo.findByDescFromDateBetween(do2.getEmployee().getId(),"INCR",DateManager.getSeventhCpcDate(), toDate);
 	    		 
 	    	 
 	     
 	     for (Do2 d : do2List){
 	    	  AuditCage incrAtCage= Utils.getAuditCage(d);
	    	  if(incrAtCage !=null){
 	    		  cancelAuditCageList.add(incrAtCage);
 	    	  }
 	     }
	}
}
