package itsdc.dolphin.do2.auditcage;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
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

@RestController
@DependsOn("utils")
public class AuditCageBase {
    AuditCage newAuditCage;
    Do2Repository do2Repo;
    PayScaleRepository psRepo;
    EmployeeServiceDetailRepository esdRepo;
    RankRepository rankRepo;
    GroupCategoryRepository gcRepo;
    PayMatrixRepository matrixRepo;
    AuditCageRepository acRepo;
    AuditCageAllowanceRepository acaRepo;
    ClassPayRateRepository classPayRepo;
    AppointmentRateRepository apptRateRepo;
    AllowanceRateRepository alwRateRepo;
    EmployeeRepository empRepo;
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

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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

	public BhutanSlabRepository getBhutanSlabRepo() {
		return bhutanSlabRepo;
	}

	public void setBhutanSlabRepo(BhutanSlabRepository bhutanSlabRepo) {
		this.bhutanSlabRepo = bhutanSlabRepo;
	}

	public LpcDeputationRepository getLpcDepRepo() {
		return lpcDepRepo;
	}

	public void setLpcDepRepo(LpcDeputationRepository lpcDepRepo) {
		this.lpcDepRepo = lpcDepRepo;
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

	public StatusCodeRepository getScRepo() {
		return scRepo;
	}

	public void setScRepo(StatusCodeRepository scRepo) {
		this.scRepo = scRepo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public AllowanceRateRepository getAlwRateRepo() {
		return alwRateRepo;
	}

	public void setAlwRateRepo(AllowanceRateRepository alwRateRepo) {
		this.alwRateRepo = alwRateRepo;
	}

	public AppointmentRateRepository getApptRateRepo() {
		return apptRateRepo;
	}

	public void setApptRateRepo(AppointmentRateRepository apptRateRepo) {
		this.apptRateRepo = apptRateRepo;
	}

	public ClassPayRateRepository getClassPayRepo() {
		return classPayRepo;
	}

	public void setClassPayRepo(ClassPayRateRepository classPayRepo) {
		this.classPayRepo = classPayRepo;
	}

	public AuditCageAllowanceRepository getAcaRepo() {
		return acaRepo;
	}

	public void setAcaRepo(AuditCageAllowanceRepository acaRepo) {
		this.acaRepo = acaRepo;
	}

	public PayMatrixRepository getMatrixRepo() {
		return matrixRepo;
	}

	public void setMatrixRepo(PayMatrixRepository matrixRepo) {
		this.matrixRepo = matrixRepo;
	}

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
	}

	public RankRepository getRankRepo() {
		return rankRepo;
	}

	public void setRankRepo(RankRepository rankRepo) {
		this.rankRepo = rankRepo;
	}

	public EmployeeServiceDetailRepository getEsdRepo() {
		return esdRepo;
	}

	public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
		this.esdRepo = esdRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public Do2 getNewDo2() {
		return newDo2;
	}

	public void setNewDo2(Do2 newDo2) {
		this.newDo2 = newDo2;
	}

	Do2 newDo2;
   
	public AuditCage getNewAuditCage() {
		return newAuditCage;
	}

	public void setNewAuditCage(AuditCage newAuditCage) {
		this.newAuditCage = newAuditCage;
	}
	
	public String process(Do2 do2) {
		return null;
	}
	public void updateBdtInfo(Do2 do2){
		
	}
}
