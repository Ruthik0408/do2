package itsdc.dolphin.do2;

import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileReader;
import java.io.FileWriter; 
import java.sql.Connection;
import java.sql.DriverManager; 
import java.sql.SQLException; 
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
public class LraDo2Generation implements java.io.Serializable {

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

	public String generateLra() {
		Usr usr = Utils.getLoggedInUsr();
		System.out.println("  AAAAAAA  BBBBBB  CCCCCC ");
		String mesg = "";
		Date effectiveDate = null;
		Date lraEffectiveDate = null;
		Date previousLraDate = null;
		Date do2Date = null;
		int recs = 0;
		int batch = 6000; // starting batch number of Lra Dos2
		int num = 6000; // starting batch number of Lra Dos2
		BufferedWriter writer = null;
		String qe = Utils.getCurrentQuarter();
		String str = "";
		String dir = env.getProperty("spring.application.filedir");

		// File file = new File("../reports/insertlrados2.csv");
		File file = new File(dir + "/insertlrados2.csv");
		File file1 = new File(dir + "/insertlradosmessage.csv");
		// File file2 = new File(dir+"/insertlrado2sbatches.csv");
		try {

			lraEffectiveDate = DateManager.getDate(Constants.LRA_GEN_EFFECTIVE_DATE);
			previousLraDate = DateManager.getDate(Constants.PREVIOUS_LRA_DATE);
			do2Date = DateManager.getToday();
			
			//Only for INT in 05/2026 -- condition to be removed or commented.
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_INT) && Utils.getCurrentQuarter().equals("05/2026"))  {		 
				lraEffectiveDate = DateManager.getDate("2023-04-01");
				previousLraDate = DateManager.getDate("2022-04-01");
			}
		
			writer = new BufferedWriter(new FileWriter(file));

			if (isLraDo2AlreadyGenerated(lraEffectiveDate)) {
				mesg = "LRA DO2s ALREADY GENERATED WEF 01-04-2025";
				writer.close();
				return mesg;
			}

			insertLraRate(previousLraDate, lraEffectiveDate);
			List<Employee> empList = empRepo.findDaGenEmployee(Utils.getCurrentQuarter());

			if (empList.size() == 0) {
				mesg = "NO EMPLOYEE FOUND";
				writer.close();
				return mesg;
			}

			System.out.println(" empList size  " + empList.size());
			for (Employee emp : empList) {

				str = ""; 
				effectiveDate = lraEffectiveDate;

				if (Utils.getPao().getPaoCode().equals("94")) {
					// Employee emp = ed.getEmployee();
					if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)) {
						effectiveDate = emp.getEnrolmentDate();
					}
				} else {
					// Employee emp = ed.getEmployee();
					if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)) {
						if (emp.getGroupCategory() != null
								&& emp.getGroupCategory().getSoldierCategory().equals("RT")) {
							effectiveDate = emp.getEnrolmentDate();
						}
					}
				}
				Date sepdate = emp.getEnrolmentDate();

				if (!Utils.getPao().getPaoCode().equals("94")) {

					if (emp.getSubstantiveRank() != null
							&& Integer.parseInt(emp.getSubstantiveRank().getRankCode()) == 10) {
						// break;
						continue;
					}
					if (emp.getSubstantiveRank() != null
							&& Integer.parseInt(emp.getSubstantiveRank().getRankCode()) > 10) {
						if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() == null) {
							// break;
							continue;
						} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() == null) {
							sepdate = emp.getAttestationDate();
						} else if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() != null) {
							sepdate = emp.getInitialMusteringDate();
						} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) {
							if (emp.getAttestationDate().before(emp.getInitialMusteringDate())) {
								sepdate = emp.getAttestationDate();
							} else {
								sepdate = emp.getInitialMusteringDate();
							}
						}
					}
				}

				if (recs > 999) {
					batch = batch + 1;
					recs = 0;
				}
				if (sepdate.before(effectiveDate)) {
					str = batch + "," + emp.getUnit().getUnitCode() + "," + emp.getUnit().getId() + ","
							+ emp.getArmyNo() + "," + emp.getId() + "," + emp.getCheckDigit() + ",IA,0,4444,444,"
							+ getStringDate(do2Date) + ",LRA," + getStringDate(effectiveDate) + ",P,S," + qe + "," + qe
							+ ",true,true,false," + usr.getId() + ",System Generated Do2";
					writer.write(str);
					writer.newLine();
					++recs; 
				}

			}
			writer.close();
			Connection con = getConnection();
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			FileReader fileReader = new FileReader(dir + "/insertlrados2.csv");
			// FileReader fileReader = new FileReader("../reports/insertlra2015dos2.csv");

			copyManager.copyIn(" COPY do2(batch_no, unit_code, fk_unit,army_no, fk_employee,check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, "
							+ " description, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) "
							+ " FROM STDIN  with delimiter as ','  NULL as ''",	fileReader);

			fileReader.close();
			file.delete();

			// file = new File("../reports/insertlrados2.csv");
			file1 = new File(dir + "/insertlradosmessage.csv");
			// file = new File(dir+"/insertlrado2smessage.csv");
			writer = new BufferedWriter(new FileWriter(file1));

			writer.write("Lra Generated,true,,," + getStringDate(effectiveDate) + "," + getStringDate(do2Date) + ",," + usr.getId());
			writer.newLine();

			while (num <= batch) {
				str = "Batch,true," + Utils.getCurrentQuarter() + ",Ready," + getStringDate(effectiveDate) + ","
						+ getStringDate(do2Date) + "," + num + "," + usr.getId();
				writer.write(str);
				writer.newLine();
				num += 1;
			}
			writer.close();

			// fileReader = new FileReader("../reports/insertlrados2.csv");
			fileReader = new FileReader(dir + "/insertlradosmessage.csv");
			copyManager.copyIn(" COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) "
					+ " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);

			fileReader.close();
			// file.delete();

			/*
			 * // file = new File("../reports/insertlrados2.csv"); file = new
			 * File(dir+"/insertlrados2.csv"); writer = new BufferedWriter(new
			 * FileWriter(file));
			 * 
			 * writer.write("Lra Generated,true,,,"+getStringDate(effectiveDate)+","+
			 * getStringDate(do2Date)+",,"+usr.getId());
			 * 
			 * writer.newLine();
			 * 
			 * while (num <= batch){
			 * str="Batch,true,"+Utils.getCurrentQuarter()+",Ready,"+do2Date+","+
			 * getStringDate(effectiveDate)+","+num+","+usr.getId(); writer.write(str);
			 * writer.newLine(); num += 1; }
			 * 
			 * writer.close(); // fileReader = new
			 * FileReader("../reports/insertlrados2.csv"); fileReader = new
			 * FileReader(dir+"/insertlrados2.csv"); copyManager.copyIn(
			 * " COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) " +
			 * " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
			 * 
			 * fileReader.close(); file.delete();
			 */
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mesg = "LRA  Do2 Generation Done.";
		return mesg;
	}

	public boolean isLraDo2AlreadyGenerated(Date lraEffectiveDate) {
		List<Misc> miscList = miscRepo.findByKey1AndDateAndCurrent("LRA GENERATED", lraEffectiveDate);
		return miscList.size() > 0;
	}

	public void insertLraRate(Date previousLraDate, Date lraEffectiveDate) {
		//Only for INT in 05/2026 -- condition to be removed or commented.
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_INT) && Utils.getCurrentQuarter().equals("05/2026"))  {		
			allowanceRateRepo.updateAllowanceRateByAllowanceName(DateManager.getPreviousDate(lraEffectiveDate), "SRA", previousLraDate);
			allowanceRateRepo.updateAllowanceRateByAllowanceName(DateManager.getPreviousDate(lraEffectiveDate), "NRA", previousLraDate);
			try {
				BufferedWriter writer = null;
				String dir = env.getProperty("spring.application.filedir");
				File file = new File(dir + "/insertAllowanceRate.csv");
				writer = new BufferedWriter(new FileWriter(file));
	 			writer.write("NRA,'2023-04-01','2024-03-31',142.75");
				writer.newLine();
				writer.write("SRA,'2023-04-01','2024-03-31',285.50");
				writer.newLine();
				writer.close();

				Connection con = getConnection();
				/*
				 * try(Statement stmt = con.createStatement()) {
				 * stmt.execute("SET application_name TO 'dolphin'"); }
				 */

				// String dir=env.getProperty("spring.application.filedir");
				CopyManager copyManager = new CopyManager((BaseConnection) con);

				// FileReader fileReader = new FileReader("../reports/insertAllowanceRate.csv");
				FileReader fileReader = new FileReader(dir + "/insertAllowanceRate.csv");
				copyManager.copyIn(" COPY allowance_rate(allowance_name,from_date,to_date,rate_amount)  "
						+ " FROM STDIN with delimiter as ','  NULL as ''", fileReader);

				fileReader.close(); 
				con.close(); 
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		System.out.println("in INSERT LRA RATE");
		System.out.println(" previous LRA date " + previousLraDate);
		System.out.println(" lra effective Date " + lraEffectiveDate);

		String mesg = null;

		List<Misc> miscList = miscRepo.findAllByKey1AndCurrent("Bulk Processor", true);
		if (miscList.size() > 0) {
			miscList.get(0).setValue1("Busy");
			miscRepo.save(miscList.get(0));
		}

		List<AllowanceRate> allowance = allowanceRateRepo.findByAlwNameFromDate("NRA", lraEffectiveDate);

		if (allowance.size() > 0) {
			mesg = "NRA Allowance Rate Already Updated";
			return;
		}

		List<AllowanceRate> allowance1 = allowanceRateRepo.findByAlwNameFromDate("SRA", lraEffectiveDate);

		if (allowance1.size() > 0) {
			mesg = "SRA Allowance Rate Already Updated";
			return;
		}

		List<AllowanceRate> allowance3 = allowanceRateRepo.findByAlwNameFromDate("NRA", previousLraDate);

		if (allowance3.size() == 1) {
			allowanceRateRepo.updateAllowanceRateByAllowanceName(DateManager.getPreviousDate(lraEffectiveDate), "NRA",
					previousLraDate);

			// allowance3.get(0).setToDate(DateManager.getPreviousDate(lraEffectiveDate));
		}

		List<AllowanceRate> allowance4 = allowanceRateRepo.findByAlwNameFromDate("SRA", previousLraDate);

		if (allowance4.size() == 1) {
			// allowance4.get(0).setToDate(DateManager.getPreviousDate(lraEffectiveDate));
			// allowanceRateRepo.updateAllowanceRateByAllowanceName(previousLraDate, "NRA");
			allowanceRateRepo.updateAllowanceRateByAllowanceName(DateManager.getPreviousDate(lraEffectiveDate), "SRA",
					previousLraDate);

		}
		String dir = env.getProperty("spring.application.filedir");
		File file = new File(dir + "/insertAllowanceRate.csv");
		// File file = new File("../reports/insertAllowanceRate.csv");

		try {
			BufferedWriter writer = null;
			writer = new BufferedWriter(new FileWriter(file));
 			writer.write("NRA,'2025-04-01',,151.93");
			writer.newLine();
			writer.write("SRA,'2025-04-01',,303.86");
			writer.newLine();
			writer.close();

			Connection con = getConnection();
			/*
			 * try(Statement stmt = con.createStatement()) {
			 * stmt.execute("SET application_name TO 'dolphin'"); }
			 */

			// String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);

			// FileReader fileReader = new FileReader("../reports/insertAllowanceRate.csv");
			FileReader fileReader = new FileReader(dir + "/insertAllowanceRate.csv");
			copyManager.copyIn(" COPY allowance_rate(allowance_name,from_date,to_date,rate_amount)  "
					+ " FROM STDIN with delimiter as ','  NULL as ''", fileReader);

			fileReader.close();
			// file.delete();
			con.close();
			mesg = "Rates for NRA & SRA w.e.f" + Constants.LRA_GEN_EFFECTIVE_DATE + "  added in allowance Rate";
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return con;
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
}
