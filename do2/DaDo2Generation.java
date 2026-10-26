package itsdc.dolphin.do2;

import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileReader;
import java.io.FileWriter;  
import java.sql.Connection;
import java.sql.DriverManager; 
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat; 
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar; 
import java.util.List; 
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection; 
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate; 
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.models.AllowanceRate; 
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class DaDo2Generation implements java.io.Serializable {

	JdbcTemplate jdbcTemplate;
	Environment env;
	EmployeeRepository empRepo;
	MiscRepository miscRepo;
	Do2Repository do2Repo;
	AllowanceRateRepository allowanceRateRepo;
	Usr usr;

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public MiscRepository getMiscRepo() {
		return miscRepo;
	}

	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public AllowanceRateRepository getAllowanceRateRepo() {
		return allowanceRateRepo;
	}

	public void setAllowanceRateRepo(AllowanceRateRepository allowanceRateRepo) {
		this.allowanceRateRepo = allowanceRateRepo;
	}

	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

	public String generateDaDo2() {
		Usr usr = Utils.getLoggedInUsr();
		System.out.println("  AAAAAAA  BBBBBB  CCCCCC ");
		String mesg = "";
		Date effectiveDate = null;
		Date daEffectiveDate = null;
		Date previousDaDate = null;
		Date do2Date = null;
		int recs = 0;
		int recordsRead = 0;
		int recordsAdded = 0;
		int taDeptCases = 0;
		int neCases = 0;
		int recruits = 0;
		int batch = 6001; // starting batch number of Da Dos2
		BufferedWriter writer = null;

		String dir = env.getProperty("spring.application.filedir");
		File file = new File(dir + "/insertdados2.csv");
		String qe = Utils.getCurrentQuarter();
		String str = "";

		try {
			daEffectiveDate = DateManager.getDate(Constants.DA_EFFECTIVE_DATE);
			previousDaDate = DateManager.getDate(Constants.DA_PREVIOUS_EFFECTIVE_DATE);
			do2Date = DateManager.getToday();
			if (isDaDo2AlreadyGenerated(effectiveDate)) {
				mesg = "DA DO2 ALREADY GENERATED WEF 01-01-2026";
				return mesg;
			}
			insertDaRate(previousDaDate, daEffectiveDate);
			List<Employee> empList = empRepo.findDaGenEmployee(Utils.getCurrentQuarter());

			if (empList.size() == 0) {
				mesg = "NO EMPLOYEE FOUND";
				return mesg;
			}
			writer = new BufferedWriter(new FileWriter(file));
			for (Employee emp : empList) {
				str = "";
				++recordsRead;
				effectiveDate = daEffectiveDate;
				if (emp.getRegularTaFlag().equals('T') && emp.getTaAppointmentCategory().equals('D')) {
					++taDeptCases;
					continue;
				} else if (emp.getSubstantiveRank() != null
						&& emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
					++recruits;
					continue;
				} else if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5 && emp.getFsDueDate() != null
						&& emp.getFsDueDate().before(effectiveDate)) {
					++neCases;
					continue;
				}

				if (Utils.getPao().getPaoCode().equals("94")) {
					if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)) {
						effectiveDate = emp.getEnrolmentDate();
					}
				} else {
					if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)) {
						if (emp.getGroupCategory() != null
								&& emp.getGroupCategory().getSoldierCategory().equals("RT")) {
							effectiveDate = emp.getEnrolmentDate();
						}
					}
				}
				if (recs > 999) {
					batch = batch + 1;
					recs = 0;
				}
				str = batch + "," + emp.getUnit().getUnitCode() + "," + emp.getArmyNo() + "," + emp.getCheckDigit()
						+ ",IA,0,5555,555," + getStringDate(do2Date) + ",DA," + getStringDate(effectiveDate) + ",P,S,"
						+ qe + "," + qe + ",true,true,false," + usr.getId() + ",System Generated Do2";
				writer.write(str);
				writer.newLine();
				++recs;
				++recordsAdded;
			}
			writer.close();
			Connection con = getConnection();
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			FileReader fileReader = new FileReader(dir + "/insertdados2.csv");
			copyManager.copyIn(" COPY do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, "
							+ " description, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) "
							+ " FROM STDIN  with delimiter as ','  NULL as ''",	fileReader);
			fileReader.close();
			file.delete();

			updateMiscTable(qe, daEffectiveDate);
			writeBatchInMisc(qe, daEffectiveDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mesg = "DA Do2 Generation Done.";
		return mesg;
	}

	public void insertDaRate(Date previousDaDate, Date effectiveDate) {

		List<AllowanceRate> allowance = allowanceRateRepo.findByAlwNameFromDate("DA", previousDaDate);
		if (allowance.size() > 0) {
			allowance.get(0).setToDate(DateManager.getPreviousDate(effectiveDate));
			AllowanceRate allowanceRate = new AllowanceRate();
			allowanceRate.setAllowanceName("DA");
			allowanceRate.setFromDate(effectiveDate);
			allowanceRate.setRatePercentage(Constants.DA_RATE);
			allowanceRate.setRateAmount(1.36);
			allowanceRate.setCreatedAt(DateManager.getCurrentTimestamp());
			allowanceRate.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			allowanceRateRepo.save(allowanceRate);
		}

		List<Misc> miscList = miscRepo.findAllByKey1AndCurrent("Bulk Processor", true);
		if (miscList.size() > 0) {
			miscList.get(0).setValue1("Busy");
		}
	}

	public boolean isDaDo2AlreadyGenerated(Date effectiveDate) {
		List<Misc> miscList = miscRepo.findByKey1AndDateAndCurrent("DA_GENERATION", effectiveDate);
		return miscList.size() > 0;
	}

	public String getStringDate(Date date) {
		String strdate = null;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		String mm = null, dd = null;

		String mth = String.valueOf(gc.get(Calendar.MONTH) + 1);
		if (mth.length() < 2)
			mm = "0".concat(mth);
		else
			mm = mth;

		String day = String.valueOf(gc.get(Calendar.DATE));

		if (day.length() < 2)
			dd = "0".concat(day);
		else
			dd = day;

		String yyyy = String.valueOf(gc.get(Calendar.YEAR));
		strdate = yyyy + "-" + mm + "-" + dd;

		return strdate;
	}

	private Connection getConnection() {

		String url = "jdbc:postgresql://localhost:5432/dolphin";
		String driver = "org.postgresql.Driver";
		String user = env.getProperty("spring.datasource.username");
		String password = env.getProperty("spring.datasource.password");
		Connection con = null;

		try {
			Class.forName(driver).newInstance();
			con = DriverManager.getConnection(url, user, password);
		} catch (InstantiationException e) {
 			e.printStackTrace();
		} catch (IllegalAccessException e) {
 			e.printStackTrace();
		} catch (ClassNotFoundException e) {
 			e.printStackTrace();
		} catch (SQLException e) {
 			e.printStackTrace();
		}

		return con;
	}

	public void updateMiscTable(String qe, Date date) {
		Usr usr = Utils.getLoggedInUsr();
		Misc misc = new Misc();
		misc.setKey1("DA_GENERATION");
		misc.setValue1(qe);
		misc.setDate1(date);
		misc.setUsr(usr);
		misc.setCurrent(true);
		miscRepo.save(misc);
	}

	private void writeBatchInMisc(String qe, Date daEffectiveDate) {
 		List<Misc> miscList = miscRepo.findDaBatch(qe);
		if ((miscList != null && miscList.size() == 0) || miscList == null) {
			List<Integer> batchList = do2Repo.findDaGeneratedBatches(daEffectiveDate, qe);
			if (batchList != null && batchList.size() > 0) {
				for (Integer batch : batchList) {
					Misc m = new Misc();
					m.setKey1("Batch");
					m.setValue1(qe);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batch);
					miscRepo.save(m);
				}
			}
		}
	}
}
