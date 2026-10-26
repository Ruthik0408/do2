package itsdc.dolphin.do2.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XmlGenerator {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Environment env;

	private static final String EMPLOYEE_DO2_SQL =

			"select " + "e.id employee_id," + "e.army_no," + "e.name employee_name," + "r.rank_name," + "d.description,"
					+ "d.from_date," + "d.column_a," + "d.column_b " + "from employee e " +

					"left join rank r " + "on r.id=e.fk_substantive_rank " +

					"left join do2 d " + "on d.fk_employee=e.id " + "and d.status='V' "
					+ "and d.description IN ('GSPAY','UPCLAS') " +

					"where e.fk_status_code=1 " + "and e.current_record=true " +

					"order by e.army_no,d.description,d.from_date";

	private static final String FAMILY_SQL =

			"select " + "fk_employee," + "relationship," + "name_of_member," + "date_of_birth " + "from family_detail "
					+ "where record_status='V' " + "AND to_date is null "
					+ "and relationship in ('SPOUSE','SON','DAUGHTER') "
					+ "order by fk_employee,relationship,date_of_birth";

	public String generateXml() throws Exception {

		Map<Long, EmployeeXml> employeeMap = new LinkedHashMap<>();

		try (Connection conn = dataSource.getConnection()) {

			loadEmployeeAndDo2(conn, employeeMap);

			loadFamily(conn, employeeMap);

		}

		return writeXml(employeeMap);

	}

	private void loadEmployeeAndDo2(Connection conn, Map<Long, EmployeeXml> employeeMap) throws Exception {

		try (PreparedStatement ps = conn.prepareStatement(EMPLOYEE_DO2_SQL); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				Long employeeId = rs.getLong("employee_id");

				EmployeeXml employee = employeeMap.get(employeeId);

				if (employee == null) {

					employee = new EmployeeXml();

					employee.setEmployeeId(employeeId);
					employee.setArmyNo(rs.getString("army_no"));
					employee.setRank(rs.getString("rank_name"));
					employee.setName(rs.getString("employee_name"));

					employeeMap.put(employeeId, employee);
				}

				String description = rs.getString("description");

				if (description == null) {
					continue;
				}

				Do2Detail detail = new Do2Detail();

				java.sql.Date sqlDate = rs.getDate("from_date");

				if (sqlDate != null) {
					detail.setFromDate(sqlDate.toLocalDate());
				}

				detail.setColumnA(rs.getString("column_a"));
				detail.setColumnB(rs.getString("column_b"));

				if ("GSPAY".equalsIgnoreCase(description)) {

					employee.getGsPayList().add(detail);

				} else if ("UPCLAS".equalsIgnoreCase(description)) {

					employee.getUpClasList().add(detail);

				}

			}

		}

	}

	private void loadFamily(Connection conn, Map<Long, EmployeeXml> employeeMap) throws Exception {

		try (PreparedStatement ps = conn.prepareStatement(FAMILY_SQL); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				Long employeeId = rs.getLong("fk_employee");

				EmployeeXml employee = employeeMap.get(employeeId);

				if (employee == null) {
					continue;
				}

				FamilyMember familyMember = new FamilyMember();

				familyMember.setRelationship(rs.getString("relationship"));
				familyMember.setName(rs.getString("name_of_member"));

				java.sql.Date sqlDate = rs.getDate("date_of_birth");

				if (sqlDate != null) {
					familyMember.setDob(sqlDate.toLocalDate());
				}

				String relationship = familyMember.getRelationship();

				if ("SPOUSE".equalsIgnoreCase(relationship)) {

					employee.setWife(familyMember);

				} else if ("SON".equalsIgnoreCase(relationship) || "DAUGHTER".equalsIgnoreCase(relationship)) {

					employee.getChildren().add(familyMember);
				}

			}

		}

	}

	private String writeXml(Map<Long, EmployeeXml> employeeMap) throws Exception {

		String reportDir = env.getProperty("spring.application.filedir") + "/reports";

		File dir = new File(reportDir);

		if (!dir.exists()) {
			dir.mkdirs();
		}

		String do2FileName = "Do2XmlReport_" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) + ".xml";

		File xmlFile = new File(dir, do2FileName);
 

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();

		Document doc = builder.newDocument();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

		Element root = doc.createElement("Employees");

		doc.appendChild(root);

		for (EmployeeXml emp : employeeMap.values()) {

			Element employee = doc.createElement("Employee");
			root.appendChild(employee);

			addElement(doc, employee, "ArmyNo", emp.getArmyNo());
			addElement(doc, employee, "Rank", emp.getRank());
			addElement(doc, employee, "Name", emp.getName());

			Element gsPays = doc.createElement("GSPAYS");

			employee.appendChild(gsPays);

			for (Do2Detail d : emp.getGsPayList()) {

				Element gsPay = doc.createElement("GSPAY");

				gsPays.appendChild(gsPay);

				addElement(doc, gsPay, "FromDate", d.getFromDate() == null ? "" : d.getFromDate().format(formatter));

				addElement(doc, gsPay, "ColumnA", d.getColumnA());

			}

			Element upClasses = doc.createElement("UPCLASSES");

			employee.appendChild(upClasses);

			for (Do2Detail d : emp.getUpClasList()) {

				Element up = doc.createElement("UPCLAS");

				upClasses.appendChild(up);

				addElement(doc, up, "FromDate", d.getFromDate() == null ? "" : d.getFromDate().format(formatter));

				addElement(doc, up, "ColumnA", d.getColumnA());

				addElement(doc, up, "Contents", d.getColumnB());

			}

			if (emp.getWife() != null) {

				Element wife = doc.createElement("Wife");

				employee.appendChild(wife);

				addElement(doc, wife, "Name", emp.getWife().getName());

				addElement(doc, wife, "DateOfBirth",
						emp.getWife().getDob() == null ? "" : emp.getWife().getDob().format(formatter));

			}

			Element children = doc.createElement("Children");

			employee.appendChild(children);

			for (FamilyMember child : emp.getChildren()) {

				Element childElement = doc.createElement("Child");

				children.appendChild(childElement);

				addElement(doc, childElement, "Relationship", child.getRelationship());

				addElement(doc, childElement, "Name", child.getName());

				addElement(doc, childElement, "DateOfBirth",
						child.getDob() == null ? "" : child.getDob().format(formatter));

			}

		}

		TransformerFactory tf = TransformerFactory.newInstance();

		Transformer transformer = tf.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc), new StreamResult(xmlFile));

		System.out.println("DO2 XML generated : " + xmlFile.getAbsolutePath());

		return xmlFile.getAbsolutePath();

	}

	private void addElement(Document doc, Element parent, String tagName, String value) {

		Element element = doc.createElement(tagName);

		element.setTextContent(value == null ? "" : value);

		parent.appendChild(element);

	}

}
