package itsdc.dolphin.do2;
 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class NeLraDo2Generation implements java.io.Serializable {

	 
	JdbcTemplate jdbcTemplate;
	
	 
	Environment env;
	
	 
	EmployeeRepository empRepo;
	 
	 
	MiscRepository miscRepo;
	
	 
	Do2Repository do2Repo;
	 	
	AllowanceRateRepository allowanceRateRepo;
	
	
	EmployeeDeductionRepository edRepo;

	
	LpcRegisterRepository lpcRepo;

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

	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
	}
	
	
	public LpcRegisterRepository getlpcRepo() {
		return lpcRepo;
	}

	public void setLpcRepo(LpcRegisterRepository lpcRepo) {
		this.lpcRepo = lpcRepo;
		
	}
	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

		public String generateNeLra() {
		
				Usr usr=Utils.getLoggedInUsr();
			//	System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
				String mesg="";
				Date effectiveDate = null;
				Date lraEffectiveDate = null;
				Date previousLraDate = null;
				Date do2Date = null;		
			   	int recs   = 0;
			   	int recordsRead  = 0;
			   	int recordsAdded  = 0;
			   	int taDeptCases  = 0;
			   	int neCases  = 0;
			   	int recruits  = 0;
			   	int thisMonthFsCases  = 0;
				int batch  = 6000; //starting batch number of Lra Dos2
				int num = 6000; //starting batch number of Lra Dos2
				BufferedWriter writer=null;
				String qe = Utils.getCurrentQuarter();
				String str = "";
				String dir=env.getProperty("spring.application.filedir");
				
	//		File file = new File("../reports/insertlrados2.csv");
			File file = new File(dir+"/insertNeLrados2.csv");
			File file1 = new File(dir+"/insertNeLradosmessage.csv");
		//	File file2 = new File(dir+"/insertlrado2sbatches.csv");
			try {	
				
				lraEffectiveDate = DateManager.getDate(Constants.LRA_GEN_EFFECTIVE_DATE);			
				previousLraDate = DateManager.getDate(Constants.PREVIOUS_LRA_DATE);
				do2Date = DateManager.getToday();
				
				writer = new BufferedWriter(new FileWriter(file));
				
				if (isNeLraDo2AlreadyGenerated(lraEffectiveDate)){
					mesg="NE LRA DO2s ALREADY GENERATED WEF 01-04-2025";
					return mesg; 
		  		}
				
			//	insertLraRate(previousLraDate,lraEffectiveDate);
				
				List<Employee> empList = empRepo.findNeLraGenEmployee(Utils.getCurrentQuarter());
				
				if (empList.size() == 0) {
					mesg = "NO EMPLOYEE FOUND";
					return mesg;
				}
				
				System.out.println ( " empList size  " +empList.size());
				for (Employee emp : empList){
				
					str = "";
					++recordsRead;
					effectiveDate = lraEffectiveDate;
					
								
								
					if (Utils.getPao().getPaoCode().equals("94")){
				    	// Employee emp = ed.getEmployee();
				    	 if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
				    		 effectiveDate = emp.getEnrolmentDate();
				    	 }
				    } else {
				    //	 Employee emp = ed.getEmployee();
				    	 if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)){
				    		 if (emp.getGroupCategory()!= null && emp.getGroupCategory().getSoldierCategory().equals("RT")){
				    			 effectiveDate = emp.getEnrolmentDate();
				    		 }
				    	 }
				    }
					Date sepdate = emp.getEnrolmentDate();
					
					if (!Utils.getPao().getPaoCode().equals("94")){
					
						if (emp.getSubstantiveRank()!=null && Integer.parseInt(emp.getSubstantiveRank().getRankCode()) == 10){
						//	break;
							continue;
						}
						if (emp.getSubstantiveRank()!=null && Integer.parseInt(emp.getSubstantiveRank().getRankCode()) > 10){
							if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() == null){
							//	break;
								continue;
							} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() == null) {
								sepdate = emp.getAttestationDate();
							} else if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() != null) {
								sepdate = emp.getInitialMusteringDate();
							} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) {
								 if (emp.getAttestationDate().before(emp.getInitialMusteringDate())){
									 sepdate = emp.getAttestationDate(); 
								 } else {
									 sepdate = emp.getInitialMusteringDate();
								 }
							}
						}
					}
				
					
					
					if (recs > 999){
						batch = batch + 1;
						recs = 0;
					}
					if(sepdate.before(effectiveDate)) {
						str = batch+","+emp.getUnit().getUnitCode()+","+emp.getUnit().getId()+","+emp.getArmyNo()+","+emp.getId()+","
								+emp.getCheckDigit()+",IA,0,4444,444,"+
						      getStringDate(do2Date)+",NELRA,"+getStringDate(effectiveDate)+",P,S,"+qe+","+qe+",true,true,false,"+
							  usr.getId()+",System Generated Do2";	
						writer.write(str);	
					    writer.newLine();			
					    ++recs;
					    ++recordsAdded;
					}
			
					
				}
				
				writer.close();
				
				 Connection con=getConnection();
								
				CopyManager copyManager = new CopyManager((BaseConnection) con);
				
				FileReader fileReader = new FileReader(dir+"/insertNeLrados2.csv");
		   //	    FileReader fileReader = new FileReader("../reports/insertlra2015dos2.csv");
		  
			    copyManager.copyIn(
		   	    		" COPY do2(batch_no, unit_code, fk_unit,army_no, fk_employee,check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
		   	            " description, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
		   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
		
			    fileReader.close();
			    file.delete();
			    		  
			//    file  = new File("../reports/insertlrados2.csv");
			    file1 = new File(dir+"/insertNeLradosmessage.csv");
			   //file = new File(dir+"/insertlrado2smessage.csv");
			    writer = new BufferedWriter(new FileWriter(file1));
			    
			    writer.write("Lra Generated,true,,,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+",,"+usr.getId());
				
		 	    writer.newLine();	
			    
			    while (num <= batch){
		 	   		str="Batch,true,"+Utils.getCurrentQuarter()+",Ready,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+","+num+","+usr.getId();
		 	   		writer.write(str);	
		 	   		writer.newLine();
		 	   		num += 1;
		 	   	}
			         
		 	   writer.close();
		 	   
		 //	   fileReader = new FileReader("../reports/insertlrados2.csv");
		 	  fileReader = new FileReader(dir+"/insertNeLradosmessage.csv");
		 	   copyManager.copyIn(
		   	    		" COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) " +
		   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
		 	    
		 	   	fileReader.close();
		        file.delete();
		        
		  /*      
		    //    file  = new File("../reports/insertlrados2.csv");
		        file = new File(dir+"/insertlrados2.csv");
			    writer = new BufferedWriter(new FileWriter(file));
			    
			    writer.write("Lra Generated,true,,,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+",,"+usr.getId());
				
		 	    writer.newLine();	
			    
			    while (num <= batch){
		 	   		str="Batch,true,"+Utils.getCurrentQuarter()+",Ready,"+do2Date+","+getStringDate(effectiveDate)+","+num+","+usr.getId();
		 	   		writer.write(str);	
		 	   		writer.newLine();
		 	   		num += 1;
		 	   	}
			         
		 	   writer.close();
		 //	   fileReader = new FileReader("../reports/insertlrados2.csv");
		 	  fileReader = new FileReader(dir+"/insertlrados2.csv");
		 	   copyManager.copyIn(
		   	    		" COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) " +
		   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
		 	    
		 	   	fileReader.close();
		        file.delete();
		        */
		       		        
		        con.close();
		 	     	    
		      
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			 mesg="NE LRA  Do2 Generation Done.";
			  return mesg;
		}
		
		
		
		public String generateNeLra2023() {
			
			Usr usr=Utils.getLoggedInUsr();
		//	System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
			String mesg="";
			Date effectiveDate = null;
			Date lraEffectiveDate = null;
			Date previousLraDate = null;
			Date do2Date = null;		
		   	int recs   = 0;
		   	int recordsRead  = 0;
		   	int recordsAdded  = 0;
		   	int taDeptCases  = 0;
		   	int neCases  = 0;
		   	int recruits  = 0;
		   	int thisMonthFsCases  = 0;
			int batch  = 6000; //starting batch number of Lra Dos2
			int num = 6000; //starting batch number of Lra Dos2
			BufferedWriter writer=null;
			String qe = Utils.getCurrentQuarter();
			String str = "";
			String dir=env.getProperty("spring.application.filedir");
			
//		File file = new File("../reports/insertlrados2.csv");
		File file = new File(dir+"/insertNeLrados2.csv");
		File file1 = new File(dir+"/insertNeLradosmessage.csv");
	//	File file2 = new File(dir+"/insertlrado2sbatches.csv");
		try {	
			
			lraEffectiveDate = DateManager.getDate(Constants.LRA_GEN_EFFECTIVE_DATE);			
			previousLraDate = DateManager.getDate(Constants.PREVIOUS_LRA_DATE);
			do2Date = DateManager.getToday();
			
			writer = new BufferedWriter(new FileWriter(file));
			
			if (isNeLraDo2AlreadyGenerated(lraEffectiveDate)){
				mesg="NE LRA DO2s ALREADY GENERATED WEF 01-04-2025";
				return mesg; 
	  		}
			
		//	insertLraRate(previousLraDate,lraEffectiveDate);
			
			List<Employee> empList = empRepo.findNeLraGenEmployee(Utils.getCurrentQuarter());
			
			if (empList.size() == 0) {
				mesg = "NO EMPLOYEE FOUND";
				return mesg;
			}
			
			System.out.println ( " empList size  " +empList.size());
			for (Employee emp : empList){
			
				str = "";
				++recordsRead;
				effectiveDate = lraEffectiveDate;
				
							
							
				if (Utils.getPao().getPaoCode().equals("94")){
			    	// Employee emp = ed.getEmployee();
			    	 if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
			    		 effectiveDate = emp.getEnrolmentDate();
			    	 }
			    } else {
			    //	 Employee emp = ed.getEmployee();
			    	 if (emp.getEnrolmentDate() != null && emp.getEnrolmentDate().after(effectiveDate)){
			    		 if (emp.getGroupCategory()!= null && emp.getGroupCategory().getSoldierCategory().equals("RT")){
			    			 effectiveDate = emp.getEnrolmentDate();
			    		 }
			    	 }
			    }
				Date sepdate = emp.getEnrolmentDate();
				
				if (!Utils.getPao().getPaoCode().equals("94")){
				
					if (emp.getSubstantiveRank()!=null && Integer.parseInt(emp.getSubstantiveRank().getRankCode()) == 10){
					//	break;
						continue;
					}
					if (emp.getSubstantiveRank()!=null && Integer.parseInt(emp.getSubstantiveRank().getRankCode()) > 10){
						if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() == null){
						//	break;
							continue;
						} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() == null) {
							sepdate = emp.getAttestationDate();
						} else if (emp.getAttestationDate() == null && emp.getInitialMusteringDate() != null) {
							sepdate = emp.getInitialMusteringDate();
						} else if (emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) {
							 if (emp.getAttestationDate().before(emp.getInitialMusteringDate())){
								 sepdate = emp.getAttestationDate(); 
							 } else {
								 sepdate = emp.getInitialMusteringDate();
							 }
						}
					}
				}
			
				
				
				if (recs > 999){
					batch = batch + 1;
					recs = 0;
				}
				if(sepdate.before(effectiveDate)) {
					str = batch+","+emp.getUnit().getUnitCode()+","+emp.getUnit().getId()+","+emp.getArmyNo()+","+emp.getId()+","
							+emp.getCheckDigit()+",IA,0,4444,444,"+
					      getStringDate(do2Date)+",NELRA,"+getStringDate(effectiveDate)+",P,S,"+qe+","+qe+",true,true,false,"+
						  usr.getId()+",System Generated Do2";	
					writer.write(str);	
				    writer.newLine();			
				    ++recs;
				    ++recordsAdded;
				}
		
				
			}
			
			writer.close();
			
			 Connection con=getConnection();
							
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
			FileReader fileReader = new FileReader(dir+"/insertNeLrados2.csv");
	   //	    FileReader fileReader = new FileReader("../reports/insertlra2015dos2.csv");
	  
		    copyManager.copyIn(
	   	    		" COPY do2(batch_no, unit_code, fk_unit,army_no, fk_employee,check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	
		    fileReader.close();
		    file.delete();
		    		  
		//    file  = new File("../reports/insertlrados2.csv");
		    file1 = new File(dir+"/insertNeLradosmessage.csv");
		   //file = new File(dir+"/insertlrado2smessage.csv");
		    writer = new BufferedWriter(new FileWriter(file1));
		    
		    writer.write("Ne Lra Generated,true,,,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+",,"+usr.getId());
			
	 	    writer.newLine();	
		    
		    while (num <= batch){
	 	   		str="Batch,true,"+Utils.getCurrentQuarter()+",Ready,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+","+num+","+usr.getId();
	 	   		writer.write(str);	
	 	   		writer.newLine();
	 	   		num += 1;
	 	   	}
		         
	 	   writer.close();
	 	   
	 //	   fileReader = new FileReader("../reports/insertlrados2.csv");
	 	  fileReader = new FileReader(dir+"/insertNeLradosmessage.csv");
	 	   copyManager.copyIn(
	   	    		" COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	 	    
	 	   	fileReader.close();
	        file.delete();
	        
	  /*      
	    //    file  = new File("../reports/insertlrados2.csv");
	        file = new File(dir+"/insertlrados2.csv");
		    writer = new BufferedWriter(new FileWriter(file));
		    
		    writer.write("Lra Generated,true,,,"+getStringDate(effectiveDate)+","+getStringDate(do2Date)+",,"+usr.getId());
			
	 	    writer.newLine();	
		    
		    while (num <= batch){
	 	   		str="Batch,true,"+Utils.getCurrentQuarter()+",Ready,"+do2Date+","+getStringDate(effectiveDate)+","+num+","+usr.getId();
	 	   		writer.write(str);	
	 	   		writer.newLine();
	 	   		num += 1;
	 	   	}
		         
	 	   writer.close();
	 //	   fileReader = new FileReader("../reports/insertlrados2.csv");
	 	  fileReader = new FileReader(dir+"/insertlrados2.csv");
	 	   copyManager.copyIn(
	   	    		" COPY misc(key1,current,value1,value2,date1,date2,int_number1,fk_usr) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	 	    
	 	   	fileReader.close();
	        file.delete();
	        */
	       		        
	        con.close();
	 	     	    
	      
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		 mesg="Ne LRA  Do2 Generation Done.";
		  return mesg;
	}
	
	
	public boolean isNeLraDo2AlreadyGenerated(Date lraEffectiveDate){
		
		List<Misc> miscList =miscRepo.findByKey1AndDateAndCurrent("NE LRA GENERATED", lraEffectiveDate);
		return miscList.size() >0;
	
	}

		
		
	
	
	
	

	public String openNeIrla() {

		String message = "Success";
		String mesg = null;
		Usr usr = Utils.getLoggedInUsr();
		
		List<Employee> empList = empRepo.findNeLraEmployee(Utils.getCurrentQuarter());
		//List<LpcRegister> lpcRegisterList = lpcRepo.findNeLraEmpList();
		System.out.println ( " EMPLOYEE LIST SIZE " +empList.size());
		for (Employee emp : empList){
	     	
      		
		
		if (emp != null) {
			
		/*	if (recordExistsInLpcRegister(emp) == false) {
				System.out.println ( " NO recod in lpc register " +emp.getArmyNo());
				mesg = "No record in Lpc Register -- Issue Tender Memo manually";
				return mesg;

			}
			if (duplicateEdRecord(emp) == true) {
				System.out.println ( " duplicate record  " +emp.getArmyNo());
				mesg = "Ed Record already created";
				return mesg;

			}

			if (recordExistsInEmployeeForCurrentMonth(emp) == true) {
				System.out.println ( " record exists for this month  " +emp.getArmyNo());
				mesg = "Employee Record in employee Table - IRLA recreated";
				return mesg;

			}*/
		
			if(!duplicateEdRecord(emp) && !recordExistsInEmployeeForCurrentMonth(emp)) {
				System.out.println  ( " ed created now " +emp.getArmyNo());
				EmployeeDeduction ed = new EmployeeDeduction();
				ed.setAcrTotalAmount(0.0);
				ed.setAdjustmentTotalAmountCr(0.0);
				ed.setAdjustmentTotalAmountDb(0.0);
				ed.setAgisAmount(0.0);
				ed.setBhutanTotalAmount(0.0);
				ed.setBonusOnCreditBalance(0.0);
				ed.setCbTotalAmountCr(0.0);
				ed.setCbTotalAmountDb(0.0);
				ed.setClosed(false);
				ed.setClosingBalance(0.0);
				ed.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				ed.setEmployee(emp);
				ed.setEmployeeType("P");
				ed.setFamoTotalAmount(0.0);
				ed.setFundSubscriptionRefund(0.0);
				ed.setFundSubscriptionRefund(0.0);
				ed.setIncomeTaxEc(0.0);
				ed.setLoanRecoveryAmount(0.0);
				ed.setMedicalClaimTotalAmount(0.0);
				ed.setNePaidAmount(0.0);
				ed.setOpeningBalance(0.0);
				ed.setPliAmount(0.0);
				ed.setQe(Utils.getCurrentQuarter());
				ed.setQuarterPay(0.0);
				ed.setTotalCredits(0.0);
				ed.setTotalDebits(0.0);
				ed.setTransferInAmount(0.0);
				ed.setTransferOutAmount(0.0);
				ed.setUdfamoTotalAmount(0.0);
				ed.setUsr(usr);
				edRepo.save(ed);
				System.out.println("employee from ed " + ed.getEmployee().getId());

				mesg = "NE Irla Opened ";
			}
		
		} else {
			mesg = "Cannot Open NE Irla, reason being  Employee with current record missing ";
		}
		
		  }	
			
			return mesg;

		}
 

	private boolean recordExistsInLpcRegister(Employee emp) {
		// TODO Auto-generated method stub

		if (emp != null) {
			List<LpcRegister> lpcList = lpcRepo
					.findByEmployeeIdRecordStatusApprovedEcsGeneratedDischargeDateGreaterThanAndQe(emp.getId(), "F",
							DateManager.getNeImplementationDate(), Utils.getCurrentQuarter());

			if (lpcList == null || lpcList.size() == 0) {
				return false;
			}
		}
		return true;

	}
	
	private boolean duplicateEdRecord(Employee emp) {
		// TODO Auto-generated method stub
		if (emp != null) {
			List<EmployeeDeduction> edList = edRepo.findByEmpIdAndQe(emp.getId(), Utils.getCurrentQuarter());

			if (edList.size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean recordExistsInEmployeeForCurrentMonth(Employee emp) {
		// TODO Auto-generated method stub
		if (emp != null) {
			List<LpcRegister> lpcList = lpcRepo
					.findByEmployeeIdRecordStatusApprovedEcsGeneratedDischargeDateGreaterThanAndQe(emp.getId(), "F",
							DateManager.getNeImplementationDate(), Utils.getCurrentQuarter());

			if (lpcList == null || lpcList.size() >= 0) {
				String armyNo = lpcList.get(0).getArmyNo();
				if (Utils.getCurrentEmployee(armyNo).getQe().equals(Utils.getCurrentQuarter())) {
					return true;
				}
			}
		}

		return false;
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
	
		
	
	public String getStringDate(Date date){
		String strdate=null;
		GregorianCalendar gc=new GregorianCalendar();
		gc.setTime(date);
		String mm = null,dd=null;
		
		String mth=String.valueOf(gc.get(Calendar.MONTH)+1);
		if (mth.length()<2)
			mm="0".concat(mth);
		else
			mm=mth;
		
		String day=String.valueOf(gc.get(Calendar.DATE));
		
		if (day.length()<2)
			dd="0".concat(day);
		else
			dd=day;
		String yyyy=String.valueOf(gc.get(Calendar.YEAR));
		
		strdate=yyyy+"-"+mm+"-"+dd;
		
		return strdate;
	}
}





