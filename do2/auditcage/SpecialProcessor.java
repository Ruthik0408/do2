package itsdc.dolphin.do2.auditcage;

 

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.repositories.AdjustmentCbRepository;
import itsdc.dolphin.repositories.AllowanceRateRepository;
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
import itsdc.dolphin.utils.Utils;

@RestController
public class SpecialProcessor {
	protected List<AuditCageAllowance> drawnList;
	protected List<AuditCageAllowance> dueList;
	protected AuditCage[] dueDrawnAuditCage;
	protected AuditCage currentAuditCage, newAuditCage, base ;
	Date lastDayOfPreviousQuarter;
	Employee employee;
	Do2AuditCageProcessor do2Processor;
	Do2Repository do2Repo;
	AuditCageAllowanceRepository acaRepo;
	AllowanceRateRepository alwRepo;
	AuditCageRepository acRepo;
	EmployeeRepository empRepo;
	PayScaleRepository psRepo;
	EmployeeServiceDetailRepository esdRepo;
	RankRepository rankRepo;
	GroupCategoryRepository gcRepo;
    PayMatrixRepository matrixRepo;
    ClassPayRateRepository classPayRepo;
    AppointmentRateRepository apptRateRepo;
    AllowanceRateRepository alwRateRepo;
    StatusCodeRepository scRepo;
    UnitRepository unitRepo;
    ReadyReckonerRepository rrRepo;
    LpcRegisterRepository lpcRepo;
    
    ArrearsRepository arrRepo;
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

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public AuditCageAllowanceRepository getAcaRepo() {
		return acaRepo;
	}

	public void setAcaRepo(AuditCageAllowanceRepository acaRepo) {
		this.acaRepo = acaRepo;
	}

	public AllowanceRateRepository getAlwRepo() {
		return alwRepo;
	}

	public void setAlwRepo(AllowanceRateRepository alwRepo) {
		this.alwRepo = alwRepo;
	}

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
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

	public ArrearsRepository getArrRepo() {
		return arrRepo;
	}

	public void setArrRepo(ArrearsRepository arrRepo) {
		this.arrRepo = arrRepo;
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

	public boolean isValid(Do2 do2) {
		if (do2.getDescription() != null && do2.getDescription().equals("CANCEL")) {
			return true;
		}
		 
		lastDayOfPreviousQuarter = Utils.getLastDayOfPreviousQuarter();
		

		if (employee == null) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		}

		try {
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"itsdc.dolphin.do2.auditcage."
							+ Utils.getCamelCase(do2.getDescription()))
					.getDeclaredConstructor().newInstance();
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
			((AuditCageBase) do2Processor).setEnrollDo2Repo(enrollDo2Repo);
			
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (!do2Processor.isValid(do2)) {
		//	System.out.println("Invalid Do2");
			do2Repo.save(do2);
			return false;
		}
		return true;
	}
	
	public List<AuditCageAllowance> getAllowances(AuditCage auditCage) {
		//System.out.println("auditCAge special processor"+auditCage.getId()+"  "+auditCage.getDo2().getDescription());
		List<AuditCageAllowance> drawnAllowanceList = null;
		
		if(auditCage!=null && auditCage.getId()>0)
		drawnAllowanceList=acaRepo.findAllByAuditCageId(auditCage.getId());
				
				 
		return drawnAllowanceList;
	}

	public AuditCage getBase() {
		return base;
	}

	public void setBase(AuditCage base) {
		this.base = base;
	}

	public void setDueDrawnAuditCage(AuditCage[] dueDrawnAuditCage) {
		this.dueDrawnAuditCage = dueDrawnAuditCage;
	}

	public String process(Do2 do2){
		return Do2Manager.SUCCESS;
	}
	
	public List<AuditCageAllowance> getDrawnList() {
		return drawnList;
	}
	public void setDrawnList(List<AuditCageAllowance> drawnList) {
		this.drawnList = drawnList;
	}
	public List<AuditCageAllowance> getDueList() {
		return dueList;
	}
	public void setDueList(List<AuditCageAllowance> dueList) {
		this.dueList = dueList;
	}
	public AuditCage[] getDueDrawnAuditCage() {
		return dueDrawnAuditCage;
	}
	public AuditCage getCurrentAuditCage() {
		return currentAuditCage;
	}

	public void setCurrentAuditCage(AuditCage currentAuditCage) {
		this.currentAuditCage = currentAuditCage;
	}
	public AuditCage getNewAuditCage() {
		return newAuditCage;
	}

	public void setNewAuditCage(AuditCage newAuditCage) {
		this.newAuditCage = newAuditCage;
	}
	Do2 newDo2;

	public Do2 getNewDo2() {
		return newDo2;
	}

	public void setNewDo2(Do2 newDo2) {
		this.newDo2 = newDo2;
	}

}
