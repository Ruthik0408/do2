package itsdc.dolphin.do2;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.accountingcage.independent.Itropt;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.utils.Utils;

 

 

@RestController
public class AccountingCageHelper {

	/**
	 * Used internally. Not required while coding for any specific Do2.
	 * 
	 * @param description
	 * @return An instance of the Do2 based on the description
	 */
	
	@Autowired
	FamilyDetailRepository fdRepo1;
	
	@Autowired
	EmployeeRepository empRepo1;
	
	@Autowired
	Do2Repository do2Repo1;
	
	@Autowired
	ArrearsRepository arrRepo1;
	
	@Autowired
	Form16Repository f16Repo1;
	
	
	@Autowired
	static JdbcTemplate jdbcTemplate;

	

	public static JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}
	public static void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		AccountingCageHelper.jdbcTemplate = jdbcTemplate;
	}
	
	
	public static FamilyDetailRepository fdRepo;
	public static EmployeeRepository empRepo;
	public static Do2Repository do2Repo;
	public static ArrearsRepository arrRepo;
	public static Form16Repository f16Repo;
	
	@PostConstruct
	public void init() {
		this.fdRepo=fdRepo1;
		this.empRepo=empRepo1;
		this.do2Repo=do2Repo1;
		this.arrRepo=arrRepo1;
		this.f16Repo=f16Repo1;	
	}
	static final public Do2AccountingCageProcessor getDo2SpecificAccountingCageProcessor(
			String description) {
		Do2AccountingCageProcessor processor = null;

		try {
			processor = (Do2AccountingCageProcessor) Class.forName(
					"itsdc.dolphin.do2.accountingcage.dependent."
							+ Utils.getCamelCase(description)).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			try {
				processor = (Do2AccountingCageProcessor) Class.forName(
						"itsdc.dolphin.do2.accountingcage.independent."
								+ Utils.getCamelCase(description)).getDeclaredConstructor()
						.newInstance();
			} catch (Exception e2) {
				Utils.handleException(e2);
			}
		}
		return processor;
	}

	/**
	 * Used internally. Reverses the amount of arrears in case a do2 is
	 * cancelled.
	 * 
	 * @param do2
	 *            The 'CANCEL' Do2. Not the Do2 that is referred.
	 */
	synchronized static public void processCancel(Do2 do2) {
	 
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for(Do2 doRef : refDo2List){
			
			if (doRef.getDescription().equals(do2.getColumnA())){
				
	 		System.out.println("do2 no    "+doRef.getId()+"  "+doRef.getStatus());
				if (doRef.getStatus() !=null && !doRef.getStatus().equals(Do2Manager.VALID) ){
					continue;
				}
				if (doRef!=null) {
					if (doRef.getDescription().equals("ENCASH") && doRef.getColumnA().equals("LTC")) {
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());
						if (employee.getLtcEncashmentDays()!=null && doRef.getTjra()!=null){
							int days = employee.getLtcEncashmentDays()-doRef.getTjra();
							if (days < 0){
								days = 0;
							}
							employee.setLtcEncashmentDays(days);
						}					
					}
					if (doRef.getDescription().equals("TB")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());
						employee.setTbFlag(false);
						employee.setTbFlagDate(null);
					}
			/*		if (doRef.getDescription().equals("PAN")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());
						employee.setPanNumber(null);
					}
				*/	

					if (doRef.getDescription().equals("PAN") && (doRef.getColumnB()!=null && doRef.getColumnB().equals("SELF"))){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());
						employee.setPanNumber(null);
					}
					if (doRef.getDescription().equals("PAN") && (doRef.getColumnB()!=null && !doRef.getColumnB().equals("SELF"))){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndPanNoAndNameOfMemberAndRelationshipAndRecordStatus(do2.getEmployee().getId(), doRef.getColumnA(), doRef.getColumnC(), doRef.getColumnB(), Do2Manager.VALID);
						 
			 			if (fdlist.size() > 0){
			 			 	fdlist.get(0).setRecordStatus("C");
			 			 	fdRepo.save(fdlist.get(0));
			 			 	 
			 			}
					}
					
					
					if (doRef.getDescription().equals("FAMDTL") || doRef.getDescription().equals("BIRTH") ||
						doRef.getDescription().equals("ADOPT") || doRef.getDescription().equals("DONADP")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist = fdRepo.findByEmployeeIdAndRelationshipAndNameOfMemberAndRecordStatus(do2.getEmployee().getId(),doRef.getColumnB(), doRef.getColumnA(),Do2Manager.VALID);
							 
			 			if (fdlist.size() > 0){
			 			 	fdlist.get(0).setRecordStatus("C");
			 			 	fdRepo.save(fdlist.get(0));
			 			 	 
			 			}
					}
					if (doRef.getDescription().equals("CHNOK") || doRef.getDescription().equals("NOK")){
						 if (doRef.getColumnC().trim().equals("AFPPF")){
							 Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());
							 List<FamilyDetail>fdlist =fdRepo.findByEmployeeIdAndAfppfNominationAndRecordStatus(do2.getEmployee().getId(), true,Do2Manager.VALID); 
								 
							 if (fdlist.size() > 0){
								 for (FamilyDetail famdet : fdlist){
									 if (famdet.getNameOfMember().equals(doRef.getColumnA().trim())){
										 famdet.setAfppfNomination(false);
										 fdRepo.save(famdet);
										  
									 }
								 }
							 }
						 }
					}
					if (doRef.getDescription().equals("MARRY") || doRef.getDescription().equals("REMARY")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndRelationshipAndNameOfMemberAndRecordStatus(do2.getEmployee().getId(),"WIFE",doRef.getColumnA(),Do2Manager.VALID);
						 
			 			if (fdlist.size() > 0){
			 			 	fdlist.get(0).setRecordStatus("V");
			 			 	fdRepo.save(fdlist.get(0));
			 			 	 
			 			}
					}
					if (doRef.getDescription().equals("ELOP") || doRef.getDescription().equals("DIVORC")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndRelationshipAndRecordStatus(do2.getEmployee().getId(),"WIFE", Do2Manager.VALID); 
						 
			 			if (fdlist.size() > 0){
			 				fdlist.get(0).setStatusOfMember(null);
			 				fdlist.get(0).setToDate(null);	
			 				fdRepo.save(fdlist.get(0));
			 				 
			 			}
					}
					if (doRef.getDescription().equals("CLDDIS") || doRef.getDescription().equals("DEMISE")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndRelationshipAndNameOfMemberAndRecordStatus(do2.getEmployee().getId(),doRef.getColumnB(),doRef.getColumnA(),Do2Manager.VALID);
						 
			 			if (fdlist.size() > 0){
			 				fdlist.get(0).setStatusOfMember(null);
			 				fdlist.get(0).setToDate(null);
			 				fdRepo.save(fdlist.get(0));
			 			 	 
			 			}
					}
					if (doRef.getDescription().equals("CHGD")){
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());						
						List<FamilyDetail> fdlist =fdRepo.findByEmployeeIdAndNameOfMemberAndRecordStatus(do2.getEmployee().getId(),doRef.getColumnC(),Do2Manager.VALID);
					 
			 			if (fdlist.size() > 0){
			 			 	fdlist.get(0).setRecordStatus("C");
			 			 	fdRepo.save(fdlist.get(0));
			 			}
					}
					if (doRef.getDescription().equals("ITROPT")) {
						Itropt itropt = new Itropt();						
						itropt.setF16Repo(f16Repo);
						itropt.processCancel(do2, doRef);
					}
			if(doRef.getDescription().equals("JCNO") || doRef.getDescription().equals("TJNO")){
						
						
						Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
						
						System.out.println (  " EMPLOYEE " +emp.getArmyNo()  +" do2 army no " +do2.getArmyNo());
						
						String qe = Utils.getCurrentQuarter();
												
						
						
						 
						String jcno = doRef.getColumnA().substring(0,8);
						String jcCheckDigit = doRef.getColumnA().substring(8);
						String oldarmyNo = doRef.getArmyNo();
						String oldCheckDigit = doRef.getCheckDigit();
						
						System.out.println ( " jcno " +jcno);
						System.out.println ( " jccd " +jcCheckDigit);;
						System.out.println ( " oldano " +oldarmyNo);
						System.out.println ( " oldanocd " +oldCheckDigit);
						
					
						List<String> tableList;	
						
						String baseSql = "SELECT id from  ";
						String baseSql1 = " where army_no = ";
				
						//requires amendment
						tableList = do2Repo.getTableNameHavingColumnWithArmyNo();
						
						
			/*			Utils.getEntityManager().createNativeQuery(
						"select table_name from information_schema.columns where column_name = 'army_no'").getResultList();*/
						
						tableList.remove("audit_cage");
						tableList.remove("audit_cage_test");
						tableList.remove("do2_test");
						tableList.remove("employee_test");
						tableList.remove("fsmo");
						tableList.remove("loan_recovery");
						tableList.remove("loan_sanction");
						tableList.remove("lpc_deputation");
						tableList.remove("paybook");
						tableList.remove("pli_adjustment");
						tableList.remove("transfer_in_out");
						tableList.remove("udfamo");
						tableList.remove("dak");
												
						int tableListSize = tableList.size();
						List<String> reasonList = Arrays.asList("do2", "enroll_do2", "fund_adjustment", "fund_subscription_deputation", 
								"lpc"); 
							
						String updatesqloldano = "Update employee set army_no="+"'"+oldarmyNo+"'";
						String updatesqloldcd = "Update employee set check_digit="+"'"+oldCheckDigit+"'";
						String updatesql = " where army_no="+"'"+jcno+"'";
						
						String updatesql1 = "Update employee set army_no=old_army_no, check_digit=old_army_no_check_digit where army_no="+"'" +jcno +"'";
										
						String totupdateanosql = updatesqloldano+updatesql;
						System.out.println("totupdateanosql"  +totupdateanosql  +" CCCC " +updatesql1);
					
						jdbcTemplate.execute(updatesql1);
						
					
						Employee employee = Utils.getCurrentEmployee(doRef.getArmyNo());		
						System.out.println ( " emp " +employee.getArmyNo()   +" do2ref " +doRef.getArmyNo());
						empRepo.save(employee);
						
						/*  
						String newSql = baseSql;
					    for (int i = 0; i <= tableListSize - 1; i++) {
					    	
					    	
					    	newSql = baseSql+tableList.get(i) + baseSql1 + "'" +jcno +"'";
					//       	List <BigInteger> tableIds = Utils.getEntityManager().createNativeQuery(newSql).getResultList();
					    	List<Map<String, Object>> tableIds  = jdbc.queryForList(newSql);
					    		
					    			
					    	String tableName=tableList.get(i);
					    	System.out.println ( " table name " +tableName   + " JCNO " +jcno);
					    			    
					 //   	List<BigInteger> tableIds = do2Repo.getTableRecordIds(tableName,jcno);
						       	if (tableIds.size() > 0 ) {
						    		for (int j = 0; j <= tableIds.size() - 1; j++) {
						    			//BigInteger updateid = tableIds.get(j);
						    			BigInteger updateid = (BigInteger) tableIds.get(j);
						    			String comsql1 = " UPDATE ";
						    			String anosql2 = " set army_no = ";
						    			String comsql2 = " where id = ";
						    			String cdsql1 = " set check_digit = ";
						    			String reasonsql1 = " set reason = ";
						    			String rejreasonsql1 = " set rejection_reason = ";
						    			String reason = "ARMY NO CHANGED DUE TO JCNO CANCELLATION ";
						    			String updateReasonSql="";
						    			
						    			if (reasonList.contains(tableList.get(i))) {
						    			 updateReasonSql = comsql1 + tableList.get(i) + reasonsql1 + "'"+reason+"'" + comsql2 +updateid;
						    			} else {
						    			 updateReasonSql = comsql1+tableList.get(i) + rejreasonsql1 + "'"+reason+"'" +  comsql2 +updateid;
						    			}
						    			String updatecdSql = comsql1 + tableList.get(i)+cdsql1+"'"+oldCheckDigit+"'"+comsql2 +updateid;
						    			String updateanoSql = comsql1 + tableList.get(i)+anosql2+"'"+oldarmyNo+"'"+comsql2 +updateid;
						    			
						    			if (!tableList.get(i).equals("employee")){
						    				
						    				jdbc.execute(updateReasonSql);  
						    				
						    		//	Utils.getEntityManager().createNativeQuery(updateReasonSql).executeUpdate();
						    			}
						    			jdbc.execute(updatecdSql);  
						    			jdbc.execute(updateanoSql);  
						    		//	Utils.getEntityManager().createNativeQuery(updatecdSql).executeUpdate();
						    		//	Utils.getEntityManager().createNativeQuery(updateanoSql).executeUpdate();
						    		}
					    	  	}
					    }	*/
					}

				}
				//refDo2 = doRef;
				insertArrears(do2, doRef);

				doRef.setStatus(Do2Manager.DO2_CANCEL_STATUS);
				do2Repo.save(doRef);
			 

			}
		}
		
			}
	synchronized static public void insertArrears(Do2 do2,Do2 refDo2){
		
		List<Arrears> arrearsList = arrRepo.findByDo2Id(refDo2.getId()); 
		Long empid = do2.getEmployee().getId();
		int neencarrs=0;
	
		String description = "";
		if (arrearsList.size() > 0) {
			for (Arrears arr : arrearsList) {			
				description = refDo2.getDescription();
				if (description.equals(Do2Manager.ENROLL) || description.equals("RECALL")){
					description = Do2Manager.DAILY;
				} else if (description.equals("FINE") || description.equals("PEDEDN")) {
					description = Do2Manager.BASIC_PAY;
				}  else if (description.equals("RL") || description.equals("PRPLVE")  || description.equals("RMAHCO")) {
					description = Do2Manager.NRA;
				} else if (description.equals("DRDSRA") || description.equals("GSRA")) {
					description = Do2Manager.SRA;
				}	
				 else if (description.equals("CEATWN") || description.equals("CEAHC") || description.equals("CEAHCT")) {
					description = "CEA";
				} else if (description.equals("HSBTWN")){
					description = "HOSSUB";
				} else if (description.equals("DMALLC")){
					description = "DIPMNY";
				} else if (description.equals("DIVALL")){
					description = "DIVING";
				} else if (description.equals("FDAILY") || description.equals("RDAILY")){
					description = Do2Manager.DAILY;
				} else if (description.equals("ENCASH") && !refDo2.getColumnA().equals("LTC")){
					description = "FSCASH";
				} else if (description.equals("ENCFIN")){					 
					
					List<Do2>neEncDo2List = do2Repo.getNeEncDo2(empid);
					if (neEncDo2List !=null && neEncDo2List.size()==1) {
						Long neEncDo2Id = neEncDo2List.get(0).getId();
						List<Arrears> neencarrearsList = arrRepo.findByDo2Id(neEncDo2Id); 
						if (neencarrearsList.size() > 0) {
							for (Arrears neencarr : neencarrearsList) {	
								neencarrs+=neencarr.getAmount();
							}
						}
					}	
					description = "FSCASH";
			/*		System.out.println ( " arr amt " +arr.getAmount());
					System.out.println ( "neenc arr amt " +neencarrs);
					System.out.println ( "tot arr neenc arr amt " +arr.getAmount()+neencarrs     +"   " +(arr.getAmount()+neencarrs) * -1 );
					System.out.println ( "tot arr neenc arr amt " +arr.getAmount()+neencarrs * -1     +"   " +(arr.getAmount()+neencarrs) * -1 );
					Utils.addArrearsForCancelDo2(do2, refDo2, ((arr.getAmount()+neencarrs) * -1), description, arr.getFromDate(), arr.getToDate());*/
					
				}
				if (description.equals("ENCFIN")) {
					description = "FSCASH";
					Utils.addArrearsForCancelDo2(do2, refDo2, ((arr.getAmount()+neencarrs) * -1), description, arr.getFromDate(), arr.getToDate());
					
				} else {
					Utils.addArrearsForCancelDo2(do2, refDo2, (arr.getAmount() * -1), description, arr.getFromDate(), arr.getToDate());
				}
					
				
			}
		}	
		
	}
	synchronized static public void processPending(Do2 do2) {
		
		
		List<Arrears> arrearsList = arrRepo.findByDo2Id(do2.getId()); 
		 
		String payCode = "";
		if (arrearsList.size() > 0) {
			for (Arrears arr : arrearsList) {
				if (Do2Manager.getPayCodeMap().get(do2.getDescription()) != null) {
					payCode = Do2Manager.getPayCodeMap().get(
							do2.getDescription());
				} else {
					payCode = do2.getDescription();
				}

				Utils.addArrears(do2, (arr.getAmount() * -1), payCode, arr
						.getFromDate(), arr.getToDate());
			}
		}
		
		
		
	}
}
