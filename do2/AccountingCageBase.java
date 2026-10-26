package itsdc.dolphin.do2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2; 
import itsdc.dolphin.repositories.AdjustmentCbRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.DakRepository;
import itsdc.dolphin.repositories.DakTypeRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.repositories.FundPaymentAuthorityRepository;
import itsdc.dolphin.repositories.FundPurposeRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.SectionRepository;
import itsdc.dolphin.repositories.UnitRepository;

@RestController
public class AccountingCageBase implements Do2AccountingCageProcessor {

	@Override
	public boolean isValid(Do2 do2) {	
		return false;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	public Do2Repository do2Repo;
	public GroupCategoryRepository gcRepo;
	public EmployeeRepository empRepo;
	public AuditCageRepository acRepo;
	public FamilyDetailRepository fdRepo;
	public AdjustmentCbRepository adjCbRepo;
	public EmployeeServiceDetailRepository esdRepo;
	public Form16Repository f16Repo;
	public UnitRepository unitRepo;
	public PayScaleRepository psRepo;
	public ArrearsRepository arrRepo;
	public PayCodeRepository pcRepo;
	public LpcRegisterRepository lpcRepo;
	public FundPaymentAuthorityRepository fpaRepo;	 
	public FundPurposeRepository fundPurposeRepo;  
	public DakTypeRepository dakTypeRepo;
	public SectionRepository sectionRepo;
	public DakRepository dakRepo;
	public EmployeeDeductionRepository edRepo;
	JdbcTemplate jdbcTemplate;

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public DakRepository getDakRepo() {
		return dakRepo;
	}

	public void setDakRepo(DakRepository dakRepo) {
		this.dakRepo = dakRepo;
	}

	public SectionRepository getSectionRepo() {
		return sectionRepo;
	}

	public void setSectionRepo(SectionRepository sectionRepo) {
		this.sectionRepo = sectionRepo;
	}

	public DakTypeRepository getDakTypeRepo() {
		return dakTypeRepo;
	}

	public void setDakTypeRepo(DakTypeRepository dakTypeRepo) {
		this.dakTypeRepo = dakTypeRepo;
	}

	public FundPurposeRepository getFundPurposeRepo() {
		return fundPurposeRepo;
	}

	public void setFundPurposeRepo(FundPurposeRepository fundPurposeRepo) {
		this.fundPurposeRepo = fundPurposeRepo;
	}

	public Form16Repository getF16Repo() {
		return f16Repo;
	}

	public void setF16Repo(Form16Repository f16Repo) {
		this.f16Repo = f16Repo;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public PayCodeRepository getPcRepo() {
		return pcRepo;
	}

	public void setPcRepo(PayCodeRepository pcRepo) {
		this.pcRepo = pcRepo;
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

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public FamilyDetailRepository getFdRepo() {
		return fdRepo;
	}

	public void setFdRepo(FamilyDetailRepository fdRepo) {
		this.fdRepo = fdRepo;
	}

	public AdjustmentCbRepository getAdjCbRepo() {
		return adjCbRepo;
	}

	public void setAdjCbRepo(AdjustmentCbRepository adjCbRepo) {
		this.adjCbRepo = adjCbRepo;
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

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}
	
	public FundPaymentAuthorityRepository getFpaRepo() {
		return fpaRepo;
	}

	public void setFpaRepo(FundPaymentAuthorityRepository fpaRepo) {
		this.fpaRepo = fpaRepo;
	}
	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
	}
}
